/**
 * AlgoViz Side Panel & Code Bridge — Controller
 *
 * Connects directly to AlgoViz's Spring Boot REST API:
 * - GET  /api/ast/health
 * - POST /api/ast/trace         (JavaParser AST + JDI step-by-step trace & visualStates)
 * - POST /api/submissions/run   (Multi-language execution: Python, C++, JavaScript, Java)
 * - POST /api/ai/analyze        (AST-assisted static complexity & bug analysis)
 * - Deep-links into /#visualize and /#problem-details?id=<id>
 */

const DEFAULT_API_BASE_URL = "http://localhost:8080";

const PRESET_SOLUTIONS = {
  "bubble-sort": {
    problemId: "",
    language: "java",
    customInput: "",
    title: "Bubble Sort Sample",
    code: `public class BubbleSortSample {
  public static void main(String[] args) {
    int[] nums = {5, 2, 8, 1, 9, 3};
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
}`
  },
  "binary-search": {
    problemId: "704",
    language: "java",
    customInput: "",
    title: "#704 Binary Search",
    code: `public class Solution {
  public static void main(String[] args) {
    int[] nums = {-1, 0, 3, 5, 9, 12};
    int target = 9;
    System.out.println(search(nums, target));
  }

  public static int search(int[] nums, int target) {
    int low = 0;
    int high = nums.length - 1;
    while (low <= high) {
      int mid = low + (high - low) / 2;
      if (nums[mid] == target) {
        return mid;
      } else if (nums[mid] < target) {
        low = mid + 1;
      } else {
        high = mid - 1;
      }
    }
    return -1;
  }
}`
  },
  "two-sum": {
    problemId: "1",
    language: "java",
    customInput: "",
    title: "#1 Two Sum",
    code: `public class Solution {
  public static void main(String[] args) {
    int[] nums = {2, 7, 11, 15};
    int target = 9;
    int[] res = twoSum(nums, target);
    System.out.println(res[0] + ", " + res[1]);
  }

  public static int[] twoSum(int[] nums, int target) {
    for (int i = 0; i < nums.length; i++) {
      for (int j = i + 1; j < nums.length; j++) {
        if (nums[i] + nums[j] == target) {
          return new int[] {i, j};
        }
      }
    }
    return new int[] {-1, -1};
  }
}`
  },
  "valid-anagram": {
    problemId: "242",
    language: "java",
    customInput: "",
    title: "#242 Valid Anagram",
    code: `public class Solution {
  public static void main(String[] args) {
    System.out.println(isAnagram("anagram", "nagaram"));
  }

  public static boolean isAnagram(String s, String t) {
    if (s.length() != t.length()) return false;
    int[] counts = new int[26];
    for (int i = 0; i < s.length(); i++) {
      counts[s.charAt(i) - 'a']++;
      counts[t.charAt(i) - 'a']--;
    }
    for (int count : counts) {
      if (count != 0) return false;
    }
    return true;
  }
}`
  }
};

let currentTraceSteps = [];
let currentStepIndex = 0;
let playIntervalId = null;

async function getApiBaseUrl() {
  const { apiBaseUrl = DEFAULT_API_BASE_URL } = await chrome.storage.local.get("apiBaseUrl");
  return String(apiBaseUrl || DEFAULT_API_BASE_URL).replace(/\/$/, "");
}

function setBanner(message, isError = false) {
  const banner = document.getElementById("status-banner");
  if (!message) {
    banner.hidden = true;
    banner.textContent = "";
    return;
  }
  banner.hidden = false;
  banner.dataset.error = isError ? "true" : "false";
  banner.textContent = message;
}

