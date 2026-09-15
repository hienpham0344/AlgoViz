package com.aisc.algoviz.sandbox.compiler;

import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Thuật toán biên dịch mã nguồn Java tại thời điểm Runtime (Bước 4).
 * Lớp này nhận vào chuỗi mã nguồn Java của học sinh, ghi ra file .java trong thư mục tạm,
 * sau đó kích hoạt javax.tools.JavaCompiler để biên dịch thành file byte-code (.class).
 */
@Component
public class JavaCompilerService {

    /**
     * Thuật toán biên dịch mã nguồn Java.
     *
     * @param sourceCode Code Java cần biên dịch
     * @param mainClassName Tên class chính chứa hàm main (ví dụ: Main)
     * @param outputDirectory Thư mục tạm lưu file biên dịch .class
     * @return CompilationResult chứa thông tin thành công hay thất bại + chi tiết lỗi
     */
    public CompilationResult compile(String sourceCode, String mainClassName, Path outputDirectory) {
        // Thuật toán Bước 4.1: Khởi tạo file source Java trong thư mục làm việc tạm
        Path sourcePath = outputDirectory.resolve(mainClassName + ".java");

        try {
            // Ghi nội dung code người dùng vào file .java tạm thời với charset UTF-8
            Files.writeString(sourcePath, sourceCode, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return CompilationResult.failure("Không thể khởi tạo file nguồn tạm thời: " + e.getMessage());
        }

        // Thuật toán Bước 4.2: Lấy bộ biên dịch Java hệ thống (System JavaCompiler API)
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return CompilationResult.failure("Hệ thống JDK không tìm thấy JavaCompiler API. Cần chạy ứng dụng trên JDK thay vì JRE.");
        }

        // Thuật toán Bước 4.3: Thu thập thông báo lỗi biên dịch thông qua DiagnosticCollector
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // Quản lý file chuẩn của JavaCompiler
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), StandardCharsets.UTF_8)) {

            // Chuyển đổi file .java tạm thành đối tượng JavaFileObject để trình biên dịch xử lý
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                    Collections.singletonList(sourcePath.toFile())
            );

            // Cấu hình tham số cho javac (chỉ định thư mục đầu ra -d cho file .class)
            List<String> options = List.of("-d", outputDirectory.toAbsolutePath().toString());

            // Tạo task biên dịch Java
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,
                    options,
                    null,
                    compilationUnits
            );

            // Thuật toán Bước 4.4: Thực thi biên dịch (Trả về true nếu compile thành công, false nếu có lỗi cú pháp)
            boolean success = task.call();

            if (success) {
                // Biên dịch thành công -> Trả về đường dẫn đến class file vừa biên dịch
                return CompilationResult.success(outputDirectory);
            } else {
                // Thuật toán Trích xuất và định dạng danh sách lỗi biên dịch (Compilation Errors)
                StringBuilder errorBuilder = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    errorBuilder.append(String.format("Lỗi dòng %d: %s%n",
                            diagnostic.getLineNumber(),
                            diagnostic.getMessage(Locale.getDefault())));
                }
                return CompilationResult.failure(errorBuilder.toString().trim());
            }

        } catch (IOException e) {
            return CompilationResult.failure("Lỗi IO trong quá trình biên dịch: " + e.getMessage());
        }
    }

    /**
     * Record đại diện cho kết quả của quá trình biên dịch.
     */
    public record CompilationResult(boolean isSuccess, Path outputDir, String errorLog) {
        public static CompilationResult success(Path outputDir) {
            return new CompilationResult(true, outputDir, null);
        }

        public static CompilationResult failure(String errorLog) {
            return new CompilationResult(false, null, errorLog);
        }
    }
}
