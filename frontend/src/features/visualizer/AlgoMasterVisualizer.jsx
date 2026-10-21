import { useState, useEffect, useRef, useMemo } from "react";
import {
  generateTwoSumHashMapTrace,
  generateTwoSumTwoPassTrace,
  generateTwoSumTrace,
  generateBinarySearchTrace,
} from "../../services/solutionTrace.js";
import "./AlgoMasterVisualizer.css";

const TWO_SUM_PRESETS = [
  { id: "p1", label: "Example 1: nums = [2, 7, 11, 15], target = 9", nums: [2, 7, 11, 15], target: 9 },
  { id: "p2", label: "Example 2: nums = [3, 2, 4], target = 6", nums: [3, 2, 4], target: 6 },
  { id: "p3", label: "Example 3: nums = [3, 3], target = 6", nums: [3, 3], target: 6 },
  { id: "p4", label: "Challenging: nums = [3, 2, 9, 4, 7], target = 9", nums: [3, 2, 9, 4, 7], target: 9 },
];

const BINARY_SEARCH_PRESETS = [
  { id: "bs1", label: "Found Target: nums = [-1, 0, 3, 5, 9, 12], target = 9", nums: [-1, 0, 3, 5, 9, 12], target: 9 },
  { id: "bs2", label: "Target Not in Array: nums = [-1, 0, 3, 5, 9, 12], target = 2", nums: [-1, 0, 3, 5, 9, 12], target: 2 },
  { id: "bs3", label: "Short Array: nums = [2, 5], target = 5", nums: [2, 5], target: 5 },
];

