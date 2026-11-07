import { useState, useMemo, useEffect } from "react";
import { createPortal } from "react-dom";
import { ProgressView } from "../../components/index.js";
import "./learn.css";
import { COURSES, CATEGORIES } from "./coursesData.js";
import { withLearningProgress } from "../../services/learning.js";
import { useLearningProgress } from "../../services/useLearningProgress.js";
import ApiState from "../../components/ApiState.jsx";

const GUIDE_CATEGORIES = [
  "All",
  "Patterns",
  "Cheatsheet",
  "Tutorial",
  "Interview Playbook",
];

const LEARN_GUIDES = [
  {
    id: "two-pointers-sliding-window",
    title: "Two Pointers & Sliding Window Blueprint",
    subtitle: "The canonical pattern for string & subarray optimization from O(n²) to O(n).",
    category: "Patterns",
    badge: "Must-Know Pattern",
    badgeColor: "#0284c7",
    badgeBg: "#e0f2fe",
    readTime: "8 min read",
    difficulty: "Essential",
    overview:
      "Sliding window and two-pointer techniques maintain a dynamic or static window over linear sequences. Instead of nested loops that rescan elements, pointers move strictly monotonically forward, achieving guaranteed linear time.",
    keyTakeaway:
      "Window Invariant: Expand the right pointer to explore candidates; shrink the left pointer once constraints are violated or when optimizing minimum window length.",
    ruleOfThumb:
      "Use whenever a problem mentions contiguous subarrays, substrings, pairwise target sums, or sorted sequences.",
    codeSnippets: {
      python: `def sliding_window(nums, target):
    left = 0
    curr_sum = 0
    max_len = 0
    for right in range(len(nums)):
        curr_sum += nums[right]
        # Invariant: shrink while window violates condition
        while curr_sum > target and left <= right:
            curr_sum -= nums[left]
            left += 1
        max_len = max(max_len, right - left + 1)
    return max_len`,
      java: `public int slidingWindow(int[] nums, int target) {
    int left = 0, currSum = 0, maxLen = 0;
    for (int right = 0; right < nums.length; right++) {
        currSum += nums[right];
        while (currSum > target && left <= right) {
            currSum -= nums[left++];
        }
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}`,
      javascript: `function slidingWindow(nums, target) {
    let left = 0, currSum = 0, maxLen = 0;
    for (let right = 0; right < nums.length; right++) {
        currSum += nums[right];
        while (currSum > target && left <= right) {
            currSum -= nums[left++];
        }
        maxLen = Math.max(maxLen, right - left + 1);
    }
    return maxLen;
}`
    },
    practiceProblems: [
      { id: 1, title: "Two Sum", difficulty: "Easy" },
      { id: 3, title: "Longest Substring", difficulty: "Medium" },
      { id: 15, title: "3Sum", difficulty: "Medium" }
    ]
  },
  {
    id: "constraint-to-complexity",
    title: "Constraint-to-Algorithm Cheat Matrix",
    subtitle: "Deduce expected Big-O complexity and algorithm class directly from input size N.",
    category: "Cheatsheet",
    badge: "Interview Gold",
    badgeColor: "#d97706",
    badgeBg: "#fef3c7",
    readTime: "5 min read",
    difficulty: "All Levels",
    overview:
      "Competitive programming and technical interviews enforce ~1.0 second per test case (~10⁸ basic CPU operations). Looking at constraints tells you the maximum allowable Big-O before you write a single line of code.",
    keyTakeaway:
      "Always inspect N first: N ≤ 20 points to backtracking; N ≤ 10⁵ strictly forbids O(n²); N ≥ 10⁷ mandates O(log n) or O(1).",
    ruleOfThumb:
      "Target total operations under 5 · 10⁷ to safely avoid Time Limit Exceeded (TLE).",
    matrixRows: [
      { constraint: "N ≤ 10 - 12", bigO: "O(N!) or O(N² · 2ᴺ)", algorithms: "Permutations, Traveling Salesperson, Bitmask DP" },
      { constraint: "N ≤ 20 - 25", bigO: "O(2ᴺ)", algorithms: "Subsets, Meet-in-the-middle, Exhaustive Backtracking" },
      { constraint: "N ≤ 500 - 1,000", bigO: "O(N³) or O(N²)", algorithms: "Floyd-Warshall, 2D Grid DP, Matrix Traversal" },
      { constraint: "N ≤ 10⁵ - 2·10⁵", bigO: "O(N log N) or O(N)", algorithms: "Sorting, Binary Search, Heaps, Two Pointers, Monotonic Stack, BFS/DFS" },
      { constraint: "N ≤ 10⁸ - 10⁹", bigO: "O(√N) or O(log N)", algorithms: "Prime Factorization, Sieve, Binary Exponentiation, Math" }
    ],
    practiceProblems: [
      { id: 704, title: "Binary Search", difficulty: "Easy" },
      { id: 4, title: "Median of Two Sorted Arrays", difficulty: "Hard" }
    ]
  },
  {
    id: "dp-4-step-framework",
    title: "Dynamic Programming 4-Step Decision Framework",
    subtitle: "A foolproof, structured system to decompose overlapping subproblems into iterative state transitions.",
    category: "Tutorial",
    badge: "Core Mastery",
    badgeColor: "#7c3aed",
    badgeBg: "#f5f3ff",
    readTime: "12 min read",
    difficulty: "Advanced",
    overview:
      "Dynamic Programming is not memorization; it is recognizing DAG (directed acyclic graph) dependencies between subproblems. Every DP challenge yields to this 4-step workflow.",
    keyTakeaway:
      "Step 1: State Representation → Step 2: Base Cases → Step 3: Recurrence Transition → Step 4: Iterative Order & Space Optimization.",
    ruleOfThumb:
      "If brute-force recursion tree exhibits identical sub-branches, memoize it or invert to bottom-up tabulation.",
    steps: [
      { step: "1. State Definition", desc: "Define precisely what dp[i] (or dp[i][j]) represents in plain English (e.g. 'Min coins to make amount i')." },
      { step: "2. Base Cases", desc: "Identify minimal trivial subproblems where the answer is known without calculation (e.g. dp[0] = 0)." },
      { step: "3. Recurrence Relation", desc: "Express dp[i] as a mathematical function of previously computed states: dp[i] = min(dp[i - c] + 1)." },
      { step: "4. Memory Optimization", desc: "If dp[i] only depends on dp[i - 1], compress full arrays into 1 or 2 rolling variables, reducing O(n) space to O(1)." }
    ],
    codeSnippets: {
      python: `# 1D Tabulation with Space Compression
def rob(nums: list[int]) -> int:
    prev1 = 0 # dp[i - 1]
    prev2 = 0 # dp[i - 2]
    for num in nums:
        current = max(prev1, prev2 + num)
        prev2 = prev1
        prev1 = current
    return prev1`,
      java: `public int rob(int[] nums) {
    int prev1 = 0, prev2 = 0;
    for (int num : nums) {
        int current = Math.max(prev1, prev2 + num);
        prev2 = prev1;
        prev1 = current;
    }
    return prev1;
}`,
      javascript: `function rob(nums) {
    let prev1 = 0, prev2 = 0;
    for (const num of nums) {
        const current = Math.max(prev1, prev2 + num);
        prev2 = prev1;
        prev1 = current;
    }
    return prev1;
}`
    },
    practiceProblems: [
      { id: 53, title: "Maximum Subarray", difficulty: "Medium" }
    ]
  },
  {
    id: "monotonic-stack-queue",
    title: "Monotonic Stack & Deque Playbook",
    subtitle: "Eliminate inner search loops for 'Next Greater Element' and range extrema queries in O(1) amortized.",
    category: "Patterns",
    badge: "High-Frequency",
    badgeColor: "#059669",
    badgeBg: "#ecfdf5",
    readTime: "9 min read",
    difficulty: "Intermediate",
    overview:
      "A monotonic stack enforces an invariant where elements strictly increase or decrease. When a new element violates the order, the stack pops until order is restored, processing all elements that this newcomer dominates.",
    keyTakeaway:
      "Every element is pushed once and popped at most once, guaranteeing strict O(N) total runtime despite nested while-loops.",
    ruleOfThumb:
      "Use whenever a problem asks for the nearest larger/smaller neighbor or boundaries of a histogram bar.",
    codeSnippets: {
      python: `def next_greater_elements(nums):
    n = len(nums)
    res = [-1] * n
    stack = [] # stores indices
    for i in range(n):
        while stack and nums[i] > nums[stack[-1]]:
            idx = stack.pop()
            res[idx] = nums[i]
        stack.append(i)
    return res`,
      java: `public int nextGreaterElements(int[] nums) {
    int n = nums.length;
    int[] res = new int[n];
    Arrays.fill(res, -1);
    Deque<Integer> stack = new ArrayDeque<>();
    for (int i = 0; i < n; i++) {
        while (!stack.isEmpty() && nums[i] > nums[stack.peek()]) {
            res[stack.pop()] = nums[i];
        }
        stack.push(i);
    }
    return res;
}`,
      javascript: `function nextGreaterElements(nums) {
    const n = nums.length;
    const res = new Array(n).fill(-1);
    const stack = [];
    for (let i = 0; i < n; i++) {
        while (stack.length && nums[i] > nums[stack[stack.length - 1]]) {
            const idx = stack.pop();
            res[idx] = nums[i];
        }
        stack.push(i);
    }
    return res;
}`
    },
    practiceProblems: [
      { id: 42, title: "Trapping Rain Water", difficulty: "Hard" },
      { id: 20, title: "Valid Parentheses", difficulty: "Easy" }
    ]
  },
  {
    id: "graph-traversal-matrix",
    title: "Graph Traversal & Shortest Path Decision Matrix",
    subtitle: "Choosing between BFS, DFS, Dijkstra, Bellman-Ford, and Union-Find without second-guessing.",
    category: "Cheatsheet",
    badge: "Architecture",
    badgeColor: "#ea580c",
    badgeBg: "#fff7ed",
    readTime: "7 min read",
    difficulty: "Intermediate",
    overview:
      "Graph problems intimidate candidates because they mix traversal orders, cycle detection, edge weights, and state representations. This decision tree cuts through the ambiguity.",
    keyTakeaway:
      "Unweighted Shortest Path = BFS (Queue). Weighted (no negative edges) = Dijkstra (Min-Heap). Negative edge weights = Bellman-Ford / SPFA. Disjoint connectivity / cycle detection in undirected graphs = Union-Find.",
    ruleOfThumb:
      "Always model state as nodes and transitions as edges; check if weights are uniform before reaching for Dijkstra.",
    matrixRows: [
      { constraint: "Unweighted Graph / Grid", bigO: "O(V + E)", algorithms: "Queue BFS (Level-order guaranteed shortest steps)" },
      { constraint: "Non-negative edge weights", bigO: "O(E log V)", algorithms: "Dijkstra with PriorityQueue (greedy relaxation)" },
      { constraint: "Negative edge weights", bigO: "O(V · E)", algorithms: "Bellman-Ford (detects negative weight cycles)" },
      { constraint: "Connected components / Cycle", bigO: "O(E · α(V))", algorithms: "Disjoint Set Union (Union-Find with path compression)" },
      { constraint: "Topological ordering (DAG)", bigO: "O(V + E)", algorithms: "Kahn's Algorithm (in-degree array) or Post-order DFS" }
    ],
    practiceProblems: [
      { id: 207, title: "Course Schedule", difficulty: "Medium" },
      { id: 200, title: "Number of Islands", difficulty: "Medium" }
    ]
  },
  {
    id: "top-10-interview-edge-cases",
    title: "The 10 Edge Cases That Break LeetCode Solutions",
    subtitle: "A pre-submission verification checklist to stop losing points on hidden test cases.",
    category: "Interview Playbook",
    badge: "Checklist",
    badgeColor: "#be185d",
    badgeBg: "#fdf2f8",
    readTime: "6 min read",
    difficulty: "Essential",
    overview:
      "Over 75% of Wrong Answer and Runtime Error penalties stem from predictable boundary omissions. Run your code through this mental checklist before clicking Submit.",
    keyTakeaway:
      "Empty collection, 1-element input, all identical elements, alternating negatives, 32-bit signed overflow (-2³¹ to 2³¹ - 1), disconnected graphs.",
    ruleOfThumb:
      "Always trace: N=0, N=1, N=2, all identical, and extremum limits.",
    steps: [
      { step: "1. Empty or Length-1 Inputs", desc: "Does your loop assume at least 2 elements? (e.g., nums[0] == nums[1] when len=1 throws IndexError)." },
      { step: "2. All Elements Equal", desc: "e.g. [5, 5, 5, 5] with target=10. Does your two-pointer or binary search loop infinitely on equals?" },
      { step: "3. Negative Numbers & Zeros", desc: "Can target or numbers be negative? (Dividing or taking modulo with negative values behaves differently in Python vs Java/C++)." },
      { step: "4. 32-bit Integer Overflow", desc: "In Java/C++, (left + right) / 2 can overflow if both are 2·10⁹. Always use left + (right - left) / 2." },
      { step: "5. Disconnected Components", desc: "In graph traversal, is the graph guaranteed to be connected? Did you loop over all vertices 0..V-1 to launch BFS/DFS?" }
    ],
    practiceProblems: [
      { id: 1, title: "Two Sum", difficulty: "Easy" },
      { id: 704, title: "Binary Search", difficulty: "Easy" }
    ]
  }
];

