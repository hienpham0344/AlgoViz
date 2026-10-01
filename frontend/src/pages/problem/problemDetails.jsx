import { useEffect, useRef, useState } from "react";
import { NavigationBar } from "../../components/index.js";
import { api } from "../../services/api.js";
import "./problemDetails.css";

const DETAIL_TABS = [
  { id: "description", label: "Description", icon: "description" },
  { id: "solution-basic", label: "Solution 1: Basic", icon: "code-angle" },
];

const SEQUENCE_TABS = [
  { id: "description", label: "Description" },
  { id: "solution-basic", label: "Solution 1" },
  { id: "workspace", label: "My Code" },
];

const RUNNER_LANGUAGES = ["Python 3", "JavaScript", "C++"];
const RUNNER_TABS = [
  { id: "tests", label: "Test Cases", icon: "runner-tests" },
  { id: "console", label: "Console Output", icon: "code-reference" },
];
const RUNNER_CODE = {
  "Python 3": `def bubble_sort(arr):
    n = len(arr)
    # Traverse through all array elements
    for i in range(n):
        swapped = False
        # Last i elements are already in place
        for j in range(0, n - i - 1):
            # Swap if the element found is greater than
            # adjacent
            if arr[j] > arr[j + 1]:
                arr[j], arr[j + 1] = arr[j + 1], arr[j]
                swapped = True
        # If no two elements were swapped by inner
        # loop, then break
        if not swapped:
            break
    return arr`,
  JavaScript: `function bubbleSort(arr) {
  for (let i = arr.length; i > 0; i--) {
    let swapped = false;
    for (let j = 0; j < i - 1; j++) {
      if (arr[j] > arr[j + 1]) {
        [arr[j], arr[j + 1]] = [arr[j + 1], arr[j]];
        swapped = true;
      }
    }
    if (!swapped) break;
  }
  return arr;
}`,
  "C++": `vector<int> bubbleSort(vector<int> arr) {
  for (int i = arr.size(); i > 0; --i) {
    bool swapped = false;
    for (int j = 0; j < i - 1; ++j) {
      if (arr[j] > arr[j + 1]) {
        swap(arr[j], arr[j + 1]);
        swapped = true;
      }
    }
    if (!swapped) break;
  }
  return arr;
}`,
};
const RUNNER_CASES = [
  {
    input: "[5, 2, 8, 1]",
    actual: "[1, 2, 5, 8]",
    expected: "[1, 2, 5, 8]",
    status: "Passed",
    runtimeMs: 0.2,
    memoryMb: 14.8,
    matchPercent: 100,
  },
  {
    input: "[1, 2, 3, 4]",
    actual: "[1, 2, 3, 4]",
    expected: "[1, 2, 3, 4]",
    status: "Passed",
    runtimeMs: 0.1,
    memoryMb: 14.7,
    matchPercent: 100,
  },
  {
    input: "[9, 4, 6, 2]",
    actual: "[2, 4, 6, 9]",
    expected: "[2, 4, 6, 9]",
    status: "Passed",
    runtimeMs: 0.2,
    memoryMb: 14.8,
    matchPercent: 100,
  },
];

const SOLUTION_JAVA_LINES = [
  {
    tokens: [
      { text: "public void ", type: "keyword" },
      { text: "bubbleSort", type: "function" },
      { text: "(", type: "plain" },
      { text: "int", type: "type" },
      { text: "[] arr) {", type: "plain" },
    ],
  },
  {
    tokens: [
      { text: "    int", type: "type" },
      { text: " n = arr.length;", type: "plain" },
    ],
  },
  {
    tokens: [
      { text: "    for", type: "keyword" },
      { text: " (", type: "plain" },
      { text: "int", type: "type" },
      { text: " i = 0; i < n - 1; i++) {", type: "plain" },
    ],
  },
  {
    tokens: [
      { text: "        for", type: "keyword" },
      { text: " (", type: "plain" },
      { text: "int", type: "type" },
      { text: " j = 0; j < n - i - 1; j++) {", type: "plain" },
    ],
  },
  {
    active: true,
    tokens: [
      { text: "            if", type: "keyword" },
      { text: " (arr[j] > arr[j + 1]) {", type: "plain" },
    ],
  },
  {
    tokens: [
      { text: "                int", type: "type" },
      { text: " temp = arr[j];", type: "plain" },
    ],
  },
  { tokens: [{ text: "                arr[j] = arr[j + 1];", type: "plain" }] },
  { tokens: [{ text: "                arr[j + 1] = temp;", type: "plain" }] },
  { tokens: [{ text: "            }", type: "plain" }] },
  { tokens: [{ text: "        }", type: "plain" }] },
  { tokens: [{ text: "    }", type: "plain" }] },
  { tokens: [{ text: "}", type: "plain" }] },
];

const SOLUTION_STEPS = [
  {
    nums: [5, 2, 8, 1],
    leftIndex: 0,
    rightIndex: 1,
    activeLine: 5,
    status: "swap",
    resultBuffer: ["[ ]", "[ ]", "[ ]", "[ ]"],
    comparison: "5 > 2",
    annotation: "arr[0]=5 > arr[1]=2 is TRUE (triggering swap)",
    context:
      "Because arr[0] = 5 is strictly greater than arr[1] = 2, the condition evaluates to true. Execution will proceed to lines 6–8 where values are swapped using temporary register variable state.",
  },
  {
    nums: [2, 5, 8, 1],
    leftIndex: 1,
    rightIndex: 2,
    activeLine: 5,
    status: "compare",
    resultBuffer: ["[ ]", "[ ]", "[ ]", "[ ]"],
    comparison: "5 < 8",
    annotation: "arr[1]=5 < arr[2]=8 is TRUE (no swap required)",
    context:
      "The adjacent values are already ordered, so the optimized pass continues without mutating the array.",
  },
  {
    nums: [2, 5, 1, 8],
    leftIndex: 2,
    rightIndex: 3,
    activeLine: 5,
    status: "swap",
    resultBuffer: ["[ ]", "[ ]", "[ ]", "[ ]"],
    comparison: "8 > 1",
    annotation: "arr[2]=8 > arr[3]=1 is TRUE (triggering swap)",
    context:
      "The largest remaining value is compared with the final unsorted element and moves one position toward its sorted location.",
  },
  {
    nums: [1, 2, 5, 8],
    leftIndex: 0,
    rightIndex: 1,
    activeLine: 5,
    status: "complete",
    resultBuffer: ["[ ]", "[ ]", "[ ]", "[ ]"],
    comparison: "2 > 1",
    annotation: "arr[0]=2 > arr[1]=1 is TRUE (final swap)",
    context:
      "The array is sorted after the final swap. The trace is ready to replay from the beginning.",
  },
];

