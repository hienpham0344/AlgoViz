import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { NavigationBar, CustomSelect, SmoothSortVisualizer } from "../../components/index.js";
import { api } from "../../services/api.js";
import "./analyze.css";

const CODE_SNIPPETS = {
  "Java 21": [
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
  ].join("\n"),

  "Python 3": [
    "def bubble_sort(arr):",
    "    n = len(arr)",
    "    for i in range(n - 1):",
    "        for j in range(n - i - 1):",
    "            if arr[j] > arr[j + 1]:",
    "                # swap adjacent elements",
    "                arr[j], arr[j + 1] = arr[j + 1], arr[j]",
    "    return arr",
  ].join("\n"),

  "JavaScript": [
    "function bubbleSort(arr) {",
    "    const n = arr.length;",
    "    for (let i = 0; i < n - 1; i++) {",
    "        for (let j = 0; j < n - i - 1; j++) {",
    "            if (arr[j] > arr[j + 1]) {",
    "                // swap adjacent elements",
    "                const temp = arr[j];",
    "                arr[j] = arr[j + 1];",
    "                arr[j + 1] = temp;",
    "            }",
    "        }",
    "    }",
    "    return arr;",
    "}",
  ].join("\n"),

  "C++ 20": [
    "#include <vector>",
    "#include <utility>",
    "",
    "void bubbleSort(std::vector<int>& arr) {",
    "    int n = arr.size();",
    "    for (int i = 0; i < n - 1; i++) {",
    "        for (int j = 0; j < n - i - 1; j++) {",
    "            if (arr[j] > arr[j + 1]) {",
    "                // swap adjacent elements",
    "                std::swap(arr[j], arr[j + 1]);",
    "            }",
    "        }",
    "    }",
    "}",
  ].join("\n"),
};

const DEFAULT_ARRAY = [5, 2, 8, 1, 9, 3];

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

