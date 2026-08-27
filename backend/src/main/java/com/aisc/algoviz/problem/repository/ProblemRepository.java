package com.aisc.algoviz.problem.repository;

import com.aisc.algoviz.problem.entity.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Tầng Repository quản lý truy vấn bảng 'problems'.
 * Kế thừa JpaRepository để có sẵn các hàm CRUD cơ bản (save, findById, findAll, delete...).
 * Kế thừa JpaSpecificationExecutor để hỗ trợ lọc động nhiều điều kiện (search title, filter difficulty, tag).
 */
@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long>, JpaSpecificationExecutor<Problem> {

    /**
     * Tìm bài toán theo đường dẫn slug thân thiện (vd: 'two-sum')
     */
    Optional<Problem> findBySlug(String slug);
}