function CourseSquircleIcon({ name, bg, color }) {
  const renderSvg = () => {
    switch (name) {
      case "sort-bars":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="6" y1="20" x2="6" y2="14" />
            <line x1="12" y1="20" x2="12" y2="4" />
            <line x1="18" y1="20" x2="18" y2="9" />
          </svg>
        );
      case "code":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <polyline points="16 18 22 12 16 6" />
            <polyline points="8 6 2 12 8 18" />
          </svg>
        );
      case "search-divide":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="7" />
            <line x1="21" y1="21" x2="16.5" y2="16.5" />
            <line x1="8" y1="11" x2="14" y2="11" />
          </svg>
        );
      case "tree":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="5" r="2.5" />
            <circle cx="6" cy="19" r="2.5" />
            <circle cx="18" cy="19" r="2.5" />
            <line x1="12" y1="7.5" x2="12" y2="11" />
            <line x1="12" y1="11" x2="6" y2="16.5" />
            <line x1="12" y1="11" x2="18" y2="16.5" />
          </svg>
        );
      case "window":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="4" width="18" height="16" rx="3" />
            <line x1="8" y1="4" x2="8" y2="20" />
            <line x1="16" y1="4" x2="16" y2="20" />
          </svg>
        );
      case "graph":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="18" cy="5" r="2.5" />
            <circle cx="6" cy="12" r="2.5" />
            <circle cx="18" cy="19" r="2.5" />
            <line x1="8.5" y1="13.5" x2="15.5" y2="17.5" />
            <line x1="15.5" y1="6.5" x2="8.5" y2="10.5" />
          </svg>
        );
      case "dp-grid":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="3" />
            <line x1="3" y1="9" x2="21" y2="9" />
            <line x1="3" y1="15" x2="21" y2="15" />
            <line x1="9" y1="3" x2="9" y2="21" />
            <line x1="15" y1="3" x2="15" y2="21" />
          </svg>
        );
      case "compass":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="12" cy="12" r="9" />
            <polygon points="16 8 13.5 13.5 8 16 10.5 10.5 16 8" />
          </svg>
        );
      case "branch":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="6" y1="3" x2="6" y2="15" />
            <circle cx="18" cy="6" r="2.5" />
            <circle cx="6" cy="18" r="2.5" />
            <path d="M18 8.5a8 8 0 0 1-8 8H8.5" />
          </svg>
        );
      case "binary":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="3" y="3" width="18" height="18" rx="3" />
            <path d="M8 8h2v8M14 8h2a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z" />
          </svg>
        );
      case "server":
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="3" width="20" height="7" rx="2" />
            <rect x="2" y="14" width="20" height="7" rx="2" />
            <line x1="6" y1="6.5" x2="6.01" y2="6.5" />
            <line x1="6" y1="17.5" x2="6.01" y2="17.5" />
          </svg>
        );
      case "trophy":
      default:
        return (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M6 9H4.5a2.5 2.5 0 0 1 0-5H6" />
            <path d="M18 9h1.5a2.5 2.5 0 0 0 0-5H18" />
            <path d="M4 22h16" />
            <path d="M10 14.66V17c0 .55-.45 1-1 1H7" />
            <path d="M14 14.66V17c0 .55.45 1 1 1h2" />
            <path d="M18 2H6v7a6 6 0 0 0 12 0V2z" />
          </svg>
        );
    }
  };

  return (
    <div
      className="kaggle-squircle-icon"
      style={{ backgroundColor: bg || "#F1F3F4", color: color || "#202124" }}
      aria-hidden="true"
    >
      {renderSvg()}
    </div>
  );
}

