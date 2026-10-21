package com.aisc.algoviz.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailDto {
    private Long id;
    private Integer leetcodeId;
    private String title;
    private String category;
    private String difficulty;
    private String description;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private String sampleInput;
    private String sampleOutput;
    private String starterCodePython;
    private String starterCodeCpp;
    private String starterCodeJava;
    private String starterCodeJs;
    private List<SampleTestCaseDto> sampleTestCases;
    private List<com.aisc.algoviz.problem.dto.response.SolutionResponseDto> solutions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SampleTestCaseDto {
        private String input;
        private String expectedOutput;
    }
}
