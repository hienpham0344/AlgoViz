package com.aisc.algoviz.problem.dto.request;

import com.aisc.algoviz.problem.enums.Difficulty;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO đóng gói toàn bộ các tham số truy vấn, lọc, phân trang và sắp xếp bài toán.
 * Giúp mã nguồn Controller ngắn gọn, dễ đọc và dễ bảo trì.
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemFilterRequest {

    /**
     * Danh sách Whitelist các trường sắp xếp an toàn được phép truy vấn
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "title", "difficulty", "leetcodeId", "createdAt"
    );

    @Builder.Default
    @Min(value = 0, message = "Chỉ số trang bắt đầu từ 0")
    @Parameter(description = "Chỉ số trang bắt đầu từ 0 (mặc định: 0)")
    private int page = 0;

    @Builder.Default
    @Min(value = 1, message = "Kích thước trang phải lớn hơn 0")
    @Max(value = 100, message = "Kích thước trang tối đa là 100")
    @Parameter(description = "Số lượng bài toán trên một trang (mặc định: 10)")
    private int size = 10;

    @Parameter(description = "Lọc theo mức độ khó (EASY, MEDIUM, HARD)")
    private Difficulty difficulty;

    @Parameter(description = "Lọc theo Tag thuật toán (vd: Array, Dynamic Programming)")
    private String tag;

    @Parameter(description = "Tìm kiếm từ khóa xuất hiện trong tiêu đề bài toán")
    private String search;

    @Builder.Default
    @Parameter(description = "Tên trường dùng để sắp xếp (mặc định: id)")
    private String sortBy = "id";

    @Builder.Default
    @Parameter(description = "Chiều sắp xếp: 'asc' (tăng dần) hoặc 'desc' (giảm dần)")
    private String sortDirection = "asc";

    /**
     * Phương thức kiểm tra và làm sạch trường sắp xếp (Sanitization).
     * Trả về 'id' nếu client truyền tên cột không hợp lệ để tránh lỗi JPA / SQL Injection.
     *
     * @return Tên trường sắp xếp an toàn
     */
    public String getSanitizedSortBy() {
        if (sortBy == null || !ALLOWED_SORT_FIELDS.contains(sortBy.trim())) {
            return "id";
        }
        return sortBy.trim();
    }
}
