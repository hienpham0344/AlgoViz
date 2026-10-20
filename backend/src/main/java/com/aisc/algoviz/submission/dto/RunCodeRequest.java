package com.aisc.algoviz.submission.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RunCodeRequest {

    @NotBlank(message = "Language is required")
    private String language; // "python", "cpp", "java", "javascript"

    @NotBlank(message = "Code is required")
    private String code;

    private String customInput;
}