async function checkAstHealth() {
  const dot = document.getElementById("health-dot");
  const label = document.getElementById("health-label");
  const baseUrl = await getApiBaseUrl();
  document.getElementById("input-api-url").value = baseUrl;

  try {
    const response = await fetch(`${baseUrl}/api/ast/health`, {
      method: "GET",
      signal: AbortSignal.timeout(3000)
    });
    const data = await response.json();
    const isUp = response.ok && data?.status === "UP";
    dot.dataset.status = isUp ? "up" : "down";
    dot.title = isUp ? `Connected to ${data.engine} at ${baseUrl}` : `AST engine unavailable at ${baseUrl}`;
    label.textContent = isUp ? `${data.engine} · Ready` : "AST Engine Degraded";
    return isUp;
  } catch {
    dot.dataset.status = "down";
    dot.title = `Cannot reach AlgoViz at ${baseUrl}`;
    label.textContent = "Backend Offline (check port 8080)";
    return false;
  }
}

async function applyStoredTheme() {
  const { theme = "dark" } = await chrome.storage.local.get("theme");
  document.documentElement.setAttribute("data-theme", theme);
}

async function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme") || "dark";
  const next = current === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", next);
  await chrome.storage.local.set({ theme: next });
}

function applyPendingPayload(payload) {
  if (!payload?.code) return;
  document.getElementById("code-input").value = payload.code;
  if (payload.language) {
    document.getElementById("select-lang").value = payload.language;
  }
  if (payload.customInput !== undefined) {
    document.getElementById("custom-input").value = payload.customInput;
  }
  if (payload.problemId !== undefined) {
    document.getElementById("input-problem-id").value = payload.problemId;
  }
  const badge = document.getElementById("source-badge");
  badge.textContent = payload.extractor ? `From ${payload.extractor}` : "Loaded";
}

async function loadInitialState() {
  await applyStoredTheme();
  await checkAstHealth();

  const { pendingTrace } = await chrome.storage.local.get("pendingTrace");
  if (pendingTrace?.code) {
    applyPendingPayload(pendingTrace);
  } else {
    applyPreset("bubble-sort");
  }
}

function applyPreset(presetKey) {
  const preset = PRESET_SOLUTIONS[presetKey];
  if (!preset) return;
  document.getElementById("code-input").value = preset.code;
  document.getElementById("select-lang").value = preset.language;
  document.getElementById("custom-input").value = preset.customInput;
  document.getElementById("input-problem-id").value = preset.problemId;
  document.getElementById("source-badge").textContent = preset.title;
  setBanner("");
}

async function handleGrabFromEditor() {
  setBanner("Extracting code from active browser tab...", false);
  const response = await chrome.runtime.sendMessage({
    type: "ALGOVIZ_EXTRACT_FROM_ACTIVE_TAB"
  });

  if (!response?.ok || !response.code) {
    setBanner(
      response?.error || "Could not detect code in active tab. Highlight code on the page and try again.",
      true
    );
    return;
  }

  applyPendingPayload(response);
  await chrome.storage.local.set({
    pendingTrace: {
      code: response.code,
      language: response.language,
      customInput: response.customInput || "",
      problemId: response.problemId || "",
      sourceTitle: response.problemTitle || "",
      extractor: response.extractor || "tab",
      timestamp: Date.now()
    }
  });
  setBanner(`Extracted ${response.language.toUpperCase()} code (${response.extractor}).`, false);
}

function renderAlgorithmHints(hints = [], catalogVersion = "intermediate-v1") {
  const panel = document.getElementById("hints-panel");
  const list = document.getElementById("hints-list");
  const versionBadge = document.getElementById("catalog-version");
  list.replaceChildren();

  if (!Array.isArray(hints) || hints.length === 0) {
    panel.hidden = true;
    return;
  }

  panel.hidden = false;
  versionBadge.textContent = catalogVersion || "intermediate-v1";

  hints.forEach((hint) => {
    const chip = document.createElement("div");
    chip.className = "sp-hint-chip";
    const pct = typeof hint.confidence === "number" ? `${Math.round(hint.confidence * 100)}%` : "";
    const plan = hint.visualPlan ? ` · ${hint.visualPlan}` : "";
    chip.textContent = `${hint.type || "algorithm"} ${pct}${plan}`.trim();
    list.appendChild(chip);
  });
}

