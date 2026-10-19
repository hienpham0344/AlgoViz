package com.aisc.algoviz.sandbox.service;

import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.enums.ExecutionStatus;
import com.aisc.algoviz.sandbox.runtime.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class SandboxServiceTest {
    private final SandboxRuntime runtime = mock(SandboxRuntime.class);
    private final SandboxService service = new SandboxServiceImpl(runtime);

    @Test void compilationErrorsComeFromContainer() throws Exception {
        when(runtime.execute(anyString(), anyString(), nullable(String.class), anyString(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new ContainerSandbox.Result(65,"","javac error\nALGOVIZ_COMPILATION_ERROR",false,false,100));
        var response = service.executeCode(ExecutionRequestDto.builder().code("broken Java").build());
        assertEquals(ExecutionStatus.COMPILATION_ERROR, response.getStatus());
        assertNotNull(response.getCompilerErrorDetails());
    }

    @Test void serverPolicyRejectsClassPathTraversalAndExcessiveResources() {
        var real = new SandboxRuntime("nonexistent-docker", "algoviz-runner:local");
        var s = new SandboxServiceImpl(real);
        assertEquals(ExecutionStatus.SECURITY_VIOLATION, s.executeCode(ExecutionRequestDto.builder()
                .code("class Main {}").mainClassName("../Main").build()).getStatus());
        assertEquals(ExecutionStatus.SECURITY_VIOLATION, s.executeCode(ExecutionRequestDto.builder()
                .code("class Main {}").memoryLimitMb(100000).build()).getStatus());
    }
}
