import { useState, useEffect, useRef, useMemo } from "react";
import {
  generateBubbleSortTrace,
  fetchDebugSteps,
  checkAstToolHealth,
  adaptStepsForVisualizer,
  BUBBLE_SORT_SAMPLE_JAVA,
} from "../../services/astApi";
import "./SmoothSortVisualizer.css";

const PRESET_ARRAYS = {
  default: [5, 2, 8, 1, 9, 3],
  reverse: [9, 8, 6, 4, 2, 1],
  nearly: [1, 2, 5, 4, 7, 9],
  small: [4, 1, 3, 2],
};

const SPEED_CONFIG = {
  "0.5x": { label: "0.5x", durationMs: 1800, animSec: "1.4s" },
  "1x": { label: "1x", durationMs: 1000, animSec: "0.85s" },
  "1.5x": { label: "1.5x", durationMs: 650, animSec: "0.5s" },
  "2x": { label: "2x", durationMs: 450, animSec: "0.32s" },
};

export default function SmoothSortVisualizer({
  initialArray = PRESET_ARRAYS.default,
  onStepChange,
}) {
  const [currentArray, setCurrentArray] = useState(initialArray);
  const [customInput, setCustomInput] = useState(initialArray.join(", "));
  const [showCustomInput, setShowCustomInput] = useState(false);
  const [viewMode, setViewMode] = useState("bars"); // 'bars' | 'boxes'
  const [speedKey, setSpeedKey] = useState("1x");

  // Trace engine state
  const [trace, setTrace] = useState(() => generateBubbleSortTrace(initialArray));
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);

  // AST backend connectivity
  const [isAstOnline, setIsAstOnline] = useState(false);
  const [isFetchingAst, setIsFetchingAst] = useState(false);
  const [statusMessage, setStatusMessage] = useState("");

  const timerRef = useRef(null);

  // Check AST server health on mount
  useEffect(() => {
    let isMounted = true;
    checkAstToolHealth().then((online) => {
      if (isMounted) setIsAstOnline(online);
    });
    return () => {
      isMounted = false;
    };
  }, []);

  const currentStep = useMemo(() => trace[stepIndex] || trace[0] || {}, [trace, stepIndex]);

  // Notify parent of active line / step
  useEffect(() => {
    if (onStepChange && currentStep) {
      onStepChange(currentStep);
    }
  }, [currentStep, onStepChange]);

  // Autoplay ticker with precise timing
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

  // Change preset
  const handlePresetChange = (presetKey) => {
    if (presetKey === "custom") {
      setShowCustomInput(true);
      return;
    }
    setShowCustomInput(false);
    const arr = PRESET_ARRAYS[presetKey] || PRESET_ARRAYS.default;
    setCurrentArray(arr);
    setTrace(generateBubbleSortTrace(arr));
    setStepIndex(0);
    setIsPlaying(false);
    setCustomInput(arr.join(", "));
  };

  // Apply custom array
  const handleApplyCustom = (e) => {
    e.preventDefault();
    const parsed = customInput
      .split(/[,\s]+/)
      .map((n) => parseInt(n.trim(), 10))
      .filter((n) => !isNaN(n));

    if (parsed.length >= 2 && parsed.length <= 15) {
      setCurrentArray(parsed);
      setTrace(generateBubbleSortTrace(parsed));
      setStepIndex(0);
      setIsPlaying(false);
      setStatusMessage(`Đã áp dụng mảng mới: [${parsed.join(", ")}]`);
      setTimeout(() => setStatusMessage(""), 2500);
    } else {
      alert("Vui lòng nhập từ 2 đến 15 số nguyên hợp lệ.");
    }
  };

  // Live AST-tool backend sync
  const handleFetchAstLive = async () => {
    setIsFetchingAst(true);
    setStatusMessage("Đang gửi Java code đến AST-tool backend (port 8081)...");
    try {
      const res = await fetchDebugSteps(BUBBLE_SORT_SAMPLE_JAVA);
      const adapted = adaptStepsForVisualizer(res);
      if (adapted.length > 0) {
        setTrace(adapted);
        setStepIndex(0);
        setIsPlaying(false);
        setIsAstOnline(true);
        setStatusMessage("✅ Đã đồng bộ trace trực quan từ AST-tool JDI!");
      }
    } catch (err) {
      console.warn("AST-tool live sync failed:", err);
      setIsAstOnline(false);
      setStatusMessage("⚠️ Không kết nối được AST-tool (port 8081). Dùng engine mô phỏng.");
    } finally {
      setIsFetchingAst(false);
      setTimeout(() => setStatusMessage(""), 3500);
    }
  };

  // Geometry calculations for smooth absolute positioning
  const numsCount = currentStep.items ? currentStep.items.length : currentArray.length;
  const cellWidth = numsCount <= 6 ? 64 : numsCount <= 10 ? 50 : 38;
  const gap = numsCount <= 6 ? 20 : numsCount <= 10 ? 14 : 10;
  const totalWidth = numsCount * cellWidth + (numsCount - 1) * gap;

  // Min and max for bar heights
  const minVal = useMemo(() => {
    const vals = (currentStep.items || []).map((it) => it.value);
    return vals.length ? Math.min(...vals) : 1;
  }, [currentStep.items]);

  const maxVal = useMemo(() => {
    const vals = (currentStep.items || []).map((it) => it.value);
    return vals.length ? Math.max(...vals) : 10;
  }, [currentStep.items]);

  // Compute pointer positions
  const pJ = currentStep.pointers?.j ?? -1;
  const pJNext = pJ >= 0 && pJ < numsCount - 1 ? pJ + 1 : -1;

  const currentDuration = SPEED_CONFIG[speedKey].animSec;

  // Active Pair tracking:
  // Item A (Left) = ALWAYS CYAN (#00e5ff)
  // Item B (Right) = ALWAYS PURPLE (#d946ef)
  const isSwapping = currentStep.type === "SWAPPING";
  const isComparing = currentStep.type === "COMPARE";
  const activePair = currentStep.activePair;

  return (
    <div
      className="smooth-vis-root"
      style={{
        "--vis-cell-width": `${cellWidth}px`,
        "--vis-gap": `${gap}px`,
        "--vis-duration": currentDuration,
      }}
    >
      {/* ---------------- Top Toolbar ---------------- */}
      <div className="smooth-vis-toolbar">
        <div className="smooth-vis-toolbar__left">
          <span className="smooth-vis-badge smooth-vis-badge--ast">
            ⚡ AST-tool Visualizer
          </span>
          <span
            className={`smooth-vis-badge ${
              isAstOnline
                ? "smooth-vis-badge--status-online"
                : "smooth-vis-badge--status-offline"
            }`}
            title={
              isAstOnline
                ? "AST-tool backend sẵn sàng trên cổng 8081"
                : "Chưa kết nối cổng 8081 (Đang chạy engine mô phỏng mượt mà)"
            }
          >
            <span className="smooth-vis-status-dot" />
            {isAstOnline ? "AST Server 8081" : "Simulation Mode"}
          </span>

          <select
            className="smooth-vis-select"
            onChange={(e) => handlePresetChange(e.target.value)}
            defaultValue="default"
            aria-label="Chọn bộ dữ liệu mảng"
          >
            <option value="default">Mảng mặc định [5, 2, 8, 1, 9, 3]</option>
            <option value="reverse">Mảng ngược [9, 8, 6, 4, 2, 1]</option>
            <option value="nearly">Gần xếp [1, 2, 5, 4, 7, 9]</option>
            <option value="small">Mảng nhỏ [4, 1, 3, 2]</option>
            <option value="custom">Tự nhập mảng...</option>
          </select>
        </div>

        <div className="smooth-vis-toolbar__right">
          <button
            type="button"
            className="smooth-vis-mode-btn"
            onClick={handleFetchAstLive}
            disabled={isFetchingAst}
            title="Gửi Java sample code tới AST-tool JDI runner"
          >
            {isFetchingAst ? "Đang gọi..." : "⚡ Sync AST Live"}
          </button>

          <div className="smooth-vis-btn-group" role="group" aria-label="Kiểu hiển thị">
            <button
              type="button"
              className={`smooth-vis-mode-btn ${viewMode === "bars" ? "is-active" : ""}`}
              onClick={() => setViewMode("bars")}
            >
              Cột (Bars)
            </button>
            <button
              type="button"
              className={`smooth-vis-mode-btn ${viewMode === "boxes" ? "is-active" : ""}`}
              onClick={() => setViewMode("boxes")}
            >
              Hộp (Boxes)
            </button>
          </div>
        </div>
      </div>

      {/* Custom input bar */}
      {showCustomInput && (
        <form className="smooth-vis-custom-bar" onSubmit={handleApplyCustom}>
          <span>Nhập mảng (cách nhau bởi dấu phẩy):</span>
          <input
            type="text"
            value={customInput}
            onChange={(e) => setCustomInput(e.target.value)}
            placeholder="Ví dụ: 8, 3, 5, 1, 9"
          />
          <button type="submit">Áp dụng</button>
        </form>
      )}

      {statusMessage && (
        <div style={{ fontSize: "11px", color: "#01e599", padding: "0 8px" }}>
          {statusMessage}
        </div>
      )}

      {/* ---------------- Main Stage & Animated Array ---------------- */}
      <div className="smooth-vis-stage">
        <div className="smooth-vis-stage__header">
          <div className="smooth-vis-metrics">
            <span>
              Pass: <strong>{(currentStep.pointers?.i ?? 0) + 1}</strong>
            </span>
            <span>
              So sánh: <strong className="metric-highlight">{currentStep.compareCount ?? 0}</strong>
            </span>
            <span>
              Hoán đổi: <strong className="metric-highlight">{currentStep.swapCount ?? 0}</strong>
            </span>
            <span>
              Kích thước: <strong>{numsCount}</strong>
            </span>
          </div>

          <span
            className={`smooth-vis-action-tag smooth-vis-action-tag--${
              currentStep.status || "idle"
            }`}
          >
            {currentStep.type === "COMPARE" && "⚡ So sánh (Compare)"}
            {currentStep.type === "SWAPPING" && "🔄 Đang hoán đổi vị trí"}
            {currentStep.type === "SWAP_SETTLED" && "✓ Đã đổi chỗ xong"}
            {currentStep.type === "PASS_END" && "✓ Xong Pass"}
            {currentStep.type === "SORTED" && "★ Đã sắp xếp hoàn tất"}
            {currentStep.type === "IDLE" && "Sẵn sàng"}
          </span>
        </div>

        <div className="smooth-vis-board">
          {/* FLIP-animated Canvas */}
          <div
            className="smooth-vis-array-canvas"
            style={{ width: `${totalWidth}px` }}
          >
            {(currentStep.items || []).map((item) => {
              const posIndex = item.pos;
              const leftPx = posIndex * (cellWidth + gap);

              // Consistent Color Tracking:
              // Left item is ALWAYS Cyan. Right item is ALWAYS Purple.
              // They retain their colors when gliding across!
              const isLeftItem = activePair && item.id === activePair.leftItemId;
              const isRightItem = activePair && item.id === activePair.rightItemId;

              const isSorted =
                currentStep.sortedIndices &&
                currentStep.sortedIndices.includes(posIndex);

              let colorClass = "is-color-default";
              let translateY = 0;

              if (isLeftItem) {
                colorClass = "is-color-cyan";
                if (isSwapping) {
                  translateY = -24; // nhấc nhẹ lên trên và trượt sang phải
                } else if (isComparing) {
                  translateY = -8;
                }
              } else if (isRightItem) {
                colorClass = "is-color-purple";
                if (isSwapping) {
                  translateY = 24; // hạ nhẹ xuống dưới và trượt sang trái
                } else if (isComparing) {
                  translateY = -8;
                }
              } else if (isSorted) {
                colorClass = "is-color-sorted";
              }

              // Bar height calculation for bar mode
              const range = maxVal - minVal || 1;
              const ratio = Math.max(0, Math.min(1, (item.value - minVal) / range));
              const barHeight = Math.round(50 + ratio * 105);

              return (
                <div
                  key={item.id}
                  className={`smooth-vis-item smooth-vis-item--${viewMode} ${colorClass}`}
                  style={{
                    transform: `translate3d(${leftPx}px, ${translateY}px, 0)`,
                    ...(viewMode === "bars" ? { height: `${barHeight}px` } : {}),
                  }}
                >
                  <span className="item-val">{item.value}</span>
                  <span className="item-idx">[{posIndex}]</span>
                </div>
              );
            })}
          </div>

          {/* Dynamic Swap Status Bar at Bottom (HÀNG NGANG DƯỚI CÙNG KHÔNG CHE CỘT) */}
          <div className="smooth-vis-swap-bottom-bar">
            {isSwapping && activePair ? (
              <div className="smooth-vis-swap-pill smooth-vis-swap-pill--swapping">
                <span className="pill-tag pill-tag--cyan">
                  Xanh: {activePair.valLeft}
                </span>
                <span className="pill-arrow">⇄ ĐANG HOÁN ĐỔI ⇄</span>
                <span className="pill-tag pill-tag--purple">
                  Tím: {activePair.valRight}
                </span>
              </div>
            ) : isComparing && activePair ? (
              <div className="smooth-vis-swap-pill">
                <span className="pill-tag pill-tag--cyan">
                  Xanh: {activePair.valLeft}
                </span>
                <span className="pill-arrow">
                  {activePair.valLeft > activePair.valRight
                    ? "> (Sẽ đổi chỗ)"
                    : "≤ (Giữ nguyên)"}
                </span>
                <span className="pill-tag pill-tag--purple">
                  Tím: {activePair.valRight}
                </span>
              </div>
            ) : currentStep.type === "SWAP_SETTLED" && activePair ? (
              <div className="smooth-vis-swap-pill">
                <span className="pill-arrow">✓ ĐÃ ĐỔI CHỖ XONG:</span>
                <span className="pill-tag pill-tag--purple">
                  Tím: {activePair.valRight}
                </span>
                <span className="pill-arrow">ở trước</span>
                <span className="pill-tag pill-tag--cyan">
                  Xanh: {activePair.valLeft}
                </span>
              </div>
            ) : null}
          </div>

          {/* Smooth Pointer Indicators Row */}
          <div
            className="smooth-vis-pointers-row"
            style={{ width: `${totalWidth}px` }}
          >
            {pJ >= 0 && (
              <div
                className="smooth-vis-pointer-marker"
                style={{
                  transform: `translate3d(${pJ * (cellWidth + gap)}px, 0, 0)`,
                }}
              >
                <b>↑</b>
                <span>j (Xanh)</span>
              </div>
            )}

            {pJNext >= 0 && (
              <div
                className="smooth-vis-pointer-marker smooth-vis-pointer-marker--j-next"
                style={{
                  transform: `translate3d(${pJNext * (cellWidth + gap)}px, 0, 0)`,
                }}
              >
                <b>↑</b>
                <span>j+1 (Tím)</span>
              </div>
            )}
          </div>
        </div>

        {/* ---------------- Explainer Banner ---------------- */}
        <div className="smooth-vis-explainer">
          <div className="smooth-vis-explainer__left">
            <span className="smooth-vis-explainer__icon">
              {currentStep.type === "SWAPPING" ? "🔄" : currentStep.type === "COMPARE" ? "⚡" : "💡"}
            </span>
            <p className="smooth-vis-explainer__text">
              <strong>{currentStep.comparison || "Thực thi"}:</strong>{" "}
              {currentStep.annotation || currentStep.context || "Đang xử lý bước chạy..."}
            </p>
          </div>
        </div>
      </div>

      {/* ---------------- Playback Controls Footer ---------------- */}
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
  );
}
