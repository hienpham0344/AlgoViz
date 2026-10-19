package com.aisc.algoviz.submission.sandbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SandboxManagerTest {

    @Test
    void selectsDockerWhenAvailable() {
        DockerSandboxProperties props = new DockerSandboxProperties();
        props.setEnabled(true);
        DockerJavaSandboxStrategy dockerStrategy = new DockerJavaSandboxStrategy(props);
        dockerStrategy.setDockerAvailableCached(true);

        NativeSandboxStrategy nativeStrategy = new NativeSandboxStrategy();

        SandboxManager manager = new SandboxManager(List.of(dockerStrategy, nativeStrategy));

        var selected = manager.getAvailableStrategy("java");
        assertTrue(selected.isPresent());
        assertEquals("DOCKER", selected.get().getStrategyName());
    }

    @Test
    void fallsBackToNativeWhenDockerUnavailable() {
        DockerSandboxProperties props = new DockerSandboxProperties();
        props.setEnabled(false); // Docker disabled
        DockerJavaSandboxStrategy dockerStrategy = new DockerJavaSandboxStrategy(props);

        NativeSandboxStrategy nativeStrategy = new NativeSandboxStrategy();

        SandboxManager manager = new SandboxManager(List.of(dockerStrategy, nativeStrategy));

        var selected = manager.getAvailableStrategy("java");
        assertTrue(selected.isPresent());
        assertEquals("NATIVE", selected.get().getStrategyName());
    }

    @Test
    void routesNonJavaLanguagesToNative() {
        DockerSandboxProperties props = new DockerSandboxProperties();
        props.setEnabled(true);
        DockerJavaSandboxStrategy dockerStrategy = new DockerJavaSandboxStrategy(props);
        dockerStrategy.setDockerAvailableCached(true);

        NativeSandboxStrategy nativeStrategy = new NativeSandboxStrategy();

        SandboxManager manager = new SandboxManager(List.of(dockerStrategy, nativeStrategy));

        var pythonStrategy = manager.getAvailableStrategy("python");
        assertTrue(pythonStrategy.isPresent());
        assertEquals("NATIVE", pythonStrategy.get().getStrategyName());

        var cppStrategy = manager.getAvailableStrategy("cpp");
        assertTrue(cppStrategy.isPresent());
        assertEquals("NATIVE", cppStrategy.get().getStrategyName());
    }

    @Test
    void throwsWhenNoStrategyAvailable(@TempDir Path tempDir) {
        SandboxManager emptyManager = new SandboxManager(Collections.emptyList());

        SandboxExecutionRequest request = SandboxExecutionRequest.builder()
                .language("java")
                .workDirectory(tempDir)
                .sourceFileName("Main.java")
                .timeoutSeconds(5)
                .build();

        assertThrows(IllegalArgumentException.class, () -> emptyManager.execute(request));
    }
}
