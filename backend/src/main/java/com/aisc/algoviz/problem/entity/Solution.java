package com.aisc.algoviz.problem.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity ánh xạ bảng 'solutions' trong PostgreSQL.
 * Lưu trữ mã nguồn mẫu chuẩn (Reference Solution), giải thích và độ phức tạp thuật toán.
 */
@Entity
@Table(name = "solutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "problem_id", nullable = false)
    private Problem problem;

    @Column(name = "pattern_id")
    private Long patternId;

    @Column(name = "code_snippet", columnDefinition = "TEXT", nullable = false)
    private String codeSnippet;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "time_complexity", length = 50)
    private String timeComplexity;

    @Column(name = "space_complexity", length = 50)
    private String spaceComplexity;

    @Column(length = 20)
    @Builder.Default
    private String language = "Java";
}
