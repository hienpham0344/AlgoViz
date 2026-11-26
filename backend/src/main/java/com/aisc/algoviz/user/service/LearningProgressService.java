package com.aisc.algoviz.user.service;

import com.aisc.algoviz.user.dto.LearningProgressRequest;
import com.aisc.algoviz.user.entity.LearningProgress;
import com.aisc.algoviz.user.entity.User;
import com.aisc.algoviz.user.repository.LearningProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearningProgressService {
    private final LearningProgressRepository repository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProgress() {
        return repository.findByUserIdAndCompletedTrue(userService.getCurrentUser().getId()).stream()
                .map(item -> Map.<String, Object>of("courseId", item.getCourseId(), "lessonId", item.getLessonId()))
                .toList();
    }

    @Transactional
    public void saveProgress(LearningProgressRequest request) {
        User user = userService.getCurrentUser();
        LearningProgress progress = repository.findByUserIdAndCourseIdAndLessonId(user.getId(), request.getCourseId(), request.getLessonId())
                .orElseGet(() -> {
                    LearningProgress item = new LearningProgress();
                    item.setUser(user);
                    item.setCourseId(request.getCourseId());
                    item.setLessonId(request.getLessonId());
                    return item;
                });
        progress.setCompleted(request.getCompleted());
        repository.save(progress);
    }
}
