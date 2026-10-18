/**
 * AST-tool API client & Sort Trace Engine.
 *
 * Talks to the AST-tool backend (default http://localhost:8081) which
 * parses Java source with JavaParser, launches it under JDI, and returns
 * per-step execution observations with visual sort states.
 * Also provides a client-side trace generator for smooth animations with
 * persistent element ID tracking.
 */

import { javaFilename } from "./solutionTrace.js";

const AST_API_BASE_URL = "http://localhost:8081";

export const BUBBLE_SORT_SAMPLE_JAVA = `public class BubbleSortSample {
  public static void main(String[] args) {
    int[] nums = {5, 2, 8, 1, 9, 3};
    sort(nums);
  }

  private static void sort(int[] nums) {
    for (int i = 0; i < nums.length - 1; i++) {
      for (int j = 0; j < nums.length - 1 - i; j++) {
        if (nums[j] > nums[j + 1]) {
          int temp = nums[j];
          nums[j] = nums[j + 1];
          nums[j + 1] = temp;
        }
      }
    }
  }
}
`;

/**
 * Check if the AST-tool backend is running and reachable.
 */
export async function checkAstToolHealth() {
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 1200);
    const response = await fetch(`${AST_API_BASE_URL}/api/ast/parse`, {
      method: "OPTIONS",
      signal: controller.signal,
    }).catch(() => null);
    clearTimeout(timer);
    return Boolean(response && response.status < 500);
  } catch {
    return false;
  }
}

/**
 * POST a Java source string as a multipart file upload.
 *
 * @param {string} path — endpoint path, e.g. "/api/debug/steps"
 * @param {string} javaSource — raw Java source code
 * @param {string} [filename] — filename sent in the multipart body
 * @returns {Promise<object>} parsed JSON response
 */
async function postJavaSource(path, javaSource, options = {}) {
  if (!javaSource?.trim()) throw new Error("This solution has no executable source code.");
  const filename = javaFilename(javaSource);
  const blob = new Blob([javaSource], { type: "text/x-java-source" });
  const formData = new FormData();
  formData.append("file", blob, filename);

  const response = await fetch(`${AST_API_BASE_URL}${path}`, {
    method: "POST",
    body: formData,
    signal: options.signal,
  });

  const data = await response.json().catch(() => null);

  if (!response.ok) {
    const message =
      data?.error?.message || data?.message || `HTTP ${response.status}`;
    const err = new Error(message);
    err.code = data?.error?.code;
    err.problems = data?.problems || [];
    throw err;
  }

  return data;
}

/**
 * Fetch step-by-step execution trace with visual sort states.
 */
export async function fetchDebugSteps(javaSource, options = {}) {
  return postJavaSource("/api/debug/steps", javaSource, options);
}

/**
 * Fetch flat list of variable observations (legacy endpoint).
 */
export async function fetchDebugObservations(javaSource = BUBBLE_SORT_SAMPLE_JAVA) {
  return postJavaSource("/api/debug", javaSource);
}

/**
 * Fetch the structured AST tree.
 */
export async function fetchAstParse(javaSource = BUBBLE_SORT_SAMPLE_JAVA) {
  return postJavaSource("/api/ast/parse", javaSource);
}

