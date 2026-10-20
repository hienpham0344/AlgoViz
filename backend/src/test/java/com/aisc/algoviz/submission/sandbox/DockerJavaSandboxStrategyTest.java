package com.aisc.algoviz.submission.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DockerJavaSandboxStrategyTest {

    private DockerSandboxProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DockerSandboxProperties();
        properties.setEnabled(true);
        properties.setDockerCommand("docker");
        properties.setImage("eclipse-temurin:21-alpine");
        properties.setMemory("256m");
        properties.setCpus("1.0");
        properties.setPidsLimit(64);
        properties.setTimeoutKillSeconds(2);
    }

    @Test
    void strategyMetadata() {
        DockerJavaSandboxStrategy strategy = new DockerJavaSandboxStrategy(properties);
        assertEquals("DOCKER", strategy.getStrategyName());
        assertTrue(strategy.supportsLanguage("java"));
        assertTrue(strategy.supportsLanguage("JAVA"));
        assertFalse(strategy.supportsLanguage("python"));
        assertFalse(strategy.supportsLanguage("cpp"));
        assertFalse(strategy.supportsLanguage(null));
    }

    @Test
    void isAvailableReturnsFalseWhenDisabled() {
        properties.setEnabled(false);
        DockerJavaSandboxStrategy strategy = new DockerJavaSandboxStrategy(properties);
        assertFalse(strategy.isAvailable());
    }

    @Test
    void isAvailableUsesCachedStatusWhenSet() {
        DockerJavaSandboxStrategy strategy = new DockerJavaSandboxStrategy(properties);
        strategy.setDockerAvailableCached(true);
        assertTrue(strategy.isAvailable());

        strategy.setDockerAvailableCached(false);
        assertFalse(strategy.isAvailable());
    }

    @Test
    void buildDockerRunCommandContainsSecurityAndResourceFlags(@TempDir Path tempDir) {
        DockerJavaSandboxStrategy strategy = new DockerJavaSandboxStrategy(properties);
        List<String> cmd = strategy.buildDockerRunCommand("algoviz-test-container", tempDir, "Solution.java");

        assertTrue(cmd.contains("docker"));
        assertTrue(cmd.contains("run"));
        assertTrue(cmd.contains("--rm"));
        assertTrue(cmd.contains("-i"));
        assertTrue(cmd.contains("--name"));
        assertTrue(cmd.contains("algoviz-test-container"));
        assertTrue(cmd.contains("--network"));
        assertTrue(cmd.contains("none"));
        assertTrue(cmd.contains("--memory"));
        assertTrue(cmd.contains("256m"));
        assertTrue(cmd.contains("--cpus"));
        assertTrue(cmd.contains("1.0"));
        assertTrue(cmd.contains("--pids-limit"));
        assertTrue(cmd.contains("64"));
        assertTrue(cmd.contains("-v"));
        assertTrue(cmd.contains(tempDir.toAbsolutePath() + ":/sandbox"));
        assertTrue(cmd.contains("-w"));
        assertTrue(cmd.contains("/sandbox"));
        assertTrue(cmd.contains("eclipse-temurin:21-alpine"));
        assertTrue(cmd.contains("java"));
        assertTrue(cmd.contains("Solution.java"));
    }

    @Test
    void executeSuccessfulRun(@TempDir Path tempDir) throws Exception {
        List<List<String>> executedCommands = new ArrayList<>();

        ProcessExecutor mockExecutor = builder -> {
            executedCommands.add(builder.command());
            return new MockProcess(0, true, 0);
        };

        DockerJavaSandboxStrategy strategy = new DockerJavaSandboxStrategy(properties, mockExecutor);
        strategy.setDockerAvailableCached(true);

        Path stdout = tempDir.resolve("stdout.txt");
        Files.writeString(stdout, "Hello from Docker");

        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
                .language("java")
                .workDirectory(tempDir)
                .sourceFileName("Main.java")
                .input("test-input")
                .timeoutSeconds(5)
                .build();

        SandboxExecutionResult result = strategy.execute(request);

        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertEquals("Hello from Docker", result.getOutput());
        assertFalse(executedCommands.isEmpty());
        assertTrue(executedCommands.get(0).contains("run"));
    }

    @Test
    void executeTimeoutTriggersKillContainer(@TempDir Path tempDir) throws Exception {
        List<List<String>> executedCommands = new ArrayList<>();

        ProcessExecutor mockExecutor = builder -> {
            executedCommands.add(builder.command());
            if (builder.command().contains("kill")) {
                return new MockProcess(0, true, 0);
            }
            // Simulate run command timing out
            return new MockProcess(1, false, 0);
        };

        DockerJavaSandboxStrategy strategy = new DockerJavaSandboxStrategy(properties, mockExecutor);
        strategy.setDockerAvailableCached(true);

        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
                .language("java")
                .workDirectory(tempDir)
                .sourceFileName("Main.java")
                .input("")
                .timeoutSeconds(1)
                .build();

        SandboxExecutionResult result = strategy.execute(request);

        assertTrue(result.isTimedOut());
        assertEquals(1, result.getExitCode());
        assertEquals("Time Limit Exceeded", result.getError());

        // Verify docker kill was invoked
        boolean killCalled = executedCommands.stream().anyMatch(cmd -> cmd.contains("kill"));
        assertTrue(killCalled, "Expected docker kill to be invoked upon timeout");
    }

    private static class MockProcess extends Process {
        private final int exitCode;
        private final boolean finishes;
        private final long sleepMs;

        MockProcess(int exitCode, boolean finishes, long sleepMs) {
            this.exitCode = exitCode;
            this.finishes = finishes;
            this.sleepMs = sleepMs;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return finishes;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {}

        @Override
        public Process destroyForcibly() {
            return this;
        }
    }
}
