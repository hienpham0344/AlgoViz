package com.aisc.algoviz.submission.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(100)
public class NativeSandboxStrategy implements SandboxStrategy {

    private final ProcessExecutor processExecutor;

    public NativeSandboxStrategy() {
        this(ProcessBuilder::start);
    }

    public NativeSandboxStrategy(ProcessExecutor processExecutor) {
        this.processExecutor = processExecutor;
    }

    @Override
    public String getStrategyName() {
        return "NATIVE";
    }

    @Override
    public boolean supportsLanguage(String language) {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public SandboxExecutionResult execute(SandboxExecutionRequest request) throws IOException, InterruptedException {
        Path workDir = request.getWorkDirectory();
        Path stdin = workDir.resolve("stdin.txt");
        Path stdout = workDir.resolve("stdout.txt");
        Path stderr = workDir.resolve("stderr.txt");

        Files.writeString(stdin, request.getInput() != null ? request.getInput() : "");

        String[] command = request.getCommand();
        if (command == null || command.length == 0) {
            if ("java".equalsIgnoreCase(request.getLanguage())) {
                command = new String[]{"java", request.getSourceFileName()};
            } else {
                throw new IllegalArgumentException("No execution command specified for language: " + request.getLanguage());
            }
        }

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(workDir.toFile())
                .redirectInput(stdin.toFile())
                .redirectOutput(stdout.toFile())
                .redirectError(stderr.toFile());

        long startTime = System.nanoTime();
        Process process = processExecutor.start(builder);

        boolean finished;
        try {
            finished = process.waitFor(request.getTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }

        double elapsedMs = (System.nanoTime() - startTime) / 1_000_000.0;

        if (!finished) {
            log.warn("Process timed out after {}s. Forcibly terminating process tree...", request.getTimeoutSeconds());
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
            try {
                process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {}

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
}
