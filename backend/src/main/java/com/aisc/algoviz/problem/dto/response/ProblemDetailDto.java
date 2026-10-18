package com.aisc.algoviz.problem.dto.response;

import com.aisc.algoviz.problem.enums.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO chứa thông tin chi tiết đầy đủ của bài toán bao gồm mô tả và các bài giải mẫu.
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa chi tiết đầy đủ bài toán và danh sách lời giải")
public class ProblemDetailDto {

    @Schema(description = "ID bài toán", example = "1")
    private Long id;

    @Schema(description = "ID LeetCode", example = "1")
    private Integer leetcodeId;

    @Schema(description = "Tiêu đề bài toán", example = "Two Sum")
    private String title;

    @Schema(description = "Slug URL", example = "two-sum")
    private String slug;

    @Schema(description = "Mức độ khó", example = "EASY")
    private Difficulty difficulty;

    @Schema(description = "Mô tả bài toán (định dạng Markdown)")
    private String description;

    @Schema(description = "Danh sách tags thuật toán")
    private List<String> patternTags;

    @Schema(description = "Danh sách lời giải tham khảo")
    private List<SolutionResponseDto> solutions;
}
