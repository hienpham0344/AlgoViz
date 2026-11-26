import assert from "node:assert/strict";
import { test } from "node:test";
import { buildGenericProblem } from "./problemDetailFallback.js";

test("imported problem placeholder contains no fabricated testcase, hint, code or solution", () => {
  const problem = buildGenericProblem(2);
  assert.equal(problem.title, "Problem #2");
  assert.deepEqual(problem.sampleTestCases, []);
  assert.deepEqual(problem.solutions, []);
  assert.deepEqual(problem.hints, []);
  assert.equal(problem.description, "");
  assert.equal(problem.starterCodePython, undefined);
});
