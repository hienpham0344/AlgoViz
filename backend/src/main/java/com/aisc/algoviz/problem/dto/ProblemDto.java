package com.aisc.algoviz.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDto {
    private Long id;
    private String title;
    private String category;
    private String difficulty;
    private String shortDescription;
    private Double acceptanceRate;
}
