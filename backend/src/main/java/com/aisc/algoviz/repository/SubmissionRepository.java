package com.aisc.algoviz.repository;

import com.aisc.algoviz.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);
    List<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(Long userId, Long problemId);
    List<Submission> findByProblemIdOrderBySubmittedAtDesc(Long problemId);
    Optional<Submission> findByIdAndUserId(Long id, Long userId);
    Boolean existsByUserIdAndProblemIdAndStatus(Long userId, Long problemId, String status);
    long countByProblemId(Long problemId);
    long countByProblemIdAndStatus(Long problemId, String status);
}