function extractArrayAndPointersFromStep(step) {
  const visualState = Array.isArray(step?.visualStates) ? step.visualStates[0] : null;
  const variables = Array.isArray(step?.variables) ? step.variables : [];

  let arrayName = "";
  let arrayValues = [];
  let statusText = visualState?.status || step?.statementKind || "OBSERVED";
  const pointerMap = new Map(); // index -> [names]
  const scalarsList = [];

  if (visualState?.array && Array.isArray(visualState.array.values)) {
    arrayName = visualState.array.variableName || "array";
    arrayValues = visualState.array.values;
  } else {
    const arrayVar = variables.find(
      (v) =>
        !String(v.variableName || "").startsWith("__algoviz") &&
        v.variableName !== "args" &&
        (v.visualType === "array" || Array.isArray(v.visualValue))
    );
    if (arrayVar) {
      arrayName = arrayVar.variableName;
      if (Array.isArray(arrayVar.visualValue)) {
        arrayValues = arrayVar.visualValue;
      }
    }
  }

  // Extract pointers from visualState.pointers
  if (visualState?.pointers && typeof visualState.pointers === "object") {
    Object.values(visualState.pointers).forEach((ptr) => {
      if (ptr && Number.isInteger(ptr.index) && ptr.index >= 0) {
        const name = ptr.variableName || "ptr";
        if (!pointerMap.has(ptr.index)) pointerMap.set(ptr.index, []);
        pointerMap.get(ptr.index).push(name);
      }
    });
  }

  // If no visualState pointers, infer from integer local variables named i, j, k, low, high, mid, left, right
  if (pointerMap.size === 0 && arrayValues.length > 0) {
    const pointerNames = new Set(["i", "j", "k", "low", "high", "mid", "left", "right", "l", "r"]);
    variables.forEach((v) => {
      if (pointerNames.has(v.variableName)) {
        const parsed = typeof v.visualValue === "number" ? v.visualValue : Number(v.runtimeValue);
        if (Number.isInteger(parsed) && parsed >= 0 && parsed < arrayValues.length) {
          if (!pointerMap.has(parsed)) pointerMap.set(parsed, []);
          pointerMap.get(parsed).push(v.variableName);
        }
      }
    });
  }

  // Extract scalars from visualState.scalars
  if (visualState?.scalars && typeof visualState.scalars === "object") {
    Object.values(visualState.scalars).forEach((sc) => {
      if (sc && sc.variableName) {
        scalarsList.push(`${sc.variableName} = ${sc.value}`);
      }
    });
  }

  return { arrayName, arrayValues, statusText, pointerMap, scalarsList };
}

