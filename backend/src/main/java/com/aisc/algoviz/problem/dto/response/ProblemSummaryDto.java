package com.aisc.algoviz.problem.dto.response;

import com.aisc.algoviz.problem.enums.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO chứa thông tin tóm tắt bài toán dành cho trang danh sách.
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa thông tin tóm tắt bài toán trong danh sách")
public class ProblemSummaryDto {

    @Schema(description = "ID bài toán trong hệ thống", example = "1")
    private Long id;

    @Schema(description = "ID LeetCode tương ứng", example = "1")
    private Integer leetcodeId;

    @Schema(description = "Tiêu đề bài toán", example = "Two Sum")
    private String title;

    @Schema(description = "Slug URL bài toán", example = "two-sum")
    private String slug;

    @Schema(description = "Mức độ khó của bài toán", example = "EASY")
    private Difficulty difficulty;

    @Schema(description = "Danh sách tags thuật toán", example = "[\"Array\", \"Hash Table\"]")
    private List<String> patternTags;
}
