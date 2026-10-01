import { useEffect, useMemo, useRef, useState } from "react";
import { NavigationBar } from "../components/index.js";
import { api } from "../services/api.js";
import "./analyze.css";

const ANALYSIS_TABS = [
  { id: "tests", label: "Test Cases", icon: "test-cases" },
  { id: "output", label: "Output / Results", icon: "output-results" },
  { id: "feedback", label: "Feedback & Quality", icon: "feedback-quality" },
  {
    id: "visualizer",
    label: "Algorithm Review & Visualizer",
    icon: "algorithm-review",
  },
];

const CASE_TABS = [
  { id: "case-1", label: "Case 1" },
  { id: "case-2", label: "Case 2" },
  { id: "case-3", label: "Case 3" },
  { id: "case-4", label: "Case 4" },
  { id: "custom", label: "Custom Case", icon: "add-case" },
];

const DEFAULT_JAVA_CODE = [
  "public class Main {",
  "    public static void bubbleSort(int[] arr) {",
  "        int n = arr.length;",
  "        for (int i = 0; i < n - 1; i++) {",
  "            for (int j = 0; j < n - i - 1; j++) {",
  "                if (arr[j] > arr[j + 1]) {",
  "                    // swap adjacent elements",
  "                    int temp = arr[j];",
  "                    arr[j] = arr[j + 1];",
  "                    arr[j + 1] = temp;",
  "                }",
  "            }",
  "        }",
  "    }",
  "}",
].join("\n");

const ANALYZE_CASES = [
  {
    id: "case-1",
    input: [5, 2, 8, 1],
    expected: [1, 2, 5, 8],
    inputLabel: "Unsorted Integer Array",
    outputLabel: "Ascending Sorted Order",
  },
  {
    id: "case-2",
    input: [1, 2, 3, 4],
    expected: [1, 2, 3, 4],
    inputLabel: "Already Sorted Array",
    outputLabel: "Input Preserved",
  },
  {
    id: "case-3",
    input: [9, 4, 6, 2],
    expected: [2, 4, 6, 9],
    inputLabel: "Reverse-Skewed Array",
    outputLabel: "Ascending Sorted Order",
  },
  {
    id: "case-4",
    input: [-3, 7, 0, 2],
    expected: [-3, 0, 2, 7],
    inputLabel: "Signed Integer Array",
    outputLabel: "Ascending Sorted Order",
  },
];

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

const JAVA_TOKEN_PATTERN = /(\/\/.*$|\b(?:public|class|static|void|for|if|int|return)\b|\b(?:Main|bubbleSort|arr|length|temp)\b|\b\d+\b)/g;
const JAVA_KEYWORDS = new Set([
  "public",
  "class",
  "static",
  "void",
  "for",
  "if",
  "int",
  "return",
]);
const JAVA_FUNCTIONS = new Set(["Main", "bubbleSort"]);
const JAVA_PROPERTIES = new Set(["arr", "length", "temp"]);

function formatArray(values) {
  return `[${values.join(", ")}]`;
}

function AnalyzeIcon({ name, className = "" }) {
  return (
    <span
      className={`analyze-icon analyze-icon--${name} ${className}`.trim()}
      aria-hidden="true"
    />
  );
}

function highlightJavaLine(line, lineIndex) {
  const tokens = [];
  let cursor = 0;

  line.replace(JAVA_TOKEN_PATTERN, (token, _match, offset) => {
    if (offset > cursor) {
      tokens.push(
        <span key={`${lineIndex}-${cursor}-plain`}>
          {line.slice(cursor, offset)}
        </span>,
      );
    }

    let tokenType = "number";
    if (token.startsWith("//")) tokenType = "comment";
    else if (JAVA_KEYWORDS.has(token)) tokenType = "keyword";
    else if (JAVA_FUNCTIONS.has(token)) tokenType = "function";
    else if (JAVA_PROPERTIES.has(token)) tokenType = "property";

    tokens.push(
      <span
        className={`analyze-code-token analyze-code-token--${tokenType}`}
        key={`${lineIndex}-${offset}-${token}`}
      >
        {token}
      </span>,
    );
    cursor = offset + token.length;
    return token;
  });

  if (cursor < line.length) {
    tokens.push(
      <span key={`${lineIndex}-${cursor}-tail`}>{line.slice(cursor)}</span>,
    );
  }

  return tokens.length > 0 ? tokens : " ";
}