function renderTraceStep(index) {
  if (!Array.isArray(currentTraceSteps) || currentTraceSteps.length === 0) return;
  const boundedIndex = Math.max(0, Math.min(index, currentTraceSteps.length - 1));
  currentStepIndex = boundedIndex;
  const step = currentTraceSteps[boundedIndex];

  document.getElementById("step-counter").textContent = `Step ${boundedIndex + 1} / ${currentTraceSteps.length}`;
  const scrubber = document.getElementById("step-scrubber");
  scrubber.max = String(currentTraceSteps.length - 1);
  scrubber.value = String(boundedIndex);

  const methodCtx = step.context?.methodName ? `${step.context.methodName}()` : "main()";
  const codeText = (step.code || step.statementKind || "statement").trim();
  document.getElementById("line-banner").textContent = `Line ${step.lineNumber} · ${methodCtx} → ${codeText}`;

  const { arrayName, arrayValues, statusText, pointerMap, scalarsList } = extractArrayAndPointersFromStep(step);
  const arrayStage = document.getElementById("array-stage");
  const arrayBars = document.getElementById("array-bars");
  const scalarsRow = document.getElementById("scalars-row");
  arrayBars.replaceChildren();
  scalarsRow.replaceChildren();

  document.getElementById("visual-status-badge").textContent = statusText;

  if (arrayValues.length > 0) {
    arrayStage.hidden = false;
    document.getElementById("array-title").textContent = `${arrayName}[${arrayValues.length}]`;
    const numericVals = arrayValues.map((v) => Math.abs(Number(v)) || 1);
    const maxVal = Math.max(...numericVals, 1);
    const isSwap = String(statusText).toUpperCase().includes("SWAP");

    arrayValues.forEach((val, idx) => {
      const col = document.createElement("div");
      col.className = "sp-bar-col";

      const bar = document.createElement("div");
      bar.className = "sp-bar";
      const heightPx = Math.max(20, Math.round(((Math.abs(Number(val)) || 1) / maxVal) * 76));
      bar.style.height = `${heightPx}px`;
      bar.textContent = String(val);

      const activePtrs = pointerMap.get(idx) || [];
      if (activePtrs.length > 0) {
        if (isSwap) {
          bar.dataset.swap = "true";
        } else {
          bar.dataset.active = "true";
        }
      }

      const idxLabel = document.createElement("span");
      idxLabel.className = "sp-bar-idx";
      idxLabel.textContent = `[${idx}]`;

      const ptrStack = document.createElement("div");
      ptrStack.className = "sp-pointer-stack";
      activePtrs.forEach((ptrName) => {
        const tag = document.createElement("span");
        tag.className = "sp-ptr-tag";
        tag.textContent = ptrName;
        ptrStack.appendChild(tag);
      });

      col.appendChild(bar);
      col.appendChild(idxLabel);
      col.appendChild(ptrStack);
      arrayBars.appendChild(col);
    });
  } else {
    arrayStage.hidden = true;
  }

  scalarsList.forEach((scalarText) => {
    const pill = document.createElement("span");
    pill.className = "sp-scalar-pill";
    pill.textContent = scalarText;
    scalarsRow.appendChild(pill);
  });

  // Render Local Variables table
  const varsTable = document.getElementById("vars-table");
  varsTable.replaceChildren();
  const variables = (step.variables || []).filter(
    (v) => v.variableName && !v.variableName.startsWith("__algoviz")
  );

  if (variables.length === 0) {
    const emptyRow = document.createElement("div");
    emptyRow.className = "sp-var-row";
    emptyRow.textContent = "No local variables in scope at this step.";
    varsTable.appendChild(emptyRow);
  } else {
    variables.forEach((v) => {
      const row = document.createElement("div");
      row.className = "sp-var-row";

      const left = document.createElement("div");
      const nameSpan = document.createElement("span");
      nameSpan.className = "sp-var-name";
      nameSpan.textContent = v.variableName;

      const typeSpan = document.createElement("span");
      typeSpan.className = "sp-var-type";
      typeSpan.textContent = v.declaredType ? `(${v.declaredType})` : "";

      left.appendChild(nameSpan);
      left.appendChild(typeSpan);

      const valSpan = document.createElement("span");
      valSpan.className = "sp-var-val";
      const displayVal = Array.isArray(v.visualValue)
        ? `[${v.visualValue.join(", ")}]`
        : v.runtimeValue ?? String(v.visualValue ?? "null");
      valSpan.textContent = displayVal;
      valSpan.title = displayVal;

      row.appendChild(left);
      row.appendChild(valSpan);
      varsTable.appendChild(row);
    });
  }
}

function stopAutoPlay() {
  if (playIntervalId !== null) {
    clearInterval(playIntervalId);
    playIntervalId = null;
  }
  const playBtn = document.getElementById("btn-play");
  if (playBtn) playBtn.textContent = "▶ Play";
}

function toggleAutoPlay() {
  if (!currentTraceSteps.length) return;
  if (playIntervalId !== null) {
    stopAutoPlay();
    return;
  }
  if (currentStepIndex >= currentTraceSteps.length - 1) {
    renderTraceStep(0);
  }
  document.getElementById("btn-play").textContent = "⏸ Pause";
  playIntervalId = setInterval(() => {
    if (currentStepIndex < currentTraceSteps.length - 1) {
      renderTraceStep(currentStepIndex + 1);
    } else {
      stopAutoPlay();
    }
  }, 550);
}

