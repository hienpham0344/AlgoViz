package com.aisc.algoviz.submission.repository;

import com.aisc.algoviz.submission.entity.Submission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    @Query("SELECT s FROM Submission s JOIN FETCH s.problem JOIN FETCH s.user WHERE s.user.id = :userId ORDER BY s.submittedAt DESC")
    List<Submission> findByUserIdOrderBySubmittedAtDesc(@Param("userId") Long userId);

    @Query(value = "SELECT s FROM Submission s JOIN FETCH s.problem JOIN FETCH s.user WHERE s.user.id = :userId ORDER BY s.submittedAt DESC",
           countQuery = "SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId")
    Page<Submission> findByUserIdOrderBySubmittedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT s FROM Submission s JOIN FETCH s.problem JOIN FETCH s.user WHERE s.user.id = :userId AND s.problem.id = :problemId ORDER BY s.submittedAt DESC")
    List<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(@Param("userId") Long userId, @Param("problemId") Long problemId);

    @Query(value = "SELECT s FROM Submission s JOIN FETCH s.problem JOIN FETCH s.user WHERE s.user.id = :userId AND s.problem.id = :problemId ORDER BY s.submittedAt DESC",
           countQuery = "SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.problem.id = :problemId")
    Page<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(@Param("userId") Long userId, @Param("problemId") Long problemId, Pageable pageable);

    List<Submission> findByProblemIdOrderBySubmittedAtDesc(Long problemId);

    Boolean existsByUserIdAndProblemIdAndStatus(Long userId, Long problemId, String status);

    long countByProblemId(Long problemId);

    long countByProblemIdAndStatus(Long problemId, String status);

    @Query("SELECT s.problem.id as problemId, COUNT(s) as totalCount, " +
           "SUM(CASE WHEN s.status = 'Accepted' THEN 1L ELSE 0L END) as acceptedCount " +
           "FROM Submission s GROUP BY s.problem.id")
    List<ProblemSubmissionStats> findSubmissionStatsGroupedByProblem();
}
