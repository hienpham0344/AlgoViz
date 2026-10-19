package com.aisc.algoviz.repository;

import com.aisc.algoviz.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {
    List<Problem> findByDifficulty(String difficulty);
    List<Problem> findByCategory(String category);
    List<Problem> findByDifficultyAndCategory(String difficulty, String category);
}
