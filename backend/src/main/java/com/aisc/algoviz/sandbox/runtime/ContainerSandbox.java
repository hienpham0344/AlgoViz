package com.aisc.algoviz.sandbox.runtime;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Docker is the mandatory execution boundary. There is deliberately no host fallback. */
public final class ContainerSandbox {
    private static final Semaphore SLOTS = new Semaphore(2);
    private final String docker;
    private final String image;
    private final int outputLimit;

    public ContainerSandbox(String docker, String image, int outputLimit) {
        this.docker = docker;
        this.image = image;
        this.outputLimit = outputLimit;
    }

    public Result run(Map<String, String> files, List<String> command, Duration timeout, int memoryMb)
            throws IOException, InterruptedException {
        if (timeout.isNegative() || timeout.isZero() || timeout.toMillis() > 45_000
                || memoryMb < 128 || memoryMb > 512) throw new IllegalArgumentException("Invalid sandbox limits");
        for (var file : files.entrySet()) {
            if (!file.getKey().matches("[A-Za-z0-9_.-]+") || file.getKey().equals("..")
                    || file.getValue().getBytes(StandardCharsets.UTF_8).length > 1_048_576)
                throw new IllegalArgumentException("Invalid sandbox input");
        }
        if (!SLOTS.tryAcquire()) throw new IOException("Sandbox is busy. Try again shortly.");
        Path input = null;
        String name = "algoviz-job-" + UUID.randomUUID();
        long started = System.nanoTime();
        try {
            input = Files.createTempDirectory("algoviz-input-").toAbsolutePath();
            if (input.toString().contains(",")) throw new IOException("Sandbox temp path cannot contain commas");
            permissions(input, "rwxr-xr-x");
            for (var entry : files.entrySet()) {
                Path file = input.resolve(entry.getKey());
                Files.writeString(file, entry.getValue(), StandardCharsets.UTF_8);
                permissions(file, "rw-r--r--");
            }
            List<String> create = createCommand(name, input, command, memoryMb);
            Result created = invoke(create, Duration.ofSeconds(10), 16_384);
            if (created.timedOut || created.exitCode != 0)
                throw new IOException("Sandbox unavailable. Start Docker and build image " + image + ". No code was run on the host.");
            Result result = invoke(List.of(docker, "start", "--attach", name), timeout, outputLimit);
            int exitCode = result.exitCode;
            if (!result.timedOut && !result.outputExceeded) {
                Result state = invoke(List.of(docker, "inspect", "--format", "{{.State.Status}} {{.State.ExitCode}}", name),
                        Duration.ofSeconds(3), 16_384);
                String[] fields = state.stdout.trim().split("\\s+");
                if (state.exitCode != 0 || fields.length != 2 || !fields[0].equals("exited"))
                    throw new IOException("Sandbox did not complete normally");
                try { exitCode = Integer.parseInt(fields[1]); }
                catch (NumberFormatException ex) { throw new IOException("Invalid sandbox exit status", ex); }
            }
            return new Result(exitCode, result.stdout, result.stderr, result.timedOut,
                    result.outputExceeded, (System.nanoTime() - started) / 1_000_000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw ex;
        } finally {
            // Killing only the docker CLI would leave the container alive. Remove the named job too.
            boolean interrupted = Thread.interrupted();
            try {
                invoke(List.of(docker, "rm", "--force", name), Duration.ofSeconds(5), 16_384);
            } catch (IOException ignored) {
                // The daemon may be unavailable or the create request may not have completed.
            } finally {
                if (input != null) {
                    try (var paths = Files.walk(input)) {
                        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                    } catch (IOException ignored) { }
                }
                SLOTS.release();
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    public List<String> createCommand(String name, Path input, List<String> command, int memoryMb) {
        List<String> args = new ArrayList<>(List.of(docker, "create", "--pull=never", "--name", name,
                "--label", "algoviz.sandbox=true", "--network=none", "--read-only",
                "--user=65534:65534", "--cap-drop=ALL", "--security-opt=no-new-privileges=true",
                "--pids-limit=64", "--cpus=1", "--memory=" + memoryMb + "m",
                "--memory-swap=" + memoryMb + "m", "--ulimit", "nofile=128:128",
                "--ulimit", "fsize=8388608:8388608", "--ulimit", "core=0:0",
                "--shm-size=8m", "--log-driver=none", "--init",
                "--tmpfs", "/work:rw,exec,nosuid,nodev,size=64m,mode=1777",
                "--tmpfs", "/tmp:rw,noexec,nosuid,nodev,size=32m,mode=1777",
                "--mount", "type=bind,source=" + input + ",target=/input,readonly",
                "--workdir=/work", image));
        args.addAll(command);
        return args;
    }

    private static Result invoke(List<String> command, Duration timeout, int limit)
            throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close();
        ExecutorService readers = Executors.newFixedThreadPool(2);
        AtomicBoolean exceeded = new AtomicBoolean();
        Future<String> out = readers.submit(() -> read(process.getInputStream(), limit, exceeded, process));
        Future<String> err = readers.submit(() -> read(process.getErrorStream(), limit, exceeded, process));
        boolean finished = false;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) process.destroyForcibly();
            process.waitFor(1, TimeUnit.SECONDS);
            String stdout = future(out);
            String stderr = future(err);
            int exit = finished ? process.exitValue() : -1;
            return new Result(exit, stdout, stderr, !finished, exceeded.get(), 0);
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            readers.shutdownNow();
        }
    }

    private static String read(InputStream stream, int limit, AtomicBoolean exceeded, Process process) throws IOException {
        try (stream; ByteArrayOutputStream result = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                int remaining = limit - result.size();
                result.write(buffer, 0, Math.min(count, Math.max(0, remaining)));
                if (count > remaining) {
                    exceeded.set(true);
                    process.destroyForcibly();
                    break;
                }
            }
            return result.toString(StandardCharsets.UTF_8);
        }
    }

    private static String future(Future<String> future) throws IOException, InterruptedException {
        try { return future.get(2, TimeUnit.SECONDS); }
        catch (ExecutionException | TimeoutException e) { throw new IOException("Unable to collect sandbox output", e); }
    }

    private static void permissions(Path path, String mode) throws IOException {
        if (path.getFileSystem().supportedFileAttributeViews().contains("posix"))
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(mode));
    }

    public record Result(int exitCode, String stdout, String stderr, boolean timedOut,
                         boolean outputExceeded, long elapsedMs) { }
}