async function handleJavaTrace() {
  stopAutoPlay();
  const code = document.getElementById("code-input").value.trim();
  const language = document.getElementById("select-lang").value;
  const customInput = document.getElementById("custom-input").value.trim();

  if (!code) {
    setBanner("Please enter or grab source code before tracing.", true);
    return;
  }

  if (language !== "java") {
    setBanner("Live AST+JDI step-by-step trace requires Java. Switching to Multi-Language Runner...", false);
    await handleRunCode();
    return;
  }

  const baseUrl = await getApiBaseUrl();
  setBanner("Launching JavaParser AST + JDI debugger trace...", false);

  try {
    const response = await fetch(`${baseUrl}/api/ast/trace`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ code, language: "java", customInput }),
      signal: AbortSignal.timeout(25000)
    });

    const data = await response.json().catch(() => null);
    if (!response.ok || !Array.isArray(data?.steps) || data.steps.length === 0) {
      const problemDetail = Array.isArray(data?.problems) && data.problems.length ? `: ${data.problems[0]}` : "";
      throw new Error((data?.message || data?.error?.message || "Failed to generate Java trace") + problemDetail);
    }

    currentTraceSteps = data.steps;
    renderAlgorithmHints(data.algorithmHints, data.catalogVersion);

    document.getElementById("trace-player").hidden = false;
    document.getElementById("runner-panel").hidden = true;
    setBanner(`Captured ${data.steps.length} JDI execution steps.`, false);
    renderTraceStep(0);
  } catch (error) {
    setBanner(error.message || "Could not connect to AlgoViz AST trace engine.", true);
  }
}

async function handleRunCode() {
  stopAutoPlay();
  const code = document.getElementById("code-input").value.trim();
  const language = document.getElementById("select-lang").value;
  const customInput = document.getElementById("custom-input").value;

  if (!code) {
    setBanner("Please enter or grab code to run.", true);
    return;
  }

  const baseUrl = await getApiBaseUrl();
  setBanner(`Running ${language.toUpperCase()} code on AlgoViz runner...`, false);

  try {
    const response = await fetch(`${baseUrl}/api/submissions/run`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ language, code, customInput }),
      signal: AbortSignal.timeout(20000)
    });

    const data = await response.json().catch(() => null);
    if (!response.ok || !data) {
      throw new Error(data?.message || `Execution request failed (${response.status})`);
    }

    const panel = document.getElementById("runner-panel");
    const statusBadge = document.getElementById("runner-status-badge");
    const metricsEl = document.getElementById("runner-metrics");
    const outputEl = document.getElementById("runner-output");

    panel.hidden = false;
    statusBadge.textContent = data.status || "Completed";
    const timeStr = typeof data.executionTimeMs === "number" ? `${data.executionTimeMs.toFixed(1)} ms` : "N/A";
    const memStr = typeof data.memoryUsageMb === "number" ? `${data.memoryUsageMb.toFixed(1)} MB` : "N/A";
    metricsEl.textContent = `Time: ${timeStr} · Memory: ${memStr}`;

    outputEl.textContent = data.errorMessage
      ? `${data.output ? data.output + "\n\n" : ""}Error:\n${data.errorMessage}`
      : data.output || "(No stdout output)";
    setBanner(`Execution finished with status: ${data.status || "OK"}`, data.status && data.status !== "Accepted");
  } catch (error) {
    setBanner(error.message || "Code execution failed.", true);
  }
}

