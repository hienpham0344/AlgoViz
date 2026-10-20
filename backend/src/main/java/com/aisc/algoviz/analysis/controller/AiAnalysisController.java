package com.aisc.algoviz.analysis.controller;

import com.aisc.algoviz.analysis.dto.AiAnalysisRequest;
import com.aisc.algoviz.analysis.dto.AiAnalysisResponse;
import com.aisc.algoviz.analysis.service.AiAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping
    public ResponseEntity<AiAnalysisResponse> analyzeCode(@Valid @RequestBody AiAnalysisRequest request) {
        return ResponseEntity.ok(aiAnalysisService.analyzeCode(request));
    }
}
