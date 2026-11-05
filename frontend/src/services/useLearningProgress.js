import { useCallback, useEffect, useState } from "react";
import { api } from "./api.js";

export function useLearningProgress(isAuthenticated) {
  const [progress, setProgress] = useState([]);
  const [loading, setLoading] = useState(Boolean(isAuthenticated));
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  useEffect(() => {
    let active = true;
    setLoading(Boolean(isAuthenticated));
    setError("");
    if (!isAuthenticated) { setProgress([]); return; }
    api.getLearningProgress().then((data) => {
      if (!Array.isArray(data)) throw new Error("Invalid learning progress response.");
      if (active) setProgress(data);
    }).catch((cause) => { if (active) setError(cause.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [isAuthenticated, reloadKey]);
  const save = async (courseId, lessonId, completed) => {
    await api.saveLearningProgress(courseId, lessonId, completed);
    setProgress((previous) => {
      const remaining = previous.filter((item) => item.courseId !== courseId || item.lessonId !== lessonId);
      return completed ? [...remaining, { courseId, lessonId }] : remaining;
    });
  };
  const retry = useCallback(() => setReloadKey((key) => key + 1), []);
  return { progress, loading, error, save, retry };
}
