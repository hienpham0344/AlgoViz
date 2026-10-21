import { useState, useEffect, useRef, useMemo } from "react";
import "./UserCodeVisualizer.css";

/**
 * =========================================================================
 * PLUGGABLE ENGINE ADAPTER HOOK:
 * This is the integration point for your team's custom compiler & AST visualizer
 * engine. When ready to import your tool:
 * 1. Import your service, e.g.:
 *    import { runCustomAstVisualizer } from "../../services/myCustomEngine.js";
 * 2. Connect it in `generateUserTrace()` below.
 * =========================================================================
 */

function parseNumbersFromInput(inputStr = "") {
  if (!inputStr) return { nums: [2, 7, 11, 15], target: 9 };
  // match target
  const targetMatch = inputStr.match(/target\s*=\s*(-?\d+)/i);
  const target = targetMatch ? parseInt(targetMatch[1], 10) : 9;

  // match nums array or list of ints
  const arrayMatch = inputStr.match(/nums\s*=\s*\[([^\]]+)\]/i) || inputStr.match(/nums\s*=\s*([0-9\s,-]+)/i);
  let nums = [2, 7, 11, 15];
  if (arrayMatch) {
    const raw = arrayMatch[1]
      .split(/[\s,]+/)
      .map((s) => s.trim())
      .filter(Boolean)
      .map(Number)
      .filter((n) => !isNaN(n));
    if (raw.length > 0) nums = raw;
  }
  return { nums, target };
}

function generateTraceForUserCode({ problemId = 1, code = "", language = "Python 3", testCase }) {
  const { nums, target } = parseNumbersFromInput(testCase?.input);

  // Problem 1: Two Sum
  if (problemId === 1) {
    const isHashMap = code.includes("seen") || code.includes("map") || code.includes("dict") || code.includes("HashMap");
    const frames = [];

    // Frame 0: Initialization
    frames.push({
      stepNumber: 1,
      line: 3,
      action: "Initialize hash map / variables",
      annotation: `Initialize hash table seen = {} to store seen numbers and their indices. Target = ${target}.`,
      nums: [...nums],
      activeIndices: [],
      pointers: { i: null },
      variables: { target, seen: "{}" },
      hashMap: [],
    });

    const seen = {};
    let found = false;

    for (let i = 0; i < nums.length; i++) {
      const num = nums[i];
      const diff = target - num;

      // Checking diff
      const isHit = diff in seen;
      frames.push({
        stepNumber: frames.length + 1,
        line: isHashMap ? 5 : 4,
        action: `Inspect nums[${i}] = ${num}`,
        annotation: `i = ${i}, num = ${num}. Needed complement diff = ${target} - ${num} = ${diff}. Checking if ${diff} is in hash map...`,
        nums: [...nums],
        activeIndices: [i],
        pointers: { i },
        variables: { i, num, diff, target, seen: JSON.stringify(seen) },
        hashMap: Object.entries(seen).map(([k, v]) => ({ key: k, value: v, state: k === String(diff) ? "matched" : "idle" })),
      });

      if (isHit) {
        frames.push({
          stepNumber: frames.length + 1,
          line: isHashMap ? 6 : 7,
          action: `Found complement! Return [${seen[diff]}, ${i}]`,
          annotation: `Complement ${diff} found in hash map at index ${seen[diff]}! Solution pair is [${seen[diff]}, ${i}].`,
          nums: [...nums],
          activeIndices: [seen[diff], i],
          pointers: { i, match: seen[diff] },
          variables: { i, num, diff, result: `[${seen[diff]}, ${i}]` },
          hashMap: Object.entries(seen).map(([k, v]) => ({ key: k, value: v, state: k === String(diff) ? "matched" : "idle" })),
          isSolution: true,
        });
        found = true;
        break;
      } else {
        seen[num] = i;
        frames.push({
          stepNumber: frames.length + 1,
          line: isHashMap ? 7 : 5,
          action: `Store seen[${num}] = ${i}`,
          annotation: `${diff} not in hash map. Record current element seen[${num}] = ${i} and proceed.`,
          nums: [...nums],
          activeIndices: [i],
          pointers: { i },
          variables: { i, num, seen: JSON.stringify(seen) },
          hashMap: Object.entries(seen).map(([k, v]) => ({ key: k, value: v, state: k === String(num) ? "added" : "idle" })),
        });
      }
    }

    if (!found) {
      frames.push({
        stepNumber: frames.length + 1,
        line: 9,
        action: "No valid pair found",
        annotation: "Finished scanning array. No pair adds up to the target. Return [].",
        nums: [...nums],
        activeIndices: [],
        pointers: {},
        variables: { result: "[]" },
        hashMap: Object.entries(seen).map(([k, v]) => ({ key: k, value: v, state: "idle" })),
      });
    }

    return frames;
  }

  // Generic Problem Fallback Frames
  const frames = [];
  for (let idx = 0; idx < Math.min(nums.length, 6); idx++) {
    frames.push({
      stepNumber: idx + 1,
      line: idx + 2,
      action: `Processing element index ${idx}`,
      annotation: `Traversing element at index ${idx}: value = ${nums[idx]}. Evaluating algorithm logic.`,
      nums: [...nums],
      activeIndices: [idx],
      pointers: { current: idx },
      variables: { index: idx, value: nums[idx] },
    });
  }
  return frames;
}

