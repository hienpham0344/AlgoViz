export function withLearningProgress(courses, progress) {
  const completed = new Set(progress.map((item) => `${item.courseId}:${item.lessonId}`));
  return courses.map((course) => {
    const lessons = course.lessons.map((lesson) => ({ ...lesson,
      status: completed.has(`${course.id}:${lesson.id}`) ? "completed" : "not-started",
    }));
    const completedLessons = lessons.filter((lesson) => lesson.status === "completed").length;
    return { ...course, lessons, completedLessons, totalLessons: lessons.length,
      enrolled: completedLessons > 0, progress: lessons.length ? Math.round(completedLessons / lessons.length * 100) : 0,
    };
  });
}
