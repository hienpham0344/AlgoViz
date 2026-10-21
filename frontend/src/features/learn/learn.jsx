import { useState, useMemo, useEffect } from "react";
import { createPortal } from "react-dom";
import { NavigationBar, ProgressView } from "../../components/index.js";
import ResizableColumns from "../../components/ResizableColumns.jsx";
import "./learn.css";

const CATEGORIES = [
  "All",
  "Foundations",
  "Sorting & Searching",
  "Data Structures",
  "Dynamic Programming",
  "Graph Theory",
  "Interview Prep",
];

const COURSES = [
  {
    id: "sorting-masterclass",
    title: "Sorting Algorithms Masterclass",
    category: "Sorting & Searching",
    difficulty: "Beginner",
    duration: "4 hours",
    description:
      "Compare Bubble, Insertion, Merge, and Quick Sort. Master stability, recursion depth, and O(n log n) lower bounds through interactive step-by-step traces.",
    icon: "sort-bars",
    iconBg: "#E6F4EA",
    iconColor: "#137333",
    enrolled: true,
    progress: 67,
    completedLessons: 4,
    totalLessons: 6,
    targetProblemId: 912,
    lessons: [
      {
        id: 1,
        title: "Comparison Invariants & Adjacent Swaps",
        duration: "25 min",
        type: "concept",
        status: "completed",
        summary: "Understand how pairwise comparisons establish sorted partitions and preserve array stability.",
      },
      {
        id: 2,
        title: "Bubble Sort: Visual Step Trace & Optimization",
        duration: "35 min",
        type: "visualizer",
        targetProblemId: 912,
        status: "completed",
        summary: "Watch elements bubble up to their correct final positions with early termination flags.",
      },
      {
        id: 3,
        title: "Merge Sort: Divide & Conquer Subarray Trees",
        duration: "40 min",
        type: "visualizer",
        targetProblemId: 912,
        status: "completed",
        summary: "Logarithmic recursion depth, two-way subarray merging, and auxiliary space requirements.",
      },
      {
        id: 4,
        title: "Quick Sort: Partitioning Invariants & Worst Cases",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 912,
        status: "completed",
        summary: "Lomuto vs Hoare partitioning schemes, pivot selection strategies, and stack depth bounds.",
      },
      {
        id: 5,
        title: "Stability, In-Place Space & Memory Trade-offs",
        duration: "30 min",
        type: "theory",
        status: "in-progress",
        summary: "Compare non-comparison counting sort, cache locality, and memory footprints in production.",
      },
      {
        id: 6,
        title: "Assessment Quiz & Sorting Lab",
        duration: "20 min",
        type: "quiz",
        status: "not-started",
        summary: "Verify your mastery of worst-case complexity, stability proofs, and asymptotic limits.",
      },
    ],
  },
  {
    id: "python-algorithms",
    title: "Python for Algorithmic Problem Solving",
    category: "Foundations",
    difficulty: "Beginner",
    duration: "3 hours",
    description:
      "Master Python idioms, slicing syntax, collections, heapq, deque, and recursion patterns tailored for fast, bug-free interview coding.",
    icon: "code",
    iconBg: "#FFF4D3",
    iconColor: "#B06000",
    enrolled: true,
    progress: 100,
    completedLessons: 5,
    totalLessons: 5,
    targetProblemId: 1,
    lessons: [
      {
        id: 1,
        title: "Python Data Model, Slices & Memory References",
        duration: "30 min",
        type: "concept",
        status: "completed",
        summary: "Mutable vs immutable objects, shallow vs deep copies, and list comprehension internals.",
      },
      {
        id: 2,
        title: "Hash Tables & Collections: dict, defaultdict, Counter",
        duration: "35 min",
        type: "concept",
        status: "completed",
        summary: "Amortized O(1) hashing, collisions, frequency counts, and grouping idioms.",
      },
      {
        id: 3,
        title: "Priority Queues with heapq & Two-ended Deques",
        duration: "40 min",
        type: "visualizer",
        targetProblemId: 1,
        status: "completed",
        summary: "Binary min-heaps in Python, heapify in linear time, and O(1) double-ended queues.",
      },
      {
        id: 4,
        title: "Recursion Limits, Call Stack & Generator Iterators",
        duration: "35 min",
        type: "theory",
        status: "completed",
        summary: "Avoid recursion limits, implement tail recursion simulations, and yield memory-efficient generators.",
      },
      {
        id: 5,
        title: "Idiomatic Python Benchmark Assessment",
        duration: "25 min",
        type: "quiz",
        status: "completed",
        summary: "Comprehensive Python speed, complexity, and idiom mastery examination.",
      },
    ],
  },
  {
    id: "binary-search",
    title: "Binary Search & Interval Halving",
    category: "Sorting & Searching",
    difficulty: "Intermediate",
    duration: "3 hours",
    description:
      "Go beyond basic array lookups. Learn monotone predicate search, rotated sorted arrays, square root halving, and continuous real intervals.",
    icon: "search-divide",
    iconBg: "#E8F7FE",
    iconColor: "#0083BF",
    enrolled: true,
    progress: 100,
    completedLessons: 4,
    totalLessons: 4,
    targetProblemId: 704,
    lessons: [
      {
        id: 1,
        title: "The Boundary Monotonicity Invariant",
        duration: "25 min",
        type: "concept",
        status: "completed",
        summary: "Identify boolean functions F(x) with monotonic false-to-true transitions.",
      },
      {
        id: 2,
        title: "Template 1 vs Template 2: Avoiding Infinite Loops",
        duration: "30 min",
        type: "concept",
        status: "completed",
        summary: "Midpoint calculation without 32-bit overflow and robust boundary updates.",
      },
      {
        id: 3,
        title: "Binary Search on Answer / Search Space",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 704,
        status: "completed",
        summary: "Solve capacity planning and allocation challenges by testing answer feasibility.",
      },
      {
        id: 4,
        title: "Rotated Sorted Arrays & Peak Element Finding",
        duration: "35 min",
        type: "quiz",
        status: "completed",
        summary: "Locate pivots and inflection points in O(log n) non-strictly sorted arrays.",
      },
    ],
  },
  {
    id: "trees-and-heaps",
    title: "Data Structures: Trees & Binary Heaps",
    category: "Data Structures",
    difficulty: "Intermediate",
    duration: "5 hours",
    description:
      "Master Binary Search Trees, AVL balance invariants, priority queues, and min-max heap properties with live interactive tree visualizers.",
    icon: "tree",
    iconBg: "#FEF7E0",
    iconColor: "#EA8600",
    enrolled: true,
    progress: 33,
    completedLessons: 2,
    totalLessons: 6,
    targetProblemId: 104,
    lessons: [
      {
        id: 1,
        title: "Binary Tree Traversals: Preorder, Inorder, Postorder",
        duration: "30 min",
        type: "concept",
        status: "completed",
        summary: "Recursive vs iterative stack traversals and reconstruction from traversal pairs.",
      },
      {
        id: 2,
        title: "BST Search, Insertion & Live Rebalancing",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 104,
        status: "completed",
        summary: "Maintain the BST property across insertions, deletions, and AVL rotation cases.",
      },
      {
        id: 3,
        title: "Binary Heaps: Array Representation & Sift-Down",
        duration: "40 min",
        type: "visualizer",
        targetProblemId: 104,
        status: "in-progress",
        summary: "Index math for complete binary trees, sift-up insertions, and sift-down deletions.",
      },
      {
        id: 4,
        title: "Trie (Prefix Trees) for Autocomplete Lookups",
        duration: "45 min",
        type: "concept",
        status: "not-started",
        summary: "String prefix storage, node child arrays, and wildcard pattern matching.",
      },
      {
        id: 5,
        title: "Lowest Common Ancestor & Path Invariants",
        duration: "35 min",
        type: "theory",
        status: "not-started",
        summary: "Binary lifting, Euler tour techniques, and recursive LCA resolution.",
      },
      {
        id: 6,
        title: "Tree Structure Mastery Assessment",
        duration: "25 min",
        type: "quiz",
        status: "not-started",
        summary: "Test tree heights, rebalancing logic, and heapify complexity.",
      },
    ],
  },
  {
    id: "two-pointers-sliding-window",
    title: "Sliding Window & Two Pointers",
    category: "Foundations",
    difficulty: "Beginner",
    duration: "4 hours",
    description:
      "Transform quadratic O(n²) subarray scans into optimal O(n) passes using monotonic sliding windows and converging two-pointer sweeps.",
    icon: "window",
    iconBg: "#E0F2F1",
    iconColor: "#00796B",
    enrolled: true,
    progress: 100,
    completedLessons: 5,
    totalLessons: 5,
    targetProblemId: 3,
    lessons: [
      {
        id: 1,
        title: "Opposite-direction Two Pointers (Pair Sums)",
        duration: "25 min",
        type: "concept",
        status: "completed",
        summary: "Sort arrays and converge inward to find target sums in linear time.",
      },
      {
        id: 2,
        title: "Fast & Slow Pointers: Floyd's Cycle Detection",
        duration: "35 min",
        type: "visualizer",
        targetProblemId: 3,
        status: "completed",
        summary: "Hare and tortoise cycle detection in linked lists and repeated number arrays.",
      },
      {
        id: 3,
        title: "Fixed-size Sliding Window Aggregations",
        duration: "30 min",
        type: "concept",
        status: "completed",
        summary: "Maintain rolling sums, maximums, and hash distributions over fixed bounds.",
      },
      {
        id: 4,
        title: "Dynamic Sliding Window with Monotonic State",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 3,
        status: "completed",
        summary: "Expand right to satisfy predicates, contract left to minimize subarray length.",
      },
      {
        id: 5,
        title: "Window Mastery Assessment",
        duration: "20 min",
        type: "quiz",
        status: "completed",
        summary: "Subarray substring invariants, two-pointer boundary edge cases, and quiz checks.",
      },
    ],
  },
  {
    id: "graph-algorithms",
    title: "Graph Traversal: BFS, DFS & Shortest Path",
    category: "Graph Theory",
    difficulty: "Intermediate",
    duration: "6 hours",
    description:
      "Explore adjacency representations, cycle detection, topological sorting, Dijkstra, and Bellman-Ford shortest paths with step-by-step visual graphs.",
    icon: "graph",
    iconBg: "#F3E8FD",
    iconColor: "#7B1FA2",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 7,
    targetProblemId: 200,
    lessons: [
      {
        id: 1,
        title: "Graph Representations: Adjacency Matrix vs List",
        duration: "25 min",
        type: "concept",
        status: "not-started",
        summary: "Space vs lookup complexity for dense and sparse algorithmic networks.",
      },
      {
        id: 2,
        title: "Breadth-First Search & Level-Order Queue State",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 200,
        status: "not-started",
        summary: "Shortest paths on unweighted graphs and frontier exploration sequences.",
      },
      {
        id: 3,
        title: "Depth-First Search & Recursion Call Graph",
        duration: "40 min",
        type: "visualizer",
        targetProblemId: 200,
        status: "not-started",
        summary: "Connected components, island counts, and discovery / finish timestamps.",
      },
      {
        id: 4,
        title: "Cycle Detection in Directed & Undirected Graphs",
        duration: "35 min",
        type: "theory",
        status: "not-started",
        summary: "Three-color DFS marking and Union-Find disjoint sets for cycle proofs.",
      },
      {
        id: 5,
        title: "Topological Sort & Kahn's Indegree Algorithm",
        duration: "40 min",
        type: "concept",
        status: "not-started",
        summary: "Dependency resolution for build systems and task scheduling in DAGs.",
      },
      {
        id: 6,
        title: "Dijkstra's Algorithm with Priority Queue",
        duration: "50 min",
        type: "visualizer",
        targetProblemId: 200,
        status: "not-started",
        summary: "Greedy relaxation of distances and edge relaxation priority ordering.",
      },
      {
        id: 7,
        title: "Graph Traversal Mastery Assessment",
        duration: "25 min",
        type: "quiz",
        status: "not-started",
        summary: "Asymptotic bounds for dense vs sparse graphs and algorithm selection.",
      },
    ],
  },
  {
    id: "dynamic-programming",
    title: "Dynamic Programming: Zero to Hero",
    category: "Dynamic Programming",
    difficulty: "Advanced",
    duration: "8 hours",
    description:
      "Formulate recurrence relations, DAG memoization, bottom-up tabulation, and space optimization for classic DP interview challenges.",
    icon: "dp-grid",
    iconBg: "#FCE8E6",
    iconColor: "#C5221F",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 8,
    targetProblemId: 70,
    lessons: [
      {
        id: 1,
        title: "Overlapping Subproblems & Optimal Substructure",
        duration: "30 min",
        type: "concept",
        status: "not-started",
        summary: "Recognize when brute force recursion repeats identical state evaluations.",
      },
      {
        id: 2,
        title: "Top-Down Memoization vs Bottom-Up Tabulation",
        duration: "45 min",
        type: "concept",
        status: "not-started",
        summary: "Call stack recursion overhead vs topological table population order.",
      },
      {
        id: 3,
        title: "1D Dynamic Programming: Climbing Stairs & House Robber",
        duration: "40 min",
        type: "visualizer",
        targetProblemId: 70,
        status: "not-started",
        summary: "State transitions dependent on the preceding one or two sub-states.",
      },
      {
        id: 4,
        title: "2D DP: Grid Paths & Minimum Path Sum",
        duration: "50 min",
        type: "visualizer",
        targetProblemId: 70,
        status: "not-started",
        summary: "Grid transitions with obstacle handling and boundary base cases.",
      },
      {
        id: 5,
        title: "0/1 Knapsack & Unbounded Knapsack Invariants",
        duration: "60 min",
        type: "theory",
        status: "not-started",
        summary: "Value vs weight optimization and rolling single-row array updates.",
      },
      {
        id: 6,
        title: "Longest Common Subsequence & Edit Distance",
        duration: "55 min",
        type: "visualizer",
        targetProblemId: 70,
        status: "not-started",
        summary: "String alignment transformations: insertion, deletion, and substitution costs.",
      },
      {
        id: 7,
        title: "State Compression & Rolling Array Optimizations",
        duration: "45 min",
        type: "theory",
        status: "not-started",
        summary: "Reduce O(n²) space complexity to O(n) or O(1) in multi-dimensional DP.",
      },
      {
        id: 8,
        title: "Dynamic Programming Comprehensive Examination",
        duration: "35 min",
        type: "quiz",
        status: "not-started",
        summary: "Formulating recurrence tables and proving optimal substructure.",
      },
    ],
  },
  {
    id: "greedy-algorithms",
    title: "Greedy Algorithms & Interval Scheduling",
    category: "Foundations",
    difficulty: "Intermediate",
    duration: "4 hours",
    description:
      "Learn when local greedy choices yield global optima. Master exchange arguments, interval scheduling, and Huffman coding optimality proofs.",
    icon: "compass",
    iconBg: "#FFF0E6",
    iconColor: "#D9381E",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 5,
    targetProblemId: 55,
    lessons: [
      {
        id: 1,
        title: "The Greedy Choice Property & Exchange Arguments",
        duration: "30 min",
        type: "concept",
        status: "not-started",
        summary: "Mathematical induction and exchange proofs showing greedy solutions match optimal.",
      },
      {
        id: 2,
        title: "Interval Scheduling & Activity Selection",
        duration: "40 min",
        type: "visualizer",
        targetProblemId: 55,
        status: "not-started",
        summary: "Sort intervals by earliest finish time to maximize non-overlapping events.",
      },
      {
        id: 3,
        title: "Fractional Knapsack vs 0/1 Knapsack",
        duration: "35 min",
        type: "concept",
        status: "not-started",
        summary: "Value-to-weight density sorting and why greedy fails on discrete items.",
      },
      {
        id: 4,
        title: "Minimum Spanning Trees: Kruskal & Prim",
        duration: "45 min",
        type: "theory",
        status: "not-started",
        summary: "Cut property proofs, cycle avoidance, and disjoint-set rank optimization.",
      },
      {
        id: 5,
        title: "Greedy Invariant Mastery Quiz",
        duration: "20 min",
        type: "quiz",
        status: "not-started",
        summary: "Distinguish greedy problems from dynamic programming problems under pressure.",
      },
    ],
  },
  {
    id: "backtracking-search",
    title: "Backtracking & State Space Trees",
    category: "Dynamic Programming",
    difficulty: "Advanced",
    duration: "5 hours",
    description:
      "Prune exponential search trees in N-Queens, Sudoku solvers, subset sums, and permutation generation with systematic constraint propagation.",
    icon: "branch",
    iconBg: "#FEEBEE",
    iconColor: "#C2185B",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 6,
    targetProblemId: 51,
    lessons: [
      {
        id: 1,
        title: "State Space Tree Representation & Traversal",
        duration: "30 min",
        type: "concept",
        status: "not-started",
        summary: "Choose, explore, and un-choose patterns in recursive backtracking algorithms.",
      },
      {
        id: 2,
        title: "Subsets & Combinations Generation",
        duration: "40 min",
        type: "concept",
        status: "not-started",
        summary: "Build power sets and handle duplicate elements through sorting.",
      },
      {
        id: 3,
        title: "Permutations & Visited State Masks",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 51,
        status: "not-started",
        summary: "Track elements using booleans and bitmasks for O(n!) generation.",
      },
      {
        id: 4,
        title: "N-Queens & Constraint Propagation Pruning",
        duration: "50 min",
        type: "visualizer",
        targetProblemId: 51,
        status: "not-started",
        summary: "Column, main diagonal, and anti-diagonal conflict tracking.",
      },
      {
        id: 5,
        title: "Word Search on 2D Grids",
        duration: "40 min",
        type: "concept",
        status: "not-started",
        summary: "In-place grid cell marking and restoration during DFS exploration.",
      },
      {
        id: 6,
        title: "Backtracking Mastery Quiz",
        duration: "25 min",
        type: "quiz",
        status: "not-started",
        summary: "Pruning efficiency, time complexity recurrence, and call stack depths.",
      },
    ],
  },
  {
    id: "bit-manipulation",
    title: "Bit Manipulation & Low-Level Tricks",
    category: "Interview Prep",
    difficulty: "Advanced",
    duration: "3 hours",
    description:
      "Exploit XOR cancellation, bitmasks, Brian Kernighan bit counts, and subset enumeration without CPU overhead or auxiliary allocations.",
    icon: "binary",
    iconBg: "#E8F5E9",
    iconColor: "#2E7D32",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 4,
    targetProblemId: 136,
    lessons: [
      {
        id: 1,
        title: "Two's Complement, Left/Right Shifts & Masking",
        duration: "25 min",
        type: "concept",
        status: "not-started",
        summary: "Binary arithmetic, logical vs arithmetic shifts, and bitwise flags.",
      },
      {
        id: 2,
        title: "Single Number & XOR Elimination Properties",
        duration: "30 min",
        type: "visualizer",
        targetProblemId: 136,
        status: "not-started",
        summary: "Commutative and associative cancellation of matching duplicate numbers.",
      },
      {
        id: 3,
        title: "Counting Set Bits: Brian Kernighan's Algorithm",
        duration: "35 min",
        type: "theory",
        status: "not-started",
        summary: "Clear the lowest set bit in O(k) steps using n & (n - 1).",
      },
      {
        id: 4,
        title: "Bitmask Subsets & Combinatorics Quiz",
        duration: "40 min",
        type: "quiz",
        status: "not-started",
        summary: "Enumerate all 2^n submasks and test bitmask operations under timed conditions.",
      },
    ],
  },
  {
    id: "system-design-big-o",
    title: "System Design & Big-O in Practice",
    category: "Interview Prep",
    difficulty: "Advanced",
    duration: "6 hours",
    description:
      "Analyze latency numbers every programmer should know, disk I/O, cache locality, and asymptotic amortized complexity at distributed production scale.",
    icon: "server",
    iconBg: "#EDE7F6",
    iconColor: "#512DA8",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 6,
    targetProblemId: 146,
    lessons: [
      {
        id: 1,
        title: "Latency Hierarchy & Memory Cache Lines",
        duration: "35 min",
        type: "concept",
        status: "not-started",
        summary: "L1/L2 caches, RAM bus limits, and SSD vs network transfer latency numbers.",
      },
      {
        id: 2,
        title: "Amortized Analysis: Dynamic Arrays & Accounting",
        duration: "40 min",
        type: "theory",
        status: "not-started",
        summary: "The aggregate method, banker's method, and geometric capacity doubling.",
      },
      {
        id: 3,
        title: "LRU Cache Design: Doubly Linked Hash Map",
        duration: "50 min",
        type: "visualizer",
        targetProblemId: 146,
        status: "not-started",
        summary: "O(1) get and put operations with head/tail sentinel node manipulation.",
      },
      {
        id: 4,
        title: "Consistent Hashing & Distributed Ring Routing",
        duration: "45 min",
        type: "concept",
        status: "not-started",
        summary: "Virtual nodes, cluster rebalancing, and minimizing key remapping during server failures.",
      },
      {
        id: 5,
        title: "Rate Limiting: Token Bucket & Leaky Bucket",
        duration: "40 min",
        type: "concept",
        status: "not-started",
        summary: "Burst allowance, smooth outflow rates, and distributed Redis counter scripts.",
      },
      {
        id: 6,
        title: "Distributed Scale Complexity Assessment",
        duration: "30 min",
        type: "quiz",
        status: "not-started",
        summary: "Quantify throughput, QPS limits, and storage projections under interview requirements.",
      },
    ],
  },
  {
    id: "technical-interview-mastery",
    title: "Technical Interview Mastery",
    category: "Interview Prep",
    difficulty: "Intermediate",
    duration: "7 hours",
    description:
      "Curated high-frequency FAANG patterns, invariant verification, behavioral communication strategies, and mock live visualizer challenges.",
    icon: "trophy",
    iconBg: "#FFF8E1",
    iconColor: "#F57F17",
    enrolled: false,
    progress: 0,
    completedLessons: 0,
    totalLessons: 8,
    targetProblemId: 1,
    lessons: [
      {
        id: 1,
        title: "The 5-Step Problem Solving Framework",
        duration: "30 min",
        type: "concept",
        status: "not-started",
        summary: "Clarify inputs/outputs, define test cases, brainstorm approaches, code cleanly, and dry run.",
      },
      {
        id: 2,
        title: "Top 15 Problem Classification Patterns",
        duration: "45 min",
        type: "concept",
        status: "not-started",
        summary: "Map problem statements to appropriate algorithmic techniques instantly.",
      },
      {
        id: 3,
        title: "Communicating Invariants & Trade-offs Out Loud",
        duration: "35 min",
        type: "theory",
        status: "not-started",
        summary: "Keep the interviewer aligned through clear structured technical explanations.",
      },
      {
        id: 4,
        title: "Live Trace 1: Two Sum & Hash Mapping",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 1,
        status: "not-started",
        summary: "Step through single-pass complement lookups with memory and index tables.",
      },
      {
        id: 5,
        title: "Live Trace 2: Graph Island Counting (BFS/DFS)",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 200,
        status: "not-started",
        summary: "Grid traversal, boundary validation, and space complexity evaluation.",
      },
      {
        id: 6,
        title: "Live Trace 3: Monotonic Stacks & Next Greater",
        duration: "45 min",
        type: "visualizer",
        targetProblemId: 739,
        status: "not-started",
        summary: "Maintain monotonic decreasing stack invariants for O(n) lookups.",
      },
      {
        id: 7,
        title: "Mock Interview Timed Assessment",
        duration: "40 min",
        type: "quiz",
        status: "not-started",
        summary: "Simulated high-pressure technical interview evaluation with instant feedback.",
      },
      {
        id: 8,
        title: "Post-Interview Debrief & Offer Negotiation",
        duration: "25 min",
        type: "concept",
        status: "not-started",
        summary: "Ask impactful interviewer questions and navigate compensation discussions.",
      },
    ],
  },
];

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
    () => COURSES.filter((c) => c.enrolled && c.progress < 100),
    [],
  );

  const completedCourses = useMemo(
    () => COURSES.filter((c) => c.enrolled && c.progress === 100),
    [],
  );

  const filteredCatalogCourses = useMemo(() => {
    return COURSES.filter((course) => {
      const matchesCategory =
        activeCategory === "All" || course.category === activeCategory;
      const matchesSearch =
        searchQuery.trim() === "" ||
        course.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
        course.description.toLowerCase().includes(searchQuery.toLowerCase()) ||
        course.category.toLowerCase().includes(searchQuery.toLowerCase());
      return matchesCategory && matchesSearch;
    });
  }, [activeCategory, searchQuery]);

  const handleOpenCurriculum = (course) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
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
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    setAnnouncement(`Navigating to ${lesson.title}`);
    setSelectedCourseModal(null);
    window.location.hash = `#lesson?courseId=${course.id}&lessonId=${lesson.id}`;
  };

  const handleOpenQuiz = (course) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    setSelectedCourseModal(null);
    const quizLesson =
      course.lessons?.find((l) => l.type === "quiz") ||
      course.lessons?.[course.lessons.length - 1];
    const targetId = quizLesson?.id || 1;
    setAnnouncement(`Opening assessment for ${course?.title || "course"}`);
    window.location.hash = `#lesson?courseId=${course.id}&lessonId=${targetId}#quiz`;
  };

  const handleSolveProblemFromGuide = (prob) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    window.location.hash = `problem-details?id=${prob?.id || 1}`;
  };

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
