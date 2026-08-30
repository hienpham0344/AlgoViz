package com.aisc.algoviz.problem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đóng gói dữ liệu yêu cầu thêm mới bài giải mẫu (Reference Solution).
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa dữ liệu yêu cầu tạo bài giải mẫu cho bài toán")
public class CreateSolutionRequestDto {

    @Schema(description = "ID thuật toán / Pattern liên quan", example = "101")
    private Long patternId;

    @NotBlank(message = "Mã nguồn bài giải không được để trống")
    @Schema(description = "Mã nguồn bài giải tham khảo", example = "class Solution { public int[] twoSum(...) }")
    private String codeSnippet;

    @Schema(description = "Giải thích thuật toán chi tiết", example = "Sử dụng HashMap để tìm cặp số có tổng bằng target")
    private String explanation;

    @Schema(description = "Độ phức tạp thời gian", example = "O(N)")
    private String timeComplexity;

    @Schema(description = "Độ phức tạp không gian bộ nhớ", example = "O(N)")
    private String spaceComplexity;

    @Builder.Default
    @Schema(description = "Ngôn ngữ lập trình", example = "Java")
    private String language = "Java";
}
