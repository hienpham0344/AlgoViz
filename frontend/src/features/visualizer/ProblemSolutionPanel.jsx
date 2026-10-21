import { useState, useEffect, useRef, useMemo } from "react";
import {
  fetchDebugSteps,
  checkAstToolHealth,
} from "../../services/astApi.js";
import {
  javaFilename,
  generateTwoSumTrace,
  buildSimulatedTraceForSolution,
  adaptAstStepsToRichVisualizer,
} from "../../services/solutionTrace.js";
import "./SmoothSortVisualizer.css";
import "./ProblemSolutionPanel.css";
import ResizableColumns from "../../components/ResizableColumns.jsx";
import ProgressView from "../../components/ProgressView.jsx";

const SPEED_CONFIG = {
  "0.5x": { label: "0.5x", durationMs: 3600, animSec: "2.4s" },
  "1x": { label: "1x", durationMs: 2000, animSec: "1.4s" },
  "1.5x": { label: "1.5x", durationMs: 1300, animSec: "0.9s" },
  "2x": { label: "2x", durationMs: 800, animSec: "0.55s" },
};

function SolutionVisualizerExecution({ problem, solution, onAnnounce, onStepChange }) {
  const source = solution?.codeSnippet || "";
  const lines = useMemo(() => source.split(/\r?\n/), [source]);
  const activeLineRef = useRef(null);
  const sourceContainerRef = useRef(null);
  const timerRef = useRef(null);
  const stageBoardRef = useRef(null);

  const [speedKey, setSpeedKey] = useState("1x");
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [showSourceCode, setShowSourceCode] = useState(false);

  // Workspace state
  const [zoomLevel] = useState(1);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const handleToggleFullscreen = () => setIsFullscreen((prev) => !prev);

  useEffect(() => {
    if (!isFullscreen) return;
    const onKeyDown = (event) => {
      if (event.key === "Escape") setIsFullscreen(false);
    };
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [isFullscreen]);

  // AST tool backend status
  const [isAstOnline, setIsAstOnline] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  // Determine initial trace based on specific problem solution
  const initialTrace = useMemo(() => {
    return buildSimulatedTraceForSolution(problem?.title, source) || generateTwoSumTrace();
  }, [problem?.title, source]);

  const [trace, setTrace] = useState(initialTrace);

  // Check AST server health on mount & try real execution if available
  useEffect(() => {
    let isMounted = true;
    checkAstToolHealth().then((online) => {
      if (isMounted) setIsAstOnline(online);
    });

    const isJava = solution?.language?.toLowerCase() === "java";
    const runnable = isJava && /\bvoid\s+main\s*\(/.test(source);

    if (runnable) {
      const controller = new AbortController();
      const timeout = setTimeout(() => controller.abort(), 6000);

      fetchDebugSteps(source, { signal: controller.signal })
        .then((res) => {
          if (!isMounted || controller.signal.aborted) return;
          if (Array.isArray(res?.steps) && res.steps.length > 0) {
            const richAdapted = adaptAstStepsToRichVisualizer(res.steps, source);
            if (richAdapted.length > 0) {
              setTrace(richAdapted);
            }
            setIsAstOnline(true);
            setStepIndex(0);
            setStatusMessage("✅ Đã kết nối & đồng bộ trace trực quan từ AST-tool JDI!");
            setTimeout(() => setStatusMessage(""), 3500);
          }
        })
        .catch(() => {
          if (isMounted) {
            setIsAstOnline(false);
          }
        })
        .finally(() => clearTimeout(timeout));

      return () => {
        isMounted = false;
        clearTimeout(timeout);
        controller.abort();
      };
    }
  }, [source, solution?.language, problem?.title]);

  // Current active step
  const currentStep = useMemo(() => trace[stepIndex] || trace[0] || {}, [trace, stepIndex]);
  const previousStep = trace[stepIndex - 1];

  // Notify parent of step announcement & active line change
  useEffect(() => {
    if (onAnnounce && currentStep) {
      onAnnounce(
        `Step ${stepIndex + 1}/${trace.length}: Line ${currentStep.activeLine} - ${currentStep.comparison || currentStep.annotation || ""}`,
      );
    }
    if (onStepChange && currentStep) {
      onStepChange(currentStep);
    }
  }, [stepIndex, currentStep, trace.length, onAnnounce, onStepChange]);

  // Autoplay ticker
  useEffect(() => {
    if (!isPlaying) {
      if (timerRef.current) clearTimeout(timerRef.current);
      return;
    }

    const speed = SPEED_CONFIG[speedKey];
    timerRef.current = setTimeout(() => {
      setStepIndex((curr) => {
        if (curr >= trace.length - 1) {
          setIsPlaying(false);
          return curr;
        }
        return curr + 1;
      });
    }, speed.durationMs);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [isPlaying, stepIndex, trace.length, speedKey]);

  // Auto-scroll active Java source code line
  useEffect(() => {
    const active = activeLineRef.current;
    const container = sourceContainerRef.current;
    if (!active || !container) return;
    if (
      active.offsetTop < container.scrollTop ||
      active.offsetTop + active.offsetHeight > container.scrollTop + container.clientHeight
    ) {
      container.scrollTop = Math.max(0, active.offsetTop - container.clientHeight / 3);
    }
  }, [currentStep?.activeLine]);

  // Playback handlers
  const handleTogglePlay = () => {
    if (!isPlaying && stepIndex >= trace.length - 1) {
      setStepIndex(0);
    }
    setIsPlaying((prev) => !prev);
  };

  const handlePrev = () => {
    setIsPlaying(false);
    setStepIndex((curr) => Math.max(0, curr - 1));
  };

  const handleNext = () => {
    setIsPlaying(false);
    setStepIndex((curr) => Math.min(trace.length - 1, curr + 1));
  };

  const handleReset = () => {
    setIsPlaying(false);
    setStepIndex(0);
  };

  const handleSeek = (e) => {
    setIsPlaying(false);
    setStepIndex(Number(e.target.value));
  };

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(source);
      if (onAnnounce) onAnnounce("Đã sao chép mã nguồn giải thuật.");
    } catch {
      if (onAnnounce) onAnnounce("Không thể sao chép mã nguồn.");
    }
  };

  // Array square cells geometry (Ô VUÔNG MẢNG ĐỒNG ĐỀU)
  const items = currentStep.items || [];
  const numsCount = items.length || (currentStep.nums || []).length || 1;
  const cellWidth = numsCount <= 5 ? 74 : numsCount <= 8 ? 62 : 48;
  const gap = numsCount <= 5 ? 20 : numsCount <= 8 ? 14 : 10;
  const totalWidth = numsCount * cellWidth + (numsCount - 1) * gap;

  // Active indices, Pointers & Structures
  const activePair = currentStep.activePair;
  const pointers = currentStep.pointers || {};
  const matchedIndices = currentStep.matchedIndices || [];
  const activeIndices = currentStep.activeIndices || [];
  const hashMapData = currentStep.hashMapData;
  const countsTableData = currentStep.countsTableData;
  const resultData = currentStep.resultData;

  const visualizerStage = (
    <div
      className="problem-solution-execution__stage independent-scroll-region"
      tabIndex={0}
      role="region"
      aria-label="Algorithm Trace Visualizer"
    >
      {/* Top Control Toolbar */}
      <div className="smooth-vis-toolbar">
        <div className="smooth-vis-toolbar__left">
          <span className="smooth-vis-badge smooth-vis-badge--ast">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
            <span>Trace Visualizer</span>
          </span>
          <span
            className={`smooth-vis-badge ${
              isAstOnline ? "smooth-vis-badge--status-online" : "smooth-vis-badge--status-offline"
            }`}
            title={
              isAstOnline
                ? "AST backend connected on port 8081"
                : "Deterministic simulation trace active"
            }
          >
            <span className="smooth-vis-status-dot" />
            {isAstOnline ? "JDI Debugger" : "Simulation Engine"}
          </span>
        </div>

        <div className="smooth-vis-toolbar__right">
          <button
            type="button"
            className={`smooth-vis-refcode-btn ${showSourceCode ? "is-active" : ""}`}
            onClick={() => setShowSourceCode(!showSourceCode)}
            title="Toggle reference solution code pane"
          >
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
            <span>{showSourceCode ? "Hide Reference Code" : "Reference Code"}</span>
          </button>
        </div>
      </div>

      {statusMessage && (
        <div style={{ fontSize: "12px", color: "#38bdf8", padding: "0 8px", fontWeight: 600 }}>
          {statusMessage}
        </div>
      )}

        {/* Main Graphical Stage Board with Array Square Cells */}
        <div className={`smooth-vis-stage ${isFullscreen ? "is-stage-fullscreen" : ""}`}>
          {/* Stage Header Metrics */}
          <div className="smooth-vis-stage__header">
            {isFullscreen && (
              <button type="button" className="smooth-vis-btn" onClick={handleToggleFullscreen}>
                Thoát toàn màn hình (Esc)
              </button>
            )}
            <div className="smooth-vis-metrics">
              <span>
                Step: <strong>{stepIndex + 1}</strong>/{trace.length}
              </span>
              <span>
                Line: <strong className="metric-highlight">Line {currentStep.activeLine || "?"}</strong>
              </span>
              {currentStep.operationCount !== undefined && (
                <span>
                  Ops: <strong className="metric-highlight">{currentStep.operationCount}</strong>
                </span>
              )}
              <span>
                Size: <strong>{numsCount}</strong>
              </span>
            </div>

            <span className={`smooth-vis-action-tag smooth-vis-action-tag--${currentStep.status || "idle"}`}>
              {currentStep.type === "COMPARE" && "Compare Elements"}
              {currentStep.type === "POINTER_MOVE" && "Move Pointer"}
              {currentStep.type === "MAP_LOOKUP" && "Hash Map Lookup"}
              {currentStep.type === "MAP_PUT" && "Hash Map Insert"}
              {currentStep.type === "COUNT_UPDATE" && "Counter Update"}
              {currentStep.type === "MATCHED" && "Target Found"}
              {currentStep.type === "RESULT_PRODUCE" && "Yield Result"}
              {currentStep.type === "COMPLETE" && "Execution Complete"}
              {currentStep.type === "IDLE" && "Initialize Data"}
            </span>
          </div>

          {/* Main Visualizer Board */}
          <div
            ref={stageBoardRef}
            className="array-stage-board"
          >
            <ResizableColumns
              className="stage-dual-grid"
              initialPrimarySize="250px"
              minPrimaryWidth={160}
              minSecondaryWidth={200}
              resizeLabel="Resize variable scope and data board"
            >
              {/* LEFT: Local Variables & Result Area */}
              <div className="stage-scope-sidebar">
                <div className="scope-variables-box">
                  <div className="scope-variables-box__header">
                    <span className="scope-title">
                      Local Variables
                    </span>
                    <span className="scope-count">
                      {currentStep.scalarVars?.length || 0} vars
                    </span>
                  </div>

                  <div className="scope-variables-box__list">
                    {(!currentStep.scalarVars || currentStep.scalarVars.length === 0) ? (
                      <div className="scope-empty-hint">
                        <span>(No active variables in scope)</span>
                      </div>
                    ) : (
                      currentStep.scalarVars.map((v) => {
                        const prevVal = previousStep?.scalarVars?.find((pv) => pv.name === v.name)?.value;
                        const isNew = v.newlyCreated || prevVal === undefined;
                        const isChanged = prevVal !== undefined && prevVal !== v.value;

                        return (
                          <div
                            key={v.name}
                            className={`scope-var-card ${isNew ? "is-newly-created" : ""} ${
                              isChanged ? "is-value-changed" : ""
                            }`}
                          >
                            <div className="scope-var-card__meta">
                              <span className="scope-var-name">{v.name}</span>
                              {isNew && <span className="scope-var-badge">New</span>}
                              {isChanged && <span className="scope-var-badge scope-var-badge--update">Updated</span>}
                            </div>
                            <strong className="scope-var-val">{String(v.value)}</strong>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>

                <div
                  className={`scope-result-box ${
                    resultData?.status === "found" ? "scope-result-box--found" : ""
                  }`}
                >
                  <div className="scope-result-box__header">
                    <span className="scope-title">
                      Return Value
                    </span>
                    {resultData?.status === "found" ? (
                      <span className="result-status-tag result-status-tag--found">Found</span>
                    ) : (
                      <span className="result-status-tag result-status-tag--pending">Pending</span>
                    )}
                  </div>

                  <div className="scope-result-box__body">
                    {resultData?.status === "found" ? (
                      <div className="result-found-details">
                        <div className="result-found-value">
                          <small>result =</small>
                          <strong>{resultData.value}</strong>
                        </div>
                        {resultData.formula && (
                          <div className="result-found-formula">
                            {resultData.formula}
                          </div>
                        )}
                        {resultData.printed && (
                          <div className="result-found-print">
                            Console: <code>{resultData.printed}</code>
                          </div>
                        )}
                      </div>
                    ) : (
                      <div className="result-pending-details">
                        <ProgressView variant="spinner" size="sm" label="Searching for target solution..." />
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* CỘT PHẢI: SÂN KHẤU MẢNG & ANIMATION DẤU PHÉP TOÁN (Có Zoom & Scroll chống tràn) */}
              <div className="stage-array-area">
                <div className="stage-array-scroll-wrapper">
                  <div
                    className="stage-array-zoomable-content"
                    style={{
                      transform: `scale(${zoomLevel})`,
                      transformOrigin: "top center",
                    }}
                  >
                    <div
                      className="array-cells-canvas"
                      style={{ width: `${totalWidth}px`, height: `${cellWidth + 28}px` }}
                    >
                      {/* Animation Dấu Phép Toán (+, -, ==, ≠) Nổi Bật Giữa 2 Ô Đang Tính */}
                      {currentStep.operatorInfo &&
                        typeof currentStep.operatorInfo.leftIdx === "number" &&
                        typeof currentStep.operatorInfo.rightIdx === "number" &&
                        currentStep.operatorInfo.leftIdx >= 0 &&
                        currentStep.operatorInfo.rightIdx >= 0 && (
                          <div
                            className="operator-connector-bridge"
                            style={{
                              left: `${
                                Math.min(currentStep.operatorInfo.leftIdx, currentStep.operatorInfo.rightIdx) *
                                  (cellWidth + gap) +
                                cellWidth / 2
                              }px`,
                              width: `${
                                Math.abs(currentStep.operatorInfo.rightIdx - currentStep.operatorInfo.leftIdx) *
                                (cellWidth + gap)
                              }px`,
                            }}
                          >
                            <div className="operator-connector-line" />
                            <div
                              className={`operator-symbol-badge operator-symbol-badge--${currentStep.operatorInfo.type.toLowerCase()}`}
                            >
                              <span>{currentStep.operatorInfo.symbol}</span>
                            </div>
                            {currentStep.operatorInfo.expr && (
                              <div className="operator-expr-tooltip">
                                {currentStep.operatorInfo.expr}
                              </div>
                            )}
                          </div>
                        )}

                      {/* Dãy các ô vuông mảng (Array Square Cells) */}
                      {items.map((item) => {
                        const posIndex = item.pos !== undefined ? item.pos : item.id;
                        const leftPx = posIndex * (cellWidth + gap);

                        const isMatchedCell = matchedIndices.includes(posIndex);
                        const isLeftActive =
                          (activePair && (item.id === activePair.leftItemId || posIndex === activePair.idxLeft)) ||
                          pointers.i === posIndex ||
                          pointers.left === posIndex ||
                          (activeIndices.length === 1 && activeIndices[0] === posIndex);

                        const isRightActive =
                          (activePair && (item.id === activePair.rightItemId || posIndex === activePair.idxRight)) ||
                          pointers.j === posIndex ||
                          pointers.right === posIndex;

                        const isMidActive =
                          currentStep.midIndex === posIndex || pointers.mid === posIndex;

                        const isDimmed =
                          currentStep.dimmedIndices && currentStep.dimmedIndices.includes(posIndex);

                        // Semantic color assignments (Ý nghĩa màu sắc cố định)
                        let colorClass = "is-color-default";
                        if (isMatchedCell) {
                          colorClass = "is-color-matched"; // Xanh lá: Nghiệm / Hoàn tất
                        } else if (isLeftActive) {
                          colorClass = "is-color-cyan"; // Xanh Cyan: Đối tượng đang xét chính (i, left)
                        } else if (isRightActive) {
                          colorClass = "is-color-purple"; // Tím: Đối tượng thứ hai (j, right)
                        } else if (isMidActive) {
                          colorClass = "is-color-amber"; // Vàng: So sánh / mid
                        } else if (isDimmed) {
                          colorClass = "is-color-dimmed"; // Xám: Bị loại
                        }

                        return (
                          <div
                            key={item.id}
                            className={`array-cell-box ${colorClass}`}
                            style={{
                              width: `${cellWidth}px`,
                              height: `${cellWidth}px`,
                              transform: `translate3d(${leftPx}px, 0, 0)`,
                            }}
                            title={`nums[${posIndex}] = ${item.value}`}
                          >
                            <span className="cell-idx">[{posIndex}]</span>
                            <span className="cell-val">{item.value}</span>
                          </div>
                        );
                      })}
                    </div>

                    {/* Dynamic Pointers Under Array Cells */}
                    <div className="array-pointers-row" style={{ width: `${totalWidth}px` }}>
                      {Object.entries(pointers).map(([ptrName, ptrIndex]) => {
                        if (typeof ptrIndex !== "number" || ptrIndex < 0 || ptrIndex >= numsCount) {
                          return null;
                        }
                        const isPurple = ptrName === "j" || ptrName === "right";
                        const isAmber = ptrName === "mid";
                        const markerClass = isPurple
                          ? "array-pointer-marker--purple"
                          : isAmber
                          ? "array-pointer-marker--amber"
                          : "";

                        return (
                          <div
                            key={ptrName}
                            className={`array-pointer-marker ${markerClass}`}
                            style={{
                              width: `${cellWidth}px`,
                              transform: `translate3d(${ptrIndex * (cellWidth + gap)}px, 0, 0)`,
                            }}
                          >
                            <b>↑</b>
                            <span>
                              {ptrName} {isPurple ? "(Tím)" : isAmber ? "(Cam)" : "(Xanh)"}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                </div>

                {/* Action Status Pill at Bottom of Array Canvas (Lời giải thích tự nhiên, không in code) */}
                <div className="array-action-status-bar">
                  {matchedIndices.length > 0 ? (
                    <div className="array-action-pill array-action-pill--matched">
                      <span>🎉 {currentStep.comparison || "ĐÃ TÌM THẤY CẶP NGHIỆM!"}</span>
                    </div>
                  ) : currentStep.comparison ? (
                    <div className="array-action-pill">
                      <span>{currentStep.comparison}</span>
                    </div>
                  ) : null}
                </div>
              </div>
            </ResizableColumns>
          </div>

          {/* Representation: Hash Map Component */}
          {Array.isArray(hashMapData) && (
            <div className="solution-hashmap-card">
              <div className="solution-hashmap-card__header">
                <span>Hash Map (Complement Key → Stored Index)</span>
                <span>{hashMapData.length} {hashMapData.length === 1 ? "entry" : "entries"}</span>
              </div>
              <div className="solution-hashmap-entries">
                {hashMapData.length === 0 ? (
                  <span style={{ fontSize: "12px", color: "#8b949e", fontStyle: "italic" }}>
                    Map is currently empty
                  </span>
                ) : (
                  hashMapData.map((e, idx) => (
                    <div
                      key={idx}
                      className={`solution-hashmap-pill ${
                        e.state === "active"
                          ? "is-active"
                          : e.state === "added"
                          ? "is-added"
                          : e.state === "matched"
                          ? "is-matched"
                          : ""
                      }`}
                    >
                      <small>Key:</small> <strong>{e.key}</strong>
                      <span>→</span>
                      <small>Index:</small> <strong>{e.value}</strong>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Representation: Frequency Counts Table */}
          {Array.isArray(countsTableData) && (
            <div className="solution-counts-card">
              <div className="solution-counts-card__header">
                Character Frequency Table
              </div>
              <div className="solution-counts-grid">
                {countsTableData.map((c, idx) => (
                  <div
                    key={idx}
                    className={`solution-count-cell ${c.state === "active" ? "is-active" : ""}`}
                  >
                    <small>'{c.char}'</small>
                    <strong>{c.count}</strong>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Explainer Narrative Banner */}
          <div className="smooth-vis-explainer">
            <div className="smooth-vis-explainer__left">
              <span className={`smooth-vis-explainer__dot smooth-vis-explainer__dot--${currentStep.status || "idle"}`} />
              <p className="smooth-vis-explainer__text">
                <strong>Line {currentStep.activeLine || "?"} · {currentStep.comparison || currentStep.type || "Step"}:</strong>{" "}
                {currentStep.annotation || currentStep.context || "Executing algorithm step..."}
              </p>
            </div>
          </div>
        </div>

        {/* Playback Controls Footer */}
        <div className="smooth-vis-controls">
          <div className="smooth-vis-controls__playback">
            <button
              type="button"
              className="smooth-vis-btn"
              onClick={handleReset}
              title="Reset to beginning"
              aria-label="Reset execution"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="1 4 1 10 7 10" />
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
              </svg>
            </button>
            <button
              type="button"
              className="smooth-vis-btn"
              onClick={handlePrev}
              disabled={stepIndex <= 0}
              title="Step backward"
              aria-label="Previous step"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="19 20 9 12 19 4 19 20" />
                <line x1="5" y1="19" x2="5" y2="5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </button>
            <button
              type="button"
              className="smooth-vis-btn smooth-vis-btn--primary"
              onClick={handleTogglePlay}
              title={isPlaying ? "Pause trace" : "Play trace"}
              aria-label={isPlaying ? "Pause" : "Play"}
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
              className="smooth-vis-btn"
              onClick={handleNext}
              disabled={stepIndex >= trace.length - 1}
              title="Step forward"
              aria-label="Next step"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="5 4 15 12 5 20 5 4" />
                <line x1="19" y1="19" x2="19" y2="5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </button>
          </div>

          {/* Timeline Scrubber */}
          <div className="smooth-vis-timeline">
            <input
              type="range"
              min="0"
              max={Math.max(0, trace.length - 1)}
              value={stepIndex}
              onChange={handleSeek}
              aria-label="Execution step scrubber"
            />
            <span className="smooth-vis-timeline__label">
              Step <strong>{stepIndex + 1}</strong> / {trace.length}
            </span>
          </div>

          {/* Speed Controller Pills */}
          <div className="smooth-vis-speed-pills">
            <span>Speed:</span>
            {Object.entries(SPEED_CONFIG).map(([key]) => (
              <button
                type="button"
                key={key}
                className={`smooth-vis-speed-btn ${speedKey === key ? "is-active" : ""}`}
                onClick={() => setSpeedKey(key)}
              >
                {key}
              </button>
            ))}
          </div>
        </div>
      </div>
  );

  if (!showSourceCode) {
    return (
      <div className="problem-solution-execution problem-solution-execution--standalone">
        {visualizerStage}
      </div>
    );
  }

  return (
    <ResizableColumns
      className="problem-solution-execution"
      initialPrimarySize="1.35fr"
      minPrimaryWidth={360}
      minSecondaryWidth={280}
      resizeLabel="Resize trace stage and reference code"
    >
      {visualizerStage}

      {/* Right Column: Reference Code */}
      <section className="solution-runtime-code" aria-label="Reference solution code">
        <header className="solution-runtime-code__header">
          <strong>
            {solution?.language || "Java"} · {javaFilename(source)}
          </strong>
          <button type="button" onClick={handleCopyCode}>
            Copy code
          </button>
        </header>

        <div
          className="solution-source-lines independent-scroll-region"
          ref={sourceContainerRef}
          tabIndex="0"
          role="region"
          aria-label="Solution source code"
        >
          {lines.map((line, index) => {
            const lineNum = index + 1;
            const isLineActive = lineNum === currentStep?.activeLine;
            return (
              <div
                key={index}
                ref={isLineActive ? activeLineRef : null}
                className={`solution-source-line ${isLineActive ? "is-active" : ""}`}
              >
                <span aria-hidden="true">{lineNum}</span>
                <code>{line || " "}</code>
              </div>
            );
          })}
          <div className="solution-runtime-statement">
            <strong>Current Statement:</strong>
            <code>
              {currentStep?.context ||
                currentStep?.rawStep?.code?.trim() ||
                currentStep?.comparison ||
                "Select a step to inspect execution flow."}
            </code>
          </div>
        </div>
      </section>
    </ResizableColumns>
  );
}

export default function ProblemSolutionPanel({ problem, onAnnounce, onStepChange }) {
  const solutions = problem?.solutions || [];
  const [selectedIndex, setSelectedIndex] = useState(0);
  const solution = solutions[selectedIndex];

  if (!problem) return <p role="status">Loading problem trace...</p>;
  if (!solution) {
    return (
      <section className="solution-runtime-summary">
        <h2>Reference Solution Pending</h2>
        <p>Interactive algorithm trace is currently in preparation for {problem.title}.</p>
      </section>
    );
  }

  return (
    <div className="problem-solution-panel">
      <header className="solution-runtime-summary">
        <div className="solution-summary-left">
          <span className="solution-summary-name">{solution.name || "Optimal Approach"}</span>
          <div className="solution-complexity-chips">
            <span className="complexity-tag">Time: <strong>{solution.timeComplexity || "O(n)"}</strong></span>
            <span className="complexity-tag">Space: <strong>{solution.spaceComplexity || "O(1)"}</strong></span>
          </div>
        </div>
        {solutions.length > 1 && (
          <label className="solution-select-label">
            Approach:
            <select
              aria-label="Select reference solution"
              value={selectedIndex}
              onChange={(e) => setSelectedIndex(Number(e.target.value))}
            >
              {solutions.map((item, index) => (
                <option key={item.id || index} value={index}>
                  Approach {index + 1} ({item.language || "Java"})
                </option>
              ))}
            </select>
          </label>
        )}
      </header>

      <SolutionVisualizerExecution
        key={`${problem.id}-${solution.id}`}
        problem={problem}
        solution={solution}
        onAnnounce={onAnnounce}
        onStepChange={onStepChange}
      />
    </div>
  );
}
