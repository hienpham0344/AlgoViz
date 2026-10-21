import { useState } from "react";
import "./VisualExampleCard.css";

/**
 * Intelligent helper to parse testcase input and output into structured MasterAlgo data
 */
function parseMasterAlgoData(tc, problemId, index) {
  const inputStr = tc.input || "";
  const expectedStr = tc.expected || tc.expectedOutput || "";
  const lines = inputStr.trim().split("\n");

  let parsed = {
    inputLabel: "nums",
    inputString: "",
    inputArray: [],
    target: null,
    targetName: "target",
    outputString: expectedStr,
    outputArray: [],
    explanation: null,
  };

  // 1. Two Sum specific parsing (Problem ID 1)
  if (problemId === 1) {
    const targetVal = lines[0] ? lines[0].trim() : "9";
    const numsLine = lines[1] || "2 7 11 15";
    const nums = numsLine.split(/\s+/).map((n) => Number(n.trim())).filter((n) => !isNaN(n));
    
    parsed.inputLabel = "nums";
    parsed.inputArray = nums;
    parsed.target = targetVal;
    parsed.inputString = `nums = [${nums.join(", ")}], target = ${targetVal}`;

    // Parse output array, e.g. [0, 1]
    let outArr = [];
    try {
      const j = JSON.parse(expectedStr);
      if (Array.isArray(j)) outArr = j;
    } catch {
      const m = expectedStr.match(/\d+/g);
      if (m) outArr = m.map(Number);
    }
    parsed.outputArray = outArr;
    parsed.outputString = `[${outArr.join(", ")}]`;

    if (outArr.length >= 2) {
      const idx1 = outArr[0];
      const idx2 = outArr[1];
      const v1 = nums[idx1];
      const v2 = nums[idx2];
      parsed.explanation = `Because nums[${idx1}] + nums[${idx2}] == ${v1} + ${v2} == ${targetVal}, we return [${idx1}, ${idx2}].`;
    }
    return parsed;
  }

  // 2. Binary Search specific parsing (Problem ID 704)
  if (problemId === 704) {
    const targetVal = lines[0] ? lines[0].trim() : "9";
    const numsLine = lines[1] || "-1 0 3 5 9 12";
    const nums = numsLine.split(/\s+/).map((n) => Number(n.trim())).filter((n) => !isNaN(n));
    const targetNum = Number(targetVal);
    const targetIdx = nums.indexOf(targetNum);

    parsed.inputLabel = "nums";
    parsed.inputArray = nums;
    parsed.target = targetVal;
    parsed.inputString = `nums = [${nums.join(", ")}], target = ${targetVal}`;
    parsed.outputString = expectedStr;
    parsed.outputArray = targetIdx >= 0 ? [targetIdx] : [-1];

    if (targetIdx >= 0) {
      parsed.explanation = `${targetVal} exists in nums and its index is ${targetIdx}.`;
    } else {
      parsed.explanation = `${targetVal} does not exist in nums so return -1.`;
    }
    return parsed;
  }

  // 3. Generic detection: search for array format or space-separated integers
  let detectedArr = [];
  for (const line of lines) {
    const trimmed = line.trim();
    if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      try {
        const arr = JSON.parse(trimmed);
        if (Array.isArray(arr)) {
          detectedArr = arr;
          break;
        }
      } catch {}
    } else if (trimmed.split(/\s+/).length > 1) {
      const parts = trimmed.split(/\s+/).map(Number);
      if (parts.every((p) => !isNaN(p))) {
        detectedArr = parts;
        break;
      }
    }
  }

  if (detectedArr.length > 0) {
    parsed.inputArray = detectedArr;
    parsed.inputString = `nums = [${detectedArr.join(", ")}]`;
  } else {
    parsed.inputString = inputStr;
  }

  // Output detection
  try {
    const outParsed = JSON.parse(expectedStr);
    if (Array.isArray(outParsed)) {
      parsed.outputArray = outParsed;
      parsed.outputString = `[${outParsed.join(", ")}]`;
    }
  } catch {
    parsed.outputString = expectedStr;
    const num = Number(expectedStr.trim());
    if (!isNaN(num)) {
      parsed.outputArray = [num];
    }
  }

  return parsed;
}

