import { useState, useEffect, useRef, useMemo } from "react";
import "./UserCodeVisualizer.css";
import ApiState from "../../components/ApiState.jsx";
import { runtimeFrames, traceMatchesEditor } from "../../services/runtimeTrace.js";
import { languageKey } from "../../services/submissions.js";

export default function UserCodeVisualizer({
  code = "",
  language = "Python 3",
  testCase,
  executionResult,
  onRunCode,
  loading = false,
  onStepChange,
}) {
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const timerRef = useRef(null);

  const captured = executionResult?.trace;
  const matches = traceMatchesEditor(captured, code, testCase?.input || "");
  const trace = useMemo(() => matches ? runtimeFrames(captured) : [], [captured, matches]);

  // Reset stepIndex when trace changes
  useEffect(() => {
    setStepIndex(0);
    setIsPlaying(false);
  }, [trace]);

  // Playback timer
  useEffect(() => {
    if (!isPlaying) {
      if (timerRef.current) clearInterval(timerRef.current);
      return;
    }

    const intervalMs = Math.round(1100 / speed);
    timerRef.current = setInterval(() => {
      setStepIndex((curr) => {
        if (curr >= trace.length - 1) {
          setIsPlaying(false);
          return curr;
        }
        return curr + 1;
      });
    }, intervalMs);

    return () => {
      if (timerRef.current) clearInterval(timerRef.current);
    };
  }, [isPlaying, speed, trace.length]);

  const currentFrame = trace[stepIndex] || trace[0] || null;
  useEffect(() => { onStepChange?.(currentFrame); }, [currentFrame, onStepChange]);
  const isComplete = stepIndex >= trace.length - 1;

  const handlePrev = () => {
    setIsPlaying(false);
    setStepIndex((c) => Math.max(0, c - 1));
  };

  const handleNext = () => {
    setIsPlaying(false);
    setStepIndex((c) => Math.min(trace.length - 1, c + 1));
  };

  const handleReset = () => {
    setIsPlaying(false);
    setStepIndex(0);
  };

  const handleTogglePlay = () => {
    if (isPlaying) {
      setIsPlaying(false);
    } else {
      if (isComplete) {
        setStepIndex(0);
      }
      setIsPlaying(true);
    }
  };

  const codeLines = useMemo(() => {
    return (code || "").split(/\r?\n/);
  }, [code]);

  if (loading) return <ApiState loading label="Waiting for execution…" />;
  if (!currentFrame) {
    const supported = languageKey(language) === "java";
    const message = !supported ? "AST-tool supports Java runtime traces. Select Java to inspect actual execution steps."
      : captured && !matches ? "Code or testcase changed. Run again to generate a matching trace."
      : captured ? "The program produced no observable source steps."
      : "Run your code to inspect the actual source lines, arrays and local variables captured by AST/JDI.";
    return <div className="user-vis-unexecuted"><div className="user-vis-empty-card">
      <h3>Runtime Code Visualization</h3><p>{message}</p>
      <ApiState error={executionResult?.traceError || captured?.errorMessage} />
      {supported && <button type="button" className="user-vis-run-now-btn" onClick={onRunCode}>
        Run Code &amp; Generate Runtime Trace
      </button>}
    </div></div>;
  }

  return (
    <div className="user-vis-root">
      {/* 1. Header Toolbar */}
      <div className="user-vis-header">
        <div className="user-vis-header-left">
          <div className="user-vis-title-pill">
            <span className="user-vis-pill-glyph">⚡</span>
            <span>Runtime Trace</span>
          </div>
          <span className="user-vis-io-tag" title="Testcase input">
            Input: {testCase?.input ? testCase.input.replace(/\n/g, " • ") : "nums = [2, 7, 11, 15], target = 9"}
          </span>
        </div>

        <div className="user-vis-header-right">
          <span className="user-vis-engine-tag" title="Source snapshots captured before each Java breakpoint">
            <span className="engine-dot" />
            AST / JDI
          </span>
        </div>
      </div>

      <ApiState error={captured.partial ? `Partial trace: ${(captured.warnings || []).join(" ")}` : captured.status !== "Success" ? captured.errorMessage || captured.status : ""} />
      {/* 2. Visualizer Canvas Stage */}
      <div className="user-vis-canvas">
        <div className="user-vis-stage-row">
          <div className="user-vis-array-box">
            <span className="user-vis-array-label">{currentFrame.arrayName || "No sequence in this scope"}:</span>
            <div className="user-vis-cells">
              {(currentFrame.nums || []).map((val, idx) => {
                const isActive = currentFrame.activeIndices?.includes(idx);
                const isMatch = currentFrame.pointers?.match === idx || (currentFrame.isSolution && isActive);
                return (
                  <div key={idx} className="user-vis-cell-wrapper">
                    <span className="user-vis-cell-idx">{idx}</span>
                    <div
                      className={`user-vis-cell ${isActive ? "is-active" : ""} ${isMatch ? "is-match" : ""}`}
                    >
                      {val}
                    </div>
                    <div className="user-vis-pointer-slot">
                      {Object.entries(currentFrame.pointers || {}).filter(([, index]) => index === idx).map(([name]) => (
                        <span key={name} className="user-vis-ptr ptr-i">↑ {name}</span>
                      ))}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Hash Map or Local Variables Table */}
        {currentFrame.hashMap && currentFrame.hashMap.length > 0 && (
          <div className="user-vis-hashmap-row">
            <span className="user-vis-hashmap-label">Hash Map (seen):</span>
            <div className="user-vis-hashmap-entries">
              {currentFrame.hashMap.map((entry, idx) => (
                <div
                  key={idx}
                  className={`user-vis-hash-pill ${entry.state === "matched" ? "is-matched" : ""} ${entry.state === "added" ? "is-added" : ""}`}
                >
                  <span className="hash-key">{entry.key}</span>
                  <span className="hash-arr">→</span>
                  <span className="hash-val">index {entry.value}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 3. Step Banner */}
      <div className="user-vis-step-banner">
        <div className="user-step-badge">
          Step {stepIndex + 1}/{trace.length}
        </div>
        <div className="user-step-text">
          {currentFrame.annotation}
        </div>
      </div>

      {/* 4. Playback Controls Bar */}
      <div className="user-vis-controls">
        <div className="user-ctrls-left">
          <button
            type="button"
            className="user-ctrl-btn"
            onClick={handleReset}
            title="Reset to beginning"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4">
              <polyline points="1 4 1 10 7 10" />
              <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
            </svg>
          </button>
          <button
            type="button"
            className="user-ctrl-btn"
            onClick={handlePrev}
            disabled={stepIndex <= 0}
            title="Step backward"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <polygon points="19 20 9 12 19 4 19 20" />
              <line x1="5" y1="19" x2="5" y2="5" stroke="currentColor" strokeWidth="2.5" />
            </svg>
          </button>
          <button
            type="button"
            className="user-ctrl-btn user-ctrl-btn--primary"
            onClick={handleTogglePlay}
            title={isPlaying ? "Pause" : "Play"}
          >
            {isPlaying ? (
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <rect x="6" y="4" width="4" height="16" rx="1" />
                <rect x="14" y="4" width="4" height="16" rx="1" />
              </svg>
            ) : (
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="6 4 20 12 6 20 6 4" />
              </svg>
            )}
          </button>
          <button
            type="button"
            className="user-ctrl-btn"
            onClick={handleNext}
            disabled={isComplete}
            title="Step forward"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
              <polygon points="5 4 15 12 5 20 5 4" />
              <line x1="19" y1="19" x2="19" y2="5" stroke="currentColor" strokeWidth="2.5" />
            </svg>
          </button>
        </div>

        <div className="user-ctrls-center">
          <input
            type="range"
            min={0}
            max={trace.length - 1}
            value={stepIndex}
            onChange={(e) => {
              setIsPlaying(false);
              setStepIndex(Number(e.target.value));
            }}
            className="user-ctrl-slider"
            aria-label="Execution timeline scrubber"
          />
          <span className="user-ctrl-step-count">
            {stepIndex + 1} / {trace.length}
          </span>
        </div>

        <div className="user-ctrls-right">
          <div className="user-speed-group">
            <span className="speed-label">Speed:</span>
            {[0.5, 1, 2].map((s) => (
              <button
                key={s}
                type="button"
                className={`speed-pill ${speed === s ? "is-active" : ""}`}
                onClick={() => setSpeed(s)}
              >
                {s}x
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 5. Synchronized User Code & Live Variable Inspector */}
      <div className="user-vis-code-dock">
        <div className="user-code-split">
          <div className="user-code-pane">
            <div className="user-code-header">
              <span className="code-header-title">{language} (Your Code)</span>
              {currentFrame.line && (
                <span className="code-header-line">Executing Line {currentFrame.line}</span>
              )}
            </div>
            <div className="user-code-content">
              {codeLines.map((lineText, idx) => {
                const lineNum = idx + 1;
                const isLineActive = currentFrame.line === lineNum;
                return (
                  <div
                    key={lineNum}
                    className={`user-code-line ${isLineActive ? "is-active" : ""}`}
                  >
                    <span className="line-num">{lineNum}</span>
                    <span className="line-text">{lineText || " "}</span>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="user-vars-pane">
            <div className="user-vars-header">
              <span>Local Variable Scope</span>
            </div>
            <div className="user-vars-list">
              {Object.entries(currentFrame.variables || {}).map(([varName, val]) => (
                <div key={varName} className="user-var-item">
                  <span className="var-name">{varName}</span>
                  <span className="var-eq">=</span>
                  <span className="var-val">{String(val)}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
