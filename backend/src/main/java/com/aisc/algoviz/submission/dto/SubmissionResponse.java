package com.aisc.algoviz.submission.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String username;
    private String language;
    private String code;
    private String status;
    private Double executionTimeMs;
    private Double memoryUsageMb;
    private Integer passedTestCases;
    private Integer totalTestCases;
    private String errorMessage;
    private LocalDateTime submittedAt;
}
