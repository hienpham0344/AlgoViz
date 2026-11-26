import { useCallback, useEffect, useState } from "react";
import { api } from "./api.js";

export function useLearningProgress(isAuthenticated) {
  const [reloadKey, setReloadKey] = useState(0);
  const [fetched, setFetched] = useState({ key: null, progress: [], error: "" });

  const authed = Boolean(isAuthenticated);
  const requestKey = authed ? `auth:${reloadKey}` : "guest";

  useEffect(() => {
    if (!authed) return undefined;
    let active = true;
    const currentKey = `auth:${reloadKey}`;
    api.getLearningProgress().then((data) => {
      if (!Array.isArray(data)) throw new Error("Invalid learning progress response.");
      if (active) setFetched({ key: currentKey, progress: data, error: "" });
    }).catch((cause) => {
      if (active) {
        setFetched((prev) => ({
          key: currentKey,
          progress: prev.key === currentKey ? prev.progress : [],
          error: cause?.message || "Failed to load learning progress.",
        }));
      }
    });
    return () => { active = false; };
  }, [authed, reloadKey]);

  const progress = authed ? fetched.progress : [];
  const loading = authed && fetched.key !== requestKey;
  const error = authed && fetched.key === requestKey ? fetched.error : "";

  const save = async (courseId, lessonId, completed) => {
    await api.saveLearningProgress(courseId, lessonId, completed);
    setFetched((previous) => {
      const remaining = previous.progress.filter(
        (item) => item.courseId !== courseId || item.lessonId !== lessonId,
      );
      return {
        ...previous,
        key: requestKey,
        progress: completed ? [...remaining, { courseId, lessonId }] : remaining,
      };
    });
  };

  const retry = useCallback(() => setReloadKey((key) => key + 1), []);
  return { progress, loading, error, save, retry };
}
