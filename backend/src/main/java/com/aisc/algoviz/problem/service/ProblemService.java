package com.aisc.algoviz.problem.service;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.request.CreateProblemRequestDto;
import com.aisc.algoviz.problem.dto.request.CreateSolutionRequestDto;
import com.aisc.algoviz.problem.dto.request.ProblemFilterRequest;
import com.aisc.algoviz.problem.dto.request.UpdateProblemRequestDto;
import com.aisc.algoviz.problem.dto.response.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.response.ProblemSummaryDto;
import com.aisc.algoviz.problem.dto.response.SolutionResponseDto;

/**
 * Interface định nghĩa các hợp đồng nghiệp vụ quản lý bài toán thuật toán.
 */
public interface ProblemService {

    /**
     * Lấy danh sách bài toán có phân trang, sắp xếp và lọc.
     */
    PageResponse<ProblemSummaryDto> getProblems(ProblemFilterRequest filterRequest);

    /**
     * Lấy chi tiết bài toán theo ID.
     */
    ProblemDetailDto getProblemById(Long id);

    /**
     * Lấy chi tiết bài toán theo Slug URL.
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
     * Thêm lời giải mẫu cho một bài toán.
     */
    SolutionResponseDto addSolutionToProblem(Long problemId, CreateSolutionRequestDto solutionDto);
}
