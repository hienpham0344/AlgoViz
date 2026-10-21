package com.aisc.algoviz.submission.controller;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.submission.dto.ExecutionResultDto;
import com.aisc.algoviz.submission.dto.RunCodeRequest;
import com.aisc.algoviz.submission.dto.SubmissionRequest;
import com.aisc.algoviz.submission.dto.SubmissionResponse;
import com.aisc.algoviz.submission.service.CodeExecutionService;
import com.aisc.algoviz.submission.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions & Execution", description = "APIs thực thi code trực tuyến, chấm bài qua Sandbox và quản lý lịch sử submit")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final CodeExecutionService codeExecutionService;

    @PostMapping("/run")
    @Operation(
            summary = "Chạy thử nghiệm code với input tùy biến (Run Code)",
            description = "Thực thi code ngay lập tức trong sandbox an toàn với input do người dùng chỉ định"
    )
    public ResponseEntity<ExecutionResultDto> runCode(@Valid @RequestBody RunCodeRequest request) {
        return ResponseEntity.ok(codeExecutionService.runCodeCustomInput(
                request.getLanguage(),
                request.getCode(),
                request.getCustomInput()
        ));
    }

    @PostMapping("/submit")
    @Operation(
            summary = "Nộp bài giải và chấm điểm tự động (Submit Solution)",
            description = "Chấm toàn bộ test cases của bài toán trong Sandbox, lưu lịch sử nộp bài và cộng điểm nếu Accepted",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<SubmissionResponse> submitCode(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(submissionService.submitCode(request));
    }

    @GetMapping("/history")
    @Operation(
            summary = "Lấy lịch sử nộp bài của người dùng hiện tại (Hỗ trợ phân trang)",
            description = "Nếu truyền page và size sẽ phân trang kết quả; nếu không truyền sẽ trả về toàn bộ danh sách",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<?> getUserHistory(
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(required = false) Integer page,
            @Parameter(description = "Kích thước mỗi trang")
            @RequestParam(required = false) Integer size) {

        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)), Sort.by(Sort.Direction.DESC, "submittedAt"));
            PageResponse<SubmissionResponse> paged = submissionService.getUserSubmissionHistoryPaged(pageable);
            return ResponseEntity.ok(paged);
        }

        return ResponseEntity.ok(submissionService.getUserSubmissionHistory());
    }

    @GetMapping("/problem/{problemId}")
    @Operation(
            summary = "Lấy lịch sử các lần nộp bài cho một bài toán cụ thể",
            description = "Trả về danh sách tất cả các lần nộp của người dùng đăng nhập cho bài toán theo problemId hoặc leetcodeId",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<List<SubmissionResponse>> getUserSubmissionsForProblem(
            @Parameter(description = "ID hoặc LeetCode ID của bài toán", example = "1")
            @PathVariable Long problemId) {
        return ResponseEntity.ok(submissionService.getUserSubmissionsForProblem(problemId));
    }
}
