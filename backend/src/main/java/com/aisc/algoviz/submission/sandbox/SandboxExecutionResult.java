package com.aisc.algoviz.submission.sandbox;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SandboxExecutionResult {
    private boolean timedOut;
    private int exitCode;
    private String output;
    private String error;
    private double executionTimeMs;
    private double memoryUsageMb;
}
