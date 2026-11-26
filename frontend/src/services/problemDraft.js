const keyFor = (problemId) => `algoviz-problem-draft:${problemId}`;

export function readProblemDraft(problemId) {
  try {
    const draft = JSON.parse(sessionStorage.getItem(keyFor(problemId)) || "null");
    return draft && typeof draft.language === "string" && draft.codeByLanguage && typeof draft.codeByLanguage === "object"
      ? draft : null;
  } catch { return null; }
}

export function saveProblemDraft(problemId, language, codeByLanguage) {
  try {
    sessionStorage.setItem(keyFor(problemId), JSON.stringify({ language, codeByLanguage }));
    return true;
  } catch { return false; }
}

export function clearProblemDraft(problemId) {
  try { sessionStorage.removeItem(keyFor(problemId)); } catch { /* Storage may be disabled. */ }
}
