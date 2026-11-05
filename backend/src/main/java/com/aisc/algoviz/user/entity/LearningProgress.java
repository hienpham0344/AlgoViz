package com.aisc.algoviz.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "learning_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "course_id", "lesson_id"}))
@Getter
@Setter
@NoArgsConstructor
public class LearningProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(name = "course_id", nullable = false, length = 100)
    private String courseId;
    @Column(name = "lesson_id", nullable = false)
    private Integer lessonId;
    private boolean completed;
}
