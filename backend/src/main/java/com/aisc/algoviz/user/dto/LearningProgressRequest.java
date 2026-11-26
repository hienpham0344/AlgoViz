package com.aisc.algoviz.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LearningProgressRequest {
    @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9-]+")
    private String courseId;
    @NotNull @Min(1) @Max(1000)
    private Integer lessonId;
    @NotNull
    private Boolean completed;
}