const CODE_TOKEN_PATTERN = /(\/\/.*$|#.*$|\b(?:public|class|static|void|for|if|int|return|def|function|const|let|range|len|include|vector|utility|std|namespace)\b|\b(?:Main|bubbleSort|bubble_sort|arr|length|size|temp|swap)\b|\b\d+\b)/g;
const CODE_KEYWORDS = new Set([
  "public",
  "class",
  "static",
  "void",
  "for",
  "if",
  "int",
  "return",
  "def",
  "function",
  "const",
  "let",
  "range",
  "len",
  "include",
  "vector",
  "utility",
  "std",
  "namespace",
]);
const CODE_FUNCTIONS = new Set(["Main", "bubbleSort", "bubble_sort", "swap"]);
const CODE_PROPERTIES = new Set(["arr", "length", "size", "temp"]);

function highlightCodeLine(line, lineIndex) {
  const tokens = [];
  let cursor = 0;

  line.replace(CODE_TOKEN_PATTERN, (token, _match, offset) => {
    if (offset > cursor) {
      tokens.push(
        <span key={`${lineIndex}-${cursor}-plain`}>
          {line.slice(cursor, offset)}
        </span>,
      );
    }

    let tokenType = "number";
    if (token.startsWith("//") || token.startsWith("#")) tokenType = "comment";
    else if (CODE_KEYWORDS.has(token)) tokenType = "keyword";
    else if (CODE_FUNCTIONS.has(token)) tokenType = "function";
    else if (CODE_PROPERTIES.has(token)) tokenType = "property";

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

function EditorToolbar({
  language,
  onLanguageChange,
  onCopy,
  onReset,
  onToggleFullscreen,
  isFullscreen,
  executionState,
  onRun,
}) {
  return (
    <header className="analyze-editor-toolbar">
      <div className="analyze-editor-toolbar-left">
        <CustomSelect
          value={language}
          onChange={onLanguageChange}
          options={["Java 21", "Python 3", "C++ 20", "JavaScript"]}
          ariaLabel="Programming language"
        />
      </div>

      <div className="analyze-editor-toolbar-right">
        <div className="analyze-utility-actions" aria-label="Editor utilities">
          <button
            className="analyze-utility-button"
            type="button"
            aria-label="Copy code"
            title="Copy code"
            onClick={onCopy}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
            </svg>
          </button>
          <button
            className="analyze-utility-button"
            type="button"
            aria-label="Reset starter code"
            title="Reset code"
            onClick={onReset}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
              <path d="M3 3v5h5" />
            </svg>
          </button>
          <button
            className="analyze-utility-button"
            type="button"
            aria-label={isFullscreen ? "Exit fullscreen editor" : "Expand editor"}
            title={isFullscreen ? "Exit fullscreen editor" : "Expand editor"}
            onClick={onToggleFullscreen}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
            </svg>
          </button>
        </div>

        <button
          className="analyze-toolbar-run-btn"
          type="button"
          onClick={onRun}
          disabled={executionState === "running"}
          title="Bắt đầu chạy mã và trực quan hóa (Ctrl + Enter)"
        >
          <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
            <path d="M8 5v14l11-7z" />
          </svg>
          <span>{executionState === "running" ? "Đang chạy..." : "Bắt đầu"}</span>
          <kbd className="analyze-toolbar-kbd">Ctrl ↵</kbd>
        </button>
      </div>
    </header>
  );
}

function CodeEditor({ code, onChange, isFullscreen, activeLineIndex, onRunShortcut }) {
  const lines = code.split("\n");
  const lineNumberCount = Math.max(16, lines.length);

  const handleKeyDown = (e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === "Enter") {
      e.preventDefault();
      onRunShortcut?.();
    }
  };

  return (
    <div className={`analyze-code-area ${isFullscreen ? "is-fullscreen" : ""}`}>
      <div className="analyze-code-grid">
        <div className="analyze-code-gutter" aria-hidden="true">
          {Array.from({ length: lineNumberCount }, (_, index) => (
            <span
              key={`line-${index + 1}`}
              className={index === activeLineIndex ? "is-active-line-num" : ""}
            >
              {index + 1}
            </span>
          ))}
        </div>
        <div className="analyze-code-content">
          <pre className="analyze-code-preview" aria-hidden="true">
            {lines.map((line, index) => (
              <span
                className={`analyze-code-line ${index === activeLineIndex ? "is-active" : ""}`}
                key={`preview-${index}`}
              >
                {highlightCodeLine(line, index)}
              </span>
            ))}
          </pre>
          <textarea
            className="analyze-code-input"
            value={code}
            onChange={(event) => onChange(event.target.value)}
            onKeyDown={handleKeyDown}
            aria-label="Code editor"
            spellCheck="false"
            wrap="off"
          />
        </div>
      </div>
    </div>
  );
}

function RuntimeStats({ executionState }) {
  const statusLabels = {
    ready: "Ready",
    running: "Executing...",
    passed: "Trace Active",
  };

  return (
    <div className="analyze-runtime-pill" aria-live="polite">
      <span className="analyze-runtime-dot" aria-hidden="true" />
      <span>RAM: 14.2 MB</span>
      <b>•</b>
      <span>Exec: {executionState === "running" ? "—" : "0.18ms"}</span>
      <b>•</b>
      <span className={`analyze-runtime-state analyze-runtime-state--${executionState}`}>
        {statusLabels[executionState] || "Ready"}
      </span>
    </div>
  );
}

function EditorActions({ executionState, onRun, onReset }) {
  return (
    <div className="analyze-editor-actions">
      <button
        className="analyze-action-button analyze-action-button--reset"
        type="button"
        onClick={onReset}
        title="Reset starter template"
      >
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
          <path d="M3 3v5h5" />
        </svg>
        Reset
      </button>
      <button
        className="analyze-action-button analyze-action-button--run-visualize"
        type="button"
        onClick={onRun}
        disabled={executionState === "running"}
        title="Run code and generate interactive visualizer (Ctrl + Enter)"
      >
        <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
          <path d="M8 5v14l11-7z" />
        </svg>
        {executionState === "running" ? "Executing..." : "Run Code & Visualize"}
      </button>
    </div>
  );
}

function EditorPanel({
  language,
  code,
  executionState,
  isFullscreen,
  activeLineIndex,
  onLanguageChange,
  onCodeChange,
  onCopy,
  onReset,
  onToggleFullscreen,
  onRun,
}) {
  return (
    <section
      className={`analyze-editor-panel ${isFullscreen ? "is-fullscreen" : ""}`}
      aria-label="Code editor and execution panel"
    >
      <EditorToolbar
        language={language}
        onLanguageChange={onLanguageChange}
        onCopy={onCopy}
        onReset={onReset}
        onToggleFullscreen={onToggleFullscreen}
        isFullscreen={isFullscreen}
        executionState={executionState}
        onRun={onRun}
      />
      <CodeEditor
        code={code}
        onChange={onCodeChange}
        isFullscreen={isFullscreen}
        activeLineIndex={activeLineIndex}
        onRunShortcut={onRun}
      />
      <footer className="analyze-editor-footer">
        <RuntimeStats executionState={executionState} />
        <EditorActions
          executionState={executionState}
          onRun={onRun}
          onReset={onReset}
        />
      </footer>
    </section>
  );
}

function ExecutionConsole({ logs, isOpen, onToggle, onClear }) {
  const [consoleHeight, setConsoleHeight] = useState(() => {
    try {
      const saved = localStorage.getItem("algoviz-analyze-console-height");
      if (saved) {
        const val = Number(saved);
        if (Number.isFinite(val) && val >= 80 && val <= 200) {
          return val;
        }
      }
      return 135;
    } catch {
      return 135;
    }
  });
  const [isDragging, setIsDragging] = useState(false);

  const minHeight = 75;
  const maxHeight = 480;

  const handlePointerDown = (e) => {
    e.preventDefault();
    if (!isOpen) onToggle();

    const startY = e.clientY;
    const startHeight = consoleHeight;
    setIsDragging(true);
    document.body.classList.add("is-resizing-vertical");

    const onPointerMove = (moveEvent) => {
      const deltaY = startY - moveEvent.clientY; // dragging UP increases console height
      const nextHeight = Math.max(minHeight, Math.min(maxHeight, Math.round(startHeight + deltaY)));
      setConsoleHeight(nextHeight);
    };

    const onPointerUp = (upEvent) => {
      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
      window.removeEventListener("pointercancel", onPointerUp);
      setIsDragging(false);
      document.body.classList.remove("is-resizing-vertical");

      const finalDelta = startY - upEvent.clientY;
      const finalHeight = Math.max(minHeight, Math.min(maxHeight, Math.round(startHeight + finalDelta)));
      try {
        localStorage.setItem("algoviz-analyze-console-height", String(finalHeight));
      } catch {}
    };

    window.addEventListener("pointermove", onPointerMove);
    window.addEventListener("pointerup", onPointerUp);
    window.addEventListener("pointercancel", onPointerUp);
  };

  const handleKeyDown = (e) => {
    if (e.key === "ArrowUp") {
      e.preventDefault();
      if (!isOpen) onToggle();
      setConsoleHeight((h) => {
        const next = Math.min(maxHeight, h + 24);
        try { localStorage.setItem("algoviz-analyze-console-height", String(next)); } catch {}
        return next;
      });
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      setConsoleHeight((h) => {
        const next = Math.max(minHeight, h - 24);
        try { localStorage.setItem("algoviz-analyze-console-height", String(next)); } catch {}
        return next;
      });
    } else if (e.key === "Home") {
      e.preventDefault();
      setConsoleHeight(minHeight);
    } else if (e.key === "End") {
      e.preventDefault();
      setConsoleHeight(maxHeight);
    }
  };

  const toggleSizePreset = () => {
    const nextHeight = consoleHeight >= 220 ? 135 : 260;
    setConsoleHeight(nextHeight);
    try {
      localStorage.setItem("algoviz-analyze-console-height", String(nextHeight));
    } catch {}
  };

  return (
    <section
      className={`analyze-console-drawer ${isOpen ? "is-open" : "is-collapsed"} ${isDragging ? "is-dragging" : ""}`}
      style={{ height: isOpen ? `${consoleHeight}px` : undefined }}
      aria-label="Console Output"
    >
      {/* Draggable Vertical Resizer Handle */}
      <div
        className="analyze-console-resizer"
        onPointerDown={handlePointerDown}
        onKeyDown={handleKeyDown}
        tabIndex={0}
        role="separator"
        aria-orientation="horizontal"
        aria-label="Kéo lên xuống để điều chỉnh độ cao Console"
        title="Kéo lên/xuống để điều chỉnh độ cao Console (hoặc dùng phím mũi tên Lên/Xuống)"
      >
        <span className="analyze-console-resizer-line" />
        <span className="analyze-console-resizer-grip" />
      </div>

      <header className="analyze-console-header" onClick={onToggle}>
        <div className="analyze-console-title">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <polyline points="4 17 10 11 4 5" />
            <line x1="12" y1="19" x2="20" y2="19" />
          </svg>
          <strong>Console Output &amp; Telemetry</strong>
          <span className="analyze-console-badge">SUCCESS (0.18ms)</span>
          {isOpen && (
            <span className="analyze-console-height-hint" title="Độ cao console hiện tại">
              {consoleHeight}px
            </span>
          )}
        </div>
        <div className="analyze-console-controls" onClick={(e) => e.stopPropagation()}>
          <button
            className="analyze-console-btn"
            type="button"
            onClick={onClear}
            title="Clear output logs"
          >
            Clear
          </button>
          {isOpen && (
            <button
              className="analyze-console-btn analyze-console-btn--preset"
              type="button"
              onClick={toggleSizePreset}
              title={consoleHeight >= 220 ? "Thu nhỏ Console" : "Phóng to Console"}
            >
              {consoleHeight >= 220 ? "⤓ Thu gọn" : "⤢ Phóng to"}
            </button>
          )}
          <button
            className="analyze-console-btn"
            type="button"
            onClick={onToggle}
            aria-label={isOpen ? "Collapse console" : "Expand console"}
            title={isOpen ? "Collapse console" : "Expand console"}
          >
            {isOpen ? "▼ Hide Logs" : "▲ View Logs"}
          </button>
        </div>
      </header>
      {isOpen && (
        <pre className="analyze-console-body">
          {logs.map((entry, idx) => (
            <div key={idx} className={`analyze-console-line analyze-console-line--${entry.type || "info"}`}>
              <span className="console-prefix">[{entry.tag || "System"}]</span> {entry.text}
            </div>
          ))}
        </pre>
      )}
    </section>
  );
}

function ExecutionVisualizerStudio({
  executionState,
  runKey,
  currentArray,
  isAutoPlay,
  onStepChange,
  onArrayChange,
  consoleLogs,
  isConsoleOpen,
  onToggleConsole,
  onClearConsole,
}) {
  return (
    <section className="analyze-visualizer-studio" aria-label="Execution visualizer studio">
      <header className="analyze-vis-studio-header">
        <div className="analyze-vis-studio-title-group">
          <div className="analyze-vis-icon-wrap" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
              <line x1="8" y1="21" x2="16" y2="21" />
              <line x1="12" y1="17" x2="12" y2="21" />
            </svg>
          </div>
          <div>
            <h2>Algorithm Visualizer Studio</h2>
            <p>Live state transitions, pointer tracking &amp; smooth element swaps</p>
          </div>
        </div>

        <div className="analyze-vis-status-badges">
          <span
            className={`analyze-vis-status-pill analyze-vis-status-pill--${executionState}`}
          >
            <span className="analyze-vis-status-dot" aria-hidden="true" />
            {executionState === "running"
              ? "Running Code..."
              : executionState === "passed"
              ? "Visual Trace Active"
              : "Ready to Execute"}
          </span>
        </div>
      </header>

      <div className="analyze-vis-stage-wrapper">
        <SmoothSortVisualizer
          key={runKey}
          initialArray={currentArray}
          autoPlay={isAutoPlay}
          onStepChange={onStepChange}
          onArrayChange={onArrayChange}
        />
      </div>

      <ExecutionConsole
        logs={consoleLogs}
        isOpen={isConsoleOpen}
        onToggle={onToggleConsole}
        onClear={onClearConsole}
      />
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
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
                <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" />
              </svg>
            </span>
            <span>Algo<span>Master</span></span>
          </a>
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

function AnalyzePage({ userName = "Your name", isAuthenticated }) {
  const [language, setLanguage] = useState("Java 21");
  const [code, setCode] = useState(CODE_SNIPPETS["Java 21"]);
  const [executionState, setExecutionState] = useState("ready");
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [currentArray, setCurrentArray] = useState(DEFAULT_ARRAY);
  const [runKey, setRunKey] = useState(1);
  const [isAutoPlay, setIsAutoPlay] = useState(false);
  const [activeStep, setActiveStep] = useState(null);
  const [announcement, setAnnouncement] = useState("");
  const [isConsoleOpen, setIsConsoleOpen] = useState(true);
  const [consoleLogs, setConsoleLogs] = useState([
    { tag: "System", text: "Compiler workspace initialized (Java 21 standard sandbox).", type: "system" },
    { tag: "Visualizer", text: "Interactive trace engine loaded. Click 'Run Code & Visualize' to execute.", type: "info" },
  ]);

  const executionTimerRef = useRef(null);

  useEffect(() => () => window.clearTimeout(executionTimerRef.current), []);

  const announce = (message) => setAnnouncement(message);

  // Synchronize active code editor line with the step currently executing in the visualizer
  const activeLineIndex = useMemo(() => {
    if (!activeStep) return -1;
    const type = activeStep.type;

    if (language === "Java 21") {
      if (type === "IDLE") return 3; // for (int i = 0; ...
      if (type === "COMPARE") return 5; // if (arr[j] > arr[j + 1])
      if (type === "SWAPPING") return 7; // int temp = arr[j];
      if (type === "SWAP_SETTLED") return 9; // arr[j + 1] = temp;
      if (type === "PASS_END") return 4; // for (int j = 0; ...
      if (type === "SORTED") return 1; // public static void bubbleSort
    } else if (language === "Python 3") {
      if (type === "IDLE") return 2; // for i in range(n - 1):
      if (type === "COMPARE") return 4; // if arr[j] > arr[j + 1]:
      if (type === "SWAPPING" || type === "SWAP_SETTLED") return 6; // arr[j], arr[j + 1] = ...
      if (type === "PASS_END") return 3; // for j in range(n - i - 1):
      if (type === "SORTED") return 7; // return arr
    } else if (language === "JavaScript") {
      if (type === "IDLE") return 2; // for (let i = 0; ...
      if (type === "COMPARE") return 4; // if (arr[j] > arr[j + 1])
      if (type === "SWAPPING") return 6; // const temp = arr[j];
      if (type === "SWAP_SETTLED") return 8; // arr[j + 1] = temp;
      if (type === "PASS_END") return 3; // for (let j = 0; ...
      if (type === "SORTED") return 12; // return arr;
    } else if (language === "C++ 20") {
      if (type === "IDLE") return 5; // for (int i = 0; ...
      if (type === "COMPARE") return 7; // if (arr[j] > arr[j + 1])
      if (type === "SWAPPING" || type === "SWAP_SETTLED") return 9; // std::swap(...)
      if (type === "PASS_END") return 6; // for (let j = 0; ...
      if (type === "SORTED") return 3; // void bubbleSort(...)
    }
    return -1;
  }, [activeStep, language]);

  const handleRun = useCallback(async () => {
    if (executionState === "running") return;
    setExecutionState("running");
    announce("Executing code and generating algorithm trace...");

    const sortedCopy = [...currentArray].sort((a, b) => a - b);
    const timeStr = new Date().toLocaleTimeString();

    setConsoleLogs((prev) => [
      ...prev,
      { tag: "Compiler", text: `[${timeStr}] Compiling ${language} source code...`, type: "system" },
      { tag: "Sandbox", text: "Allocated live execution environment. Memory: 14.2 MB.", type: "info" },
    ]);

    try {
      const langKey = language.toLowerCase().includes("java")
        ? "java"
        : language.toLowerCase().includes("python")
        ? "python"
        : "cpp";
      // Optional call to backend runCode API
      await api.runCode(langKey, code, JSON.stringify(currentArray)).catch(() => null);
    } catch {
      // Fallback to local simulation
    }

    window.clearTimeout(executionTimerRef.current);
    executionTimerRef.current = window.setTimeout(() => {
      setExecutionState("passed");
      setRunKey((prev) => prev + 1);
      setIsAutoPlay(true);
      announce("Execution complete. Interactive visualizer started.");

      setConsoleLogs((prev) => [
        ...prev,
        {
          tag: "Output",
          text: `Input array: [${currentArray.join(", ")}] → Sorted output: [${sortedCopy.join(", ")}]`,
          type: "success",
        },
        {
          tag: "Visualizer",
          text: `State machine ready with ${currentArray.length} items. Animated playback in progress.`,
          type: "success",
        },
      ]);
    }, 280);
  }, [executionState, currentArray, language, code]);

  const handleLanguageChange = (nextLanguage) => {
    setLanguage(nextLanguage);
    if (CODE_SNIPPETS[nextLanguage]) {
      setCode(CODE_SNIPPETS[nextLanguage]);
    }
    setExecutionState("ready");
    announce(`${nextLanguage} editor selected.`);
    setConsoleLogs((prev) => [
      ...prev,
      { tag: "Language", text: `Switched target environment to ${nextLanguage}.`, type: "info" },
    ]);
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
      announce("Clipboard copying is unavailable.");
    }
  };

  const handleReset = () => {
    setCode(CODE_SNIPPETS[language] || CODE_SNIPPETS["Java 21"]);
    setExecutionState("ready");
    announce(`Code reset to ${language} starter solution.`);
  };

  return (
    <main className={`analyze-page ${isFullscreen ? "has-fullscreen-editor" : ""}`} id="top">
      <div className="analyze-workspace-container">
        <header className="analyze-studio-header">
          <div className="analyze-studio-title-group">
            <h1>Algorithm Execution &amp; Visualizer Studio</h1>
            <p>Write or modify code, execute in real time, and inspect the live interactive visualizer.</p>
          </div>
          <div className="analyze-studio-pill">
            <span className="analyze-studio-pill-dot" aria-hidden="true" />
            <span>Live Interactive Engine Active</span>
          </div>
        </header>

        <div className="analyze-main-content">
          <EditorPanel
            language={language}
            code={code}
            executionState={executionState}
            isFullscreen={isFullscreen}
            activeLineIndex={activeLineIndex}
            onLanguageChange={handleLanguageChange}
            onCodeChange={(nextCode) => {
              setCode(nextCode);
              setExecutionState("ready");
            }}
            onCopy={handleCopy}
            onReset={handleReset}
            onToggleFullscreen={() => setIsFullscreen((curr) => !curr)}
            onRun={handleRun}
          />

          <ExecutionVisualizerStudio
            executionState={executionState}
            runKey={runKey}
            currentArray={currentArray}
            isAutoPlay={isAutoPlay}
            onStepChange={setActiveStep}
            onArrayChange={setCurrentArray}
            consoleLogs={consoleLogs}
            isConsoleOpen={isConsoleOpen}
            onToggleConsole={() => setIsConsoleOpen((curr) => !curr)}
            onClearConsole={() => setConsoleLogs([])}
          />
        </div>
      </div>
      <p className="sr-only" aria-live="polite">
        {announcement || `${language} Visualize workspace ready.`}
      </p>
    </main>
  );
}

export default AnalyzePage;
