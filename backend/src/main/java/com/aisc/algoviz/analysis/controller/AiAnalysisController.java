package com.aisc.algoviz.analysis.controller;

import com.aisc.algoviz.analysis.dto.AiAnalysisRequest;
import com.aisc.algoviz.analysis.dto.AiAnalysisResponse;
import com.aisc.algoviz.analysis.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/analyze", "/api/ai/analyze"})
@RequiredArgsConstructor
@Tag(name = "AI Code Analysis", description = "APIs phân tích độ phức tạp thời gian/không gian (Big-O) và tối ưu hóa mã nguồn")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping
    @Operation(
            summary = "Phân tích mã nguồn và giải thích thuật toán bằng AI",
            description = "Trả về phân tích Big-O (Time & Space complexity), phát hiện lỗi tiềm ẩn, gợi ý tối ưu và giải thích từng bước"
    )
    public ResponseEntity<AiAnalysisResponse> analyzeCode(@Valid @RequestBody AiAnalysisRequest request) {
        return ResponseEntity.ok(aiAnalysisService.analyzeCode(request));
    }
}