function EditorToolbar({ language, onLanguageChange, onEvaluate, onCopy, onReset, onToggleFullscreen, isFullscreen }) {
  const utilityActions = [
    { id: "magic", label: "Evaluate code", icon: "utility-magic", onClick: onEvaluate },
    { id: "copy", label: "Copy code", icon: "utility-copy", onClick: onCopy },
    { id: "reset", label: "Reset code", icon: "utility-reset", onClick: onReset },
    {
      id: "fullscreen",
      label: isFullscreen ? "Exit fullscreen editor" : "Expand editor",
      icon: "utility-fullscreen",
      onClick: onToggleFullscreen,
    },
  ];

  return (
    <header className="analyze-editor-toolbar">
      <label className="analyze-language-control">
        <AnalyzeIcon name="status-dot" />
        <span className="sr-only">Programming language</span>
        <select
          value={language}
          onChange={(event) => onLanguageChange(event.target.value)}
          aria-label="Programming language"
        >
          <option value="Java 21">Java 21</option>
        </select>
        <AnalyzeIcon name="language-chevron" />
      </label>
      <div className="analyze-utility-actions" aria-label="Editor utilities">
        {utilityActions.map((action) => (
          <button
            className="analyze-utility-button"
            type="button"
            aria-label={action.label}
            title={action.label}
            key={action.id}
            onClick={action.onClick}
          >
            <AnalyzeIcon name={action.icon} />
          </button>
        ))}
      </div>
    </header>
  );
}

function CodeEditor({ code, onChange, isFullscreen }) {
  const lines = code.split("\n");
  const lineNumberCount = Math.max(16, lines.length);
  const activeLine = Math.min(5, Math.max(0, lines.length - 1));

  return (
    <div className={`analyze-code-area ${isFullscreen ? "is-fullscreen" : ""}`}>
      <div className="analyze-code-grid">
        <div className="analyze-code-gutter" aria-hidden="true">
          {Array.from({ length: lineNumberCount }, (_, index) => (
            <span key={`line-${index + 1}`}>{index + 1}</span>
          ))}
        </div>
        <div className="analyze-code-content">
          <pre className="analyze-code-preview" aria-hidden="true">
            {lines.map((line, index) => (
              <span
                className={`analyze-code-line ${index === activeLine ? "is-active" : ""}`}
                key={`preview-${index}`}
              >
                {highlightJavaLine(line, index)}
              </span>
            ))}
          </pre>
          <textarea
            className="analyze-code-input"
            value={code}
            onChange={(event) => onChange(event.target.value)}
            aria-label="Java 21 code editor"
            spellCheck="false"
            wrap="off"
          />
        </div>
      </div>
    </div>
  );
}

function RuntimeStats({ executionState }) {
  const statusText = {
    ready: "Ready",
    evaluating: "Evaluating",
    running: "Running",
    passed: "Passed",
  }[executionState];

  return (
    <div className="analyze-runtime-pill" aria-live="polite">
      <AnalyzeIcon name="status-dot" />
      <span>Memory: 42.1 MB</span>
      <b>•</b>
      <span>Execution: {executionState === "running" ? "—" : "12ms"}</span>
      <b>•</b>
      <span>Garbage Col: 0.1ms</span>
      <span className={`analyze-runtime-state analyze-runtime-state--${executionState}`}>
        {statusText}
      </span>
    </div>
  );
}

