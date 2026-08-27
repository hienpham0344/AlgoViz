package com.aisc.algoviz.problem.dto;

import com.aisc.algoviz.problem.entity.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO chi tiết đầy đủ bài toán phục vụ màn hình làm bài (/problems/:id).
 * Bao gồm mô tả đề bài đầy đủ, ràng buộc, và danh sách các giải pháp mẫu (Reference Solutions).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetailDto {

    private Long id;
    private Integer leetcodeId;
    private String title;
    private String slug;
    private Difficulty difficulty;
    private String description;
    private List<String> patternTags;
    private List<SolutionResponseDto> solutions;
}
