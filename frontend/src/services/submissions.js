export const isAccepted = (status) => String(status).toLowerCase() === "accepted";
export function languageKey(label = "") {
  const value = label.toLowerCase();
  if (value.includes("javascript") || value === "js") return "javascript";
  if (value.includes("python") || value === "py") return "python";
  if (value.includes("c++") || value.includes("cpp")) return "cpp";
  if (value.includes("java")) return "java";
  return value;
}
export function languageLabel(value) {
  return ({ python: "Python", javascript: "JavaScript", cpp: "C++", java: "Java" })[languageKey(value)] || value || "—";
}
export function mapSubmission(item = {}) {
  const safeItem = item || {};
  const parsedDate = safeItem.submittedAt ? new Date(safeItem.submittedAt) : null;
  const date = parsedDate && !Number.isNaN(parsedDate.getTime()) ? parsedDate.toLocaleString() : "—";
  return {
    ...safeItem,
    algorithm: safeItem.problemTitle || `Problem #${safeItem.problemId ?? 1}`,
    topic: safeItem.topic || "",
    complexity: safeItem.executionTimeMs != null ? `${Number(safeItem.executionTimeMs).toFixed(1)} ms` : "—",
    runtimeMs: safeItem.executionTimeMs ?? null,
    memoryMb: safeItem.memoryUsageMb ?? null,
    language: languageLabel(safeItem.language),
    date,
    completed: isAccepted(safeItem.status),
    rawStatus: safeItem.status,
  };
}
export function runResult(result, expected = "") {
  if (!result?.status) throw new Error("The server returned no execution result.");
  const failed = /error|exceeded|unsupported|failed|limit|wrong answer|not judged/i.test(result.status) || Boolean(result.errorMessage) || Boolean(result.partial);
  const actual = String(result.output ?? "").trim();
  const normalize = (text) => String(text).trim().replaceAll("[", " ").replaceAll("]", " ").replaceAll(",", " ").replace(/\s+/g, " ");
  const hasExpected = String(expected).trim() !== "";
  const passed = !failed && (!hasExpected || normalize(actual) === normalize(expected));
  return {
    passed, status: failed ? result.status : !hasExpected ? "Executed" : passed ? "Passed" : "Wrong Answer",
    actual: result.errorMessage || actual || "(No output returned)", expected,
    runtimeMs: result.executionTimeMs ?? null, memoryMb: result.memoryUsageMb ?? null,
  };
}
const dayKey = (date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
export function profileStats(history = [], problems = [], today = new Date()) {
  const safeHistory = Array.isArray(history) ? history : [];
  const safeProblems = Array.isArray(problems) ? problems : [];
  const solvedIds = new Set(safeHistory.filter((item) => isAccepted(item?.status)).map((item) => item.problemId));
  const attemptedIds = new Set(safeHistory.map((item) => item?.problemId).filter((id) => id != null));
  const byDay = new Map();
  safeHistory.forEach((item) => {
    const date = new Date(item?.submittedAt);
    if (Number.isNaN(date.getTime())) return;
    const key = dayKey(date);
    byDay.set(key, (byDay.get(key) || 0) + 1);
  });
  const start = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  start.setDate(start.getDate() - start.getDay() - 51 * 7);
  const cells = Array.from({ length: 364 }, (_, index) => {
    const date = new Date(start);
    date.setDate(date.getDate() + index);
    const key = dayKey(date);
    return { date: key, count: byDay.get(key) || 0 };
  });
  const cursor = new Date(today.getFullYear(), today.getMonth(), today.getDate());
  if (!byDay.has(dayKey(cursor))) cursor.setDate(cursor.getDate() - 1);
  let currentStreak = 0;
  while (byDay.has(dayKey(cursor))) { currentStreak++; cursor.setDate(cursor.getDate() - 1); }
  const topics = new Map();
  safeProblems.forEach((problem) => {
    if (!problem) return;
    const name = problem.category || "Algorithm";
    const topic = topics.get(name) || { name, total: 0, solved: 0 };
    topic.total++;
    if (solvedIds.has(problem.id) || (problem.leetcodeId != null && solvedIds.has(problem.leetcodeId))) {
      topic.solved++;
    }
    topics.set(name, topic);
  });
  return {
    attempts: safeHistory.length, solved: solvedIds.size,
    inProgress: [...attemptedIds].filter((id) => !solvedIds.has(id)).length,
    total: safeProblems.length, points: solvedIds.size * 10,
    successRate: safeHistory.length ? Math.round(safeHistory.filter((item) => isAccepted(item?.status)).length / safeHistory.length * 100) : 0,
    activeDays: byDay.size, currentStreak, cells, topics: [...topics.values()],
  };
}
