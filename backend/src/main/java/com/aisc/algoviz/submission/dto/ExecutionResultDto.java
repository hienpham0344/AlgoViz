package com.aisc.algoviz.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResultDto {
    private String status; // "Accepted", "Wrong Answer", "Time Limit Exceeded", "Runtime Error", "Compilation Error"
    private String output;
    private String expectedOutput;
    private Double executionTimeMs;
    private Double memoryUsageMb;
    private Integer passedTestCases;
    private Integer totalTestCases;
    private String errorMessage;
}
