package com.aisc.algoviz.user.repository;

import com.aisc.algoviz.user.entity.LearningProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, Long> {
    List<LearningProgress> findByUserIdAndCompletedTrue(Long userId);
    Optional<LearningProgress> findByUserIdAndCourseIdAndLessonId(Long userId, String courseId, Integer lessonId);
}
