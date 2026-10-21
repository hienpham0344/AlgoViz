package com.aisc.algoviz.problem.controller;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemDto;
import com.aisc.algoviz.problem.service.LegacyProblemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/problems")
@RequiredArgsConstructor
@Tag(name = "Problems (Core & Studio)", description = "APIs phục vụ danh sách bài tập, visualizer studio và chi tiết bài toán")
public class LegacyProblemController {

    private final LegacyProblemService legacyProblemService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách bài toán (Hỗ trợ cả List đầy đủ lẫn Phân trang)",
            description = "Nếu truyền page và size sẽ trả về PageResponse có phân trang. Nếu không truyền page/size sẽ trả về toàn bộ danh sách phù hợp với frontend visualizer."
    )
    public ResponseEntity<?> getProblems(
            @Parameter(description = "Lọc theo độ khó: Easy, Medium, Hard")
            @RequestParam(required = false) String difficulty,
            @Parameter(description = "Lọc theo danh mục/chủ đề thuật toán")
            @RequestParam(required = false) String category,
            @Parameter(description = "Tìm kiếm từ khóa theo tiêu đề bài toán")
            @RequestParam(required = false) String search,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Kích thước mỗi trang")
            @RequestParam(required = false) Integer size,
            @Parameter(description = "Trường sắp xếp (mặc định id)")
            @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Hướng sắp xếp: asc hoặc desc")
            @RequestParam(defaultValue = "asc") String sortDirection) {

        if (page != null && size != null) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(direction, sortBy));
            PageResponse<ProblemDto> pagedResult = legacyProblemService.getProblemsPaged(difficulty, category, search, pageable);
            return ResponseEntity.ok(pagedResult);
        }

        List<ProblemDto> allProblems = legacyProblemService.getAllProblems(difficulty, category);
        return ResponseEntity.ok(allProblems);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết bài toán theo ID hoặc LeetCode ID",
            description = "Trả về đầy đủ mô tả, ràng buộc, starter code (Python, Java, C++, JS), test cases mẫu và lời giải tham khảo"
    )
    public ResponseEntity<ProblemDetailDto> getProblemById(
            @Parameter(description = "ID hoặc LeetCode ID của bài toán", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(legacyProblemService.getProblemById(id));
    }
}
