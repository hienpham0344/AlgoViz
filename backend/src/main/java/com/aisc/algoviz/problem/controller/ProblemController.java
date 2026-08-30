package com.aisc.algoviz.problem.controller;

import com.aisc.algoviz.common.dto.ApiResponse;
import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.SolutionResponseDto;
import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.ProblemFilterRequest;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;
import com.aisc.algoviz.problem.service.ProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller tiếp nhận các yêu cầu HTTP liên quan đến bài toán (Problem Management).
 * Cung cấp các endpoint cho Frontend hiển thị danh sách, chi tiết và quản lý dữ liệu bài toán.
 */
@RestController
@RequestMapping("/api/v1/problems")
@RequiredArgsConstructor
@Tag(name = "Problem Management", description = "Các API tra cứu và quản lý bài toán thuật toán")
public class ProblemController {

    private final ProblemService problemService;

    /**
     * Lấy danh sách bài toán có hỗ trợ phân trang, sắp xếp và lọc.
     * Ví dụ: GET /api/v1/problems?page=0&size=10&difficulty=EASY&search=two sum
     */
    @GetMapping
    @Operation(
            summary = "Lấy danh sách bài toán (Phân trang & Lọc)",
            description = "Trả về danh sách tóm tắt các bài toán, hỗ trợ lọc theo độ khó, tag và từ khóa tìm kiếm."
    )
    public ResponseEntity<ApiResponse<PageResponse<ProblemSummaryDto>>> getProblems(
            @Valid ProblemFilterRequest filterRequest
    ) {
        PageResponse<ProblemSummaryDto> result = problemService.getProblems(filterRequest);
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

    /**
     * Tạo mới một bài toán thuật toán.
     * Ví dụ: POST /api/v1/problems
     */
    @PostMapping
    @Operation(
            summary = "Tạo mới bài toán thuật toán",
            description = "Thêm một bài toán mới kèm mô tả và các giải pháp tham khảo vào hệ thống."
    )
    public ResponseEntity<ApiResponse<ProblemDetailDto>> createProblem(
            @Valid @RequestBody CreateProblemRequestDto request
    ) {
        ProblemDetailDto createdProblem = problemService.createProblem(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bài toán mới thành công", createdProblem));
    }

    /**
     * Cập nhật thông tin bài toán theo ID.
     * Ví dụ: PUT /api/v1/problems/1
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Cập nhật bài toán theo ID",
            description = "Chỉnh sửa các trường thông tin tiêu đề, độ khó, mô tả hoặc tags của bài toán."
    )
    public ResponseEntity<ApiResponse<ProblemDetailDto>> updateProblem(
            @Parameter(description = "ID của bài toán cần cập nhật", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateProblemRequestDto request
    ) {
        ProblemDetailDto updatedProblem = problemService.updateProblem(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật bài toán thành công", updatedProblem));
    }

    /**
     * Xóa bài toán theo ID.
     * Ví dụ: DELETE /api/v1/problems/1
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Xóa bài toán theo ID",
            description = "Xóa bài toán cùng toàn bộ lời giải thuộc bài toán khỏi hệ thống."
    )
    public ResponseEntity<ApiResponse<Void>> deleteProblem(
            @Parameter(description = "ID của bài toán cần xóa", example = "1")
            @PathVariable Long id
    ) {
        problemService.deleteProblem(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa bài toán thành công", null));
    }

    /**
     * Thêm lời giải mẫu cho một bài toán đã tồn tại.
     * Ví dụ: POST /api/v1/problems/1/solutions
     */
    @PostMapping("/{id}/solutions")
    @Operation(
            summary = "Thêm lời giải mẫu cho bài toán",
            description = "Bổ sung một lời giải thuật toán tham khảo vào bài toán theo ID."
    )
    public ResponseEntity<ApiResponse<SolutionResponseDto>> addSolution(
            @Parameter(description = "ID của bài toán", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody CreateSolutionRequestDto request
    ) {
        SolutionResponseDto solution = problemService.addSolutionToProblem(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm lời giải mẫu thành công", solution));
    }
}
