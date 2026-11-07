import assert from "node:assert/strict";
import { test } from "node:test";
import { hasReferenceTrace } from "./problemCapabilities.js";

test("only problems with a built-in reference trace are advertised as visualizable", () => {
  assert.equal(hasReferenceTrace(1), true);
  assert.equal(hasReferenceTrace(704), true);
  assert.equal(hasReferenceTrace(912), false);
  assert.equal(hasReferenceTrace(2), false);
});
