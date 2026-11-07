import assert from "node:assert/strict";
import { test } from "node:test";
import { problemImages, problemText, readableMath } from "./problemText.js";

test("imported problem HTML becomes readable text without live markup", () => {
  assert.equal(problemText("<p>Given an <code>m x n</code> matrix&nbsp;&amp; value.</p><p>Return true.</p>"),
    "Given an `m x n` matrix & value.\n\nReturn true.");
  assert.equal(problemText("<script>alert(1)</script><p>A &lt; B</p>"), "A < B");
  assert.equal(problemText("Return [0, 1]"), "Return [0, 1]");
});

test("imported problem illustrations keep only trusted HTTPS sources", () => {
  const html = '<img src="https://fastly.jsdelivr.net/gh/doocs/leetcode/images/example.jpg" alt="List diagram">'
    + '<img src="javascript:alert(1)"><img src="https://evil.example/a.png">';
  assert.deepEqual(problemImages(html), [{
    src: "https://fastly.jsdelivr.net/gh/doocs/leetcode/images/example.jpg",
    alt: "List diagram",
  }]);
});

test("reference explanations show readable inline mathematics", () => {
  assert.equal(readableMath("Traverse $l_1$ and $l_2$ in $O(\\max (m, n))$ time."),
    "Traverse l₁ and l₂ in O(max (m, n)) time.");
});