function CourseCurriculumModal({ course, onClose, onLaunchLesson, onOpenQuiz }) {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  if (!course) return null;

  const modalContent = (
    <div className="curriculum-modal-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="curriculum-modal-title">
      <div className="curriculum-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="curriculum-modal-header">
          <div className="curriculum-header-left">
            <CourseSquircleIcon name={course.icon} bg={course.iconBg} color={course.iconColor} />
            <div>
              <div className="curriculum-tags">
                <span className="curriculum-category">{course.category}</span>
                <span className={`curriculum-difficulty curriculum-difficulty--${course.difficulty.toLowerCase()}`}>
                  {course.difficulty}
                </span>
                <span className="curriculum-duration">{course.duration}</span>
              </div>
              <h2 id="curriculum-modal-title">{course.title}</h2>
            </div>
          </div>
          <button type="button" className="curriculum-close-btn" onClick={onClose} aria-label="Close curriculum">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <div className="curriculum-modal-body">
          <p className="curriculum-description">{course.description}</p>

          <div className="curriculum-progress-summary">
            <div className="curriculum-progress-info">
              <span>Course Progress</span>
              <strong>
                {course.completedLessons} of {course.totalLessons} Lessons Completed ({course.progress}%)
              </strong>
            </div>
            <div className="curriculum-progress-bar">
              <ProgressView
                variant="linear"
                value={course.progress}
                max={100}
                label={`${course.title} progress`}
                className="curriculum-progress-bar-el"
              />
            </div>
          </div>

          <div className="curriculum-lessons-list">
            <h3 className="curriculum-lessons-heading">Syllabus &amp; Individual Lessons</h3>
            {course.lessons.map((lesson, idx) => (
              <div
                key={lesson.id}
                className={`curriculum-lesson-item curriculum-lesson-item--${lesson.status}`}
                onClick={() => onLaunchLesson(course, lesson)}
                style={{ cursor: "pointer" }}
              >
                <div className="lesson-left">
                  <span className="lesson-status-icon">
                    {lesson.status === "completed" ? (
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#137333" strokeWidth="3">
                        <polyline points="20 6 9 17 4 12" />
                      </svg>
                    ) : lesson.status === "in-progress" ? (
                      <span className="lesson-pulse-dot" />
                    ) : (
                      <span className="lesson-pending-num">{idx + 1}</span>
                    )}
                  </span>
                  <div className="lesson-info">
                    <h4>
                      Lesson {idx + 1}: {lesson.title}
                    </h4>
                    <p>{lesson.summary}</p>
                    <div className="lesson-meta-chips">
                      <span className="lesson-duration-chip">⏱ {lesson.duration}</span>
                      <span className={`lesson-type-chip lesson-type-chip--${lesson.type}`}>
                        {lesson.type === "visualizer"
                          ? "Interactive Visualizer Trace"
                          : lesson.type === "quiz"
                          ? "Assessment Lab"
                          : lesson.type === "theory"
                          ? "Theory & Invariant Proof"
                          : "Core Algorithmic Concept"}
                      </span>
                    </div>
                  </div>
                </div>

                <div className="lesson-action" onClick={(e) => e.stopPropagation()}>
                  {lesson.type === "visualizer" ? (
                    <div style={{ display: "flex", gap: "8px", alignItems: "center" }}>
                      <button
                        type="button"
                        className="lesson-btn-action is-secondary"
                        onClick={() => onLaunchLesson(course, lesson)}
                        title="Read lesson theory & notebook lab"
                      >
                        <span>Notebook</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <polyline points="9 18 15 12 9 6" />
                        </svg>
                      </button>
                      <a
                        href={`#problem-details?id=${lesson.targetProblemId || course.targetProblemId || 1}&tab=visualizer`}
                        className="lesson-btn-action is-primary"
                        onClick={(e) => {
                          e.stopPropagation();
                          onClose();
                        }}
                        title="Open interactive step-by-step visualizer"
                      >
                        <span>Visualizer</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
                          <polyline points="15 3 21 3 21 9" />
                          <line x1="10" y1="14" x2="21" y2="3" />
                        </svg>
                      </a>
                    </div>
                  ) : lesson.type === "quiz" ? (
                    <button
                      type="button"
                      className="lesson-btn-action is-accent"
                      onClick={() => onOpenQuiz(course)}
                    >
                      <span>Take Quiz</span>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <line x1="5" y1="12" x2="19" y2="12" />
                        <polyline points="12 5 19 12 12 19" />
                      </svg>
                    </button>
                  ) : (
                    <button
                      type="button"
                      className="lesson-btn-action is-secondary"
                      onClick={() => onLaunchLesson(course, lesson)}
                    >
                      <span>{lesson.status === "completed" ? "Review Lesson" : "Start Lesson"}</span>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="9 18 15 12 9 6" />
                      </svg>
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        <div className="curriculum-modal-footer">
          <a
            href={`#problem-details?id=${course.targetProblemId || 1}&tab=visualizer`}
            className="curriculum-footer-primary"
            onClick={() => onClose()}
          >
            Open Interactive Visualizer Studio →
          </a>
          <button type="button" className="curriculum-footer-close" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );

  return typeof document !== "undefined"
    ? createPortal(modalContent, document.body)
    : modalContent;
}

function CertificateModal({ course, userName, onClose }) {
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  if (!course) return null;
  const displayName = userName && userName !== "Your name" ? userName : "Algorithmic Engineer";

  const modalContent = (
    <div className="curriculum-modal-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="certificate-modal-title">
      <div className="certificate-modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="certificate-frame">
          <div className="certificate-inner-border">
            <div className="certificate-badge-top">
              <span className="certificate-seal-icon" aria-hidden="true">
                <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="#B06000" strokeWidth="2">
                  <circle cx="12" cy="8" r="6" />
                  <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11" />
                </svg>
              </span>
              <span className="certificate-header-label">ALGOVIZ OFFICIAL VERIFIED CERTIFICATE</span>
            </div>

            <h2 id="certificate-modal-title">Certificate of Completion</h2>
            <p className="certificate-subtitle">This document proudly certifies that</p>
            <h3 className="certificate-recipient-name">{displayName}</h3>
            <p className="certificate-body-text">
              has successfully mastered all foundational concepts, interactive visual execution traces,
              time-space invariants, and assessment criteria for
            </p>
            <div className="certificate-course-box">
              <strong>{course.title}</strong>
              <small>{course.category} • {course.duration} Comprehensive Curriculum</small>
            </div>

            <div className="certificate-signatures">
              <div>
                <span className="signature-line">AlgoViz Core Guild</span>
                <small>Academic Director</small>
              </div>
              <div className="certificate-seal-center">
                <span className="seal-round">VERIFIED</span>
                <small>ID: AGV-{course.id.slice(0, 4).toUpperCase()}-9482X</small>
              </div>
              <div>
                <span className="signature-line">October 2026</span>
                <small>Date Completed</small>
              </div>
            </div>
          </div>
        </div>

        <div className="certificate-modal-actions">
          <button
            type="button"
            className="certificate-btn-download"
            onClick={() => window.print()}
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span>Print / Save Certificate PDF</span>
          </button>
          <button type="button" className="certificate-btn-close" onClick={onClose}>
            Close
          </button>
        </div>
      </div>
    </div>
  );

  return typeof document !== "undefined"
    ? createPortal(modalContent, document.body)
    : modalContent;
}

function GuideReaderModal({ guide, onClose, onSolveProblem }) {
  const [lang, setLang] = useState("python");
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  if (!guide) return null;

  const handleCopy = () => {
    const code = guide.codeSnippets?.[lang];
    if (code && navigator.clipboard) {
      navigator.clipboard.writeText(code);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const modalContent = (
    <div className="guide-modal-backdrop" onClick={onClose} role="dialog" aria-modal="true" aria-labelledby="guide-title">
      <div className="guide-modal" onClick={(e) => e.stopPropagation()}>
        <header className="guide-modal-header">
          <div className="guide-modal-header-info">
            <div className="guide-modal-header-chips">
              <span
                className="learn-guide-badge"
                style={{ color: guide.badgeColor, background: guide.badgeBg }}
              >
                {guide.badge}
              </span>
              <span className="learn-guide-meta">{guide.readTime} • {guide.difficulty}</span>
            </div>
            <h2 id="guide-title" className="guide-modal-title">{guide.title}</h2>
            <p className="guide-modal-sub">{guide.subtitle}</p>
          </div>
          <button
            type="button"
            className="guide-modal-close-btn"
            onClick={onClose}
            aria-label="Close guide modal"
          >
            ✕
          </button>
        </header>

        <div className="guide-modal-body">
          <div className="guide-overview-box">
            <p>{guide.overview}</p>
          </div>

          <div className="guide-key-takeaway-banner">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <circle cx="12" cy="12" r="10" />
              <line x1="12" y1="8" x2="12" y2="12" />
              <line x1="12" y1="16" x2="12.01" y2="16" />
            </svg>
            <div>
              <strong>Key Invariant / Takeaway:</strong>
              <div style={{ marginTop: "4px" }}>{guide.keyTakeaway}</div>
            </div>
          </div>

          {guide.codeSnippets && (
            <div className="guide-code-container">
              <div className="guide-code-header">
                <div className="guide-lang-tabs" role="tablist">
                  {Object.keys(guide.codeSnippets).map((l) => (
                    <button
                      key={l}
                      type="button"
                      role="tab"
                      aria-selected={lang === l}
                      className={`guide-lang-tab ${lang === l ? "is-active" : ""}`}
                      onClick={() => setLang(l)}
                    >
                      {l.toUpperCase()}
                    </button>
                  ))}
                </div>
                <button
                  type="button"
                  className="guide-copy-code-btn"
                  onClick={handleCopy}
                  title="Copy code template"
                >
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2" />
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                  </svg>
                  <span>{copied ? "Copied!" : "Copy Template"}</span>
                </button>
              </div>
              <pre className="guide-code-block">
                <code>{guide.codeSnippets[lang]}</code>
              </pre>
            </div>
          )}

          {guide.matrixRows && (
            <div>
              <h3 className="guide-modal-section-title">Constraint-to-Complexity Matrix</h3>
              <div className="guide-matrix-table-wrap">
                <table className="guide-matrix-table">
                  <thead>
                    <tr>
                      <th>Input Size (N)</th>
                      <th>Expected Big-O</th>
                      <th>Optimal Algorithm Families</th>
                    </tr>
                  </thead>
                  <tbody>
                    {guide.matrixRows.map((row, idx) => (
                      <tr key={idx}>
                        <td>{row.constraint}</td>
                        <td>{row.bigO}</td>
                        <td>{row.algorithms}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {guide.steps && (
            <div>
              <h3 className="guide-modal-section-title">Execution Steps</h3>
              <div className="guide-steps-list">
                {guide.steps.map((st, idx) => (
                  <div key={idx} className="guide-step-item">
                    <div className="guide-step-title">{st.step}</div>
                    <p className="guide-step-desc">{st.desc}</p>
                  </div>
                ))}
              </div>
            </div>
          )}

          {guide.ruleOfThumb && (
            <div style={{ fontSize: "12.5px", color: "#475569", background: "#f8fafc", padding: "10px 14px", borderRadius: "6px", border: "1px dashed #cbd5e1" }}>
              <strong>💡 Pro Rule of Thumb:</strong> {guide.ruleOfThumb}
            </div>
          )}
        </div>

        <footer className="guide-modal-footer">
          <div className="guide-modal-problems">
            <span>Practice Related:</span>
            {guide.practiceProblems?.map((prob) => (
              <button
                key={prob.id}
                type="button"
                className="guide-modal-prob-btn"
                onClick={() => onSolveProblem(prob)}
              >
                <span>#{prob.id} {prob.title}</span>
                <span style={{ fontSize: "10px", opacity: 0.7 }}>({prob.difficulty})</span>
              </button>
            ))}
          </div>
          <button
            type="button"
            className="guide-modal-close-action"
            onClick={onClose}
          >
            Close Guide
          </button>
        </footer>
      </div>
    </div>
  );

  return typeof document !== "undefined"
    ? createPortal(modalContent, document.body)
    : modalContent;
}

function GuidesSection({ activeCategory, onSelectCategory, onOpenGuide, onSolveProblem }) {
  const filteredGuides = useMemo(() => {
    if (activeCategory === "All") return LEARN_GUIDES;
    return LEARN_GUIDES.filter((g) => g.category === activeCategory);
  }, [activeCategory]);

  return (
    <section className="learn-guides-section" id="guides-tutorials" aria-labelledby="guides-heading">
      <div className="learn-guides-header">
        <div className="learn-guides-title-group">
          <div className="learn-guides-title-row">
            <span className="learn-guides-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
                <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
              </svg>
            </span>
            <h2 id="guides-heading">Guides, Patterns &amp; Tutorials</h2>
          </div>
          <p className="learn-guides-subtitle">
            Battle-tested problem patterns, Big-O deduction blueprints, and visual interview cheatsheets.
          </p>
        </div>

        <div className="learn-guides-pills" role="tablist" aria-label="Guide categories">
          {GUIDE_CATEGORIES.map((cat) => (
            <button
              type="button"
              key={cat}
              role="tab"
              aria-selected={activeCategory === cat}
              className={`learn-guide-pill ${activeCategory === cat ? "is-active" : ""}`}
              onClick={() => onSelectCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      <div className="learn-guides-grid">
        {filteredGuides.map((guide) => (
          <article
            key={guide.id}
            className="learn-guide-card"
            onClick={() => onOpenGuide(guide)}
            tabIndex={0}
            role="button"
            aria-label={`Open guide: ${guide.title}`}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                onOpenGuide(guide);
              }
            }}
          >
            <div>
              <div className="learn-guide-card-top">
                <span
                  className="learn-guide-badge"
                  style={{ color: guide.badgeColor, background: guide.badgeBg }}
                >
                  {guide.badge}
                </span>
                <span className="learn-guide-meta">{guide.readTime}</span>
              </div>

              <h3 className="learn-guide-title">{guide.title}</h3>
              <p className="learn-guide-subtitle-text">{guide.subtitle}</p>

              <div className="learn-guide-takeaway">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="16" x2="12" y2="12" />
                  <line x1="12" y1="8" x2="12.01" y2="8" />
                </svg>
                <span>{guide.keyTakeaway}</span>
              </div>
            </div>

            <div className="learn-guide-footer">
              <div className="learn-guide-problems-chips" onClick={(e) => e.stopPropagation()}>
                {guide.practiceProblems?.map((prob) => (
                  <button
                    key={prob.id}
                    type="button"
                    className="learn-guide-prob-chip"
                    onClick={() => onSolveProblem(prob)}
                    title={`Solve #${prob.id} ${prob.title} (${prob.difficulty})`}
                  >
                    #{prob.id} {prob.title}
                  </button>
                ))}
              </div>

              <button
                type="button"
                className="learn-guide-read-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  onOpenGuide(guide);
                }}
              >
                <span>Read Guide</span>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </button>
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

function LearnFooter() {
  return (
    <footer className="learn-footer">
      <div className="learn-footer-inner">
        <div>
          <a className="learn-footer-brand" href="#problems">
            Algo<strong>Viz</strong>
          </a>
          <p>AlgoViz Interactive Algorithmic Learning Studio</p>
          <small>© 2026 AlgoViz. Master algorithms through visual execution.</small>
        </div>
        <nav aria-label="Learn footer links">
          <a href="#problems">Problem Set</a>
          <a href="#problem-details?id=1&tab=visualizer">Visualizer Studio</a>
          <a href="#analyze">Telemetry &amp; Analysis</a>
        </nav>
      </div>
    </footer>
  );
}

function LearnPage({ userName = "Your name", isAuthenticated }) {
  const { progress, loading, error, retry } = useLearningProgress(isAuthenticated);
  const courses = useMemo(() => withLearningProgress(COURSES, progress), [progress]);
  const [activeCategory, setActiveCategory] = useState("All");
  const [searchQuery, setSearchQuery] = useState("");
  const [userCoursesTab, setUserCoursesTab] = useState("in-progress"); // "in-progress" | "completed"
  const [viewMode, setViewMode] = useState("grid"); // "grid" | "list"
  const [selectedCourseModal, setSelectedCourseModal] = useState(null);
  const [certificateCourse, setCertificateCourse] = useState(null);
  const [activeGuideCategory, setActiveGuideCategory] = useState("All");
  const [selectedGuideModal, setSelectedGuideModal] = useState(null);
  const [announcement, setAnnouncement] = useState("");

  const inProgressCourses = useMemo(
    () => courses.filter((c) => c.enrolled && c.progress < 100),
    [courses],
  );

  const completedCourses = useMemo(
    () => courses.filter((c) => c.enrolled && c.progress === 100),
    [courses],
  );

  const filteredCatalogCourses = useMemo(() => {
    return courses.filter((course) => {
      const matchesCategory =
        activeCategory === "All" || course.category === activeCategory;
      const matchesSearch =
        searchQuery.trim() === "" ||
        course.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        course.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
        course.category.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }, [activeCategory, searchQuery, courses]);

  const handleOpenCurriculum = (course) => {
    setSelectedCourseModal(course);
    setAnnouncement(`Opened curriculum for ${course.title}`);
  };

  const handleOpenCertificate = (course) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    setCertificateCourse(course);
    setAnnouncement(`Opened certificate for ${course.title}`);
  };

  const handleLaunchLesson = (course, lesson) => {
    setAnnouncement(`Navigating to ${lesson.title}`);
    setSelectedCourseModal(null);
    window.location.hash = `#lesson?courseId=${course.id}&lessonId=${lesson.id}`;
  };

  const handleOpenQuiz = (course) => {
    setSelectedCourseModal(null);
    const quizLesson =
      course.lessons?.find((l) => l.type === "quiz") ||
      course.lessons?.[course.lessons.length - 1];
    const targetId = quizLesson?.id || 1;
    setAnnouncement(`Opening assessment for ${course?.title || "course"}`);
    window.location.hash = `#lesson?courseId=${course.id}&lessonId=${targetId}#quiz`;
  };

  const handleSolveProblemFromGuide = (prob) => {
    window.location.hash = `problem-details?id=${prob?.id || 1}`;
  };

  if (loading || error) return (
    <main className="learn-page" aria-busy={loading}>
      <div className="learn-container">
        <ApiState loading={loading} error={error} onRetry={retry} label="Loading learning progress…" page />
      </div>
    </main>
  );

  return (
    <main className="learn-page" id="top">
      <div className="learn-container">
        {/* Header */}
        <header className="learn-header">
          <nav className="learn-breadcrumb" aria-label="Breadcrumb">
            <a href="#problems">Problems</a>
            <span aria-hidden="true">/</span>
            <a href="#learn">Learn</a>
            <span aria-hidden="true">/</span>
            <strong>Courses &amp; Guides</strong>
          </nav>
          <h1>
            Learn <span>Algorithms</span>
          </h1>
        </header>

        {/* Section 1: "Your Courses" (Kaggle Style with User Avatar and Tabs - only for authenticated users) */}
        {isAuthenticated && (
          <section className="learn-your-courses-section" aria-labelledby="your-courses-title">
            <div className="your-courses-header">
              <div className="your-courses-title-row">
                <span className="your-courses-user-icon" aria-hidden="true">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                </span>
                <h2 id="your-courses-title">Your Courses</h2>
              </div>

              <div className="your-courses-tabs" role="tablist">
                <button
                  type="button"
                  role="tab"
                  aria-selected={userCoursesTab === "in-progress"}
                  className={`your-courses-tab ${userCoursesTab === "in-progress" ? "is-active" : ""}`}
                  onClick={() => setUserCoursesTab("in-progress")}
                >
                  In Progress ({inProgressCourses.length})
                </button>
                <button
                  type="button"
                  role="tab"
                  aria-selected={userCoursesTab === "completed"}
                  className={`your-courses-tab ${userCoursesTab === "completed" ? "is-active" : ""}`}
                  onClick={() => setUserCoursesTab("completed")}
                >
                  Completed ({completedCourses.length})
                </button>
              </div>
            </div>

            <div className="your-courses-list" role="tabpanel">
              {userCoursesTab === "in-progress" ? (
                inProgressCourses.map((course) => (
                  <article key={course.id} className="your-course-row">
                    <div className="your-course-row-left" onClick={() => handleOpenCurriculum(course)}>
                      <CourseSquircleIcon name={course.icon} bg={course.iconBg} color={course.iconColor} />
                      <div className="your-course-info">
                        <h3>{course.title}</h3>
                        <div className="your-course-subtext">
                          <span>
                            Lesson {course.completedLessons} of {course.totalLessons} completed • {course.progress}%
                          </span>
                          <div className="your-course-mini-bar">
                            <ProgressView
                              variant="linear"
                              value={course.progress}
                              max={100}
                              label={`${course.title} completion`}
                              className="progress-view-bar--thin"
                            />
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="your-course-actions">
                      <button
                        type="button"
                        className="your-course-btn-continue"
                        onClick={() => handleOpenCurriculum(course)}
                      >
                        <span>Continue Course</span>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <polyline points="9 18 15 12 9 6" />
                        </svg>
                      </button>
                    </div>
                  </article>
                ))
              ) : (
                completedCourses.map((course) => (
                  <article key={course.id} className="your-course-row">
                    <div className="your-course-row-left" onClick={() => handleOpenCurriculum(course)}>
                      <CourseSquircleIcon name={course.icon} bg={course.iconBg} color={course.iconColor} />
                      <div className="your-course-info">
                        <h3>{course.title}</h3>
                        <p className="your-course-completed-text">
                          Congratulations, you did it! Your verified certificate is ready.
                        </p>
                      </div>
                    </div>
                    <div className="your-course-actions">
                      <button
                        type="button"
                        className="your-course-btn-certificate"
                        onClick={() => handleOpenCertificate(course)}
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <circle cx="12" cy="8" r="6" />
                          <path d="M15.477 12.89 17 22l-5-3-5 3 1.523-9.11" />
                        </svg>
                        <span>View Certificate</span>
                      </button>
                      <button
                        type="button"
                        className="your-course-btn-explore"
                        onClick={() => handleOpenCurriculum(course)}
                      >
                        <span>Review Lessons</span>
                      </button>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>
        )}

        {/* Section 2: "Courses & Guides" (4 Columns Grid with Squircles, List/Grid switch) */}
        <section className="learn-catalog-section" aria-labelledby="catalog-title">
          <div className="catalog-header">
            <div className="catalog-title-group">
              <div className="catalog-title-row">
                <span className="catalog-icon" aria-hidden="true">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                  </svg>
                </span>
                <h2 id="catalog-title">Courses</h2>
              </div>
            </div>

            <div className="catalog-view-switcher" role="group" aria-label="Course view mode">
              <button
                type="button"
                className={`view-switcher-btn ${viewMode === "list" ? "is-active" : ""}`}
                onClick={() => setViewMode("list")}
                aria-label="List view"
                title="List view"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <line x1="8" y1="6" x2="21" y2="6" />
                  <line x1="8" y1="12" x2="21" y2="12" />
                  <line x1="8" y1="18" x2="21" y2="18" />
                  <line x1="3" y1="6" x2="3.01" y2="6" />
                  <line x1="3" y1="12" x2="3.01" y2="12" />
                  <line x1="3" y1="18" x2="3.01" y2="18" />
                </svg>
              </button>
              <button
                type="button"
                className={`view-switcher-btn ${viewMode === "grid" ? "is-active" : ""}`}
                onClick={() => setViewMode("grid")}
                aria-label="Grid view"
                title="Grid view"
              >
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="3" width="7" height="7" rx="1" />
                  <rect x="14" y="3" width="7" height="7" rx="1" />
                  <rect x="14" y="14" width="7" height="7" rx="1" />
                  <rect x="3" y="14" width="7" height="7" rx="1" />
                </svg>
              </button>
            </div>
          </div>

          {/* Filter Pills & Search */}
          <div className="catalog-toolbar">
            <div className="catalog-category-pills" role="tablist" aria-label="Course categories">
              {CATEGORIES.map((cat) => (
                <button
                  type="button"
                  key={cat}
                  role="tab"
                  aria-selected={activeCategory === cat}
                  className={`catalog-category-pill ${activeCategory === cat ? "is-active" : ""}`}
                  onClick={() => setActiveCategory(cat)}
                >
                  {cat}
                </button>
              ))}
            </div>

            <div className="catalog-search-box">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#5F6368" strokeWidth="2" aria-hidden="true">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
              <input
                type="search"
                placeholder="Search courses or topics..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                aria-label="Search courses"
              />
              {searchQuery && (
                <button
                  type="button"
                  className="catalog-search-clear"
                  onClick={() => setSearchQuery("")}
                  aria-label="Clear search"
                >
                  ✕
                </button>
              )}
            </div>
          </div>

          {/* Catalog Layout: Grid or List */}
          {filteredCatalogCourses.length === 0 ? (
            <div className="catalog-empty-state">
              <p>No courses found matching &ldquo;{searchQuery}&rdquo;.</p>
              <button
                type="button"
                className="catalog-empty-reset"
                onClick={() => {
                  setSearchQuery("");
                  setActiveCategory("All");
                }}
              >
                Reset filters
              </button>
            </div>
          ) : viewMode === "grid" ? (
            <div className="kaggle-course-grid">
              {filteredCatalogCourses.map((course) => (
                <article
                  key={course.id}
                  className="kaggle-course-card"
                  onClick={() => handleOpenCurriculum(course)}
                  tabIndex={0}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      handleOpenCurriculum(course);
                    }
                  }}
                  role="button"
                  aria-label={`View curriculum for ${course.title}`}
                >
                  <div className="kaggle-card-top">
                    <h3 className="kaggle-card-title">{course.title}</h3>
                    <CourseSquircleIcon name={course.icon} bg={course.iconBg} color={course.iconColor} />
                  </div>

                  <p className="kaggle-card-description">{course.description}</p>

                  <div className="kaggle-card-footer">
                    <div className="kaggle-card-chips">
                      <span className={`kaggle-chip-difficulty kaggle-chip-difficulty--${course.difficulty.toLowerCase()}`}>
                        {course.difficulty}
                      </span>
                      <span className="kaggle-chip-lessons">{course.totalLessons} lessons • {course.duration}</span>
                    </div>

                    <div className="kaggle-card-action">
                      <span className="kaggle-card-action-text">
                        {isAuthenticated && course.enrolled && course.progress > 0
                          ? `Resume (${course.progress}%) →`
                          : "Explore →"}
                      </span>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="kaggle-course-list">
              {filteredCatalogCourses.map((course) => (
                <article
                  key={course.id}
                  className="kaggle-course-list-row"
                  onClick={() => handleOpenCurriculum(course)}
                >
                  <div className="kaggle-list-left">
                    <CourseSquircleIcon name={course.icon} bg={course.iconBg} color={course.iconColor} />
                    <div className="kaggle-list-info">
                      <div className="kaggle-list-tags">
                        <span className="kaggle-list-category">{course.category}</span>
                        <span className={`kaggle-chip-difficulty kaggle-chip-difficulty--${course.difficulty.toLowerCase()}`}>
                          {course.difficulty}
                        </span>
                        <span className="kaggle-list-duration">{course.duration}</span>
                      </div>
                      <h3>{course.title}</h3>
                      <p>{course.description}</p>
                    </div>
                  </div>

                  <div className="kaggle-list-actions">
                    <button
                      type="button"
                      className="kaggle-list-btn-explore"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleOpenCurriculum(course);
                      }}
                    >
                      <span>Explore Syllabus ({course.totalLessons} Lessons)</span>
                      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <polyline points="9 18 15 12 9 6" />
                      </svg>
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        {/* Section 3: Algorithm Guides, Patterns & Interactive Tutorials */}
        <GuidesSection
          activeCategory={activeGuideCategory}
          onSelectCategory={setActiveGuideCategory}
          onOpenGuide={setSelectedGuideModal}
          onSolveProblem={handleSolveProblemFromGuide}
        />
      </div>

      {/* Curriculum Modal */}
      {selectedCourseModal && (
        <CourseCurriculumModal
          course={selectedCourseModal}
          onClose={() => setSelectedCourseModal(null)}
          onLaunchLesson={handleLaunchLesson}
          onOpenQuiz={handleOpenQuiz}
        />
      )}

      {/* Certificate Modal */}
      {certificateCourse && (
        <CertificateModal
          course={certificateCourse}
          userName={userName}
          onClose={() => setCertificateCourse(null)}
        />
      )}

      {/* Guide Reader Modal */}
      {selectedGuideModal && (
        <GuideReaderModal
          guide={selectedGuideModal}
          onClose={() => setSelectedGuideModal(null)}
          onSolveProblem={handleSolveProblemFromGuide}
        />
      )}

      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>

      <LearnFooter />
    </main>
  );
}

export default LearnPage;
