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
            if (result.timedOut) {
                return ExecutionResultDto.builder()
                        .status("Time Limit Exceeded")
                        .errorMessage("Execution timed out after " + TIMEOUT_SECONDS + " seconds.")
                        .build();
            }

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
                    .status("Success")
                    .output(result.output != null ? result.output.trim() : "")
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

                if (outputsMatch(actualOutput, expectedOutput)) {
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

    private boolean outputsMatch(String actual, String expected) {
        if (actual == null && expected == null) return true;
        if (actual == null || expected == null) return false;
        String a = actual.trim().replaceAll("\r\n", "\n");
        String e = expected.trim().replaceAll("\r\n", "\n");
        if (a.equalsIgnoreCase(e)) return true;
        String normA = a.replaceAll("[\\[\\],]", " ").replaceAll("\\s+", " ").trim();
        String normE = e.replaceAll("[\\[\\],]", " ").replaceAll("\\s+", " ").trim();
        return !normA.isEmpty() && normA.equalsIgnoreCase(normE);
    }

    private ProcessResult executeCode(String language, String code, String input) throws IOException, InterruptedException {
        String lang = language.toLowerCase();
        Path tempDir = Files.createTempDirectory("algoviz_sandbox_");
        String filename;
        String[] runCommand;
        String runnableCode = prepareCodeWithDriver(lang, code);

        if (lang.equals("python") || lang.equals("py")) {
            filename = "script.py";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, runnableCode);
            runCommand = new String[]{"python", scriptFile.toAbsolutePath().toString()};
        } else if (lang.equals("javascript") || lang.equals("js")) {
            filename = "script.js";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, runnableCode);
            runCommand = new String[]{"node", scriptFile.toAbsolutePath().toString()};
        } else if (lang.equals("java")) {
            filename = "Main.java";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, runnableCode);
            runCommand = new String[]{"java", scriptFile.toAbsolutePath().toString()};
        } else {
            // Default python simulator fallback if runtime compiler is absent
            filename = "script.py";
            Path scriptFile = tempDir.resolve(filename);
            Files.writeString(scriptFile, runnableCode);
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

    private String prepareCodeWithDriver(String language, String code) {
        String lang = language.toLowerCase();
        if ((lang.equals("javascript") || lang.equals("js")) && !code.contains("console.log")) {
            if (code.contains("twoSum")) {
                return code + "\n\nconst fs = require('fs');\n" +
                        "const raw = fs.readFileSync(0, 'utf-8').trim();\n" +
                        "if (raw) {\n" +
                        "  let target, nums;\n" +
                        "  const lines = raw.split(/\\r?\\n/).map(s => s.trim()).filter(Boolean);\n" +
                        "  if (lines.length >= 2) {\n" +
                        "    if (lines[0].includes(' ') || lines[0].includes(',')) {\n" +
                        "      nums = lines[0].replace(/[\\[\\]]/g, '').trim().split(/[\\s,]+/).map(Number);\n" +
                        "      target = Number(lines[1]);\n" +
                        "    } else {\n" +
                        "      target = Number(lines[0]);\n" +
                        "      nums = lines[1].replace(/[\\[\\]]/g, '').trim().split(/[\\s,]+/).map(Number);\n" +
                        "    }\n" +
                        "  } else {\n" +
                        "    const match = raw.match(/\\[(.*?)\\]/);\n" +
                        "    if (match) {\n" +
                        "      nums = match[1].split(/[\\s,]+/).filter(Boolean).map(Number);\n" +
                        "      const rest = raw.replace(match[0], '').replace(/[,\\s]/g, '');\n" +
                        "      target = Number(rest);\n" +
                        "    } else {\n" +
                        "      const parts = raw.split(/[\\s,]+/).map(Number);\n" +
                        "      target = parts[0];\n" +
                        "      nums = parts.slice(1);\n" +
                        "    }\n" +
                        "  }\n" +
                        "  let res;\n" +
                        "  try {\n" +
                        "    res = twoSum(target, nums);\n" +
                        "    if (res === undefined) res = twoSum(nums, target);\n" +
                        "  } catch (e) {\n" +
                        "    res = twoSum(nums, target);\n" +
                        "  }\n" +
                        "  if (Array.isArray(res)) {\n" +
                        "    console.log(JSON.stringify(res));\n" +
                        "  } else if (res !== undefined) {\n" +
                        "    console.log(res);\n" +
                        "  }\n" +
                        "}\n";
            } else if (code.contains("isAnagram")) {
                return code + "\n\nconst fs = require('fs');\n" +
                        "const raw = fs.readFileSync(0, 'utf-8').trim();\n" +
                        "if (raw) {\n" +
                        "  const lines = raw.split(/\\r?\\n/).map(s => s.trim()).filter(Boolean);\n" +
                        "  let s = lines[0] || '', t = lines[1] || '';\n" +
                        "  if (!t && s.includes(' ')) { const parts = s.split(/\\s+/); s = parts[0]; t = parts[1]; }\n" +
                        "  const res = isAnagram(s, t);\n" +
                        "  console.log(res);\n" +
                        "}\n";
            } else if (code.contains("binarySearch")) {
                return code + "\n\nconst fs = require('fs');\n" +
                        "const raw = fs.readFileSync(0, 'utf-8').trim();\n" +
                        "if (raw) {\n" +
                        "  const lines = raw.split(/\\r?\\n/).map(s => s.trim()).filter(Boolean);\n" +
                        "  let target = Number(lines[0]);\n" +
                        "  let nums = (lines[1] || '').replace(/[\\[\\]]/g, '').trim().split(/[\\s,]+/).map(Number);\n" +
                        "  let res;\n" +
                        "  try { res = binarySearch(target, nums); if (res === undefined) res = binarySearch(nums, target); }\n" +
                        "  catch(e) { res = binarySearch(nums, target); }\n" +
                        "  console.log(res !== undefined ? res : -1);\n" +
                        "}\n";
            }
        } else if ((lang.equals("python") || lang.equals("py")) && !code.contains("print(")) {
            if (code.contains("two_sum")) {
                return code + "\n\nif __name__ == '__main__':\n" +
                        "    import sys, json, re\n" +
                        "    raw = sys.stdin.read().strip()\n" +
                        "    if raw:\n" +
                        "        lines = [l.strip() for l in raw.splitlines() if l.strip()]\n" +
                        "        target, nums = None, []\n" +
                        "        if len(lines) >= 2:\n" +
                        "            if ' ' in lines[0] or ',' in lines[0]:\n" +
                        "                nums = [int(x) for x in re.sub(r'[\\[\\]]', '', lines[0]).replace(',', ' ').split()]\n" +
                        "                target = int(lines[1])\n" +
                        "            else:\n" +
                        "                target = int(lines[0])\n" +
                        "                nums = [int(x) for x in re.sub(r'[\\[\\]]', '', lines[1]).replace(',', ' ').split()]\n" +
                        "        else:\n" +
                        "            match = re.search(r'\\[(.*?)\\]', raw)\n" +
                        "            if match:\n" +
                        "                nums = [int(x) for x in match.group(1).replace(',', ' ').split()]\n" +
                        "                rest = re.sub(r'\\[.*?\\]|[,\\s]', '', raw)\n" +
                        "                target = int(rest) if rest else 0\n" +
                        "            else:\n" +
                        "                parts = [int(x) for x in raw.replace(',', ' ').split()]\n" +
                        "                target = parts[0] if parts else 0\n" +
                        "                nums = parts[1:] if len(parts) > 1 else []\n" +
                        "        try:\n" +
                        "            res = two_sum(target, nums)\n" +
                        "            if res is None: res = two_sum(nums, target)\n" +
                        "        except Exception:\n" +
                        "            res = two_sum(nums, target)\n" +
                        "        if isinstance(res, (list, tuple)):\n" +
                        "            print(json.dumps(list(res)))\n" +
                        "        elif res is not None:\n" +
                        "            print(res)\n";
            } else if (code.contains("is_anagram")) {
                return code + "\n\nif __name__ == '__main__':\n" +
                        "    import sys\n" +
                        "    raw = sys.stdin.read().strip()\n" +
                        "    if raw:\n" +
                        "        lines = [l.strip() for l in raw.splitlines() if l.strip()]\n" +
                        "        s = lines[0] if len(lines) > 0 else ''\n" +
                        "        t = lines[1] if len(lines) > 1 else ''\n" +
                        "        if not t and ' ' in s:\n" +
                        "            parts = s.split()\n" +
                        "            s, t = parts[0], parts[1]\n" +
                        "        res = is_anagram(s, t)\n" +
                        "        print(str(res).lower())\n";
            } else if (code.contains("binary_search")) {
                return code + "\n\nif __name__ == '__main__':\n" +
                        "    import sys, re\n" +
                        "    raw = sys.stdin.read().strip()\n" +
                        "    if raw:\n" +
                        "        lines = [l.strip() for l in raw.splitlines() if l.strip()]\n" +
                        "        target = int(lines[0]) if len(lines) > 0 else 0\n" +
                        "        nums = [int(x) for x in re.sub(r'[\\[\\]]', '', lines[1]).replace(',', ' ').split()] if len(lines) > 1 else []\n" +
                        "        try:\n" +
                        "            res = binary_search(target, nums)\n" +
                        "            if res is None: res = binary_search(nums, target)\n" +
                        "        except Exception:\n" +
                        "            res = binary_search(nums, target)\n" +
                        "        print(res if res is not None else -1)\n";
            }
        }
        return code;
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
