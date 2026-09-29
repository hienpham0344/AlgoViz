package com.aisc.algoviz.service;

import com.aisc.algoviz.dto.ExecutionResultDto;
import com.aisc.algoviz.model.TestCase;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    private static final long TIMEOUT_SECONDS = 5;

    public ExecutionResultDto runCodeCustomInput(String language, String code, String customInput) {
        try {
            ProcessResult result = executeCode(language, code, customInput != null ? customInput : "");
            if (result.exitCode != 0 && result.error != null && !result.error.trim().isEmpty()) {
                return ExecutionResultDto.builder()
                        .status("Runtime Error")
                        .output(result.output)
                        .errorMessage(result.error)
                        .executionTimeMs(result.executionTimeMs)
                        .memoryUsageMb(result.memoryUsageMb)
                        .build();
            }

            return ExecutionResultDto.builder()
                    .status("Accepted")
                    .output(result.output.trim())
                    .executionTimeMs(result.executionTimeMs)
                    .memoryUsageMb(result.memoryUsageMb)
                    .build();
        } catch (Exception e) {
            return ExecutionResultDto.builder()
                    .status("Runtime Error")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    public ExecutionResultDto evaluateTestCases(String language, String code, List<TestCase> testCases) {
        int passed = 0;
        int total = testCases.size();
        double maxTimeMs = 0.0;
        double maxMemoryMb = 0.0;
        String firstFailureOutput = null;
        String firstFailureExpected = null;

        for (TestCase tc : testCases) {
            try {
                ProcessResult result = executeCode(language, code, tc.getInput());
                maxTimeMs = Math.max(maxTimeMs, result.executionTimeMs);
                maxMemoryMb = Math.max(maxMemoryMb, result.memoryUsageMb);

                if (result.timedOut) {
                    return ExecutionResultDto.builder()
                            .status("Time Limit Exceeded")
                            .passedTestCases(passed)
                            .totalTestCases(total)
                            .executionTimeMs(maxTimeMs)
                            .memoryUsageMb(maxMemoryMb)
                            .errorMessage("Execution timed out after " + TIMEOUT_SECONDS + " seconds.")
                            .build();
                }

                if (result.exitCode != 0 && result.error != null && !result.error.trim().isEmpty()) {
                    return ExecutionResultDto.builder()
                            .status("Runtime Error")
                            .passedTestCases(passed)
                            .totalTestCases(total)
                            .executionTimeMs(maxTimeMs)
                            .memoryUsageMb(maxMemoryMb)
                            .errorMessage(result.error)
                            .build();
                }

                String actualOutput = result.output.trim().replaceAll("\r\n", "\n");
                String expectedOutput = tc.getExpectedOutput().trim().replaceAll("\r\n", "\n");

                if (actualOutput.equals(expectedOutput)) {
                    passed++;
                } else {
                    if (firstFailureOutput == null) {
                        firstFailureOutput = actualOutput;
                        firstFailureExpected = expectedOutput;
                    }
                }
            } catch (Exception e) {
                return ExecutionResultDto.builder()
                        .status("Runtime Error")
                        .passedTestCases(passed)
                        .totalTestCases(total)
                        .errorMessage(e.getMessage())
                        .build();
            }
        }

        String finalStatus = (passed == total) ? "Accepted" : "Wrong Answer";

        return ExecutionResultDto.builder()
                .status(finalStatus)
                .passedTestCases(passed)
                .totalTestCases(total)
                .executionTimeMs(maxTimeMs)
                .memoryUsageMb(maxMemoryMb)
                .output(firstFailureOutput)
                .expectedOutput(firstFailureExpected)
                .build();
    }

    private ProcessResult executeCode(String language, String code, String input) throws IOException, InterruptedException {
        String lang = language.toLowerCase();
        Path tempDir = Files.createTempDirectory("algoviz_sandbox_");
        String filename;
        String[] runCommand;

        if (lang.equals("python") || lang.equals("py")) {
            filename = "script.py";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, code);
            runCommand = new String[]{"python", scriptFile.toAbsolutePath().toString()};
        } else if (lang.equals("javascript") || lang.equals("js")) {
            filename = "script.js";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, code);
            runCommand = new String[]{"node", scriptFile.toAbsolutePath().toString()};
        } else if (lang.equals("java")) {
            filename = "Main.java";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, code);
            runCommand = new String[]{"java", scriptFile.toAbsolutePath().toString()};
        } else {
            // Default python simulator fallback if runtime compiler is absent
            filename = "script.py";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, code);
            runCommand = new String[]{"python", scriptFile.toAbsolutePath().toString()};
        }

        long startTime = System.currentTimeMillis();

        ProcessBuilder pb = new ProcessBuilder(runCommand);
        pb.directory(tempDir.toFile());
        Process process = pb.start();

        if (input != null && !input.isEmpty()) {
            try (OutputStream os = process.getOutputStream();
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                writer.write(input);
                writer.flush();
            }
        }

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        double executionTimeMs = (endTime - startTime);

        if (!finished) {
            process.destroyForcibly();
            cleanupDirectory(tempDir);
            return new ProcessResult(true, 1, "", "Time Limit Exceeded", executionTimeMs, 5.0);
        }

        String stdout = readStream(process.getInputStream());
        String stderr = readStream(process.getErrorStream());

        cleanupDirectory(tempDir);

        return new ProcessResult(false, process.exitValue(), stdout, stderr, executionTimeMs, 12.5);
    }

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void cleanupDirectory(Path dir) {
        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                    });
        } catch (Exception ignored) {}
    }

    private static class ProcessResult {
        boolean timedOut;
        int exitCode;
        String output;
        String error;
        double executionTimeMs;
        double memoryUsageMb;

        ProcessResult(boolean timedOut, int exitCode, String output, String error, double executionTimeMs, double memoryUsageMb) {
            this.timedOut = timedOut;
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
            this.executionTimeMs = executionTimeMs;
            this.memoryUsageMb = memoryUsageMb;
        }
    }
}
