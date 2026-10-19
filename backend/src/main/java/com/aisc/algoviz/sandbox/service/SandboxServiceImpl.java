package com.aisc.algoviz.sandbox.service;

import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;
import com.aisc.algoviz.sandbox.enums.ExecutionStatus;
import com.aisc.algoviz.sandbox.runtime.SandboxRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Compilation and execution always take place in a disposable restricted container. */
@Service
@RequiredArgsConstructor
public class SandboxServiceImpl implements SandboxService {
    private final SandboxRuntime runtime;

    @Override
    public ExecutionResultDto executeCode(ExecutionRequestDto request) {
        try {
            var run = runtime.execute("java", request.getCode(), request.getInput(),
                    request.getMainClassName(), request.getTimeLimitMs(),
                    request.getMemoryLimitMb(), request.getStackLimitKb());
            ExecutionStatus status;
            String error = run.stderr();
            if (run.outputExceeded()) {
                status = ExecutionStatus.OUTPUT_LIMIT_EXCEEDED;
                error = "Output limit exceeded (1 MiB per stream)";
            } else if (run.timedOut() || run.exitCode() == 124 || run.exitCode() == 137) {
                status = ExecutionStatus.TIME_LIMIT_EXCEEDED;
                error = "Execution exceeded its time or memory limit";
            } else if (run.exitCode() == 65 && error.contains("ALGOVIZ_COMPILATION_ERROR")) {
                status = ExecutionStatus.COMPILATION_ERROR;
            } else status = run.exitCode() == 0 ? ExecutionStatus.SUCCESS : ExecutionStatus.RUNTIME_ERROR;
            return ExecutionResultDto.builder().status(status).stdout(run.stdout().strip())
                    .stderr(error).compilerErrorDetails(status == ExecutionStatus.COMPILATION_ERROR ? error : null)
                    .executionTimeMs(run.elapsedMs()).exitCode(run.exitCode()).build();
        } catch (IllegalArgumentException ex) {
            return ExecutionResultDto.builder().status(ExecutionStatus.SECURITY_VIOLATION).stderr(ex.getMessage()).build();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ExecutionResultDto.builder().status(ExecutionStatus.SYSTEM_ERROR).stderr("Sandbox request interrupted").build();
        } catch (java.io.IOException ex) {
            return ExecutionResultDto.builder().status(ExecutionStatus.SYSTEM_ERROR).stderr(ex.getMessage()).build();
        }
    }
}
