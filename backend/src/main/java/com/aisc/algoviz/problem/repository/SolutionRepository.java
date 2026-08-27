package com.aisc.algoviz.problem.repository;

import com.aisc.algoviz.problem.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Tầng Repository quản lý truy vấn bảng 'solutions'.
 */
@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {

    /**
     * Tìm tất cả bài giải mẫu thuộc về một bài toán cụ thể theo Problem ID
     */
    List<Solution> findByProblemId(Long problemId);
}
