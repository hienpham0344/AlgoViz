package com.aisc.algoviz.problem.service;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.SolutionResponseDto;
import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.ProblemFilterRequest;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ (Business Logic) liên quan đến bài toán AlgoViz.
 */
public interface ProblemService {

    /**
     * Lấy danh sách bài toán có phân trang và bộ lọc linh hoạt qua ProblemFilterRequest.
     */
    PageResponse<ProblemSummaryDto> getProblems(ProblemFilterRequest filterRequest);

    /**
     * Lấy thông tin chi tiết bài toán theo ID (bao gồm danh sách bài giải mẫu).
     */
    ProblemDetailDto getProblemById(Long id);

    /**
     * Lấy thông tin chi tiết bài toán theo đường dẫn slug.
     */
    ProblemDetailDto getProblemBySlug(String slug);

    /**
     * Tạo mới một bài toán thuật toán.
     */
    ProblemDetailDto createProblem(CreateProblemRequestDto createDto);

    /**
     * Cập nhật thông tin bài toán theo ID.
     */
    ProblemDetailDto updateProblem(Long id, UpdateProblemRequestDto updateDto);

    /**
     * Xóa bài toán theo ID.
     */
    void deleteProblem(Long id);

    /**
     * Bổ sung một lời giải mẫu (Reference Solution) cho bài toán.
     */
    SolutionResponseDto addSolutionToProblem(Long problemId, CreateSolutionRequestDto solutionDto);
}
