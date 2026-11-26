import assert from "node:assert/strict";
import { test } from "node:test";
import { clearProblemDraft, readProblemDraft, saveProblemDraft } from "./problemDraft.js";

test("a guest editor draft is scoped to its problem and can be cleared", () => {
  const oldStorage = globalThis.sessionStorage;
  const values = new Map();
  globalThis.sessionStorage = {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
  };
  try {
    assert.equal(saveProblemDraft(2, "Java", { Java: "class Main {}" }), true);
    assert.deepEqual(readProblemDraft(2), { language: "Java", codeByLanguage: { Java: "class Main {}" } });
    assert.equal(readProblemDraft(1), null);
    clearProblemDraft(2);
    assert.equal(readProblemDraft(2), null);
  } finally {
    globalThis.sessionStorage = oldStorage;
  }
});
