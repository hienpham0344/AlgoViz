package com.aisc.algoviz.submission.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class NativeSandboxStrategyTest {

    private final NativeSandboxStrategy strategy = new NativeSandboxStrategy();

    @Test
    void strategyMetadata() {
        assertEquals("NATIVE", strategy.getStrategyName());
        assertTrue(strategy.isAvailable());
        assertTrue(strategy.supportsLanguage("java"));
        assertTrue(strategy.supportsLanguage("python"));
        assertTrue(strategy.supportsLanguage("anything"));
    }

    @Test
    void executesJavaCodeSuccessfully(@TempDir Path tempDir) throws Exception {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Native Sandbox Output");
                    }
                }
                """;
        Files.writeString(tempDir.resolve("Main.java"), code);

        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
                .language("java")
                .workDirectory(tempDir)
                .sourceFileName("Main.java")
                .input("")
                .timeoutSeconds(5)
                .build();

        SandboxExecutionResult result = strategy.execute(request);

        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertEquals("Native Sandbox Output", result.getOutput().trim());
        assertTrue(result.getExecutionTimeMs() > 0);
    }

    @Test
    void pipesInputToStandardIn(@TempDir Path tempDir) throws Exception {
        String code = """
                import java.util.Scanner;
                public class EchoInput {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        if (sc.hasNextLine()) {
                            System.out.println("Read: " + sc.nextLine());
                        }
                    }
                }
                """;
        Files.writeString(tempDir.resolve("EchoInput.java"), code);

        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
                .language("java")
                .workDirectory(tempDir)
                .sourceFileName("EchoInput.java")
                .input("HelloAlgoViz")
                .timeoutSeconds(5)
                .build();

        SandboxExecutionResult result = strategy.execute(request);

        assertFalse(result.isTimedOut());
        assertEquals(0, result.getExitCode());
        assertEquals("Read: HelloAlgoViz", result.getOutput().trim());
    }

    @Test
    void handlesTimeoutProperly(@TempDir Path tempDir) throws Exception {
        String code = """
                public class InfiniteLoop {
                    public static void main(String[] args) {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException ignored) {}
                    }
                }
                """;
        Files.writeString(tempDir.resolve("InfiniteLoop.java"), code);

        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
                .language("java")
                .workDirectory(tempDir)
                .sourceFileName("InfiniteLoop.java")
                .input("")
                .timeoutSeconds(1)
                .build();

        SandboxExecutionResult result = strategy.execute(request);

        assertTrue(result.isTimedOut());
        assertEquals(1, result.getExitCode());
        assertEquals("Time Limit Exceeded", result.getError());
    }
}
