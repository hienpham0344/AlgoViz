package com.aisc.algoviz.problem.dto.request;

import com.aisc.algoviz.problem.entity.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO đóng gói dữ liệu yêu cầu cập nhật bài toán thuật toán (PUT /api/v1/problems/{id}).
 * Hỗ trợ cập nhật từng phần thông tin.
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa dữ liệu yêu cầu cập nhật bài toán thuật toán")
public class UpdateProblemRequestDto {

    @Schema(description = "Mã ID LeetCode cập nhật", example = "1")
    private Integer leetcodeId;

    @Size(max = 255, message = "Tiêu đề bài toán không vượt quá 255 ký tự")
    @Schema(description = "Tiêu đề mới của bài toán", example = "Two Sum (Updated)")
    private String title;

    @Size(max = 255, message = "Slug định tuyến không vượt quá 255 ký tự")
    @Schema(description = "Slug mới (nếu để trống hệ thống tự cập nhật từ tiêu đề mới)", example = "two-sum-updated")
    private String slug;

    @Schema(description = "Mức độ khó cập nhật (EASY, MEDIUM, HARD)", example = "EASY")
    private Difficulty difficulty;

    @Schema(description = "Nội dung mô tả đề bài mới (định dạng Markdown)")
    private String description;

    @Schema(description = "Danh sách các tags thuật toán mới")
    private List<String> patternTags;
}
