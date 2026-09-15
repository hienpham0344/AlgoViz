package com.aisc.algoviz.sandbox.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Thuật toán kiểm tra an toàn tĩnh (Static Code Security Analysis - Bước 5b).
 * Lớp này phân tích mã nguồn Java của người dùng trước khi biên dịch nhằm phát hiện
 * các truy cập hệ thống nguy hiểm như đọc/ghi file, kết nối mạng, tạo thread mới hoặc gọi lệnh OS.
 */
@Component
public class CodeSecurityValidator {

    /**
     * Danh sách các mẫu Pattern (Regex) đại diện cho các hành vi bị cấm trong Sandbox.
     */
    private static final List<ForbiddenRule> FORBIDDEN_RULES = new ArrayList<>();

    static {
        // --- 1. Cấm thao tác Mạng (Network Access) ---
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("import\\s+java\\.net\\..*|java\\.net\\."),
                "Cấm truy cập thư viện mạng (java.net)"
        ));

        // --- 2. Cấm thao tác File System / Đọc ghi ổ đĩa (File I/O) ---
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("import\\s+java\\.io\\.(?!InputStream|OutputStream|PrintStream|BufferedReader|InputStreamReader|Scanner).*"),
                "Cấm import các lớp đọc/ghi file trực tiếp từ java.io (chỉ cho phép các stream chuẩn)"
        ));
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("import\\s+java\\.nio\\..*|java\\.nio\\."),
                "Cấm truy cập thư viện NIO / File System (java.nio)"
        ));
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("\\b(File|FileReader|FileWriter|FileInputStream|FileOutputStream|RandomAccessFile|Path|Files)\\b"),
                "Cấm thao tác trực tiếp với File hoặc Thư mục hệ thống"
        ));

        // --- 3. Cấm gọi Lệnh Hệ điều hành & Runtime Manipulation ---
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("\\bSystem\\.exit\\s*\\("),
                "Cấm gọi System.exit() để dừng hệ thống"
        ));
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("\\bRuntime\\.getRuntime\\s*\\("),
                "Cấm gọi Runtime.getRuntime() để truy cập tài nguyên OS"
        ));
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("\\bProcessBuilder\\b"),
                "Cấm sử dụng ProcessBuilder để tạo process con"
        ));

        // --- 4. Cấm Reflection & ClassLoader (để tránh bypass sandbox) ---
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("import\\s+java\\.lang\\.reflect\\..*|java\\.lang\\.reflect\\."),
                "Cấm sử dụng Java Reflection (java.lang.reflect)"
        ));
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("\\bClassLoader\\b"),
                "Cấm thao tác với ClassLoader"
        ));

        // --- 5. Cấm Đa luồng (Multithreading / Concurrency) ---
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("import\\s+java\\.util\\.concurrent\\..*"),
                "Cấm sử dụng thư viện đa luồng (java.util.concurrent)"
        ));
        FORBIDDEN_RULES.add(new ForbiddenRule(
                Pattern.compile("\\b(Thread|Runnable|ExecutorService|ThreadPoolExecutor)\\b"),
                "Cấm tạo luồng phụ (Thread) để tránh treo CPU hoặc vượt qua kiểm soát Process"
        ));
    }

    /**
     * Thuật toán quét và kiểm duyệt mã nguồn Java.
     *
     * @param code Chuỗi mã nguồn Java cần kiểm tra
     * @return SingleValidationResult chứa thông báo lỗi vi phạm (nếu có)
     */
    public SecurityCheckResult validate(String code) {
        if (code == null || code.trim().isEmpty()) {
            return SecurityCheckResult.invalid("Mã nguồn không được để trống.");
        }

        // Loại bỏ comment trong code trước khi phân tích để tránh việc match nhầm regex trong comment
        String cleanedCode = removeComments(code);

        // Duyệt qua từng quy tắc bảo mật trong danh sách
        for (ForbiddenRule rule : FORBIDDEN_RULES) {
            if (rule.pattern.matcher(cleanedCode).find()) {
                // Thuật toán phát hiện vi phạm quy tắc an toàn -> Trả về kết quả từ chối ngay lập tức
                return SecurityCheckResult.invalid("Vi phạm quy định an toàn Sandbox: " + rule.description);
            }
        }

        // Nếu vượt qua tất cả quy tắc -> Mã nguồn an toàn hợp lệ
        return SecurityCheckResult.valid();
    }

    /**
     * Thuật toán loại bỏ các đoạn comment (single line // và block comment /* ... *\/)
     * giúp quá trình soi Regex chính xác hơn.
     */
    private String removeComments(String code) {
        // Thuật toán Regex xóa block comment /* ... */ và single-line comment // ...
        return code.replaceAll("//.*|/\\*[\\s\\S]*?\\*/", "");
    }

    /**
     * Class chứa quy tắc cấm (Regex Pattern + Mô tả bằng tiếng Việt)
     */
    private record ForbiddenRule(Pattern pattern, String description) {}

    /**
     * Class lưu trữ kết quả kiểm tra an toàn tĩnh.
     */
    public record SecurityCheckResult(boolean isValid, String errorMessage) {
        public static SecurityCheckResult valid() {
            return new SecurityCheckResult(true, null);
        }

        public static SecurityCheckResult invalid(String errorMessage) {
            return new SecurityCheckResult(false, errorMessage);
        }
    }
}
