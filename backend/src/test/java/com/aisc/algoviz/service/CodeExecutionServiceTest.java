package com.aisc.algoviz.service;

import com.aisc.algoviz.sandbox.runtime.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CodeExecutionServiceTest {
    private final SandboxRuntime runtime = mock(SandboxRuntime.class);
    private final CodeExecutionService service = new CodeExecutionService(runtime);

    @Test void returnsContainerOutput() throws Exception {
        when(runtime.execute(anyString(), anyString(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new ContainerSandbox.Result(0,"EOF\n","",false,false,100));
        var result = service.runCodeCustomInput("java", "class Main {}", "");
        assertEquals("Success", result.getStatus());
        assertEquals("EOF", result.getOutput());
    }

    @Test void dockerFailureNeverFallsBackToHost() throws Exception {
        when(runtime.execute(anyString(), anyString(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
                .thenThrow(new java.io.IOException("Sandbox unavailable"));
        var result = service.runCodeCustomInput("java", "class Main {}", "");
        assertEquals("Runtime Error", result.getStatus());
        assertEquals("Sandbox unavailable", result.getErrorMessage());
        verify(runtime, times(1)).execute(anyString(), anyString(), anyString(), anyString(), anyLong(), anyInt(), anyInt());
    }

    @Test void outputLimitIsNotReportedAsSuccess() throws Exception {
        when(runtime.execute(anyString(), anyString(), anyString(), anyString(), anyLong(), anyInt(), anyInt()))
                .thenReturn(new ContainerSandbox.Result(0,"truncated","",false,true,100));
        var result = service.runCodeCustomInput("java", "class Main {}", "");
        assertEquals("Runtime Error", result.getStatus());
        assertTrue(result.getErrorMessage().contains("Output limit"));
    }
}
