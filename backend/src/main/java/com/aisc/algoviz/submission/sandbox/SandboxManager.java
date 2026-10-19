package com.aisc.algoviz.submission.sandbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SandboxManager {

    private final List<SandboxStrategy> strategies;

    public SandboxManager(List<SandboxStrategy> strategies) {
        this.strategies = strategies;
    }

    public SandboxExecutionResult execute(SandboxExecutionRequest request) throws IOException, InterruptedException {
        SandboxStrategy selected = getAvailableStrategy(request.getLanguage())
                .orElseThrow(() -> new IllegalArgumentException("No available sandbox strategy for language: " + request.getLanguage()));

        log.info("Executing [{}] submission using sandbox strategy: [{}]", request.getLanguage(), selected.getStrategyName());
        return selected.execute(request);
    }

    public Optional<SandboxStrategy> getAvailableStrategy(String language) {
        return strategies.stream()
                .filter(s -> s.supportsLanguage(language) && s.isAvailable())
                .findFirst();
    }

    public List<SandboxStrategy> getStrategies() {
        return strategies;
    }
}
