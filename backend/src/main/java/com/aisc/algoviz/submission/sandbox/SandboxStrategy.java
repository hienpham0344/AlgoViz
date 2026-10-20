package com.aisc.algoviz.submission.sandbox;

import java.io.IOException;

public interface SandboxStrategy {
    String getStrategyName();

    boolean isAvailable();

    boolean supportsLanguage(String language);

    SandboxExecutionResult execute(SandboxExecutionRequest request) throws IOException, InterruptedException;
}
