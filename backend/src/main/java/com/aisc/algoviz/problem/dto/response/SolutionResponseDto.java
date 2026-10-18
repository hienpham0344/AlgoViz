package com.aisc.algoviz.problem.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin phản hồi bài giải mẫu (Reference Solution).
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa thông tin bài giải tham khảo")
public class SolutionResponseDto {

    @Schema(description = "ID lời giải", example = "1")
    private Long id;

    @Schema(description = "ID thuật toán / Pattern liên quan", example = "101")
    private Long patternId;

    @Schema(description = "Mã nguồn bài giải tham khảo")
    private String codeSnippet;

    @Schema(description = "Giải thích chi tiết")
    private String explanation;

    @Schema(description = "Độ phức tạp thời gian", example = "O(N)")
    private String timeComplexity;

    @Schema(description = "Độ phức tạp bộ nhớ", example = "O(N)")
    private String spaceComplexity;

    @Schema(description = "Ngôn ngữ lập trình", example = "Java")
    private String language;
}
