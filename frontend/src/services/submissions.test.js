import assert from "node:assert/strict";
import { test } from "node:test";
import { languageKey, mapSubmission, runResult, profileStats } from "./submissions.js";

test("all editor languages map to their own runner", () => {
  assert.equal(languageKey("JavaScript"), "javascript");
  assert.equal(languageKey("C++ 20"), "cpp");
  assert.equal(languageKey("Java 21"), "java");
  assert.equal(languageKey("Python 3"), "python");
});
test("compiler errors and empty responses cannot pass a run", () => {
  assert.throws(() => runResult(null, "1"));
  assert.equal(runResult({ status: "Compilation Error", output: "", errorMessage: "Syntax error" }, "").passed, false);
  assert.equal(runResult({ status: "Success", output: "1 23" }, "12 3").passed, false);
  assert.equal(runResult({ status: "Success", output: "[1, 2]" }, "[1,2]").passed, true);
});
test("submission formatting preserves the real verdict and zero metrics", () => {
  const row = mapSubmission({ id: 4, status: "Wrong Answer", executionTimeMs: 0, language: "cpp" });
  assert.equal(row.id, 4);
  assert.equal(row.completed, false);
  assert.equal(row.runtimeMs, 0);
  assert.equal(row.language, "C++");
});
test("profile totals derive from submissions and count solved problems once", () => {
  const history = [
    { problemId: 1, status: "Accepted", submittedAt: "2026-11-04T10:00:00" },
    { problemId: 1, status: "Accepted", submittedAt: "2026-11-05T10:00:00" },
    { problemId: 2, status: "Wrong Answer", submittedAt: "2026-11-05T11:00:00" },
  ];
  const stats = profileStats(history, [{ id: 1, category: "Array" }, { id: 2, category: "String" }], new Date(2026, 10, 5, 12));
  assert.equal(stats.solved, 1);
  assert.equal(stats.attempts, 3);
  assert.equal(stats.inProgress, 1);
  assert.equal(stats.currentStreak, 2);
  assert.equal(stats.activeDays, 2);
  assert.equal(stats.cells.reduce((sum, day) => sum + day.count, 0), 3);
  assert.equal(profileStats([], []).successRate, 0);
});

test('a partial runtime trace cannot pass merely because stdout matches the expected value', () => {
  const result = runResult({status:'Trace Limit Reached',output:'[0, 1]',partial:true},'[0, 1]');
  assert.equal(result.passed,false);
  assert.equal(result.status,'Trace Limit Reached');
});
