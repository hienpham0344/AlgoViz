export function withLearningProgress(courses = [], progress = []) {
  const safeProgress = Array.isArray(progress) ? progress : [];
  const completed = new Set(safeProgress.map((item) => `${item?.courseId}:${item?.lessonId}`));
  return (Array.isArray(courses) ? courses : []).map((course) => {
    const rawLessons = Array.isArray(course?.lessons) ? course.lessons : [];
    const lessons = rawLessons.map((lesson) => ({ ...lesson,
      status: completed.has(`${course.id}:${lesson.id}`) ? "completed" : "not-started",
    }));
    const completedLessons = lessons.filter((lesson) => lesson.status === "completed").length;
    return { ...course, lessons, completedLessons, totalLessons: lessons.length,
      enrolled: completedLessons > 0, progress: lessons.length ? Math.round(completedLessons / lessons.length * 100) : 0,
    };
  });
}