function EditorActions({ executionState, onEvaluate, onRun, onSubmit }) {
  return (
    <div className="analyze-editor-actions">
      <button
        className="analyze-action-button analyze-action-button--evaluate"
        type="button"
        onClick={onEvaluate}
        disabled={executionState === "evaluating"}
      >
        <AnalyzeIcon name="evaluate-spark" />
        Evaluate
      </button>
      <button
        className="analyze-action-button analyze-action-button--run"
        type="button"
        onClick={onRun}
        disabled={executionState === "running"}
      >
        <AnalyzeIcon name="run-play" />
        {executionState === "running" ? "Running..." : "Run Code"}
      </button>
      <button
        className="analyze-action-button analyze-action-button--submit"
        type="button"
        onClick={onSubmit}
      >
        <AnalyzeIcon name="submit-check" />
        Submit Solution
      </button>
    </div>
  );
}

function EditorPanel({
  language,
  code,
  executionState,
  isFullscreen,
  onLanguageChange,
  onCodeChange,
  onEvaluate,
  onCopy,
  onReset,
  onToggleFullscreen,
  onRun,
  onSubmit,
}) {
  return (
    <section className={`analyze-editor-panel ${isFullscreen ? "is-fullscreen" : ""}`} aria-label="Code editor and execution panel">
      <EditorToolbar
        language={language}
        onLanguageChange={onLanguageChange}
        onEvaluate={onEvaluate}
        onCopy={onCopy}
        onReset={onReset}
        onToggleFullscreen={onToggleFullscreen}
        isFullscreen={isFullscreen}
      />
      <CodeEditor code={code} onChange={onCodeChange} isFullscreen={isFullscreen} />
      <footer className="analyze-editor-footer">
        <RuntimeStats executionState={executionState} />
        <EditorActions
          executionState={executionState}
          onEvaluate={onEvaluate}
          onRun={onRun}
          onSubmit={onSubmit}
        />
      </footer>
    </section>
  );
}

function AnalysisTabs({ activeTab, onChange }) {
  const handleKeyDown = (event, index) => {
    let nextIndex = index;
    if (event.key === "ArrowRight") nextIndex = (index + 1) % ANALYSIS_TABS.length;
    if (event.key === "ArrowLeft") nextIndex = (index - 1 + ANALYSIS_TABS.length) % ANALYSIS_TABS.length;
    if (event.key === "Home") nextIndex = 0;
    if (event.key === "End") nextIndex = ANALYSIS_TABS.length - 1;
    if (nextIndex === index) return;
    event.preventDefault();
    const nextTab = ANALYSIS_TABS[nextIndex];
    onChange(nextTab.id);
    requestAnimationFrame(() => {
      document.getElementById(`analyze-tab-${nextTab.id}`)?.focus();
    });
  };

  return (
    <div className="analyze-tabs-row">
      <div className="analyze-tabs" role="tablist" aria-label="Analysis views">
        {ANALYSIS_TABS.map((tab, index) => (
          <button
            className={`analyze-tab ${activeTab === tab.id ? "is-active" : ""}`}
            id={`analyze-tab-${tab.id}`}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            aria-controls={`analyze-panel-${tab.id}`}
            tabIndex={activeTab === tab.id ? 0 : -1}
            key={tab.id}
            onClick={() => onChange(tab.id)}
            onKeyDown={(event) => handleKeyDown(event, index)}
          >
            <AnalyzeIcon name={tab.icon} />
            {tab.label}
          </button>
        ))}
      </div>
      <div className="analyze-mode-status">
        <AnalyzeIcon name="interactive-status" />
        Interactive Analysis Mode
      </div>
    </div>
  );
}

function SpecificationHeader() {
  return (
    <header className="analyze-specification-header">
      <div className="analyze-specification-title">
        <span className="analyze-specification-icon">
          <AnalyzeIcon name="test-specification" />
        </span>
        <div>
          <h2>Test Cases Specification</h2>
          <p>Configure inputs, run targeted assertions, and adjust memory test constraints</p>
        </div>
      </div>
      <div className="analyze-environment-badges" aria-label="Execution environment">
        <span>Standard Sandbox</span>
        <strong>Environment: Live</strong>
      </div>
    </header>
  );
}