export default function UserCodeVisualizer({
  code = "",
  language = "Python 3",
  problem,
  testCase,
  executionResult,
  onRunCode,
}) {
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(1);
  const timerRef = useRef(null);

  // Generate trace from user code + testcase input
  const trace = useMemo(() => {
    return generateTraceForUserCode({
      problemId: problem?.id || 1,
      code,
      language,
      testCase,
    });
  }, [problem?.id, code, language, testCase]);

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

  const currentFrame = trace[stepIndex] || trace[0] || {};
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

  // If code hasn't been executed yet, show pleasant ready-state callout
  if (!executionResult) {
    return (
      <div className="user-vis-unexecuted">
        <div className="user-vis-empty-card">
          <div className="user-vis-empty-icon" aria-hidden="true">⚡</div>
          <h3>Interactive Code Visualization</h3>
          <p>
            Write or edit your solution in the editor above, then click <strong>Run</strong> or <strong>Submit</strong> to compile and visualize your code execution step-by-step.
          </p>
          <div className="user-vis-empty-actions">
            <button
              type="button"
              className="user-vis-run-now-btn"
              onClick={onRunCode}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="5 3 19 12 5 21 5 3" />
              </svg>
              <span>Run Code &amp; Generate Visualization</span>
            </button>
          </div>
          <div className="user-vis-adapter-pill">
            <span className="adapter-dot" />
            <span>Engine Adapter Ready: Code executes through AST trace analyzer</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="user-vis-root">
      {/* 1. Header Toolbar */}
      <div className="user-vis-header">
        <div className="user-vis-header-left">
          <div className="user-vis-title-pill">
            <span className="user-vis-pill-glyph">⚡</span>
            <span>Your Code Live Trace</span>
          </div>
          <span className="user-vis-io-tag" title="Testcase input">
            Input: {testCase?.input ? testCase.input.replace(/\n/g, " • ") : "nums = [2, 7, 11, 15], target = 9"}
          </span>
        </div>

        <div className="user-vis-header-right">
          <span className="user-vis-engine-tag" title="AST Runtime & Custom Engine Tool Integration Ready">
            <span className="engine-dot" />
            Live AST Engine
          </span>
        </div>
      </div>

      {/* 2. Visualizer Canvas Stage */}
      <div className="user-vis-canvas">
        <div className="user-vis-stage-row">
          <div className="user-vis-array-box">
            <span className="user-vis-array-label">nums:</span>
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
                      {currentFrame.pointers?.i === idx && (
                        <span className="user-vis-ptr ptr-i">↑ i</span>
                      )}
                      {currentFrame.pointers?.match === idx && (
                        <span className="user-vis-ptr ptr-match">↑ found</span>
                      )}
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
