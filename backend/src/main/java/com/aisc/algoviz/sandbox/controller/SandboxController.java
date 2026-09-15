package com.aisc.algoviz.sandbox.controller;

import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;
import com.aisc.algoviz.sandbox.service.SandboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST API tiếp nhận yêu cầu chạy thử nghiệm mã nguồn Java trong Sandbox.
 */
@RestController
@RequestMapping("/api/v1/sandbox")
@RequiredArgsConstructor
@Tag(name = "Sandbox API", description = "Các API thực thi và kiểm thử mã nguồn cô lập")
public class SandboxController {

    private final SandboxService sandboxService;

    /**
     * Endpoint API thực thi mã nguồn trong môi trường Sandbox bằng ProcessBuilder.
     *
     * @param request DTO chứa thông tin mã nguồn, input và các cấu hình giới hạn
     * @return ExecutionResultDto Kết quả thực thi
     */
    @PostMapping("/execute")
    @Operation(summary = "Thực thi mã nguồn Java", description = "Chạy code trong môi trường Sandbox cô lập với ProcessBuilder, giới hạn tài nguyên và thời gian.")
    public ResponseEntity<ExecutionResultDto> executeCode(@RequestBody ExecutionRequestDto request) {
        ExecutionResultDto result = sandboxService.executeCode(request);
        return ResponseEntity.ok(result);
    }
}
