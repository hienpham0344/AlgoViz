package com.aisc.algoviz.problem.controller;

import com.aisc.algoviz.common.dto.ApiResponse;
import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.entity.Difficulty;
import com.aisc.algoviz.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tiếp nhận các yêu cầu HTTP liên quan đến danh mục bài toán (Problem Management).
 * Cung cấp các endpoint cho Frontend hiển thị danh sách bài toán (màn /problems) và chi tiết (màn /problems/:id).
 */
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Tag(name = "Problem Management", description = "Các API tra cứu và quản lý bài toán thuật toán")
public class ProblemController {

    private final ProblemService problemService;

    /**
     * Lấy danh sách bài toán có hỗ trợ phân trang và tìm kiếm / lọc.
     * Ví dụ: GET /api/v1/problems?page=0&size=10&difficulty=EASY&search=two sum
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách bài toán (Phân trang & Lọc)",
            description = "Trả về danh sách tóm tắt các bài toán, hỗ trợ lọc theo độ khó, tag và từ khóa tìm kiếm."
    )
    public ResponseEntity<ApiResponse<PageResponse<ProblemSummaryDto>>> getProblems(
            @Parameter(description = "Chỉ số trang bắt đầu từ 0 (mặc định: 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Số lượng bài toán trên một trang (mặc định: 10)")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Lọc theo độ khó (EASY, MEDIUM, HARD)")
            @RequestParam(required = false) Difficulty difficulty,

            @Parameter(description = "Lọc theo Tag thuật toán (vd: Array, Hash Table, Dynamic Programming)")
            @RequestParam(required = false) String tag,

            @Parameter(description = "Tìm kiếm từ khóa trong tiêu đề bài toán")
            @RequestParam(required = false) String search,

            @Parameter(description = "Tên trường dùng để sắp xếp (mặc định: id)")
            @RequestParam(defaultValue = "id") String sortBy,

            @Parameter(description = "Chiều sắp xếp: 'asc' hoặc 'desc' (mặc định: asc)")
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        PageResponse<ProblemSummaryDto> result = problemService.getProblems(
                page, size, difficulty, tag, search, sortBy, sortDirection
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Lấy thông tin chi tiết bài toán theo ID (bao gồm mô tả đầy đủ và danh sách bài giải mẫu).
     * Ví dụ: GET /api/v1/problems/1
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết bài toán theo ID",
            description = "Trả về đầy đủ mô tả đề bài, ví dụ, ràng buộc và các giải pháp tham khảo (Reference Solutions)."
    )
    public ResponseEntity<ApiResponse<ProblemDetailDto>> getProblemById(
            @Parameter(description = "ID của bài toán", example = "1")
            @PathVariable Long id
    ) {
        ProblemDetailDto problem = problemService.getProblemById(id);
        return ResponseEntity.ok(ApiResponse.success(problem));
    }

    /**
     * Lấy thông tin chi tiết bài toán theo Slug (vd: 'two-sum').
     * Ví dụ: GET /api/v1/problems/slug/two-sum
     */
    @GetMapping("/slug/{slug}")
    @Operation(
            summary = "Lấy chi tiết bài toán theo Slug URL",
            description = "Hỗ trợ định tuyến URL thân thiện cho Frontend."
    )
    public ResponseEntity<ApiResponse<ProblemDetailDto>> getProblemBySlug(
            @Parameter(description = "Slug của bài toán (vd: two-sum)", example = "two-sum")
            @PathVariable String slug
    ) {
        ProblemDetailDto problem = problemService.getProblemBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(problem));
    }
}
