package com.aisc.algoviz.problem.repository;

import com.aisc.algoviz.problem.entity.LegacyProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegacyProblemRepository extends JpaRepository<LegacyProblem, Long> {
    List<LegacyProblem> findByDifficulty(String difficulty);
    List<LegacyProblem> findByCategory(String category);
    List<LegacyProblem> findByDifficultyAndCategory(String difficulty, String category);
}