function primitiveLabel(value) {
  if (value === null || value === undefined) return "null";
  if (typeof value === "string") return value;
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

/**
 * Generate a complete, high-fidelity Bubble Sort trace with persistent element IDs.
 * This guarantees smooth FLIP/CSS transform transitions where elements retain their identity
 * and visibly glide across positions during swaps.
 *
 * @param {number[]} initialArray
 * @returns {Array<object>} list of trace steps
 */
export function generateBubbleSortTrace(initialArray = [5, 2, 8, 1, 9, 3]) {
  const n = initialArray.length;
  // Initialize items with stable unique ID
  let currentItems = initialArray.map((val, idx) => ({
    id: idx,
    value: val,
    pos: idx,
  }));

  const steps = [];
  let swapsTotal = 0;
  let comparisonsTotal = 0;
  const sortedIndices = new Set();

  const cloneItems = (items) => items.map((it) => ({ ...it }));

  // Initial step (Idle)
  steps.push({
    stepIndex: 0,
    items: cloneItems(currentItems),
    nums: currentItems.slice().sort((a, b) => a.pos - b.pos).map((it) => it.value),
    type: "IDLE",
    status: "idle",
    leftIndex: -1,
    rightIndex: -1,
    comparingIndices: [],
    swappingIndices: [],
    sortedIndices: Array.from(sortedIndices),
    pointers: { i: 0, j: 0 },
    activeLine: 18,
    comparison: "Start",
    annotation: "Khởi tạo mảng ban đầu",
    context: `Bubble Sort: Mảng có ${n} phần tử. Bắt đầu duyệt pass đầu tiên.`,
    swapCount: 0,
    compareCount: 0,
  });

  for (let i = 0; i < n - 1; i++) {
    for (let j = 0; j < n - 1 - i; j++) {
      // Find items currently at position j and j + 1
      const itemAtJ = currentItems.find((it) => it.pos === j);
      const itemAtJ1 = currentItems.find((it) => it.pos === j + 1);
      const valJ = itemAtJ.value;
      const valJ1 = itemAtJ1.value;

      comparisonsTotal++;
      const needSwap = valJ > valJ1;
      // Common active pair for this comparison & swap:
      // Item A (bên trái) luôn là XANH, Item B (bên phải) luôn là TÍM.
      const pairInfo = {
        leftItemId: itemAtJ.id,
        rightItemId: itemAtJ1.id,
        valLeft: valJ,
        valRight: valJ1,
        idxLeft: j,
        idxRight: j + 1,
      };

      // 1. STEP: COMPARE
      steps.push({
        stepIndex: steps.length,
        items: cloneItems(currentItems),
        nums: currentItems.slice().sort((a, b) => a.pos - b.pos).map((it) => it.value),
        type: "COMPARE",
        status: "compare",
        leftIndex: j,
        rightIndex: j + 1,
        comparingIndices: [j, j + 1],
        swappingIndices: [],
        activePair: pairInfo,
        sortedIndices: Array.from(sortedIndices),
        pointers: { i, j },
        activeLine: 20,
        comparison: `${valJ} ${needSwap ? ">" : "≤"} ${valJ1}`,
        annotation: `So sánh nums[${j}]=${valJ} (Xanh) và nums[${j + 1}]=${valJ1} (Tím) → ${
          needSwap ? "Cần đổi chỗ (Swap)" : "Đúng thứ tự (Giữ nguyên)"
        }`,
        context: `Pass ${i + 1}/${n - 1}: So sánh [${j}] (${valJ}) và [${j + 1}] (${valJ1}). ${
          needSwap ? "Chuẩn bị hoán đổi!" : "Giữ nguyên thứ tự."
        }`,
        swapCount: swapsTotal,
        compareCount: comparisonsTotal,
      });

      if (needSwap) {
        swapsTotal++;
        // 2. PHASE: SWAPPING (Hai phần tử trượt từ từ qua nhau theo quỹ đạo vòng cung)
        itemAtJ.pos = j + 1;
        itemAtJ1.pos = j;

        steps.push({
          stepIndex: steps.length,
          items: cloneItems(currentItems),
          nums: currentItems.slice().sort((a, b) => a.pos - b.pos).map((it) => it.value),
          type: "SWAPPING",
          status: "swapping",
          leftIndex: j,
          rightIndex: j + 1,
          comparingIndices: [],
          swappingIndices: [j, j + 1],
          activePair: pairInfo,
          sortedIndices: Array.from(sortedIndices),
          pointers: { i, j },
          activeLine: 22,
          comparison: `${valJ} ⮂ ${valJ1}`,
          annotation: `Đang di chuyển: Xanh (${valJ}) sang phải [${j + 1}], Tím (${valJ1}) sang trái [${j}]`,
          context: `Đang hoán đổi: Phần tử Xanh (${valJ}) trượt sang [${j + 1}], phần tử Tím (${valJ1}) trượt sang [${j}]...`,
          swapCount: swapsTotal,
          compareCount: comparisonsTotal,
        });

        // 3. PHASE: SWAP_SETTLED (Hai phần tử đáp xuống vị trí mới an toàn)
        steps.push({
          stepIndex: steps.length,
          items: cloneItems(currentItems),
          nums: currentItems.slice().sort((a, b) => a.pos - b.pos).map((it) => it.value),
          type: "SWAP_SETTLED",
          status: "swap_settled",
          leftIndex: j,
          rightIndex: j + 1,
          comparingIndices: [],
          swappingIndices: [],
          settledIndices: [j, j + 1],
          activePair: pairInfo,
          sortedIndices: Array.from(sortedIndices),
          pointers: { i, j },
          activeLine: 23,
          comparison: `[${j}]=${valJ1}, [${j + 1}]=${valJ}`,
          annotation: `Hoán đổi xong! Vị trí [${j}] là Tím (${valJ1}), vị trí [${j + 1}] là Xanh (${valJ})`,
          context: `Đã đổi chỗ hoàn tất: [${j}]=${valJ1}, [${j + 1}]=${valJ}.`,
          swapCount: swapsTotal,
          compareCount: comparisonsTotal,
        });
      }
    }

    // Element at n - 1 - i is now settled
    sortedIndices.add(n - 1 - i);

    steps.push({
      stepIndex: steps.length,
      items: cloneItems(currentItems),
      nums: currentItems.slice().sort((a, b) => a.pos - b.pos).map((it) => it.value),
      type: "PASS_END",
      status: "pass_end",
      leftIndex: -1,
      rightIndex: n - 1 - i,
      comparingIndices: [],
      swappingIndices: [],
      sortedIndices: Array.from(sortedIndices),
      pointers: { i, j: n - 1 - i },
      activeLine: 18,
      comparison: `Pass ${i + 1} xong`,
      annotation: `Phần tử tại index ${n - 1 - i} (${
        currentItems.find((it) => it.pos === n - 1 - i)?.value
      }) đã ở vị trí chính xác!`,
      context: `Kết thúc pass ${i + 1}. Phần tử lớn nhất còn lại đã nổi về cuối.`,
      swapCount: swapsTotal,
      compareCount: comparisonsTotal,
    });
  }

  // First element is also sorted by elimination
  sortedIndices.add(0);

  // FINAL STEP: COMPLETE
  steps.push({
    stepIndex: steps.length,
    items: cloneItems(currentItems),
    nums: currentItems.slice().sort((a, b) => a.pos - b.pos).map((it) => it.value),
    type: "SORTED",
    status: "complete",
    leftIndex: -1,
    rightIndex: -1,
    comparingIndices: [],
    swappingIndices: [],
    sortedIndices: Array.from({ length: n }, (_, k) => k),
    pointers: { i: n - 1, j: n - 1 },
    activeLine: 27,
    comparison: "Đã sắp xếp hoàn tất!",
    annotation: `Mảng đã sắp xếp tăng dần thành công sau ${comparisonsTotal} phép so sánh & ${swapsTotal} hoán đổi.`,
    context: `Toàn bộ mảng đã được sắp xếp tăng dần!`,
    swapCount: swapsTotal,
    compareCount: comparisonsTotal,
  });

  return steps;
}

/**
 * Convert a DebugStepsResponse from AST-tool into smooth animated steps.
 */
export function adaptStepsForVisualizer(response) {
  const steps = Array.isArray(response?.steps) ? response.steps : [];
  if (steps.length === 0) {
    return [];
  }

  // Filter steps that have sort visual state
  const sortSteps = steps.filter((step) => {
    const vs = (step.visualStates || [])[0];
    return vs && vs.sortFrame;
  });

  if (sortSteps.length === 0) {
    return [];
  }

  // Find initial array from first frame
  const firstValues = sortSteps[0].visualStates[0].array?.values || [];
  let currentItems = firstValues.map((v, idx) => ({
    id: idx,
    value: Number(v),
    pos: idx,
  }));

  let lastArrayStr = JSON.stringify(firstValues);
  const sortedIndices = new Set();
  let swapsTotal = 0;
  let comparisonsTotal = 0;

  return sortSteps.map((step, index) => {
    const vs = step.visualStates[0];
    const frame = vs.sortFrame;
    const comparison = frame.comparison;
    const arrayValues = (vs.array?.values || []).map((v) => Number(v));
    const pointers = vs.pointers || {};

    const passPointer = pointers.pass;
    const scanPointer = pointers.scan;
    const selectedPointer = pointers.selected;

    const leftIndex =
      passPointer?.index ?? selectedPointer?.index ?? comparison?.firstIndex ?? 0;
    const rightIndex =
      scanPointer?.index ?? comparison?.secondIndex ?? (leftIndex + 1);

    const phase = (frame.phase || "").toUpperCase();
    let status = "compare";
    if (phase.includes("SWAP") || phase.includes("WRITE") || phase.includes("INSERT")) {
      status = "swap";
      swapsTotal++;
    } else if (phase.includes("COMPLET") || phase.includes("SORTED")) {
      status = "complete";
    } else {
      comparisonsTotal++;
    }

    // Update item positions if array mutated
    const curArrayStr = JSON.stringify(arrayValues);
    if (curArrayStr !== lastArrayStr) {
      // Find matching items by value greedily or if swap
      if (status === "swap" && leftIndex >= 0 && rightIndex >= 0) {
        const itL = currentItems.find((it) => it.pos === leftIndex);
        const itR = currentItems.find((it) => it.pos === rightIndex);
        if (itL && itR) {
          itL.pos = rightIndex;
          itR.pos = leftIndex;
        }
      }
      lastArrayStr = curArrayStr;
    }

    if (frame.sortedRegion?.status === "VERIFIED_SORTED") {
      const start = frame.sortedRegion.start || 0;
      const end = frame.sortedRegion.endExclusive || arrayValues.length;
      for (let k = start; k < end; k++) {
        sortedIndices.add(k);
      }
    }

    const leftVal = comparison?.firstValue;
    const rightVal = comparison?.secondValue;
    const compText =
      comparison && leftVal !== undefined && rightVal !== undefined
        ? `${primitiveLabel(leftVal)} ${
            comparison.swapRequired || comparison.actionRequired ? ">" : "≤"
          } ${primitiveLabel(rightVal)}`
        : "";

    const annotation =
      comparison && comparison.firstIndex !== undefined
        ? `nums[${comparison.firstIndex}]=${primitiveLabel(leftVal)} ${
            comparison.swapRequired ? ">" : "≤"
          } nums[${comparison.secondIndex}]=${primitiveLabel(rightVal)} → ${
            comparison.swapRequired ? "Swap triggered" : "No swap"
          }`
        : step.code || "";

    const cloneItems = currentItems.map((it) => ({ ...it }));

    return {
      stepIndex: index,
      items: cloneItems,
      nums: arrayValues,
      leftIndex,
      rightIndex,
      comparingIndices: status === "compare" ? [leftIndex, rightIndex] : [],
      swappingIndices: status === "swap" ? [leftIndex, rightIndex] : [],
      sortedIndices: Array.from(sortedIndices),
      status,
      type: status.toUpperCase(),
      pointers: { i: leftIndex, j: rightIndex },
      activeLine: step.lineNumber || 20,
      comparison: compText || status,
      annotation,
      context: `Line ${step.lineNumber}: ${step.code?.trim() || ""}`,
      swapCount: swapsTotal,
      compareCount: comparisonsTotal,
      sortFrame: frame,
      visualState: vs,
      rawStep: step,
    };
  });
}