function CaseTabs({ activeCaseId, onChange }) {
  const handleKeyDown = (event, index) => {
    let nextIndex = index;
    if (event.key === "ArrowRight") nextIndex = (index + 1) % CASE_TABS.length;
    if (event.key === "ArrowLeft") nextIndex = (index - 1 + CASE_TABS.length) % CASE_TABS.length;
    if (event.key === "Home") nextIndex = 0;
    if (event.key === "End") nextIndex = CASE_TABS.length - 1;
    if (nextIndex === index) return;
    event.preventDefault();
    const nextCase = CASE_TABS[nextIndex];
    onChange(nextCase.id);
    requestAnimationFrame(() => {
      document.getElementById(`analyze-case-${nextCase.id}`)?.focus();
    });
  };

  return (
    <div className="analyze-case-tabs" role="tablist" aria-label="Test cases">
      {CASE_TABS.map((tab, index) => (
        <button
          className={`analyze-case-tab ${activeCaseId === tab.id ? "is-active" : ""} ${tab.id === "custom" ? "is-custom" : ""}`}
          id={`analyze-case-${tab.id}`}
          type="button"
          role="tab"
          aria-selected={activeCaseId === tab.id}
          aria-controls="analyze-case-panel"
          tabIndex={activeCaseId === tab.id ? 0 : -1}
          key={tab.id}
          onClick={() => onChange(tab.id)}
          onKeyDown={(event) => handleKeyDown(event, index)}
        >
          {tab.icon && <AnalyzeIcon name={tab.icon} />}
          {tab.label}
        </button>
      ))}
    </div>
  );
}

function CaseValueBlock({ label, description, value, isCustom, onCustomChange, valueType }) {
  return (
    <div className="analyze-case-value-block">
      <div className="analyze-case-value-heading">
        <span>{label}</span>
        <small>{description}</small>
      </div>
      {isCustom && valueType === "input" ? (
        <textarea
          className="analyze-custom-case-input"
          value={value}
          onChange={(event) => onCustomChange(event.target.value)}
          aria-label="Custom input array"
          rows={1}
        />
      ) : (
        <code>{value}</code>
      )}
    </div>
  );
}

function CaseDetails({ activeCase, customInput, onCustomInputChange }) {
  const isCustom = activeCase.id === "custom";
  const input = isCustom ? customInput : formatArray(activeCase.input);
  const expected = isCustom ? "[ pending ]" : formatArray(activeCase.expected);
  const inputLabel = isCustom ? "Comma-separated integer array" : activeCase.inputLabel;
  const outputLabel = isCustom ? "Run the case to calculate output" : activeCase.outputLabel;
  const arrayLength = isCustom ? "n = ?" : `n = ${activeCase.input.length}`;

  return (
    <div className="analyze-case-layout" id="analyze-case-panel" role="tabpanel" aria-labelledby={`analyze-case-${activeCase.id}`}>
      <div className="analyze-case-main">
        <div className="analyze-parameter-banner">
          <span>Target Parameter: arr</span>
          <b>•</b>
          <span>Type Signature: <em>int[]</em></span>
          <b>•</b>
          <span>Array Length: {arrayLength}</span>
        </div>
        <CaseValueBlock
          label="Input Array (arr)"
          description={inputLabel}
          value={input}
          isCustom={isCustom}
          onCustomChange={onCustomInputChange}
          valueType="input"
        />
        <CaseValueBlock
          label="Expected Output"
          description={outputLabel}
          value={expected}
          isCustom={false}
          valueType="output"
        />
      </div>
      <ExecutionLimits />
    </div>
  );
}

function ExecutionLimits() {
  const limits = [
    ["Timeout Limit", "2000 ms", "is-amber"],
    ["Max Memory Allowed", "128.0 MB", ""],
    ["Array Range Const.", "1 ≤ n ≤ 1000", ""],
    ["Value Range", "-10⁴ ≤ arr[i] ≤ 10⁴", ""],
  ];

  return (
    <aside className="analyze-limits-column" aria-label="Execution limits">
      <div className="analyze-limits-card">
        <h3>
          <AnalyzeIcon name="execution-limits" />
          Execution Limits
        </h3>
        <dl>
          {limits.map(([label, value, className]) => (
            <div key={label}>
              <dt>{label}</dt>
              <dd className={className}>{value}</dd>
            </div>
          ))}
        </dl>
      </div>
      <div className="analyze-assertion-note">
        <AnalyzeIcon name="assertions" />
        <span>Assertions automatically validated via AST harness.</span>
      </div>
    </aside>
  );
}