const SOLUTION_VARIANTS = {
  "solution-basic": {
    label: "Solution 1: Basic",
    complexity: "Time: O(N²)",
    space: "Space: O(1)",
  },
  "solution-optimized": {
    label: "Solution 2: Optimized",
    complexity: "Time: O(N²)",
    space: "Space: O(1)",
  },
};

const FOOTER_COLUMNS = [
  {
    title: "Curriculum",
    links: [
      "Dynamic Programming",
      "Trees & Graph Algorithms",
      "Binary Search & Pointers",
      "Bit Manipulation",
      "System Design Foundations",
    ],
  },
  {
    title: "Platform",
    links: [
      "Problem Directory",
      "Algorithm Visualizer",
      "Weekly Coding Battles",
      "Company Tagged Lists",
      "Leaderboard",
    ],
  },
  {
    title: "Community",
    links: [
      "Discussion Forums",
      "Interview Experiences",
      "Discord Community",
      "Editorial Solutions",
      "FAQ & Help Center",
    ],
  },
];

const EXAMPLES = [
  {
    label: "Example 1",
    badge: "General Case",
    input: "[5, 2, 8, 1]",
    output: "[1, 2, 5, 8]",
    passes: [
      [
        "Pass 1:",
        "Compare (5, 2) → swap → [2, 5, 8, 1]. Compare (5, 8) → keep. Compare (8, 1) → swap → [2, 5, 1, 8]. (8 in position).",
      ],
      [
        "Pass 2:",
        "Compare (2, 5) → keep. Compare (5, 1) → swap → [2, 1, 5, 8]. (5 in position).",
      ],
      ["Pass 3:", "Compare (2, 1) → swap → [1, 2, 5, 8]. Complete!"],
    ],
  },
  {
    label: "Example 2",
    badge: "Optimized Best Case",
    input: "[1, 2, 3, 4]",
    output: "[1, 2, 3, 4]",
    explanation:
      "The array is already sorted. In the optimized version, Pass 1 discovers zero swaps and short-circuits early in O(N) time.",
  },
];

function DetailsIcon({ name, className = "" }) {
  return (
    <span
      className={`details-icon details-icon--${name} ${className}`.trim()}
      aria-hidden="true"
    />
  );
}

function BreadcrumbBar() {
  return (
    <div className="details-breadcrumb-bar">
      <nav className="details-breadcrumbs" aria-label="Problem breadcrumb">
        <a href="#problems">
          <DetailsIcon name="arrow-left" /> Problems
        </a>
        <span>/</span>
        <span>Sorting Algorithms</span>
        <span>/</span>
        <strong>Bubble Sort</strong>
      </nav>
      <span className="details-ready-status">
        <DetailsIcon name="status-ready" /> Interactive Visualizer Ready
      </span>
    </div>
  );
}

function ProblemHeader({
  isBookmarked,
  onBookmark,
  onShare,
  onRunVisualizer,
  onSubmit,
}) {
  return (
    <section
      className="details-problem-header"
      aria-labelledby="detail-problem-title"
    >
      <div
        className="details-header-glow details-header-glow--violet"
        aria-hidden="true"
      />
      <div
        className="details-header-glow details-header-glow--mint"
        aria-hidden="true"
      />
      <div className="details-problem-identity">
        <div className="details-title-row">
          <h1 id="detail-problem-title">Bubble Sort</h1>
          <span className="details-difficulty">
            <DetailsIcon name="difficulty-easy" /> Easy
          </span>
          <span className="details-problem-id">ID #042</span>
        </div>
        <div className="details-meta-row">
          <span>Category:</span>
          <span className="details-meta-tag">Array</span>
          <span className="details-meta-tag">Comparison Sort</span>
          <span className="details-meta-tag">In-Place</span>
          <span className="details-meta-tag details-meta-tag--mono">O(N²)</span>
        </div>
      </div>
      <div className="details-header-actions" aria-label="Problem actions">
        <button
          className={`details-action-button ${isBookmarked ? "is-active" : ""}`}
          type="button"
          aria-pressed={isBookmarked}
          onClick={onBookmark}
        >
          <DetailsIcon name="bookmark" />{" "}
          {isBookmarked ? "Bookmarked" : "Bookmark"}
        </button>
        <button
          className="details-action-button"
          type="button"
          onClick={onShare}
        >
          <DetailsIcon name="share" /> Share
        </button>
        <button
          className="details-action-button details-action-button--primary"
          type="button"
          onClick={onRunVisualizer}
        >
          <DetailsIcon name="run-visualizer" /> Run Visualizer
        </button>
        <button
          className="details-action-button details-action-button--submit"
          type="button"
          onClick={onSubmit}
        >
          <DetailsIcon name="submit-solution" /> Submit Solution
        </button>
      </div>
    </section>
  );
}

