package com.aisc.algoviz.submission.repository;

/**
 * Projection interface for aggregated submission statistics grouped by problem.
 * Used to eliminate O(N+1) query loops when computing acceptance rates across problem sets.
 */
public interface ProblemSubmissionStats {
    Long getProblemId();
    Long getTotalCount();
    Long getAcceptedCount();
}
