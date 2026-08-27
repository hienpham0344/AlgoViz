package com.aisc.algoviz.problem.service;

import com.aisc.algoviz.common.dto.PageResponse;
import com.aisc.algoviz.problem.dto.ProblemDetailDto;
import com.aisc.algoviz.problem.dto.ProblemSummaryDto;
import com.aisc.algoviz.problem.entity.Difficulty;

/**
 * Interface định nghĩa các dịch vụ nghiệp vụ (Business Logic) liên quan đến bài toán AlgoViz.
 */
public interface ProblemService {

    /**
     * Lấy danh sách bài toán có phân trang và bộ lọc linh hoạt (độ khó, tags, tìm kiếm tiêu đề)
     */
    PageResponse<ProblemSummaryDto> getProblems(
            int page,
            int size,
            Difficulty difficulty,
            String tag,
            String search,
            String sortBy,
            String sortDirection
    );

    /**
     * Lấy thông tin chi tiết bài toán theo ID (bao gồm danh sách bài giải mẫu)
     */
    ProblemDetailDto getProblemById(Long id);

    /**
     * Lấy thông tin chi tiết bài toán theo đường dẫn slug
     */
    ProblemDetailDto getProblemBySlug(String slug);
}