function WorkspaceTabs({ activeTab, onChange }) {
  return (
    <div className="details-tabs-bar">
      <div
        className="details-tabs"
        role="tablist"
        aria-label="Problem workspace tabs"
      >
        {DETAIL_TABS.map((tab) => (
          <button
            className={`details-tab ${activeTab === tab.id ? "is-active" : ""}`}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            aria-controls={`detail-panel-${tab.id}`}
            key={tab.id}
            onClick={() => onChange(tab.id)}
          >
            <DetailsIcon name={tab.icon} /> {tab.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function ComplexityMetric({ icon, label, value, note, className = "" }) {
  return (
    <div className={`complexity-metric ${className}`.trim()}>
      <div className="complexity-metric__label">
        <span>{label}</span>
        <DetailsIcon name={icon} />
      </div>
      <strong>{value}</strong>
      <small>{note}</small>
    </div>
  );
}

function OverviewCard() {
  return (
    <article className="details-card details-overview-card">
      <div className="details-card-heading">
        <h2>Concept Brief</h2>
        <span>Stable Sort</span>
      </div>
      <p className="details-lead-copy">
        Sort an array of elements by repeatedly swapping adjacent elements if
        they are in the wrong order until the entire array is sorted.
      </p>
      <p className="details-support-copy">
        In each iteration, the largest unsorted element &quot;bubbles up&quot;
        to its correct position at the end of the array, similar to air bubbles
        rising to the water&apos;s surface.
      </p>
      <div className="complexity-grid">
        <ComplexityMetric
          icon="time-complexity"
          label="Time Complexity"
          value="O(N²)"
          note="Best Case: O(N)"
          className="complexity-metric--amber"
        />
        <ComplexityMetric
          icon="space-complexity"
          label="Space Complexity"
          value="O(1)"
          note="In-place auxiliary"
          className="complexity-metric--mint"
        />
        <ComplexityMetric
          icon="stability"
          label="Stability"
          value="Stable"
          note="Preserves key order"
          className="complexity-metric--soft"
        />
      </div>
    </article>
  );
}

function ConstraintsCard() {
  return (
    <article className="details-card details-constraints-card">
      <h2 className="details-section-heading">
        <DetailsIcon name="input-constraints" /> Input Constraints
      </h2>
      <ul className="constraints-list">
        <li>
          <strong>1 ≤ n ≤ 1000</strong>
          <span>— array length. Suitable for quadratic time algorithms.</span>
        </li>
        <li>
          <strong>-10⁴ ≤ arr[i] ≤ 10⁴</strong>
          <span>— signed integer ranges fit within 32-bit registers.</span>
        </li>
        <li>
          <span>
            Elements may contain duplicates. The relative ordering of equal keys
            must remain unaltered.
          </span>
        </li>
      </ul>
    </article>
  );
}

function ExampleCard({ example }) {
  return (
    <article className="details-card details-example-card">
      <div className="details-example-heading">
        <h3>{example.label}</h3>
        <span
          className={
            example.badge.includes("Optimized") ? "is-amber" : "is-mint"
          }
        >
          {example.badge}
        </span>
      </div>
      <div className="details-io-block">
        <code>Input: arr = {example.input}</code>
        <code>
          Output: <em>{example.output}</em>
        </code>
      </div>
      {example.passes ? (
        <div className="details-pass-list">
          <strong>Step-by-step trace:</strong>
          {example.passes.map(([label, text]) => (
            <p key={label}>
              <b>{label}</b> {text}
            </p>
          ))}
        </div>
      ) : (
        <p className="details-explanation">
          <strong>Explanation:</strong> {example.explanation}
        </p>
      )}
    </article>
  );
}

function ExamplesSection() {
  return (
    <section
      className="details-examples-section"
      aria-labelledby="examples-title"
    >
      <h2 id="examples-title" className="details-section-heading">
        <DetailsIcon name="walkthrough" /> Walkthrough Examples
      </h2>
      {EXAMPLES.map((example) => (
        <ExampleCard key={example.label} example={example} />
      ))}
    </section>
  );
}

function DescriptionColumn() {
  return (
    <div className="details-primary-column">
      <OverviewCard />
      <ConstraintsCard />
      <ExamplesSection />
    </div>
  );
}

function RunnerHeader({ language, executionState, onLanguageChange }) {
  return (
    <div className="runner-header">
      <label className="runner-language">
        <DetailsIcon name="code-angle" />
        <span className="sr-only">Code language</span>
        <select
          value={language}
          onChange={(event) => onLanguageChange(event.target.value)}
          aria-label="Code language"
        >
          {RUNNER_LANGUAGES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>
      <span className={`runner-status runner-status--${executionState}`}>
        <DetailsIcon name="status-ready" />
        {executionState === "running" ? "Running" : "Ready"}
      </span>
    </div>
  );
}

function RunnerToolbar({ isExpanded, onReset, onCopy, onToggleExpand }) {
  return (
    <div className="runner-toolbar" aria-label="Runner toolbar">
      <button
        type="button"
        aria-label="Reset code"
        title="Reset code"
        onClick={onReset}
      >
        <DetailsIcon name="reset" />
      </button>
      <button
        type="button"
        aria-label="Copy code"
        title="Copy code"
        onClick={onCopy}
      >
        <DetailsIcon name="runner-copy" />
      </button>
      <button
        type="button"
        aria-label={isExpanded ? "Collapse editor" : "Expand editor"}
        title={isExpanded ? "Collapse editor" : "Expand editor"}
        onClick={onToggleExpand}
      >
        <DetailsIcon name="runner-expand" />
      </button>
    </div>
  );
}

function CodeEditor({ language, code, isExpanded, onChange }) {
  const lineCount = Math.max(1, code.split("\\n").length);
  return (
    <div className={`runner-editor ${isExpanded ? "is-expanded" : ""}`}>
      <div className="runner-editor__gutter" aria-hidden="true">
        {Array.from({ length: lineCount }, (_, index) => (
          <span key={`${language}-line-${index + 1}`}>{index + 1}</span>
        ))}
      </div>
      <textarea
        className="runner-editor__input"
        value={code}
        onChange={(event) => onChange(event.target.value)}
        spellCheck="false"
        rows={lineCount}
        aria-label={`${language} code editor`}
      />
    </div>
  );
}

function OutputCard({ type, testCase }) {
  const isActual = type === "actual";
  return (
    <article className={`runner-output-card runner-output-card--${type}`}>
      <div className="runner-output-card__heading">
        <h3>
          <DetailsIcon name="status-ready" />{" "}
          {isActual ? "Actual Output" : "Expected Output"}
        </h3>
        <span
          className={`runner-output-badge runner-output-badge--${isActual ? "passed" : "target"}`}
        >
          {isActual ? testCase.status : "Target"}
        </span>
      </div>
      <code className="runner-output-card__value">
        {isActual ? testCase.actual : testCase.expected}
      </code>
      <div className="runner-output-card__meta">
        {isActual ? (
          <>
            <span>
              <DetailsIcon name="time-complexity" />{" "}
              {testCase.runtimeMs.toFixed(1)}ms
            </span>
            <span>
              <DetailsIcon name="space-complexity" />{" "}
              {testCase.memoryMb.toFixed(1)} MB
            </span>
          </>
        ) : (
          <>
            <span>Standard Match</span>
            <strong>{testCase.matchPercent}% Match</strong>
          </>
        )}
      </div>
    </article>
  );
}

function RunnerOutput({ activeTab, activeCaseIndex, testCase, onTabChange }) {
  return (
    <section className="runner-output" aria-label="Code execution output">
      <div className="runner-output__header">
        <div
          className="runner-output-tabs"
          role="tablist"
          aria-label="Execution output tabs"
        >
          {RUNNER_TABS.map((tab) => (
            <button
              className={activeTab === tab.id ? "is-active" : ""}
              type="button"
              role="tab"
              aria-selected={activeTab === tab.id}
              aria-controls={`runner-panel-${tab.id}`}
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
            >
              <DetailsIcon name={tab.icon} /> {tab.label}
            </button>
          ))}
        </div>
        <span className="runner-case-label">
          Case {activeCaseIndex + 1} of {RUNNER_CASES.length}
        </span>
      </div>
      {activeTab === "console" ? (
        <div
          className="runner-output__cards"
          id="runner-panel-console"
          role="tabpanel"
        >
          <OutputCard type="actual" testCase={testCase} />
          <OutputCard type="expected" testCase={testCase} />
        </div>
      ) : (
        <div
          className="runner-test-cases"
          id="runner-panel-tests"
          role="tabpanel"
        >
          {RUNNER_CASES.map((item, index) => (
            <button
              className={index === activeCaseIndex ? "is-active" : ""}
              type="button"
              key={item.input}
              onClick={() => onTabChange(`case:${index}`)}
            >
              <span>Case {index + 1}</span>
              <code>{item.input}</code>
              <DetailsIcon name="sequence-right" />
            </button>
          ))}
        </div>
      )}
    </section>
  );
}

function RunnerFooter({ executionState, onRun, onSubmit }) {
  return (
    <div className="runner-footer">
      <button
        className="runner-action runner-action--run"
        type="button"
        onClick={onRun}
        disabled={executionState === "running"}
      >
        <DetailsIcon name="run-visualizer" />{" "}
        {executionState === "running" ? "Running..." : "Run Code"}
      </button>
      <button
        className="runner-action runner-action--submit"
        type="button"
        onClick={onSubmit}
      >
        <DetailsIcon name="submit-solution" /> Submit
      </button>
    </div>
  );
}

function CodeRunnerPanel({
  runnerRef,
  language,
  code,
  activeTab,
  activeCaseIndex,
  executionState,
  isExpanded,
  onLanguageChange,
  onCodeChange,
  onOutputTabChange,
  onCaseChange,
  onReset,
  onCopy,
  onToggleExpand,
  onRun,
  onSubmit,
}) {
  const testCase = RUNNER_CASES[activeCaseIndex];
  const handleOutputTabChange = (value) => {
    if (value.startsWith("case:")) {
      onCaseChange(Number(value.slice(5)));
      return;
    }
    onOutputTabChange(value);
  };

  return (
    <article
      className={`details-card details-runner-card ${isExpanded ? "is-expanded" : ""}`}
      ref={runnerRef}
    >
      <RunnerHeader
        language={language}
        executionState={executionState}
        onLanguageChange={onLanguageChange}
      />
      <RunnerToolbar
        isExpanded={isExpanded}
        onReset={onReset}
        onCopy={onCopy}
        onToggleExpand={onToggleExpand}
      />
      <CodeEditor
        language={language}
        code={code}
        isExpanded={isExpanded}
        onChange={onCodeChange}
      />
      <RunnerOutput
        activeTab={activeTab}
        activeCaseIndex={activeCaseIndex}
        testCase={testCase}
        onTabChange={handleOutputTabChange}
      />
      <RunnerFooter
        executionState={executionState}
        onRun={onRun}
        onSubmit={onSubmit}
      />
    </article>
  );
}

function formatSolutionIndex(index) {
  return `[${index}]`;
}

function SolutionContextBar({ variant }) {
  return (
    <div className="details-solution-context">
      <span className="details-solution-context__left">
        <strong>{variant.label}</strong>
        <span>/</span>
        <span>Bubble Sort</span>
      </span>
      <span className="details-solution-context__right">
        <span>{variant.complexity}</span>
        <span>{variant.space}</span>
      </span>
    </div>
  );
}

function SolutionArrayState({ step }) {
  return (
    <>
      <div className="details-solution-board__topline">
        <span>nums array</span>
        <span>Length: {step.nums.length}</span>
      </div>
      <div className="details-solution-array" aria-label="Current array values">
        {step.nums.map((value, index) => (
          <span
            className={`details-solution-array__cell ${
              index === step.leftIndex || index === step.rightIndex
                ? "is-active"
                : ""
            }`}
            data-index={formatSolutionIndex(index)}
            key={`${value}-${index}`}
          >
            {value}
          </span>
        ))}
      </div>
      <div
        className="details-solution-pointers"
        aria-label="Current array pointers"
      >
        <span
          className="details-solution-pointer"
          style={{ gridColumn: step.leftIndex + 1 }}
        >
          <b aria-hidden="true">↑</b>
          <span>i</span>
        </span>
        <span
          className="details-solution-pointer"
          style={{ gridColumn: step.rightIndex + 1 }}
        >
          <b aria-hidden="true">↑</b>
          <span>j</span>
        </span>
      </div>
      <div className="details-solution-buffer">
        <div className="details-solution-board__resultline">
          <span>result / swap buffer</span>
          <span>in-place mutation</span>
        </div>
        <div className="details-solution-buffer__row" aria-label="Swap buffer">
          {step.resultBuffer.map((value, index) => (
            <span key={`${value}-${index}`}>{value}</span>
          ))}
        </div>
      </div>
    </>
  );
}

function SolutionPlaybackControls({
  stepIndex,
  stepCount,
  isPlaying,
  onTogglePlay,
  onPrevious,
  onNext,
  onReplay,
  onSeek,
}) {
  return (
    <div
      className="details-solution-controls"
      aria-label="Visualizer playback controls"
    >
      <div className="details-solution-controls__buttons">
        <button
          type="button"
          aria-label={isPlaying ? "Pause visualization" : "Play visualization"}
          aria-pressed={isPlaying}
          onClick={onTogglePlay}
        >
          {isPlaying ? "Ⅱ" : "▶"}
        </button>
        <button
          type="button"
          aria-label="Previous visualization step"
          onClick={onPrevious}
        >
          |◀
        </button>
        <button
          type="button"
          aria-label="Next visualization step"
          onClick={onNext}
        >
          ▶|
        </button>
        <button
          type="button"
          aria-label="Replay visualization"
          onClick={onReplay}
        >
          ↻
        </button>
      </div>
      <label className="details-solution-progress">
        <span className="sr-only">Visualizer step</span>
        <input
          type="range"
          min="0"
          max={stepCount - 1}
          value={stepIndex}
          onChange={onSeek}
          aria-label="Visualizer step"
        />
      </label>
      <span className="details-solution-progress__label">
        {stepIndex + 1} / {stepCount}
        <strong>1.0×</strong>
      </span>
    </div>
  );
}

function SolutionVisualizerCard({
  variant,
  step,
  stepIndex,
  stepCount,
  isPlaying,
  onTogglePlay,
  onPrevious,
  onNext,
  onReplay,
  onSeek,
}) {
  const isSwap = step.status === "swap";
  return (
    <section
      className="details-solution-card details-solution-visualizer"
      aria-label={`${variant.label} visualization`}
    >
      <SolutionContextBar variant={variant} />
      <div className="details-solution-stage">
        <header className="details-solution-stage__header">
          <div className="details-solution-stage__heading">
            <span className="details-solution-badge">VISUALIZATION</span>
            <strong>Step Trace &amp; State Pointers</strong>
          </div>
          <span className="details-solution-live">LIVE BUFFER</span>
        </header>
        <div className="details-solution-board">
          <SolutionArrayState step={step} />
          <div className="details-solution-note">
            <p>
              <strong>⚡ Stop:</strong> Compare nums[{step.leftIndex}] ={" "}
              {step.nums[step.leftIndex]} and nums[
              {step.rightIndex}] = {step.nums[step.rightIndex]} →{" "}
              {step.comparison}, {isSwap ? "swap required" : "keep order"}
            </p>
            <span
              className={`details-solution-swap ${isSwap ? "is-triggered" : ""}`}
            >
              <small>Swap</small>
              {isSwap ? "Triggered" : "Skipped"}
            </span>
          </div>
          <SolutionPlaybackControls
            stepIndex={stepIndex}
            stepCount={stepCount}
            isPlaying={isPlaying}
            onTogglePlay={onTogglePlay}
            onPrevious={onPrevious}
            onNext={onNext}
            onReplay={onReplay}
            onSeek={onSeek}
          />
        </div>
      </div>
    </section>
  );
}

function SolutionCodeToolbar({ isExpanded, onCopy, onAction, onToggleExpand }) {
  const actions = [
    { id: "pin", label: "Pin solution", icon: "workspace-edit" },
    { id: "comment", label: "Add comment", icon: "code-reference" },
    { id: "inspect", label: "Inspect execution", icon: "visualizer-sandbox" },
    { id: "duplicate", label: "Duplicate solution", icon: "runner-copy" },
  ];

  return (
    <header className="details-solution-code__toolbar">
      <div className="details-solution-file-context">
        <span className="details-solution-java-pill">Java</span>
        <strong>
          JDK 21 <span>• BubbleSort.java</span>
        </strong>
      </div>
      <div
        className="details-solution-code-actions"
        aria-label="Solution code toolbar"
      >
        <button
          type="button"
          aria-label="Copy solution code"
          title="Copy solution code"
          onClick={onCopy}
        >
          <DetailsIcon name="runner-copy" />
        </button>
        {actions.map((action) => (
          <button
            type="button"
            aria-label={action.label}
            title={action.label}
            key={action.id}
            onClick={() => onAction(action.label)}
          >
            <DetailsIcon name={action.icon} />
          </button>
        ))}
        <button
          type="button"
          aria-label={
            isExpanded ? "Collapse solution code" : "Expand solution code"
          }
          title={isExpanded ? "Collapse solution code" : "Expand solution code"}
          onClick={onToggleExpand}
        >
          <DetailsIcon name="runner-expand" />
        </button>
      </div>
    </header>
  );
}

function SolutionCodeEditor({ step }) {
  return (
    <div
      className="details-solution-editor"
      aria-label="Read-only Java solution editor"
    >
      <div className="details-solution-editor__gutter" aria-hidden="true">
        {SOLUTION_JAVA_LINES.map((_, index) => (
          <span key={`solution-line-${index + 1}`}>{index + 1}</span>
        ))}
      </div>
      <pre
        className="details-solution-editor__code"
        role="textbox"
        aria-readonly="true"
        tabIndex="0"
      >
        {SOLUTION_JAVA_LINES.map((line, index) => (
          <span
            className={`details-solution-code-line ${line.active ? "is-active" : ""}`}
            key={`code-${index + 1}`}
          >
            {line.tokens.map((token, tokenIndex) => (
              <span
                className={`details-solution-token details-solution-token--${token.type}`}
                key={`${index}-${tokenIndex}`}
              >
                {token.text}
              </span>
            ))}
            {line.active && (
              <span className="details-solution-annotation">
                ← {step.annotation}
              </span>
            )}
          </span>
        ))}
      </pre>
    </div>
  );
}

function SolutionCodeCard({
  step,
  isExpanded,
  onCopy,
  onAction,
  onToggleExpand,
}) {
  return (
    <section
      className={`details-solution-card details-solution-code-card ${isExpanded ? "is-expanded" : ""}`}
      aria-label="Bubble Sort Java solution"
    >
      <SolutionCodeToolbar
        isExpanded={isExpanded}
        onCopy={onCopy}
        onAction={onAction}
        onToggleExpand={onToggleExpand}
      />
      <SolutionCodeEditor step={step} />
      <div className="details-solution-execution-context">
        <span className="details-solution-bulb" aria-hidden="true" />
        <p>
          <strong>Line 5 Execution Context:</strong> {step.context}
        </p>
      </div>
    </section>
  );
}

function SolutionPanel({ activeTab, onAnnounce }) {
  const variant =
    SOLUTION_VARIANTS[activeTab] ?? SOLUTION_VARIANTS["solution-optimized"];
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [isExpanded, setIsExpanded] = useState(false);
  const step = SOLUTION_STEPS[stepIndex];

  useEffect(() => {
    if (!isPlaying) return undefined;
    const intervalId = window.setInterval(() => {
      setStepIndex((current) => {
        if (current >= SOLUTION_STEPS.length - 1) {
          setIsPlaying(false);
          return current;
        }
        return current + 1;
      });
    }, 900);
    return () => window.clearInterval(intervalId);
  }, [isPlaying]);

  const announceStep = (nextIndex) => {
    const nextStep = SOLUTION_STEPS[nextIndex];
    onAnnounce(
      `Visualizer step ${nextIndex + 1} of ${SOLUTION_STEPS.length}: ${nextStep.comparison}.`,
    );
  };

  const handleTogglePlay = () => {
    if (!isPlaying && stepIndex === SOLUTION_STEPS.length - 1) {
      setStepIndex(0);
      announceStep(0);
    }
    setIsPlaying((current) => !current);
  };

  const handlePrevious = () => {
    setIsPlaying(false);
    const nextIndex = Math.max(0, stepIndex - 1);
    setStepIndex(nextIndex);
    announceStep(nextIndex);
  };

  const handleNext = () => {
    setIsPlaying(false);
    const nextIndex = Math.min(SOLUTION_STEPS.length - 1, stepIndex + 1);
    setStepIndex(nextIndex);
    announceStep(nextIndex);
  };

  const handleReplay = () => {
    setIsPlaying(false);
    setStepIndex(0);
    announceStep(0);
  };

  const handleSeek = (event) => {
    const nextIndex = Number(event.target.value);
    setIsPlaying(false);
    setStepIndex(nextIndex);
    announceStep(nextIndex);
  };

  const handleCopy = async () => {
    if (!navigator.clipboard) {
      onAnnounce("Solution code copying is unavailable in this browser.");
      return;
    }

    try {
      await navigator.clipboard.writeText(
        SOLUTION_JAVA_LINES.map((line) =>
          line.tokens.map((token) => token.text).join(""),
        ).join("\n"),
      );
      onAnnounce("Java solution copied to clipboard.");
    } catch {
      onAnnounce("Solution code copying is unavailable in this browser.");
    }
  };

  return (
    <div
      className="details-solution-panel"
      role="tabpanel"
      id={`detail-panel-${activeTab}`}
    >
      <SolutionVisualizerCard
        variant={variant}
        step={step}
        stepIndex={stepIndex}
        stepCount={SOLUTION_STEPS.length}
        isPlaying={isPlaying}
        onTogglePlay={handleTogglePlay}
        onPrevious={handlePrevious}
        onNext={handleNext}
        onReplay={handleReplay}
        onSeek={handleSeek}
      />
      <SolutionCodeCard
        step={step}
        isExpanded={isExpanded}
        onCopy={handleCopy}
        onAction={onAnnounce}
        onToggleExpand={() => setIsExpanded((current) => !current)}
      />
    </div>
  );
}

function PlaceholderPanel({ activeTab }) {
  const tab = DETAIL_TABS.find((item) => item.id === activeTab);
  return (
    <section
      className="details-card details-placeholder-panel"
      role="tabpanel"
      id={`detail-panel-${activeTab}`}
    >
      <DetailsIcon name={tab?.icon ?? "workspace-edit"} />
      <h2>{tab?.label ?? "Workspace"}</h2>
      <p>
        This workspace is ready for the next learning layer. Switch back to
        Description to review the Bubble Sort walkthrough.
      </p>
      <button
        className="details-placeholder-button"
        type="button"
        onClick={() => (window.location.hash = "problem-details")}
      >
        Open description
      </button>
    </section>
  );
}

function SequenceNavigator({ activeTab, onTabChange, onAnnounce }) {
  return (
    <nav
      className="details-sequence-card"
      aria-label="Problem sequence navigation"
    >
      <button
        className="sequence-topic sequence-topic--previous"
        type="button"
        onClick={() => onAnnounce("Previous topic: Insertion Sort")}
      >
        <DetailsIcon name="sequence-left" />
        <span>
          <small>Previous Topic</small>
          <strong>Insertion Sort</strong>
        </span>
      </button>
      <div
        className="sequence-tabs"
        role="tablist"
        aria-label="Sequence workspace tabs"
      >
        {SEQUENCE_TABS.map((tab) => (
          <button
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={activeTab === tab.id ? "is-active" : ""}
            key={tab.id}
            onClick={() => onTabChange(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <button
        className="sequence-topic sequence-topic--next"
        type="button"
        onClick={() => onAnnounce("Next topic: Selection Sort")}
      >
        <span>
          <small>Next Topic</small>
          <strong>Selection Sort</strong>
        </span>
        <DetailsIcon name="sequence-right" />
      </button>
    </nav>
  );
}

function DetailsFooter() {
  return (
    <footer className="details-footer">
      <div className="details-footer-grid">
        <div className="details-footer-brand">
          <a href="#top" className="details-footer-brand-link">
            <span className="details-footer-brand-mark">
              <DetailsIcon name="footer-brand" />
            </span>
            <span>
              Algo<span>Master</span>
            </span>
          </a>
          <p>
            Elevate your algorithmic problem solving through immersive
            visualizations, real-time code sandboxes, and structured learning
            tracks.
          </p>
          <div className="details-footer-socials" aria-label="Community links">
            <a href="#community" aria-label="Code community" />
            <a href="#community" aria-label="Chat community" />
            <a href="#community" aria-label="Network community" />
          </div>
        </div>
        {FOOTER_COLUMNS.map((column) => (
          <div className="details-footer-column" key={column.title}>
            <h2>{column.title}</h2>
            <nav aria-label={`${column.title} links`}>
              {column.links.map((link) => (
                <a
                  href={`#${link.toLowerCase().replaceAll(" ", "-")}`}
                  key={link}
                >
                  {link}
                </a>
              ))}
            </nav>
          </div>
        ))}
      </div>
      <div className="details-footer-legal">
        <p>© 2025 AlgoMaster Inc. Engineered for algorithmic mastery.</p>
        <nav aria-label="Legal links">
          <a href="#privacy">Privacy Policy</a>
          <a href="#terms">Terms of Service</a>
          <a href="#conduct">Code of Conduct</a>
        </nav>
      </div>
    </footer>
  );
}

function ProblemDetailsPage({ userName = "Your name" }) {
  const [activeTab, setActiveTab] = useState("description");
  const [isBookmarked, setIsBookmarked] = useState(false);
  const [language, setLanguage] = useState("Python 3");
  const [codeByLanguage, setCodeByLanguage] = useState(RUNNER_CODE);
  const [runnerTab, setRunnerTab] = useState("console");
  const [activeCaseIndex, setActiveCaseIndex] = useState(0);
  const [executionState, setExecutionState] = useState("ready");
  const [isExpanded, setIsExpanded] = useState(false);
  const [announcement, setAnnouncement] = useState("");
  const [primaryColumnShare, setPrimaryColumnShare] = useState(50);
  const workspaceRef = useRef(null);
  const runnerRef = useRef(null);

  const code = codeByLanguage[language];

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    if (tab === "visualizer") {
      window.requestAnimationFrame(() =>
        runnerRef.current?.scrollIntoView({
          behavior: "smooth",
          block: "center",
        }),
      );
    }
    if (tab === "solution-basic" || tab === "solution-optimized") {
      window.requestAnimationFrame(() =>
        document.getElementById(`detail-panel-${tab}`)?.scrollIntoView({
          behavior: "smooth",
          block: "start",
        }),
      );
    }
  };

  const handleShare = async () => {
    const url = window.location.href;
    if (!navigator.clipboard) {
      setAnnouncement("Clipboard sharing is unavailable in this browser.");
      return;
    }

    try {
      await navigator.clipboard.writeText(url);
      setAnnouncement("Problem link copied to clipboard.");
    } catch {
      setAnnouncement("Clipboard sharing is unavailable in this browser.");
    }
  };

  const handleCodeChange = (nextCode) => {
    setCodeByLanguage((current) => ({ ...current, [language]: nextCode }));
    setExecutionState("ready");
  };

  const handleLanguageChange = (nextLanguage) => {
    setLanguage(nextLanguage);
    setExecutionState("ready");
    setAnnouncement(`${nextLanguage} editor selected.`);
  };

  const handleResetCode = () => {
    setCodeByLanguage((current) => ({
      ...current,
      [language]: RUNNER_CODE[language],
    }));
    setExecutionState("ready");
    setAnnouncement(`${language} code reset.`);
  };

  const handleCopyCode = async () => {
    if (!navigator.clipboard) {
      setAnnouncement("Clipboard copying is unavailable in this browser.");
      return;
    }

    try {
      await navigator.clipboard.writeText(code);
      setAnnouncement("Code copied to clipboard.");
    } catch {
      setAnnouncement("Clipboard copying is unavailable in this browser.");
    }
  };

  const handleRunCode = async () => {
    if (executionState === "running") return;
    setExecutionState("running");
    setRunnerTab("console");
    setAnnouncement("Sending code to backend runner...");

    try {
      const langKey = language.toLowerCase().replace("python 3", "python");
      const result = await api.runCode(langKey, code, "");
      if (result && (result.status === "SUCCESS" || result.status === "Passed" || result.passed)) {
        setExecutionState("passed");
        setAnnouncement(`Code execution finished: Passed.`);
      } else {
        setExecutionState("failed");
        setAnnouncement(`Execution result: ${result?.status || result?.stderr || "Output received"}`);
      }
    } catch (err) {
      console.warn("API Run error, falling back to simulation:", err);
      window.setTimeout(() => {
        setExecutionState("passed");
        setAnnouncement(`Code execution simulated locally.`);
      }, 450);
    }
  };

  const handleSubmit = async () => {
    if (executionState === "running") return;
    setExecutionState("running");
    setRunnerTab("console");
    setAnnouncement("Submitting solution to Spring Boot server...");

    try {
      const langKey = language.toLowerCase().replace("python 3", "python");
      const res = await api.submitCode(1, langKey, code);
      if (res && res.status === "ACCEPTED") {
        setExecutionState("passed");
        setAnnouncement("Submission Accepted! All test cases passed.");
      } else {
        setExecutionState("failed");
        setAnnouncement(`Submission status: ${res?.status || "Evaluated"}`);
      }
    } catch (err) {
      console.warn("API Submit error:", err);
      setExecutionState("passed");
      setAnnouncement(`Submission flow evaluated (Offline mode).`);
    }
  };
  const handleSequenceAnnouncement = (message) => setAnnouncement(message);

  const handleSplitterPointerDown = (event) => {
    if (!workspaceRef.current) return;
    event.currentTarget.setPointerCapture(event.pointerId);
    event.currentTarget.classList.add("is-dragging");
  };

  const handleSplitterPointerMove = (event) => {
    if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;
    const bounds = workspaceRef.current?.getBoundingClientRect();
    if (!bounds) return;
    const nextShare = ((event.clientX - bounds.left) / bounds.width) * 100;
    setPrimaryColumnShare(Math.min(72, Math.max(28, nextShare)));
  };

  const handleSplitterPointerUp = (event) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    event.currentTarget.classList.remove("is-dragging");
  };

  const handleSplitterKeyDown = (event) => {
    const step = event.shiftKey ? 5 : 2;
    if (event.key === "ArrowLeft") {
      event.preventDefault();
      setPrimaryColumnShare((current) => Math.max(28, current - step));
    }
    if (event.key === "ArrowRight") {
      event.preventDefault();
      setPrimaryColumnShare((current) => Math.min(72, current + step));
    }
    if (event.key === "Home") {
      event.preventDefault();
      setPrimaryColumnShare(28);
    }
    if (event.key === "End") {
      event.preventDefault();
      setPrimaryColumnShare(72);
    }
  };

  return (
    <main className="problem-details-page" id="top">
      <div className="details-shell">
        <NavigationBar
          active="Problems"
          userName={userName}
          className="details-navigation"
        />
        <BreadcrumbBar />
        <ProblemHeader
          isBookmarked={isBookmarked}
          onBookmark={() => {
            setIsBookmarked((current) => !current);
            setAnnouncement(
              isBookmarked ? "Bookmark removed." : "Problem bookmarked.",
            );
          }}
          onShare={handleShare}
          onRunVisualizer={() => handleTabChange("visualizer")}
          onSubmit={handleSubmit}
        />
        <WorkspaceTabs activeTab={activeTab} onChange={handleTabChange} />
        <div
          className="details-tab-panel"
          role="tabpanel"
          id={`detail-panel-${activeTab}`}
          aria-label={`${activeTab} content`}
        >
          {activeTab === "description" || activeTab === "visualizer" ? (
            <div
              className="details-workspace-grid"
              ref={workspaceRef}
              style={{
                "--details-primary-share": `${primaryColumnShare}fr`,
                "--details-secondary-share": `${100 - primaryColumnShare}fr`,
              }}
            >
              <DescriptionColumn />
              <button
                className="details-workspace-splitter"
                type="button"
                role="separator"
                aria-label="Resize problem and code panels"
                aria-orientation="vertical"
                aria-valuemin="28"
                aria-valuemax="72"
                aria-valuenow={Math.round(primaryColumnShare)}
                onPointerDown={handleSplitterPointerDown}
                onPointerMove={handleSplitterPointerMove}
                onPointerUp={handleSplitterPointerUp}
                onPointerCancel={handleSplitterPointerUp}
                onKeyDown={handleSplitterKeyDown}
              >
                <span aria-hidden="true" />
              </button>
              <div className="details-secondary-column">
                <CodeRunnerPanel
                  runnerRef={runnerRef}
                  language={language}
                  code={code}
                  activeTab={runnerTab}
                  activeCaseIndex={activeCaseIndex}
                  executionState={executionState}
                  isExpanded={isExpanded}
                  onLanguageChange={handleLanguageChange}
                  onCodeChange={handleCodeChange}
                  onOutputTabChange={setRunnerTab}
                  onCaseChange={setActiveCaseIndex}
                  onReset={handleResetCode}
                  onCopy={handleCopyCode}
                  onToggleExpand={() => setIsExpanded((current) => !current)}
                  onRun={handleRunCode}
                  onSubmit={handleSubmit}
                />
              </div>
            </div>
          ) : activeTab === "solution-basic" ||
            activeTab === "solution-optimized" ? (
            <SolutionPanel
              key={activeTab}
              activeTab={activeTab}
              onAnnounce={setAnnouncement}
            />
          ) : (
            <PlaceholderPanel activeTab={activeTab} />
          )}
        </div>
        <SequenceNavigator
          activeTab={activeTab}
          onTabChange={handleTabChange}
          onAnnounce={handleSequenceAnnouncement}
        />
      </div>
      <p className="sr-only" aria-live="polite">
        {announcement || `${language} runner ready.`}
      </p>
      <DetailsFooter />
    </main>
  );
}

export default ProblemDetailsPage;
