package com.aisc.algoviz.analysis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiAnalysisRequest {

    @NotBlank(message = "Code is required")
    private String code;

    private String language;

    private String problemContext;
}
