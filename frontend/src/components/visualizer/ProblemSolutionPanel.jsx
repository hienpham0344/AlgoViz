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
import ResizableColumns from "../ResizableColumns.jsx";

const SPEED_CONFIG = {
  "0.5x": { label: "0.5x", durationMs: 3600, animSec: "2.4s" },
  "1x": { label: "1x", durationMs: 2000, animSec: "1.4s" },
  "1.5x": { label: "1.5x", durationMs: 1300, animSec: "0.9s" },
  "2x": { label: "2x", durationMs: 800, animSec: "0.55s" },
};

function SolutionVisualizerExecution({ problem, solution, onAnnounce }) {
  const source = solution?.codeSnippet || "";
  const lines = useMemo(() => source.split(/\r?\n/), [source]);
  const activeLineRef = useRef(null);
  const sourceContainerRef = useRef(null);
  const timerRef = useRef(null);
  const stageBoardRef = useRef(null);
  const heightDragRef = useRef(null);

  const [speedKey, setSpeedKey] = useState("1x");
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);

  // Zoom & Resize state for anti-overflow & customizable workspace
  const [zoomLevel, setZoomLevel] = useState(1);
  const [stageHeight, setStageHeight] = useState(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  const handleZoomIn = () => setZoomLevel((z) => Math.min(1.8, +(z + 0.15).toFixed(2)));
  const handleZoomOut = () => setZoomLevel((z) => Math.max(0.4, +(z - 0.15).toFixed(2)));
  const handleResetZoom = () => setZoomLevel(1);
  const handleToggleFullscreen = () => setIsFullscreen((prev) => !prev);

  const handleHeightResizePointerDown = (event) => {
    const board = stageBoardRef.current;
    if (!board || !event.isPrimary || event.button !== 0) return;
    event.preventDefault();
    event.stopPropagation();
    event.currentTarget.focus({ preventScroll: true });

    const startY = event.clientY;
    const startHeight = board.getBoundingClientRect().height;
    let finalHeight = startHeight;

    heightDragRef.current = {
      y: startY,
      height: startHeight,
      previous: stageHeight,
    };
    document.body.classList.add("is-resizing-height");

    const onPointerMove = (moveEvent) => {
      if (!heightDragRef.current) return;
      moveEvent.preventDefault();
      const deltaY = moveEvent.clientY - startY;
      const nextHeight = Math.max(220, Math.min(1000, Math.round(startHeight + deltaY)));
      finalHeight = nextHeight;
      // Real-time zero-latency DOM height update directly on stage board
      board.style.height = `${nextHeight}px`;
    };

    const onPointerUp = () => {
      if (!heightDragRef.current) return;
      heightDragRef.current = null;
      document.body.classList.remove("is-resizing-height");

      window.removeEventListener("pointermove", onPointerMove);
      window.removeEventListener("pointerup", onPointerUp);
      window.removeEventListener("pointercancel", onPointerUp);

      setStageHeight(finalHeight);
    };

    window.addEventListener("pointermove", onPointerMove, { passive: false });
    window.addEventListener("pointerup", onPointerUp, { passive: false });
    window.addEventListener("pointercancel", onPointerUp, { passive: false });
  };

  const handleHeightResizeKeyDown = (event) => {
    if (event.key === "Escape" && heightDragRef.current) {
      const prev = heightDragRef.current.previous;
      setStageHeight(prev);
      if (stageBoardRef.current) {
        stageBoardRef.current.style.height = prev ? `${prev}px` : "";
      }
      heightDragRef.current = null;
      document.body.classList.remove("is-resizing-height");
      return;
    }
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      setStageHeight(null);
      if (stageBoardRef.current) {
        stageBoardRef.current.style.height = "";
      }
    }
    if (event.key === "ArrowUp" || event.key === "ArrowDown") {
      event.preventDefault();
      const delta = (event.shiftKey ? 50 : 15) * (event.key === "ArrowUp" ? -1 : 1);
      const currentH = stageBoardRef.current?.getBoundingClientRect().height || 250;
      const newH = Math.max(220, Math.min(1000, Math.round(currentH + delta)));
      setStageHeight(newH);
      if (stageBoardRef.current) {
        stageBoardRef.current.style.height = `${newH}px`;
      }
    }
  };

  const handleResetHeight = () => {
    setStageHeight(null);
    if (stageBoardRef.current) {
      stageBoardRef.current.style.height = "";
    }
  };

  useEffect(() => () => {
    if (heightDragRef.current) document.body.classList.remove("is-resizing-height");
  }, []);

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
  const [isFetchingAst, setIsFetchingAst] = useState(false);
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

  // Notify parent of step announcement
  useEffect(() => {
    if (onAnnounce && currentStep) {
      onAnnounce(
        `Step ${stepIndex + 1}/${trace.length}: Line ${currentStep.activeLine} - ${currentStep.comparison || currentStep.annotation || ""}`,
      );
    }
  }, [stepIndex, currentStep, trace.length, onAnnounce]);

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

  const handleRerunAst = async () => {
    setIsFetchingAst(true);
    setStatusMessage("Đang gửi mã nguồn Java tới AST-tool backend (port 8081)...");
    try {
      const res = await fetchDebugSteps(source);
      if (Array.isArray(res?.steps) && res.steps.length > 0) {
        const richAdapted = adaptAstStepsToRichVisualizer(res.steps, source);
        if (richAdapted.length > 0) {
          setTrace(richAdapted);
        }
        setIsAstOnline(true);
        setStepIndex(0);
        setIsPlaying(false);
        setStatusMessage("✅ Đồng bộ trace thành công từ AST-tool JDI!");
      } else {
        throw new Error("No steps returned");
      }
    } catch (err) {
      console.warn("AST-tool live sync failed:", err);
      setIsAstOnline(false);
      const sim = buildSimulatedTraceForSolution(problem.title, source);
      if (sim && sim.length > 0) {
        setTrace(sim);
      } else {
        setTrace(initialTrace);
      }
      setStepIndex(0);
      setIsPlaying(false);
      setStatusMessage("⚡ Đang chạy Simulation Engine mô phỏng từng bước!");
    } finally {
      setIsFetchingAst(false);
      setTimeout(() => setStatusMessage(""), 3500);
    }
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

  return (
    <ResizableColumns
      className="problem-solution-execution"
      initialPrimarySize="1.35fr"
      minPrimaryWidth={360}
      minSecondaryWidth={280}
      resizeLabel="Đổi độ rộng mô phỏng và mã nguồn solution"
    >
      {/* ---------------- Left Column: Visualizer Stage ---------------- */}
      <div className="problem-solution-execution__stage independent-scroll-region" tabIndex={0} role="region" aria-label="Trực quan thuật toán">
        {/* Top Control Toolbar */}
        <div className="smooth-vis-toolbar">
          <div className="smooth-vis-toolbar__left">
            <span className="smooth-vis-badge smooth-vis-badge--ast">⚡ AST-tool Visualizer</span>
            <span
              className={`smooth-vis-badge ${
                isAstOnline ? "smooth-vis-badge--status-online" : "smooth-vis-badge--status-offline"
              }`}
              title={
                isAstOnline
                  ? "AST-tool backend sẵn sàng trên cổng 8081"
                  : "Đang chạy chế độ mô phỏng mượt mà (Simulation Engine)"
              }
            >
              <span className="smooth-vis-status-dot" />
              {isAstOnline ? "AST Server 8081" : "Simulation Mode"}
            </span>
          </div>

          <div className="smooth-vis-toolbar__right">
            {/* Bộ điều khiển Thu phóng & Toàn màn hình (Zoom & Fullscreen Controls) */}
            <div className="smooth-vis-zoom-group" role="group" aria-label="Bộ điều khiển thu phóng sân khấu">
              <button
                type="button"
                className="smooth-vis-zoom-btn"
                onClick={handleZoomOut}
                disabled={zoomLevel <= 0.45}
                title="Thu nhỏ sân khấu (Zoom Out)"
              >
                −
              </button>
              <button
                type="button"
                className="smooth-vis-zoom-val"
                onClick={handleResetZoom}
                title="Nhấp để trở về kích thước chuẩn 100%"
              >
                {Math.round(zoomLevel * 100)}%
              </button>
              <button
                type="button"
                className="smooth-vis-zoom-btn"
                onClick={handleZoomIn}
                disabled={zoomLevel >= 1.8}
                title="Phóng to sân khấu (Zoom In)"
              >
                +
              </button>
              <button
                type="button"
                className="smooth-vis-zoom-btn"
                onClick={handleResetZoom}
                title="Căn vừa màn hình (Fit to Screen)"
              >
                ⛶
              </button>
              <button
                type="button"
                className={`smooth-vis-zoom-btn ${isFullscreen ? "is-active" : ""}`}
                onClick={handleToggleFullscreen}
                title={isFullscreen ? "Thu nhỏ về giao diện thường" : "Mở rộng toàn màn hình (Fullscreen)"}
              >
                {isFullscreen ? "🗗" : "⤢"}
              </button>
            </div>

            <button
              type="button"
              className="smooth-vis-mode-btn"
              onClick={handleRerunAst}
              disabled={isFetchingAst}
              title="Gửi mã nguồn Java tới AST-tool backend port 8081"
            >
              {isFetchingAst ? "Đang gọi..." : "⚡ Sync AST Live"}
            </button>
          </div>
        </div>

        {statusMessage && (
          <div style={{ fontSize: "12px", color: "#01e599", padding: "0 8px", fontWeight: 600 }}>
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
                Bước: <strong>{stepIndex + 1}</strong>/{trace.length}
              </span>
              <span>
                Dòng code: <strong className="metric-highlight">Line {currentStep.activeLine || "?"}</strong>
              </span>
              {currentStep.operationCount !== undefined && (
                <span>
                  Thao tác: <strong className="metric-highlight">{currentStep.operationCount}</strong>
                </span>
              )}
              <span>
                Kích thước: <strong>{numsCount}</strong>
              </span>
              {stageHeight && (
                <span style={{ fontSize: "11px", color: "#8b7ca6" }}>
                  Cao: <strong>{stageHeight}px</strong>
                </span>
              )}
            </div>

            <span className={`smooth-vis-action-tag smooth-vis-action-tag--${currentStep.status || "idle"}`}>
              {currentStep.type === "COMPARE" && "⚡ So sánh (Compare)"}
              {currentStep.type === "POINTER_MOVE" && "👉 Di chuyển con trỏ"}
              {currentStep.type === "MAP_LOOKUP" && "🔍 Tra cứu Hash Map"}
              {currentStep.type === "MAP_PUT" && "📥 Thêm vào Hash Map"}
              {currentStep.type === "COUNT_UPDATE" && "📊 Cập nhật bảng đếm"}
              {currentStep.type === "MATCHED" && "🎯 ĐÃ TÌM THẤY NGHIỆM!"}
              {currentStep.type === "RESULT_PRODUCE" && "★ Tạo kết quả (Result)"}
              {currentStep.type === "COMPLETE" && "✓ Hoàn tất thực thi"}
              {currentStep.type === "IDLE" && "Khởi tạo dữ liệu"}
            </span>
          </div>

          {/* Sân khấu chính: Layout 2 cột cân đối có thanh kéo giãn và thu phóng chống tràn */}
          <div
            ref={stageBoardRef}
            className="array-stage-board"
            style={{ height: stageHeight ? `${stageHeight}px` : undefined }}
          >
            <ResizableColumns
              className="stage-dual-grid"
              initialPrimarySize="250px"
              minPrimaryWidth={160}
              minSecondaryWidth={200}
              resizeLabel="Đổi độ rộng vùng biến và sân khấu dữ liệu"
            >
              {/* CỘT TRÁI: BIẾN CỤC BỘ & VÙNG KẾT QUẢ (Scope & Result) */}
              <div className="stage-scope-sidebar">
                {/* 1. Panel Biến Cục Bộ: Xuất hiện đúng lúc dòng code khởi tạo chạy */}
                <div className="scope-variables-box">
                  <div className="scope-variables-box__header">
                    <span className="scope-title">
                      <span className="scope-icon">📦</span> BIẾN CỤC BỘ
                    </span>
                    <span className="scope-count">
                      {currentStep.scalarVars?.length || 0} biến
                    </span>
                  </div>

                  <div className="scope-variables-box__list">
                    {(!currentStep.scalarVars || currentStep.scalarVars.length === 0) ? (
                      <div className="scope-empty-hint">
                        <span>(Chưa có biến trong phạm vi này)</span>
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
                              {isNew && <span className="scope-var-badge">Mới</span>}
                              {isChanged && <span className="scope-var-badge scope-var-badge--update">Đổi</span>}
                            </div>
                            <strong className="scope-var-val">{String(v.value)}</strong>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>

                {/* 2. Panel Vùng Kết Quả: Bừng sáng Xanh Lá Neon khi tìm thấy nghiệm */}
                <div
                  className={`scope-result-box ${
                    resultData?.status === "found" ? "scope-result-box--found" : ""
                  }`}
                >
                  <div className="scope-result-box__header">
                    <span className="scope-title">
                      <span className="scope-icon">🏆</span> VÙNG KẾT QUẢ
                    </span>
                    {resultData?.status === "found" ? (
                      <span className="result-status-tag result-status-tag--found">✓ TÌM THẤY</span>
                    ) : (
                      <span className="result-status-tag result-status-tag--pending">Chờ kết quả</span>
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
                        <span className="result-spinner" />
                        <span>Đang tìm kiếm nghiệm...</span>
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

            {/* THANH KÉO CO GIÃN CHIỀU CAO SÂN KHẤU (Vertical Height Resizer Handle ở đáy) */}
            <button
              type="button"
              className="stage-resizer-height"
              onPointerDown={handleHeightResizePointerDown}
              onKeyDown={handleHeightResizeKeyDown}
              onDoubleClick={handleResetHeight}
              title="Bấm giữ và kéo lên/xuống để mở rộng hoặc thu hẹp chiều cao sân khấu (Nhấp đúp để đặt lại tự động)"
              aria-label="Đổi chiều cao sân khấu; nhấp đúp hoặc Enter để đặt lại"
              role="separator"
              aria-orientation="horizontal"
              aria-valuemin={220}
              aria-valuemax={1000}
              aria-valuenow={stageHeight ?? 250}
              aria-valuetext={stageHeight ? `${Math.round(stageHeight)} px` : "Chiều cao tự động"}
            >
              <span className="resizer-handle-pill" />
            </button>

          {/* Representation: Hash Map Component (khi solution dùng Hash Map) */}
          {Array.isArray(hashMapData) && (
            <div className="solution-hashmap-card">
              <div className="solution-hashmap-card__header">
                <span>Bảng tra cứu Hash Map (Key: Số bù → Value: Index)</span>
                <span>{hashMapData.length} phần tử</span>
              </div>
              <div className="solution-hashmap-entries">
                {hashMapData.length === 0 ? (
                  <span style={{ fontSize: "11px", color: "#8b7ca6", fontStyle: "italic" }}>
                    Map rỗng (chưa có phần tử nào được thêm)
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
                      <small>Khóa:</small> <strong>{e.key}</strong>
                      <span>→</span>
                      <small>Index:</small> <strong>{e.value}</strong>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}

          {/* Representation: Frequency Counts Table (khi bài toán dùng bảng đếm như Anagram) */}
          {Array.isArray(countsTableData) && (
            <div className="solution-counts-card">
              <div className="solution-counts-card__header">
                Bảng đếm tần suất ký tự (Frequency Table)
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

          {/* Explainer Banner chuẩn: Lời giải thích tự nhiên, dễ hiểu */}
          <div className="smooth-vis-explainer">
            <div className="smooth-vis-explainer__left">
              <span className="smooth-vis-explainer__icon">
                {currentStep.type === "MATCHED" || currentStep.type === "RESULT_PRODUCE"
                  ? "🎯"
                  : currentStep.type === "COMPARE" || currentStep.type === "MAP_LOOKUP"
                  ? "⚡"
                  : currentStep.type === "COUNT_UPDATE" || currentStep.type === "MAP_PUT"
                  ? "📥"
                  : currentStep.type === "COMPLETE"
                  ? "★"
                  : "💡"}
              </span>
              <p className="smooth-vis-explainer__text">
                <strong>Line {currentStep.activeLine || "?"} · {currentStep.comparison || "Thực thi"}:</strong>{" "}
                {currentStep.annotation || currentStep.context || "Đang xử lý bước chạy thuật toán..."}
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
              title="Quay lại bước đầu (Reset)"
            >
              ⏮
            </button>
            <button
              type="button"
              className="smooth-vis-btn"
              onClick={handlePrev}
              disabled={stepIndex <= 0}
              title="Lùi một bước"
            >
              ◀
            </button>
            <button
              type="button"
              className="smooth-vis-btn smooth-vis-btn--primary"
              onClick={handleTogglePlay}
              title={isPlaying ? "Tạm dừng" : "Phát chuyển động"}
            >
              {isPlaying ? "⏸" : "▶"}
            </button>
            <button
              type="button"
              className="smooth-vis-btn"
              onClick={handleNext}
              disabled={stepIndex >= trace.length - 1}
              title="Tiến một bước"
            >
              ▶
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
              aria-label="Thanh trượt tiến độ bước chạy"
            />
            <span className="smooth-vis-timeline__label">
              Bước <strong>{stepIndex + 1}</strong> / {trace.length}
            </span>
          </div>

          {/* Speed Controller Pills */}
          <div className="smooth-vis-speed-pills">
            <span>Tốc độ:</span>
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

      {/* ---------------- Right Column: Synchronized Code Editor ---------------- */}
      <section className="solution-runtime-code" aria-label="Mã nguồn giải thuật">
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
                "Chọn một bước để kiểm tra luồng thực thi."}
            </code>
          </div>
        </div>
      </section>
    </ResizableColumns>
  );
}

export default function ProblemSolutionPanel({ problem, onAnnounce }) {
  const solutions = problem?.solutions || [];
  const [selectedIndex, setSelectedIndex] = useState(0);
  const solution = solutions[selectedIndex];

  if (!problem) return <p role="status">Đang tải bài toán…</p>;
  if (!solution) {
    return (
      <section className="solution-runtime-summary">
        <h2>Chưa có giải pháp mẫu</h2>
        <p>Bài toán {problem.title} hiện chưa được thêm bài giải mẫu.</p>
      </section>
    );
  }

  return (
    <div className="problem-solution-panel">
      <header className="solution-runtime-summary">
        <div>
          <h2>{problem.title}</h2>
          <p>{solution.explanation}</p>
        </div>
        <label>
          Giải pháp:{" "}
          <select
            aria-label="Chọn bài giải mẫu"
            value={selectedIndex}
            onChange={(e) => setSelectedIndex(Number(e.target.value))}
          >
            {solutions.map((item, index) => (
              <option key={item.id} value={index}>
                Solution {index + 1} · {item.language}
              </option>
            ))}
          </select>
        </label>
        <span>
          Độ phức tạp: Thời gian {solution.timeComplexity || "O(N²)"} · Bộ nhớ {solution.spaceComplexity || "O(1)"}
        </span>
      </header>

      <SolutionVisualizerExecution
        key={`${problem.id}-${solution.id}`}
        problem={problem}
        solution={solution}
        onAnnounce={onAnnounce}
      />
    </div>
  );
}
