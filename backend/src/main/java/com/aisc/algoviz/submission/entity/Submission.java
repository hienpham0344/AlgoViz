package com.aisc.algoviz.submission.entity;

import com.aisc.algoviz.problem.entity.LegacyProblem;
import com.aisc.algoviz.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private LegacyProblem problem;

    @Column(nullable = false)
    private String language; // "python", "cpp", "java", "javascript"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String code;

    private String status; // "Accepted", "Wrong Answer", "Time Limit Exceeded", "Runtime Error", "Compilation Error"

    private Double executionTimeMs;

    private Double memoryUsageMb;

    private Integer passedTestCases;

    private Integer totalTestCases;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "submitted_at", updatable = false)
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        this.submittedAt = LocalDateTime.now();
    }
}
