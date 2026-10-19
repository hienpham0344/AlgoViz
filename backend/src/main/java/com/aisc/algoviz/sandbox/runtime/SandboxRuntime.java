package com.aisc.algoviz.sandbox.runtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class SandboxRuntime {
    private final ContainerSandbox sandbox;

    public SandboxRuntime(@Value("${algoviz.sandbox.docker-command:docker}") String docker,
                          @Value("${algoviz.sandbox.image:algoviz-runner:local}") String image) {
        this.sandbox = new ContainerSandbox(docker, image, 1_048_576);
    }

    public ContainerSandbox.Result execute(String language, String code, String input,
                                           String className, long timeMs, int heapMb, int stackKb)
            throws IOException, InterruptedException {
        if (code == null || code.isBlank() || code.getBytes(StandardCharsets.UTF_8).length > 200_000)
            throw new IllegalArgumentException("Source must contain between 1 and 200,000 bytes");
        if (timeMs < 100 || timeMs > 10_000 || heapMb < 16 || heapMb > 128 || stackKb < 256 || stackKb > 2048)
            throw new IllegalArgumentException("Execution limits exceed server policy");
        // Validate before deriving any path or command argument from caller-supplied class names.
        if (className == null || !className.matches("[A-Za-z_$][A-Za-z0-9_$]*"))
            throw new IllegalArgumentException("mainClassName must be a simple Java class name");
        String filename = switch (language) {
            case "java", "java-source" -> className + ".java";
            case "python", "py" -> "script.py";
            case "javascript", "js" -> "script.js";
            case "cpp", "c++" -> "main.cpp";
            default -> throw new IllegalArgumentException("Unsupported execution language: " + language);
        };
        return sandbox.run(Map.of(filename, code, "stdin.txt", input == null ? "" : input),
                List.of("run", language, filename, className, Long.toString(timeMs),
                        Integer.toString(heapMb), Integer.toString(stackKb)),
                Duration.ofMillis(timeMs + 15_000), 256);
    }
}
