package com.aisc.algoviz.problem.dto;

import com.aisc.algoviz.problem.entity.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO tóm tắt thông tin bài toán phục vụ màn hình danh sách (/problems).
 * Tối ưu kích thước payload truyền tải qua mạng bằng cách không tải toàn bộ mô tả HTML dài.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSummaryDto {

    private Long id;
    private Integer leetcodeId;
    private String title;
    private String slug;
    private Difficulty difficulty;
    private List<String> patternTags;
}
