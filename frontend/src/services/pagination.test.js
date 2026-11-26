import assert from "node:assert/strict";
import { test } from "node:test";
import { paginationItems } from "./pagination.js";

test("large catalogs show a bounded set of page controls with useful jumps", () => {
  assert.deepEqual(paginationItems(1, 403), [1, 2, "ellipsis", 403]);
  assert.deepEqual(paginationItems(201, 403), [1, "ellipsis", 200, 201, 202, "ellipsis", 403]);
  assert.deepEqual(paginationItems(403, 403), [1, "ellipsis", 402, 403]);
  assert.deepEqual(paginationItems(1, 1), [1]);
});