async function handleAnalyzeComplexity() {
  const code = document.getElementById("code-input").value.trim();
  const language = document.getElementById("select-lang").value;

  if (!code) {
    setBanner("Please enter code to analyze complexity.", true);
    return;
  }

  const baseUrl = await getApiBaseUrl();
  setBanner("Analyzing time/space complexity & algorithm patterns...", false);

  try {
    const response = await fetch(`${baseUrl}/api/ai/analyze`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        code,
        language,
        problemContext: document.getElementById("source-badge").textContent || "Side Panel"
      }),
      signal: AbortSignal.timeout(15000)
    });

    const data = await response.json().catch(() => null);
    if (!response.ok || !data) {
      throw new Error(data?.message || "Complexity analysis failed.");
    }

    const panel = document.getElementById("analysis-panel");
    panel.hidden = false;
    document.getElementById("pill-time").textContent = `Time: ${data.timeComplexity || "N/A"}`;
    document.getElementById("pill-space").textContent = `Space: ${data.spaceComplexity || "N/A"}`;
    document.getElementById("analysis-summary").textContent = data.summary || "";

    const listsEl = document.getElementById("analysis-lists");
    listsEl.replaceChildren();

    const suggestions = Array.isArray(data.optimizationSuggestions) ? data.optimizationSuggestions : [];
    const bugs = Array.isArray(data.potentialBugs) ? data.potentialBugs : [];

    [...suggestions.map((s) => `💡 ${s}`), ...bugs.map((b) => `⚠️ ${b}`)].forEach((text) => {
      const item = document.createElement("div");
      item.className = "sp-analysis-item";
      item.textContent = text;
      listsEl.appendChild(item);
    });

    setBanner("Complexity analysis complete.", false);
  } catch (error) {
    setBanner(error.message || "Could not analyze code complexity.", true);
  }
}

chrome.storage.onChanged.addListener((changes, areaName) => {
  if (areaName === "local" && changes.pendingTrace?.newValue) {
    applyPendingPayload(changes.pendingTrace.newValue);
    setBanner("Loaded code selection from active tab.", false);
  }
});

document.addEventListener("DOMContentLoaded", async () => {
  await loadInitialState();

  document.getElementById("btn-theme").addEventListener("click", toggleTheme);

  document.getElementById("btn-toggle-settings").addEventListener("click", () => {
    const drawer = document.getElementById("settings-drawer");
    drawer.hidden = !drawer.hidden;
  });

  document.getElementById("btn-save-url").addEventListener("click", async () => {
    const rawUrl = document.getElementById("input-api-url").value.trim() || DEFAULT_API_BASE_URL;
    const cleanUrl = rawUrl.replace(/\/$/, "");
    await chrome.storage.local.set({ apiBaseUrl: cleanUrl });
    const isUp = await checkAstHealth();
    document.getElementById("settings-status").textContent = isUp
      ? `Connected to ${cleanUrl}`
      : `Saved ${cleanUrl}, but /api/ast/health is unreachable.`;
  });

  document.getElementById("btn-open-studio").addEventListener("click", async () => {
    const baseUrl = await getApiBaseUrl();
    await chrome.tabs.create({ url: `${baseUrl}/#visualize` });
  });

  document.getElementById("btn-open-problem").addEventListener("click", async () => {
    const baseUrl = await getApiBaseUrl();
    const problemId = document.getElementById("input-problem-id").value.trim() || "1";
    await chrome.tabs.create({
      url: `${baseUrl}/#problem-details?id=${encodeURIComponent(problemId)}`
    });
  });

  document.getElementById("select-preset").addEventListener("change", (event) => {
    const key = event.target.value;
    if (key) {
      applyPreset(key);
      event.target.value = "";
    }
  });

  document.getElementById("btn-grab-code").addEventListener("click", handleGrabFromEditor);
  document.getElementById("btn-trace").addEventListener("click", handleJavaTrace);
  document.getElementById("btn-run").addEventListener("click", handleRunCode);
  document.getElementById("btn-analyze").addEventListener("click", handleAnalyzeComplexity);

  document.getElementById("btn-first").addEventListener("click", () => {
    stopAutoPlay();
    renderTraceStep(0);
  });
  document.getElementById("btn-prev").addEventListener("click", () => {
    stopAutoPlay();
    renderTraceStep(currentStepIndex - 1);
  });
  document.getElementById("btn-play").addEventListener("click", toggleAutoPlay);
  document.getElementById("btn-next").addEventListener("click", () => {
    stopAutoPlay();
    renderTraceStep(currentStepIndex + 1);
  });
  document.getElementById("btn-last").addEventListener("click", () => {
    stopAutoPlay();
    renderTraceStep(currentTraceSteps.length - 1);
  });
  document.getElementById("step-scrubber").addEventListener("input", (event) => {
    stopAutoPlay();
    renderTraceStep(Number(event.target.value));
  });
});
