package com.aisc.algoviz.submission.sandbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "algoviz.sandbox.docker")
public class DockerSandboxProperties {
    private boolean enabled = false;
    private String dockerCommand = "docker";
    private String image = "eclipse-temurin:21-alpine";
    private String memory = "256m";
    private String cpus = "1.0";
    private int pidsLimit = 64;
    private long timeoutKillSeconds = 3;
}