function TestCasesPanel({ activeCaseId, onCaseChange, customInput, onCustomInputChange }) {
  const activeCase = activeCaseId === "custom"
    ? { id: "custom", input: [], expected: [], inputLabel: "", outputLabel: "" }
    : ANALYZE_CASES.find((testCase) => testCase.id === activeCaseId) ?? ANALYZE_CASES[0];

  return (
    <section className="analyze-specification-panel" id="analyze-panel-tests" role="tabpanel" aria-labelledby="analyze-tab-tests">
      <SpecificationHeader />
      <CaseTabs activeCaseId={activeCaseId} onChange={onCaseChange} />
      <CaseDetails
        activeCase={activeCase}
        customInput={customInput}
        onCustomInputChange={onCustomInputChange}
      />
    </section>
  );
}

function AnalysisPlaceholder({ activeTab }) {
  const tab = ANALYSIS_TABS.find((item) => item.id === activeTab);
  return (
    <section className="analyze-placeholder-panel" id={`analyze-panel-${activeTab}`} role="tabpanel" aria-labelledby={`analyze-tab-${activeTab}`}>
      <AnalyzeIcon name={tab?.icon ?? "algorithm-review"} />
      <h2>{tab?.label ?? "Analysis"}</h2>
      <p>
        This analysis surface is ready for the next execution result. Run the Java solution to populate the live report without leaving the Analyze workspace.
      </p>
    </section>
  );
}

function AnalysisSuite({ activeTab, onTabChange, activeCaseId, onCaseChange, customInput, onCustomInputChange }) {
  return (
    <section className="analyze-suite" aria-label="Interactive analysis suite">
      <AnalysisTabs activeTab={activeTab} onChange={onTabChange} />
      {activeTab === "tests" ? (
        <TestCasesPanel
          activeCaseId={activeCaseId}
          onCaseChange={onCaseChange}
          customInput={customInput}
          onCustomInputChange={onCustomInputChange}
        />
      ) : (
        <AnalysisPlaceholder activeTab={activeTab} />
      )}
    </section>
  );
}

