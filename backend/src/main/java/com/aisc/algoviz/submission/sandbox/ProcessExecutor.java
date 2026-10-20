package com.aisc.algoviz.submission.sandbox;

import java.io.IOException;

@FunctionalInterface
public interface ProcessExecutor {
    Process start(ProcessBuilder builder) throws IOException;
}
