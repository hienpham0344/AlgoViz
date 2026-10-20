package com.aisc.algoviz.submission.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(1)
public class DockerJavaSandboxStrategy implements SandboxStrategy {

    private final DockerSandboxProperties properties;
    private final ProcessExecutor processExecutor;
    private volatile Boolean dockerAvailableCached = null;

    @org.springframework.beans.factory.annotation.Autowired
    public DockerJavaSandboxStrategy(DockerSandboxProperties properties) {
        this(properties, ProcessBuilder::start);
    }

    public DockerJavaSandboxStrategy(DockerSandboxProperties properties, ProcessExecutor processExecutor) {
        this.properties = properties;
        this.processExecutor = processExecutor;
    }

    @Override
    public String getStrategyName() {
        return "DOCKER";
    }

    @Override
    public boolean supportsLanguage(String language) {
        return language != null && "java".equalsIgnoreCase(language.trim());
    }

    @Override
    public boolean isAvailable() {
        if (!properties.isEnabled()) {
            return false;
        }
        return checkDockerAvailable();
    }

    public void setDockerAvailableCached(Boolean cached) {
        this.dockerAvailableCached = cached;
    }

    public List<String> buildDockerRunCommand(String containerName, Path workDir, String sourceFileName) {
        List<String> cmd = new ArrayList<>();
        cmd.add(properties.getDockerCommand());
        cmd.add("run");
        cmd.add("--rm");
        cmd.add("-i");
        cmd.add("--name");
        cmd.add(containerName);
        cmd.add("--network");
        cmd.add("none");
        cmd.add("--memory");
        cmd.add(properties.getMemory());
        cmd.add("--cpus");
        cmd.add(properties.getCpus());
        cmd.add("--pids-limit");
        cmd.add(String.valueOf(properties.getPidsLimit()));
        cmd.add("-v");
        cmd.add(workDir.toAbsolutePath() + ":/sandbox");
        cmd.add("-w");
        cmd.add("/sandbox");
        cmd.add(properties.getImage());
        cmd.add("java");
        cmd.add(sourceFileName);
        return cmd;
    }

    @Override
    public SandboxExecutionResult execute(SandboxExecutionRequest request) throws IOException, InterruptedException {
        String containerName = "algoviz-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Path workDir = request.getWorkDirectory();
        Path stdin = workDir.resolve("stdin.txt");
        Path stdout = workDir.resolve("stdout.txt");
        Path stderr = workDir.resolve("stderr.txt");

        Files.writeString(stdin, request.getInput() != null ? request.getInput() : "");

        List<String> command = buildDockerRunCommand(containerName, workDir, request.getSourceFileName());
        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectInput(stdin.toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile());

        log.info("Starting Docker container {} with image {}", containerName, properties.getImage());
        long startTime = System.nanoTime();
        Process process = processExecutor.start(builder);

        boolean finished;
        try {
            finished = process.waitFor(request.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            killContainer(containerName);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }

        double elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0;

        if (!finished) {
            log.warn("Docker container {} timed out after {}s. Forcibly killing...", containerName, request.getTimeoutSeconds());
            killContainer(containerName);
            process.destroyForcibly();
            return SandboxExecutionResult.builder()
                    .timedOut(true)
                    .exitCode(1)
                    .output("")
                    .error("Time Limit Exceeded")
                    .executionTimeMs(elapsedMs)
                    .memoryUsageMb(0.0)
                    .build();
        }

        int exitCode = process.exitValue();
        String outContent = Files.exists(stdout) ? Files.readString(stdout) : "";
        String errContent = Files.exists(stderr) ? Files.readString(stderr) : "";

        return SandboxExecutionResult.builder()
                .timedOut(false)
                .exitCode(exitCode)
                .output(outContent)
                .error(errContent)
                .executionTimeMs(elapsedMs)
                .memoryUsageMb(0.0)
                .build();
    }

    void killContainer(String containerName) {
        try {
            List<String> killCmd = List.of(properties.getDockerCommand(), "kill", containerName);
            Process killProc = processExecutor.start(new ProcessBuilder(killCmd));
            killProc.waitFor(properties.getTimeoutKillSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Could not kill container {}: {}", containerName, e.getMessage());
        }
    }

    private boolean checkDockerAvailable() {
        if (dockerAvailableCached != null) {
            return dockerAvailableCached;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(properties.getDockerCommand(), "--version");
            Process p = processExecutor.start(pb);
            boolean finished = p.waitFor(2, TimeUnit.SECONDS);
            boolean ok = finished && p.exitValue() == 0;
            dockerAvailableCached = ok;
            if (!ok) {
                log.warn("Docker command '{}' returned non-zero or timed out. Falling back to native sandbox.", properties.getDockerCommand());
            }
            return ok;
        } catch (Exception e) {
            log.warn("Docker is not available on host system: {}. Falling back to native sandbox.", e.getMessage());
            dockerAvailableCached = false;
            return false;
        }
    }
}
