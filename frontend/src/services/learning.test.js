import assert from "node:assert/strict";
import { test } from "node:test";
import { withLearningProgress } from "./learning.js";

test("course completion uses stored lessons instead of catalog sample progress", () => {
  const courses = [{ id: "sorting", progress: 100, enrolled: true, lessons: [{ id: 1, status: "completed" }, { id: 2, status: "completed" }] }];
  const fresh = withLearningProgress(courses, [])[0];
  assert.equal(fresh.progress, 0);
  assert.equal(fresh.enrolled, false);
  const updated = withLearningProgress(courses, [{ courseId: "sorting", lessonId: 2 }, { courseId: "other", lessonId: 1 }])[0];
  assert.equal(updated.progress, 50);
  assert.equal(updated.completedLessons, 1);
  assert.equal(updated.lessons[1].status, "completed");
});
