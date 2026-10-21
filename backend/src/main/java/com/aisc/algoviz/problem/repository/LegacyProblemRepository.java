package com.aisc.algoviz.problem.repository;

import com.aisc.algoviz.problem.entity.LegacyProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegacyProblemRepository extends JpaRepository<LegacyProblem, Long>, JpaSpecificationExecutor<LegacyProblem> {
    List<LegacyProblem> findByDifficulty(String difficulty);
    List<LegacyProblem> findByCategory(String category);
    List<LegacyProblem> findByDifficultyAndCategory(String difficulty, String category);

    Page<LegacyProblem> findByDifficulty(String difficulty, Pageable pageable);
    Page<LegacyProblem> findByCategory(String category, Pageable pageable);
    Page<LegacyProblem> findByDifficultyAndCategory(String difficulty, String category, Pageable pageable);

    java.util.Optional<LegacyProblem> findByLeetcodeId(Integer leetcodeId);
    java.util.Optional<LegacyProblem> findByTitle(String title);
}
