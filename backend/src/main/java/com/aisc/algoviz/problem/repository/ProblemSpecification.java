package com.aisc.algoviz.problem.repository;

import com.aisc.algoviz.problem.dto.request.ProblemFilterRequest;
import com.aisc.algoviz.problem.entity.Problem;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Lớp tiện ích hỗ trợ xây dựng truy vấn động JPA Specification cho entity Problem.
 * Tách biệt hoàn toàn logic tạo Predicates SQL ra khỏi tầng Service.
 *
 * @author AlgoViz Development Team
 */
public class ProblemSpecification {

    private ProblemSpecification() {
        // Class tiện ích không khởi tạo instance
    }

    /**
     * Tạo đối tượng Specification tổng hợp dựa trên các tiêu chí lọc truyền vào từ ProblemFilterRequest.
     *
     * @param request Đội tượng chứa các tham số lọc (difficulty, search, tag...)
     * @return JPA Specification đại diện cho câu lệnh WHERE trong SQL
     */
    public static Specification<Problem> buildSpecification(ProblemFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Lọc chính xác theo mức độ khó (EASY, MEDIUM, HARD)
            if (request.getDifficulty() != null) {
                predicates.add(criteriaBuilder.equal(root.get("difficulty"), request.getDifficulty()));
            }

            // 2. Tìm kiếm từ khóa xuất hiện trong tiêu đề (Không phân biệt chữ hoa / chữ thường)
            if (request.getSearch() != null && !request.getSearch().trim().isEmpty()) {
                String searchPattern = "%" + request.getSearch().trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), searchPattern));
            }

            // 3. Lọc theo Tag thuật toán (Thuộc danh sách ElementCollection)
            if (request.getTag() != null && !request.getTag().trim().isEmpty()) {
                predicates.add(criteriaBuilder.isMember(request.getTag().trim(), root.get("patternTags")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
