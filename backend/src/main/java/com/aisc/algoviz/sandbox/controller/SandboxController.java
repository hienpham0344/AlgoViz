package com.aisc.algoviz.sandbox.controller;

import com.aisc.algoviz.sandbox.dto.ExecutionRequestDto;
import com.aisc.algoviz.sandbox.dto.ExecutionResultDto;
import com.aisc.algoviz.sandbox.service.SandboxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
     * Compile and execute one Java program inside a restricted, disposable container.
     *
     * @param request DTO chứa thông tin mã nguồn, input và các cấu hình giới hạn
     * @return ExecutionResultDto Kết quả thực thi
     */
    @PostMapping("/execute")
    @Operation(summary = "Thực thi mã nguồn Java", description = "Biên dịch và chạy Java trong container riêng, không có mạng hoặc quyền truy cập filesystem host.")
    public ResponseEntity<ExecutionResultDto> executeCode(@Valid @RequestBody ExecutionRequestDto request) {
        ExecutionResultDto result = sandboxService.executeCode(request);
        return ResponseEntity.ok(result);
    }
}
