package com.aisc.algoviz.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO chứa thông tin giải pháp bài mẫu (Reference Solution).
 * Phục vụ hiển thị trên Visualization Player trong màn Problem Detail (/problems/:id).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolutionResponseDto {

    private Long id;
    private Long patternId;
    private String codeSnippet;
    private String explanation;
    private String timeComplexity;
    private String spaceComplexity;
    private String language;
}
