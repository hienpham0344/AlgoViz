package com.aisc.algoviz.submission.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class CodeExecutionServiceTest {
    private final CodeExecutionService service = new CodeExecutionService();

    @Test
    void emptyInputClosesStdinInsteadOfHanging() {
        var result = service.runCodeCustomInput("java", "class Main { public static void main(String[] args) { System.out.println(new java.util.Scanner(System.in).hasNextLine() ? \"input\" : \"EOF\"); } }", "");
        assertEquals("Success", result.getStatus(), result.getErrorMessage());
        assertEquals("EOF", result.getOutput());
    }

    @Test
    void nonzeroExitWithoutStderrIsStillAnError() {
        var result = service.runCodeCustomInput("java", "class Main { public static void main(String[] args) { System.exit(3); } }", "");
        assertEquals("Runtime Error", result.getStatus());
        assertEquals("Program exited with code 3", result.getErrorMessage());
    }

    @Test
    void outputLargerThanAPipeBufferDoesNotCauseTimeout() {
        var result = service.runCodeCustomInput("java", "class Main { public static void main(String[] args) { System.out.print(\"x\".repeat(131072)); } }", "");
        assertEquals("Success", result.getStatus(), result.getErrorMessage());
        assertEquals(131072, result.getOutput().length());
    }

    @Test
    void missingCppCompilerIsReportedInsteadOfExecutingCppAsPython() {
        ReflectionTestUtils.setField(service, "cppCommand", "algoviz-missing-cpp-compiler");
        var result = service.runCodeCustomInput("cpp", "int main() { return 0; }", "");
        assertEquals("Runtime Error", result.getStatus());
        assertTrue(result.getErrorMessage().contains("ALGOVIZ_CPP_COMMAND"));
    }

    @Test
    void rejectsUnknownLanguages() {
        var result = service.runCodeCustomInput("ruby", "puts 2", "");
        assertEquals("Runtime Error", result.getStatus());
        assertTrue(result.getErrorMessage().contains("Unsupported execution language"));
    }

    @Test
    void configuredPythonRunsTheProblemFunctionWithItsSampleInput() {
        String python = System.getenv("ALGOVIZ_PYTHON_COMMAND");
        assumeTrue(python != null && !python.isBlank(), "Set ALGOVIZ_PYTHON_COMMAND to run the Python integration check");
        ReflectionTestUtils.setField(service, "pythonCommand", python);
        String code = "def two_sum(target, nums):\n    for i in range(len(nums)):\n        for j in range(i + 1, len(nums)):\n            if nums[i] + nums[j] == target:\n                return [i, j]\n";
        var result = service.runCodeCustomInput("python", code, "9\n2 7 11 15");
        assertEquals("Success", result.getStatus(), result.getErrorMessage());
        assertEquals("[0, 1]", result.getOutput());
    }
}
