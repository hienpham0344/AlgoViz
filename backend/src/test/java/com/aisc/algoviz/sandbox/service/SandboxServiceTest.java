package com.aisc.algoviz.sandbox.service;

import com.aisc.algoviz.sandbox.compiler.JavaCompilerService;
import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;
import com.aisc.algoviz.sandbox.enums.ExecutionStatus;
import com.aisc.algoviz.sandbox.executor.ProcessBuilderExecutor;
import com.aisc.algoviz.sandbox.security.CodeSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lớp kiểm thử đơn vị (Unit Test) cho toàn bộ hệ thống Sandbox bằng ProcessBuilder.
 */
class SandboxServiceTest {

    private SandboxService sandboxService;

    @BeforeEach
    void setUp() {
        CodeSecurityValidator securityValidator = new CodeSecurityValidator();
        JavaCompilerService compilerService = new JavaCompilerService();
        ProcessBuilderExecutor processBuilderExecutor = new ProcessBuilderExecutor();

        sandboxService = new SandboxServiceImpl(securityValidator, compilerService, processBuilderExecutor);
    }

    @Test
    @DisplayName("Kiểm thử thực thi thành công với Input và Output tiêu chuẩn (SUCCESS)")
    void testSuccessfulExecution() {
        String code = """
                import java.util.Scanner;
                public class Main {
                    public static void main(String[] args) {
                        Scanner sc = new Scanner(System.in);
                        int a = sc.nextInt();
                        int b = sc.nextInt();
                        System.out.println("SUM=" + (a + b));
                    }
                }
                """;

        ExecutionRequestDto request = ExecutionRequestDto.builder()
                .code(code)
                .input("15 25")
                .timeLimitMs(3000L)
                .memoryLimitMb(64)
                .build();

        ExecutionResultDto result = sandboxService.executeCode(request);

        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals("SUM=40", result.getStdout());
        assertEquals(0, result.getExitCode());
    }

    @Test
    @DisplayName("Kiểm thử lỗi biên dịch cú pháp (COMPILATION_ERROR)")
    void testCompilationError() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Loi thieu dau cham phay")
                    }
                }
                """;

        ExecutionRequestDto request = ExecutionRequestDto.builder()
                .code(code)
                .build();

        ExecutionResultDto result = sandboxService.executeCode(request);

        assertEquals(ExecutionStatus.COMPILATION_ERROR, result.getStatus());
        assertNotNull(result.getCompilerErrorDetails());
        assertTrue(result.getCompilerErrorDetails().contains("Lỗi dòng 3"));
    }

    @Test
    @DisplayName("Kiểm thử chặn vòng lặp vô hạn (TIME_LIMIT_EXCEEDED)")
    void testTimeLimitExceeded() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        while (true) {
                            // Vong lap vo han
                        }
                    }
                }
                """;

        ExecutionRequestDto request = ExecutionRequestDto.builder()
                .code(code)
                .timeLimitMs(1000L) // Giới hạn 1 giây
                .build();

        ExecutionResultDto result = sandboxService.executeCode(request);

        assertEquals(ExecutionStatus.TIME_LIMIT_EXCEEDED, result.getStatus());
        assertTrue(result.getExecutionTimeMs() >= 1000L);
    }

    @Test
    @DisplayName("Kiểm thử cô lập đệ quy vô hạn với cờ -Xss (RUNTIME_ERROR - StackOverflowError)")
    void testStackOverflowIsolation() {
        String code = """
                public class Main {
                    public static void recursiveFunction() {
                        recursiveFunction();
                    }
                    public static void main(String[] args) {
                        recursiveFunction();
                    }
                }
                """;

        ExecutionRequestDto request = ExecutionRequestDto.builder()
                .code(code)
                .stackLimitKb(256) // Giới hạn Stack nhỏ để nhanh chóng phát hiện tràn bộ nhớ Stack
                .build();

        ExecutionResultDto result = sandboxService.executeCode(request);

        assertEquals(ExecutionStatus.RUNTIME_ERROR, result.getStatus());
        assertTrue(result.getStderr().contains("StackOverflowError"));
    }

    @Test
    @DisplayName("Kiểm thử ngăn chặn mã nguồn nguy hiểm truy cập Mạng (SECURITY_VIOLATION)")
    void testSecurityViolationNetwork() {
        String code = """
                import java.net.Socket;
                public class Main {
                    public static void main(String[] args) {
                        System.out.println("Try connect network");
                    }
                }
                """;

        ExecutionRequestDto request = ExecutionRequestDto.builder()
                .code(code)
                .build();

        ExecutionResultDto result = sandboxService.executeCode(request);

        assertEquals(ExecutionStatus.SECURITY_VIOLATION, result.getStatus());
        assertTrue(result.getStderr().contains("Vi phạm quy định an toàn Sandbox"));
    }

    @Test
    @DisplayName("Kiểm thử ngăn chặn mã nguồn gọi System.exit (SECURITY_VIOLATION)")
    void testSecurityViolationSystemExit() {
        String code = """
                public class Main {
                    public static void main(String[] args) {
                        System.exit(0);
                    }
                }
                """;

        ExecutionRequestDto request = ExecutionRequestDto.builder()
                .code(code)
                .build();

        ExecutionResultDto result = sandboxService.executeCode(request);

        assertEquals(ExecutionStatus.SECURITY_VIOLATION, result.getStatus());
        assertTrue(result.getStderr().contains("Cấm gọi System.exit()"));
    }
}
