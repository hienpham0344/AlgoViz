import test from "node:test";
import assert from "node:assert/strict";
import { adaptStepsForVisualizer, fetchDebugSteps } from "./astApi.js";
import { javaFilename, pointersForVariable, sequenceValues } from "./solutionTrace.js";

test("each request submits the selected solution verbatim with its own Java filename", async () => {
  const originalFetch = globalThis.fetch;
  const requests = [];
  globalThis.fetch = async (url, options) => {
    const file = options.body.get("file");
    requests.push({ url, filename: file.name, source: await file.text(), signal: options.signal });
    return { ok: true, json: async () => ({ steps: [{ lineNumber: 3, variables: [] }] }) };
  };
  try {
    const controller = new AbortController();
    for (const name of ["TwoSumSolution", "BinarySearchSolution"]) {
      const source = `public class ${name} { public static void main(String[] args) {} }`;
      await fetchDebugSteps(source, { signal: controller.signal });
      const request = requests.at(-1);
      assert.equal(request.filename, `${name}.java`);
      assert.equal(request.source, source);
      assert.equal(request.signal, controller.signal);
      assert.equal(request.url, "http://localhost:8081/api/debug/steps");
    }
    await assert.rejects(fetchDebugSteps(), /no executable source/);
    assert.equal(requests.length, 2, "missing source must never run the Bubble Sort sample");
  } finally { globalThis.fetch = originalFetch; }
});

test("non-sort and empty traces never turn into a Bubble Sort simulation", () => {
  assert.deepEqual(adaptStepsForVisualizer({ steps: [] }), []);
  assert.deepEqual(adaptStepsForVisualizer({ steps: [{ variables: [{ variableName: "result", visualValue: true }] }] }), []);
});

test("arrays and strings display their captured runtime values unchanged", () => {
  assert.deepEqual(sequenceValues({ visualValue: [-1, 0, 9] }), [-1, 0, 9]);
  assert.deepEqual(sequenceValues({ visualType: "string", visualValue: "rat" }), ["r", "a", "t"]);
  assert.equal(sequenceValues({ visualValue: true }), null);
});

test("pointer labels follow source array references instead of assuming adjacent sorting indices", () => {
  const source = "int sum = nums[i] + nums[j]; counts[letter]++; s.charAt(i);";
  const step = { variables: [
    { variableName: "i", visualValue: 0 }, { variableName: "j", visualValue: 3 },
    { variableName: "letter", visualValue: 6 }, { variableName: "target", visualValue: 9 },
  ] };
  assert.deepEqual(pointersForVariable({ variableName: "nums" }, step, source), [{ name: "i", index: 0 }, { name: "j", index: 3 }]);
  assert.deepEqual(pointersForVariable({ variableName: "counts" }, step, source), [{ name: "letter", index: 6 }]);
  assert.deepEqual(pointersForVariable({ variableName: "s" }, step, source), [{ name: "i", index: 0 }]);
  assert.equal(javaFilename("public final class AnagramSolution {}"), "AnagramSolution.java");
});