export function VisualExampleCard({ tc, index, problemId }) {
  const [isInputCollapsed, setIsInputCollapsed] = useState(false);
  const [isOutputCollapsed, setIsOutputCollapsed] = useState(false);
  const [copiedInput, setCopiedInput] = useState(false);
  const [copiedOutput, setCopiedOutput] = useState(false);

  const data = parseMasterAlgoData(tc, problemId, index);

  const handleCopyInput = () => {
    navigator.clipboard.writeText(data.inputString);
    setCopiedInput(true);
    setTimeout(() => setCopiedInput(false), 1500);
  };

  const handleCopyOutput = () => {
    navigator.clipboard.writeText(data.outputString);
    setCopiedOutput(true);
    setTimeout(() => setCopiedOutput(false), 1500);
  };

  return (
    <div className="ma-example-container">
      <div className="ma-example-title">Example {index + 1}:</div>

      {/* 1. INPUT PANEL (MasterAlgo Style) */}
      <div className="ma-panel ma-panel--input">
        <div className="ma-panel-header">
          <div className="ma-panel-header-left">
            <span className="ma-badge ma-badge--input">Input</span>
            <span className="ma-header-code">{data.inputString}</span>
          </div>

          <div className="ma-panel-actions">
            <button
              type="button"
              className="ma-action-btn"
              onClick={handleCopyInput}
              title={copiedInput ? "Đã sao chép!" : "Sao chép Input"}
              aria-label="Copy input"
            >
              {copiedInput ? (
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              ) : (
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
              )}
            </button>

            <button
              type="button"
              className="ma-action-btn"
              onClick={() => setIsInputCollapsed(!isInputCollapsed)}
              title={isInputCollapsed ? "Mở rộng mảng" : "Thu gọn mảng"}
              aria-label="Toggle input blocks"
            >
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                style={{
                  transform: isInputCollapsed ? "rotate(180deg)" : "rotate(0deg)",
                  transition: "transform 180ms ease",
                }}
              >
                <polyline points="18 15 12 9 6 15" />
              </svg>
            </button>
          </div>
        </div>

        {/* Visual Blocks Body */}
        {!isInputCollapsed && (
          <div className="ma-panel-body">
            {data.inputArray.length > 0 ? (
              <div className="ma-blocks-track">
                {data.inputArray.map((val, idx) => (
                  <div className="ma-block-column" key={idx}>
                    <div className="ma-block-idx">{idx}</div>
                    <div className="ma-block-box ma-block-box--input">{val}</div>
                  </div>
                ))}

                {data.target !== null && (
                  <div className="ma-target-indicator">
                    <span className="ma-target-tag">Target: {data.target}</span>
                  </div>
                )}
              </div>
            ) : (
              <div className="ma-empty-fallback">{data.inputString}</div>
            )}
          </div>
        )}
      </div>

      {/* 2. OUTPUT PANEL (MasterAlgo Style) */}
      <div className="ma-panel ma-panel--output">
        <div className="ma-panel-header">
          <div className="ma-panel-header-left">
            <span className="ma-badge ma-badge--output">Output</span>
            <span className="ma-header-code">{data.outputString}</span>
          </div>

          <div className="ma-panel-actions">
            <button
              type="button"
              className="ma-action-btn"
              onClick={handleCopyOutput}
              title={copiedOutput ? "Đã sao chép!" : "Sao chép Output"}
              aria-label="Copy output"
            >
              {copiedOutput ? (
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#10b981" strokeWidth="2.5">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              ) : (
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
              )}
            </button>

            <button
              type="button"
              className="ma-action-btn"
              onClick={() => setIsOutputCollapsed(!isOutputCollapsed)}
              title={isOutputCollapsed ? "Mở rộng mảng" : "Thu gọn mảng"}
              aria-label="Toggle output blocks"
            >
              <svg
                width="13"
                height="13"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                style={{
                  transform: isOutputCollapsed ? "rotate(180deg)" : "rotate(0deg)",
                  transition: "transform 180ms ease",
                }}
              >
                <polyline points="18 15 12 9 6 15" />
              </svg>
            </button>
          </div>
        </div>

        {/* Visual Blocks Body */}
        {!isOutputCollapsed && (
          <div className="ma-panel-body">
            {data.outputArray.length > 0 ? (
              <div className="ma-blocks-track">
                {data.outputArray.map((val, idx) => (
                  <div className="ma-block-column" key={idx}>
                    <div className="ma-block-idx">{idx}</div>
                    <div className="ma-block-box ma-block-box--output">{val}</div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="ma-empty-fallback">{data.outputString}</div>
            )}
          </div>
        )}
      </div>

      {/* 3. EXPLANATION (Optional Callout) */}
      {data.explanation && (
        <div className="ma-explanation-row">
          <span className="ma-explain-label">Explanation:</span>
          <span className="ma-explain-text">{data.explanation}</span>
        </div>
      )}
    </div>
  );
}

export default VisualExampleCard;
