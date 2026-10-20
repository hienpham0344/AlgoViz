package com.aisc.algoviz.submission.sandbox;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@Builder
public class SandboxExecutionRequest {
    private final String language;
    private final Path workDirectory;
    private final String sourceFileName;
    private final String[] command;
    private final String input;
    private final long timeoutSeconds;
}