const APPROACH_CODES = {
  1: {
    "hash-map": {
      "Python 3": `class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        seen = {}
        for i, num in enumerate(nums):
            diff = target - num
            if diff in seen:
                return [seen[diff], i]
            seen[num] = i
        return []`,
      JavaScript: `function twoSum(nums, target) {
    const map = new Map();
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (map.has(complement)) {
            return [map.get(complement), i];
        }
        map.set(nums[i], i);
    }
    return [];
}`,
      Java: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }
        return new int[] {};
    }
}`,
      "C++": `class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        unordered_map<int, int> map;
        for (int i = 0; i < nums.size(); i++) {
            int complement = target - nums[i];
            if (map.find(complement) != map.end()) {
                return {map[complement], i};
            }
            map[nums[i]] = i;
        }
        return {};
    }
};`,
    },
    "two-pass": {
      "Python 3": `class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        hashmap = {}
        for i, num in enumerate(nums):
            hashmap[num] = i
        for i, num in enumerate(nums):
            complement = target - num
            if complement in hashmap and hashmap[complement] != i:
                return [i, hashmap[complement]]
        return []`,
      JavaScript: `function twoSum(nums, target) {
    const map = new Map();
    for (let i = 0; i < nums.length; i++) {
        map.set(nums[i], i);
    }
    for (let i = 0; i < nums.length; i++) {
        const complement = target - nums[i];
        if (map.has(complement) && map.get(complement) !== i) {
            return [i, map.get(complement)];
        }
    }
    return [];
}`,
      Java: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) map.put(nums[i], i);
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement) && map.get(complement) != i) {
                return new int[] { i, map.get(complement) };
            }
        }
        return new int[] {};
    }
}`,
      "C++": `class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        unordered_map<int, int> map;
        for (int i = 0; i < nums.size(); i++) map[nums[i]] = i;
        for (int i = 0; i < nums.size(); i++) {
            int complement = target - nums[i];
            if (map.find(complement) != map.end() && map[complement] != i) {
                return {i, map[complement]};
            }
        }
        return {};
    }
};`,
    },
    "brute-force": {
      "Python 3": `class Solution:
    def twoSum(self, nums: list[int], target: int) -> list[int]:
        n = len(nums)
        for i in range(n):
            for j in range(i + 1, n):
                if nums[i] + nums[j] == target:
                    return [i, j]
        return []`,
      JavaScript: `function twoSum(nums, target) {
    const n = nums.length;
    for (let i = 0; i < n; i++) {
        for (let j = i + 1; j < n; j++) {
            if (nums[i] + nums[j] === target) {
                return [i, j];
            }
        }
    }
    return [];
}`,
      Java: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        for (int i = 0; i < nums.length; i++) {
            for (int j = i + 1; j < nums.length; j++) {
                if (nums[i] + nums[j] == target) {
                    return new int[] { i, j };
                }
            }
        }
        return new int[] {};
    }
}`,
      "C++": `class Solution {
public:
    vector<int> twoSum(vector<int>& nums, int target) {
        for (int i = 0; i < nums.size(); i++) {
            for (int j = i + 1; j < nums.size(); j++) {
                if (nums[i] + nums[j] == target) {
                    return {i, j};
                }
            }
        }
        return {};
    }
};`,
    },
  },
  704: {
    "binary-search": {
      "Python 3": `class Solution:
    def search(self, nums: list[int], target: int) -> int:
        left, right = 0, len(nums) - 1
        while left <= right:
            mid = (left + right) // 2
            if nums[mid] == target:
                return mid
            elif nums[mid] < target:
                left = mid + 1
            else:
                right = mid - 1
        return -1`,
      JavaScript: `function search(nums, target) {
    let left = 0, right = nums.length - 1;
    while (left <= right) {
        const mid = Math.floor((left + right) / 2);
        if (nums[mid] === target) return mid;
        if (nums[mid] < target) left = mid + 1;
        else right = mid - 1;
    }
    return -1;
}`,
      Java: `class Solution {
    public int search(int[] nums, int target) {
        int left = 0, right = nums.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) return mid;
            if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }
}`,
      "C++": `class Solution {
public:
    int search(vector<int>& nums, int target) {
        int left = 0, right = nums.size() - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (nums[mid] == target) return mid;
            if (nums[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }
};`,
    },
  },
};

const SPEED_CONFIG = {
  "0.5x": { label: "0.5x", ms: 3000 },
  "1x": { label: "1x", ms: 1800 },
  "1.5x": { label: "1.5x", ms: 1100 },
  "2x": { label: "2x", ms: 600 },
};

function getSynchronizedLineNumber(step, approach, language) {
  if (!step) return null;
  const type = step.type;

  if (approach === "hash-map") {
    if (language === "Python 3") {
      if (type === "IDLE") return 3;
      if (type === "POINTER_MOVE") return 4;
      if (type === "COMPARE") return 5;
      if (type === "MAP_LOOKUP") return 6;
      if (type === "RESULT_PRODUCE") return 7;
      if (type === "MAP_PUT") return 8;
      return 4;
    }
    if (language === "JavaScript") {
      if (type === "IDLE") return 2;
      if (type === "POINTER_MOVE") return 3;
      if (type === "COMPARE") return 4;
      if (type === "MAP_LOOKUP") return 5;
      if (type === "RESULT_PRODUCE") return 6;
      if (type === "MAP_PUT") return 8;
      return 3;
    }
    if (language === "Java") {
      if (type === "IDLE") return 3;
      if (type === "POINTER_MOVE") return 4;
      if (type === "COMPARE") return 5;
      if (type === "MAP_LOOKUP") return 6;
      if (type === "RESULT_PRODUCE") return 7;
      if (type === "MAP_PUT") return 9;
      return 4;
    }
    if (language === "C++") {
      if (type === "IDLE") return 4;
      if (type === "POINTER_MOVE") return 5;
      if (type === "COMPARE") return 6;
      if (type === "MAP_LOOKUP") return 7;
      if (type === "RESULT_PRODUCE") return 8;
      if (type === "MAP_PUT") return 10;
      return 5;
    }
  }

  if (approach === "two-pass") {
    if (language === "Python 3") {
      if (type === "IDLE") return 3;
      if (type === "MAP_PUT") return 5;
      if (type === "COMPARE") return 7;
      if (type === "RESULT_PRODUCE") return 9;
      return 4;
    }
    if (language === "JavaScript") {
      if (type === "IDLE") return 2;
      if (type === "MAP_PUT") return 4;
      if (type === "COMPARE") return 7;
      if (type === "RESULT_PRODUCE") return 9;
      return 3;
    }
    if (language === "Java") {
      if (type === "IDLE") return 3;
      if (type === "MAP_PUT") return 4;
      if (type === "COMPARE") return 6;
      if (type === "RESULT_PRODUCE") return 8;
      return 4;
    }
    if (language === "C++") {
      if (type === "IDLE") return 4;
      if (type === "MAP_PUT") return 5;
      if (type === "COMPARE") return 7;
      if (type === "RESULT_PRODUCE") return 9;
      return 5;
    }
  }

  if (approach === "brute-force") {
    if (language === "Python 3") {
      if (type === "IDLE") return 3;
      if (type === "POINTER_MOVE") return 4;
      if (type === "COMPARE") return 6;
      if (type === "RESULT_PRODUCE") return 7;
      return 5;
    }
    if (language === "JavaScript") {
      if (type === "IDLE") return 2;
      if (type === "POINTER_MOVE") return 3;
      if (type === "COMPARE") return 5;
      if (type === "RESULT_PRODUCE") return 6;
      return 4;
    }
    if (language === "Java") {
      if (type === "POINTER_MOVE") return 3;
      if (type === "COMPARE") return 5;
      if (type === "RESULT_PRODUCE") return 6;
      return 4;
    }
    if (language === "C++") {
      if (type === "POINTER_MOVE") return 4;
      if (type === "COMPARE") return 6;
      if (type === "RESULT_PRODUCE") return 7;
      return 5;
    }
  }

  return step.activeLine || 4;
}

export default function AlgoMasterVisualizer({
  problem,
  language = "Python 3",
  onLanguageChange,
  onAnnounce,
}) {
  const problemId = problem?.id || 1;

  // Approaches
  const approachOptions = useMemo(() => {
    if (problemId === 1) {
      return [
        { id: "hash-map", label: "Hash Map (Optimal)", badge: "O(n)" },
        { id: "two-pass", label: "Two-Pass Hash Table", badge: "O(n)" },
        { id: "brute-force", label: "Brute Force", badge: "O(n²)" },
      ];
    }
    if (problemId === 704) {
      return [
        { id: "binary-search", label: "Binary Search", badge: "O(log n)" },
        { id: "brute-force", label: "Linear Scan", badge: "O(n)" },
      ];
    }
    return [{ id: "standard", label: "Optimal Approach", badge: "O(n)" }];
  }, [problemId]);

  const [activeApproach, setActiveApproach] = useState(() => approachOptions[0]?.id || "hash-map");

  // Presets
  const presets = useMemo(() => {
    if (problemId === 1) return TWO_SUM_PRESETS;
    if (problemId === 704) return BINARY_SEARCH_PRESETS;
    return [
      { id: "default", label: "Default Sample Input", nums: [2, 7, 11, 15], target: 9 },
    ];
  }, [problemId]);

  const [selectedPresetId, setSelectedPresetId] = useState(() => presets[0]?.id || "p1");
  const currentPreset = useMemo(() => {
    return presets.find((p) => p.id === selectedPresetId) || presets[0];
  }, [presets, selectedPresetId]);

  // Playback state
  const [stepIndex, setStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speedKey, setSpeedKey] = useState("1x");
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [copyFeedback, setCopyFeedback] = useState(false);

  const timerRef = useRef(null);
  const containerRef = useRef(null);

  // Trace Generation
  const trace = useMemo(() => {
    if (problemId === 1) {
      const { nums, target } = currentPreset;
      if (activeApproach === "hash-map") {
        return generateTwoSumHashMapTrace(nums, target);
      }
      if (activeApproach === "two-pass") {
        return generateTwoSumTwoPassTrace(nums, target);
      }
      if (activeApproach === "brute-force") {
        return generateTwoSumTrace(nums, target);
      }
    }
    if (problemId === 704) {
      const { nums, target } = currentPreset;
      return generateBinarySearchTrace(nums, target);
    }
    return generateTwoSumHashMapTrace([2, 7, 11, 15], 9);
  }, [problemId, activeApproach, currentPreset]);

  // Reset step index when approach or preset changes
  useEffect(() => {
    setStepIndex(0);
    setIsPlaying(false);
  }, [activeApproach, selectedPresetId]);

  // Autoplay ticker
  useEffect(() => {
    if (!isPlaying) {
      if (timerRef.current) clearTimeout(timerRef.current);
      return;
    }
    const speed = SPEED_CONFIG[speedKey] || SPEED_CONFIG["1x"];
    timerRef.current = setTimeout(() => {
      setStepIndex((curr) => {
        if (curr >= trace.length - 1) {
          setIsPlaying(false);
          return curr;
        }
        return curr + 1;
      });
    }, speed.ms);

    return () => {
      if (timerRef.current) clearTimeout(timerRef.current);
    };
  }, [isPlaying, stepIndex, trace.length, speedKey]);

  // Current Step
  const currentStep = trace[stepIndex] || trace[0] || {};
  const isComplete = stepIndex >= trace.length - 1;

  // Active line calculation
  const activeLine = useMemo(() => {
    return getSynchronizedLineNumber(currentStep, activeApproach, language);
  }, [currentStep, activeApproach, language]);

  // Announce step
  useEffect(() => {
    if (onAnnounce && currentStep) {
      onAnnounce(`AlgoMaster step ${stepIndex + 1} of ${trace.length}: ${currentStep.comparison || ""}`);
    }
  }, [stepIndex, currentStep, trace.length, onAnnounce]);

  // Handlers
  const handleTogglePlay = () => {
    if (!isPlaying && isComplete) {
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

  // Fullscreen toggle
  const handleToggleFullscreen = () => {
    setIsFullscreen((prev) => !prev);
  };

  // Escape to exit fullscreen
  useEffect(() => {
    const onKey = (e) => {
      if (e.key === "Escape" && isFullscreen) setIsFullscreen(false);
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [isFullscreen]);

  // Active code snippet
  const activeCodeSnippet = useMemo(() => {
    const codes = APPROACH_CODES[problemId]?.[activeApproach] || APPROACH_CODES[1]["hash-map"];
    return codes[language] || codes["Python 3"] || "";
  }, [problemId, activeApproach, language]);

  const codeLines = useMemo(() => {
    return activeCodeSnippet.split("\n");
  }, [activeCodeSnippet]);

  const handleCopyCode = async () => {
    try {
      await navigator.clipboard.writeText(activeCodeSnippet);
      setCopyFeedback(true);
      setTimeout(() => setCopyFeedback(false), 2000);
    } catch {
      // fallback
    }
  };

  // Array items & geometry
  const items = currentStep.items || currentPreset.nums.map((v, i) => ({ id: i, value: v, pos: i }));
  const pointers = currentStep.pointers || {};
  const matchedIndices = currentStep.matchedIndices || [];
  const activeIndices = currentStep.activeIndices || [];
  const hashMapData = currentStep.hashMapData || [];

  return (
    <div
      ref={containerRef}
      className={`algomaster-root ${isFullscreen ? "is-fullscreen" : ""}`}
      tabIndex={0}
      aria-label="AlgoMaster Visualizer"
    >
      {/* 1. TOP HEADER & APPROACH TABS (AlgoMaster style) */}
      <div className="algomaster-header">
        <div className="algomaster-header__title-row">
          <div className="algomaster-badge">
            <span className="algomaster-badge__icon">⚡</span>
            <span className="algomaster-badge__text">Visualization and Code</span>
          </div>

          <div className="algomaster-header__actions">
            <button
              type="button"
              className="algomaster-action-btn"
              onClick={handleToggleFullscreen}
              title={isFullscreen ? "Exit Fullscreen (Esc)" : "Fullscreen"}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                {isFullscreen ? (
                  <>
                    <polyline points="4 14 10 14 10 20" />
                    <polyline points="20 10 14 10 14 4" />
                    <line x1="14" y1="10" x2="21" y2="3" />
                    <line x1="3" y1="21" x2="10" y2="14" />
                  </>
                ) : (
                  <>
                    <polyline points="15 3 21 3 21 9" />
                    <polyline points="9 21 3 21 3 15" />
                    <line x1="21" y1="3" x2="14" y2="10" />
                    <line x1="3" y1="21" x2="10" y2="14" />
                  </>
                )}
              </svg>
            </button>
          </div>
        </div>

        {/* Approach Selector Pills */}
        <div className="algomaster-approaches-row">
          <div className="algomaster-approach-pills">
            {approachOptions.map((app) => (
              <button
                type="button"
                key={app.id}
                className={`algomaster-approach-pill ${activeApproach === app.id ? "is-active" : ""}`}
                onClick={() => setActiveApproach(app.id)}
              >
                <span>{app.label}</span>
                <span className="approach-complexity">{app.badge}</span>
              </button>
            ))}
          </div>

          {/* Test Input Dropdown Selector */}
          <div className="algomaster-input-selector">
            <span className="input-label">Input:</span>
            <select
              className="input-select"
              value={selectedPresetId}
              onChange={(e) => setSelectedPresetId(e.target.value)}
              aria-label="Select test case input"
            >
              {presets.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.label}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* 2. VISUAL CANVAS */}
      <div className="algomaster-canvas">
        {/* Array Visualization */}
        <div className="algomaster-array-stage">
          <div className="algomaster-array-track">
            <span className="array-label">nums:</span>
            <div className="array-cells-row">
              {items.map((item, idx) => {
                const pos = item.pos !== undefined ? item.pos : idx;
                const isMatched = matchedIndices.includes(pos);
                const isActive = activeIndices.includes(pos) || pointers.i === pos || pointers.left === pos;
                const isSecondary = pointers.j === pos || pointers.right === pos;

                let cellClass = "cell-default";
                if (isMatched) cellClass = "cell-matched";
                else if (isActive) cellClass = "cell-active";
                else if (isSecondary) cellClass = "cell-secondary";

                return (
                  <div key={item.id ?? idx} className="array-cell-col">
                    <span className="cell-index">{pos}</span>
                    <div className={`algomaster-cell ${cellClass}`}>
                      <span className="cell-value">{item.value}</span>
                    </div>
                    {/* Active Pointer Arrow */}
                    <div className="cell-pointer-slot">
                      {pointers.i === pos && <span className="pointer-tag pointer-i">↑ i</span>}
                      {pointers.j === pos && <span className="pointer-tag pointer-j">↑ j</span>}
                      {pointers.left === pos && <span className="pointer-tag pointer-i">↑ left</span>}
                      {pointers.right === pos && <span className="pointer-tag pointer-j">↑ right</span>}
                      {pointers.mid === pos && <span className="pointer-tag pointer-mid">↑ mid</span>}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Optional Complement & Hash Map Stage (AlgoMaster) */}
          {activeApproach !== "brute-force" && problemId === 1 && (
            <div className="algomaster-hashmap-card">
              <div className="hashmap-header">
                <span className="hashmap-title">Hash Map (seen)</span>
                <span className="hashmap-count">{hashMapData.length} entries</span>
              </div>
              <div className="hashmap-entries">
                {hashMapData.length === 0 ? (
                  <span className="hashmap-empty">(Empty hash map — starting traversal)</span>
                ) : (
                  hashMapData.map((entry, eIdx) => (
                    <div
                      key={eIdx}
                      className={`hashmap-pill ${
                        entry.state === "active"
                          ? "is-active"
                          : entry.state === "added"
                          ? "is-added"
                          : entry.state === "matched"
                          ? "is-matched"
                          : ""
                      }`}
                    >
                      <span className="pill-key">{entry.key}</span>
                      <span className="pill-arrow">→</span>
                      <span className="pill-val">idx {entry.value}</span>
                    </div>
                  ))
                )}
              </div>
            </div>
          )}
        </div>

        {/* 3. STEP EXPLANATION BANNER (Emerald Green Box like AlgoMaster) */}
        <div className="algomaster-step-banner">
          <div className="step-banner-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <div className="step-banner-content">
            <span className="step-banner-label">
              Step {stepIndex + 1}/{trace.length}:
            </span>{" "}
            <span className="step-banner-text">
              {currentStep.annotation || currentStep.comparison || "Executing algorithm step..."}
            </span>
          </div>
        </div>

        {/* 4. PLAYBACK CONTROLS & TIMELINE SCRUBBER */}
        <div className="algomaster-controls">
          <div className="controls-left">
            <button
              type="button"
              className="ctrl-btn"
              onClick={handleReset}
              title="Reset execution"
              aria-label="Reset execution"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="1 4 1 10 7 10" />
                <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10" />
              </svg>
            </button>
            <button
              type="button"
              className="ctrl-btn"
              onClick={handlePrev}
              disabled={stepIndex <= 0}
              title="Step backward"
              aria-label="Step backward"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="19 20 9 12 19 4 19 20" />
                <line x1="5" y1="19" x2="5" y2="5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </button>
            <button
              type="button"
              className="ctrl-btn ctrl-btn--primary"
              onClick={handleTogglePlay}
              title={isPlaying ? "Pause" : "Play"}
              aria-label={isPlaying ? "Pause" : "Play"}
            >
              {isPlaying ? (
                <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="4" width="4" height="16" rx="1" />
                  <rect x="14" y="4" width="4" height="16" rx="1" />
                </svg>
              ) : (
                <svg width="13" height="13" viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="6 4 20 12 6 20 6 4" />
                </svg>
              )}
            </button>
            <button
              type="button"
              className="ctrl-btn"
              onClick={handleNext}
              disabled={isComplete}
              title="Step forward"
              aria-label="Step forward"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="currentColor">
                <polygon points="5 4 15 12 5 20 5 4" />
                <line x1="19" y1="19" x2="19" y2="5" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
              </svg>
            </button>
          </div>

          {/* Timeline Scrubber */}
          <div className="controls-scrubber">
            <input
              type="range"
              min="0"
              max={Math.max(0, trace.length - 1)}
              value={stepIndex}
              onChange={handleSeek}
              aria-label="Execution timeline scrubber"
            />
            <span className="scrubber-counter">
              <strong>{stepIndex + 1}</strong> / {trace.length}
            </span>
          </div>

          {/* Speed Pills */}
          <div className="controls-speed">
            <span className="speed-label">Speed:</span>
            {Object.keys(SPEED_CONFIG).map((sp) => (
              <button
                type="button"
                key={sp}
                className={`speed-btn ${speedKey === sp ? "is-active" : ""}`}
                onClick={() => setSpeedKey(sp)}
              >
                {sp}
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* 5. SYNCHRONIZED CODE VIEWER (AlgoMaster Lower Area) */}
      <div className="algomaster-code-section">
        <div className="code-subbar">
          <div className="code-subbar__left">
            <span className="code-lang-tag">
              <span className="code-glyph">&lt;/&gt;</span>
              <strong>{language}</strong>
              <span className="code-sync-badge">
                <span className="sync-pulse" />
                Line {activeLine}
              </span>
            </span>
          </div>

          <div className="code-subbar__right">
            {/* Quick Language Switcher */}
            <div className="code-lang-pills">
              {["Python 3", "JavaScript", "Java", "C++"].map((lang) => (
                <button
                  type="button"
                  key={lang}
                  className={`lang-pill ${language === lang ? "is-active" : ""}`}
                  onClick={() => onLanguageChange && onLanguageChange(lang)}
                >
                  {lang}
                </button>
              ))}
            </div>

            <button
              type="button"
              className="code-copy-btn"
              onClick={handleCopyCode}
              title="Copy synchronized code snippet"
            >
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
              </svg>
              <span>{copyFeedback ? "Copied!" : "Copy"}</span>
            </button>
          </div>
        </div>

        {/* Code Canvas with Active Line Highlight */}
        <div className="code-viewport">
          <div className="code-gutter" aria-hidden="true">
            {codeLines.map((_, i) => (
              <span
                key={i}
                className={`line-num ${i + 1 === activeLine ? "is-active-line" : ""}`}
              >
                {i + 1}
              </span>
            ))}
          </div>
          <div className="code-lines">
            {codeLines.map((line, i) => {
              const lineNum = i + 1;
              const isActive = lineNum === activeLine;
              return (
                <div
                  key={i}
                  className={`code-line ${isActive ? "is-active-line" : ""}`}
                >
                  {isActive && <span className="active-arrow-indicator">▶</span>}
                  <code>{line || " "}</code>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
