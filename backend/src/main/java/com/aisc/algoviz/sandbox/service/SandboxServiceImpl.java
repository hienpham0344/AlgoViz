package com.aisc.algoviz.sandbox.service;

import com.aisc.algoviz.sandbox.compiler.JavaCompilerService;
import com.aisc.algoviz.sandbox.compiler.JavaCompilerService.CompilationResult;
import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;
import com.aisc.algoviz.sandbox.enums.ExecutionStatus;
import com.aisc.algoviz.sandbox.executor.ProcessBuilderExecutor;
import com.aisc.algoviz.sandbox.security.CodeSecurityValidator;
import com.aisc.algoviz.sandbox.security.CodeSecurityValidator.SecurityCheckResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Điều phối luồng xử lý toàn bộ quá trình Sandbox (Sandbox Pipeline Orchestrator).
 * Luồng thực hiện bao gồm 4 bước:
 * 1. Kiểm tra an toàn tĩnh (CodeSecurityValidator - Bước 5b)
 * 2. Khởi tạo thư mục tạm thời Cô lập Sandbox
 * 3. Biên dịch mã nguồn Java tại runtime (JavaCompilerService - Bước 4)
 * 4. Chạy Process con JVM độc lập với tài nguyên đã giới hạn (ProcessBuilderExecutor - Bước 2, 3)
 * 5. Tự động dọn dẹp (Cleanup) dữ liệu tạm thời
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SandboxServiceImpl implements SandboxService {

    private final CodeSecurityValidator securityValidator;
    private final JavaCompilerService compilerService;
    private final ProcessBuilderExecutor processBuilderExecutor;

    @Override
    public ExecutionResultDto executeCode(ExecutionRequestDto request) {
        log.info("Bắt đầu tiến trình Sandbox cho class: {}", request.getMainClassName());

        // --- BƯỚC 1: Kiểm tra an toàn tĩnh mã nguồn (Security Validation - Bước 5b) ---
        SecurityCheckResult securityResult = securityValidator.validate(request.getCode());
        if (!securityResult.isValid()) {
            log.warn("Mã nguồn vi phạm an toàn Sandbox: {}", securityResult.errorMessage());
            return ExecutionResultDto.builder()
                    .status(ExecutionStatus.SECURITY_VIOLATION)
                    .stderr(securityResult.errorMessage())
                    .build();
        }

        // --- BƯỚC 2: Khởi tạo thư mục tạm thời cho phiên Sandbox này ---
        Path tempDirectory = null;
        try {
            tempDirectory = Files.createTempDirectory("algoviz_sandbox_");
        } catch (IOException e) {
            log.error("Không thể tạo thư mục làm việc tạm thời cho Sandbox", e);
            return ExecutionResultDto.builder()
                    .status(ExecutionStatus.SYSTEM_ERROR)
                    .stderr("Lỗi hệ thống: Không thể khởi tạo môi trường thực thi tạm thời.")
                    .build();
        }

        try {
            // --- BƯỚC 3: Biên dịch mã nguồn Java tại runtime (JavaCompiler - Bước 4) ---
            CompilationResult compileResult = compilerService.compile(
                    request.getCode(),
                    request.getMainClassName(),
                    tempDirectory
            );

            if (!compileResult.isSuccess()) {
                log.info("Lỗi biên dịch mã nguồn trong Sandbox");
                return ExecutionResultDto.builder()
                        .status(ExecutionStatus.COMPILATION_ERROR)
                        .compilerErrorDetails(compileResult.errorLog())
                        .stderr(compileResult.errorLog())
                        .build();
            }

            // --- BƯỚC 4: Thực thi mã nguồn bằng ProcessBuilder (ProcessBuilder & JVM Flags - Bước 2, 3) ---
            return processBuilderExecutor.execute(request, tempDirectory);

        } finally {
            // --- BƯỚC 5: Thuật toán dọn dẹp thư mục tạm (Directory Cleanup) ---
            deleteDirectoryRecursively(tempDirectory);
        }
    }

    /**
     * Thuật toán xóa đệ quy toàn bộ thư mục tạm và các file .java / .class bên trong
     * nhằm giải phóng ổ đĩa server ngay sau khi thực thi xong.
     */
    private void deleteDirectoryRecursively(Path directoryPath) {
        if (directoryPath == null || !Files.exists(directoryPath)) {
            return;
        }

        try (Stream<Path> walk = Files.walk(directoryPath)) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            log.error("Lỗi dọn dẹp thư mục tạm Sandbox: {}", directoryPath, e);
        }
    }
}
