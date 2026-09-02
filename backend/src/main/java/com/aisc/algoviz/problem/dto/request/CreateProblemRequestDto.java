package com.aisc.algoviz.problem.dto.request;

import com.aisc.algoviz.problem.enums.Difficulty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO đóng gói dữ liệu yêu cầu tạo mới bài toán thuật toán từ Client.
 * Tích hợp Bean Validation tự động kiểm tra tính hợp lệ dữ liệu.
 *
 * @author AlgoViz Development Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng chứa dữ liệu yêu cầu tạo mới bài toán thuật toán")
public class CreateProblemRequestDto {

    @Schema(description = "Mã ID bài toán tương ứng trên LeetCode (nếu có)", example = "1")
    private Integer leetcodeId;

    @NotBlank(message = "Tiêu đề bài toán không được để trống")
    @Size(max = 255, message = "Tiêu đề bài toán không vượt quá 255 ký tự")
    @Schema(description = "Tiêu đề chính của bài toán", example = "Two Sum")
    private String title;

    @Size(max = 255, message = "Slug định tuyến không vượt quá 255 ký tự")
    @Schema(description = "Đường dẫn Slug (nếu để trống hệ thống tự tạo từ tiêu đề)", example = "two-sum")
    private String slug;

    @NotNull(message = "Độ khó bài toán không được để trống")
    @Schema(description = "Mức độ khó của bài toán (EASY, MEDIUM, HARD)", example = "EASY")
    private Difficulty difficulty;

    @Schema(description = "Nội dung mô tả đề bài đầy đủ (định dạng Markdown)", example = "Given an array of integers nums...")
    private String description;

    @Builder.Default
    @Schema(description = "Danh sách các tags thuật toán liên quan", example = "[\"Array\", \"Hash Table\"]")
    private List<String> patternTags = new ArrayList<>();

    @Valid
    @Builder.Default
    @Schema(description = "Danh sách các bài giải mẫu kèm theo bài toán")
    private List<CreateSolutionRequestDto> solutions = new ArrayList<>();
}