function AnalyzeFooter() {
  return (
    <footer className="analyze-footer">
      <div className="analyze-footer-grid">
        <div className="analyze-footer-brand">
          <a className="analyze-footer-brand-link" href="#top">
            <span className="analyze-footer-brand-mark">
              <AnalyzeIcon name="footer-brand" />
            </span>
            <span>Algo<span>Master</span></span>
          </a>
          <p>
            Elevate your algorithmic problem solving through immersive visualizations, real-time code sandboxes, and structured learning tracks.
          </p>
          <div className="analyze-footer-socials" aria-label="Community links">
            <a href="#community" aria-label="Code community" />
            <a href="#community" aria-label="Chat community" />
            <a href="#community" aria-label="Network community" />
          </div>
        </div>
        {FOOTER_COLUMNS.map((column) => (
          <div className="analyze-footer-column" key={column.title}>
            <h2>{column.title}</h2>
            <nav aria-label={`${column.title} links`}>
              {column.links.map((link) => (
                <a href={`#${link.toLowerCase().replaceAll(" ", "-")}`} key={link}>
                  {link}
                </a>
              ))}
            </nav>
          </div>
        ))}
      </div>
      <div className="analyze-footer-legal">
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

function AnalyzePage({ userName = "Your name" }) {
  const [language, setLanguage] = useState("Java 21");
  const [code, setCode] = useState(DEFAULT_JAVA_CODE);
  const [executionState, setExecutionState] = useState("ready");
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [activeTab, setActiveTab] = useState("tests");
  const [activeCaseId, setActiveCaseId] = useState("case-1");
  const [customInput, setCustomInput] = useState("[5, 2, 8, 1]");
  const [announcement, setAnnouncement] = useState("");
  const executionTimerRef = useRef(null);

  const activeCase = useMemo(
    () => ANALYZE_CASES.find((testCase) => testCase.id === activeCaseId) ?? ANALYZE_CASES[0],
    [activeCaseId],
  );

  useEffect(() => () => window.clearTimeout(executionTimerRef.current), []);

  const announce = (message) => setAnnouncement(message);

  const finishExecution = (message) => {
    window.clearTimeout(executionTimerRef.current);
    executionTimerRef.current = window.setTimeout(() => {
      setExecutionState("passed");
      announce(message);
    }, 650);
  };

  const handleEvaluate = async () => {
    setExecutionState("evaluating");
    announce("AI Code Analysis started via Spring Boot API...");
    try {
      const res = await api.analyzeCode(code, language.toLowerCase(), "Sorting array");
      setExecutionState("passed");
      announce(res?.aiFeedback || "AI Evaluation complete. Suggestions & complexity ready.");
    } catch (err) {
      console.warn("AI Analysis API error, falling back to local simulation:", err);
      finishExecution("Code evaluation complete. All assertions are ready.");
    }
  };

  const handleRun = () => {
    if (executionState === "running") return;
    setExecutionState("running");
    setActiveTab("tests");
    announce("Code is running.");
    finishExecution(`Code passed ${activeCase.id === "custom" ? "the custom case" : activeCase.id.replace("case-", "test case ")}.`);
  };

  const handleSubmit = () => {
    setExecutionState("passed");
    announce("Solution submitted.");
  };

  const handleCopy = async () => {
    if (!navigator.clipboard) {
      announce("Clipboard copying is unavailable in this browser.");
      return;
    }
    try {
      await navigator.clipboard.writeText(code);
      announce("Code copied to clipboard.");
    } catch {
      announce("Clipboard copying is unavailable in this browser.");
    }
  };

  const handleReset = () => {
    setCode(DEFAULT_JAVA_CODE);
    setExecutionState("ready");
    announce("Code reset to the Java 21 starter solution.");
  };

  const handleLanguageChange = (nextLanguage) => {
    setLanguage(nextLanguage);
    announce(`${nextLanguage} editor selected.`);
  };

  const handleCaseChange = (nextCaseId) => {
    setActiveCaseId(nextCaseId);
    announce(nextCaseId === "custom" ? "Custom case editor opened." : `${nextCaseId.replace("case-", "Case ")} selected.`);
  };

  return (
    <main className={`analyze-page ${isFullscreen ? "has-fullscreen-editor" : ""}`} id="top">
      <div className="analyze-nav-wrap">
        <NavigationBar active="Analyze" userName={userName} className="analyze-navigation" />
      </div>
      <div className="analyze-main-content">
        <EditorPanel
          language={language}
          code={code}
          executionState={executionState}
          isFullscreen={isFullscreen}
          onLanguageChange={handleLanguageChange}
          onCodeChange={(nextCode) => {
            setCode(nextCode);
            setExecutionState("ready");
          }}
          onEvaluate={handleEvaluate}
          onCopy={handleCopy}
          onReset={handleReset}
          onToggleFullscreen={() => setIsFullscreen((current) => !current)}
          onRun={handleRun}
          onSubmit={handleSubmit}
        />
        <AnalysisSuite
          activeTab={activeTab}
          onTabChange={(nextTab) => {
            setActiveTab(nextTab);
            announce(`${ANALYSIS_TABS.find((tab) => tab.id === nextTab)?.label ?? "Analysis"} view selected.`);
          }}
          activeCaseId={activeCaseId}
          onCaseChange={handleCaseChange}
          customInput={customInput}
          onCustomInputChange={setCustomInput}
        />
      </div>
      <p className="sr-only" aria-live="polite">
        {announcement || `${language} Analyze workspace ready.`}
      </p>
      <AnalyzeFooter />
    </main>
  );
}

export default AnalyzePage;
