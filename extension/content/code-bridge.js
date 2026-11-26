/**
 * AlgoViz Code Bridge — Content Script for Wecode UIT, LeetCode & Coding Portals
 *
 * Extracts source code, detected language, problem title/ID, and custom input
 * from Monaco, Ace, CodeMirror, or selected code blocks.
 * Batch-updates DOM overlays with requestAnimationFrame to never block the main thread.
 */

function detectLanguageOnPage(code = "") {
  const langBadge = document.querySelector(
    '[data-cy="lang-select"], button[id*="headlessui-listbox-button"], .select-language, select[name="language"]'
  );
  const badgeText = (langBadge?.value || langBadge?.innerText || "").toLowerCase();
  if (badgeText.includes("java") && !badgeText.includes("javascript")) return "java";
  if (badgeText.includes("python")) return "python";
  if (badgeText.includes("c++") || badgeText.includes("cpp")) return "cpp";
  if (badgeText.includes("javascript") || badgeText.includes("node")) return "javascript";

  if (/public\s+class\s+|System\.out\.print|public\s+static\s+void\s+main/.test(code)) return "java";
  if (/#include\s*<|std::|cout\s*<</.test(code)) return "cpp";
  if (/def\s+\w+\s*\(|print\s*\(/.test(code)) return "python";
  if (/function\s+\w+\s*\(|console\.log\s*\(/.test(code)) return "javascript";
  return "java";
}

function extractProblemMeta() {
  const titleEl = document.querySelector(
    '[data-cy="question-title"], .text-title-large, a[href*="/problems/"], .problem-title, h1, h2'
  );
  const problemTitle = titleEl?.innerText?.trim() || document.title || "Coding Problem";

  let problemId = "";
  const numberPrefix = problemTitle.match(/^(\d+)\.\s+/);
  if (numberPrefix) {
    problemId = numberPrefix[1];
  } else {
    const slugMatch = window.location.pathname.match(/\/problems\/([a-z0-9-]+)/i);
    if (slugMatch) {
      const slug = slugMatch[1].toLowerCase();
      const slugMap = {
        "two-sum": "1",
        "valid-anagram": "242",
        "binary-search": "704"
      };
      problemId = slugMap[slug] || slug;
    }
  }

  const customInputEl = document.querySelector(
    'textarea[data-cy="custom-testcase"], textarea[name*="input"], textarea#custom_input'
  );
  const customInput = customInputEl?.value?.trim() || "";

  return { problemTitle, problemId, customInput };
}

function extractEditorCode() {
  // 1. Explicit text selection on page
  const selection = window.getSelection()?.toString()?.trim();
  if (selection && selection.length > 10) {
    return { code: selection, extractor: "selection" };
  }

  // 2. Monaco Editor (LeetCode & modern Wecode UIT)
  const monacoLines = document.querySelectorAll(".monaco-editor .view-lines .view-line");
  if (monacoLines.length > 0) {
    const sortedLines = Array.from(monacoLines).sort((a, b) => {
      const topA = parseFloat(a.style.top || "0");
      const topB = parseFloat(b.style.top || "0");
      return topA - topB;
    });
    const code = sortedLines
      .map((line) => line.innerText.replace(/\u00a0/g, " "))
      .join("\n")
      .trim();
    if (code) return { code, extractor: "monaco" };
  }

  // 3. Ace Editor (Wecode UIT classic assignments)
  const aceLines = document.querySelectorAll(".ace_editor .ace_text-layer .ace_line");
  if (aceLines.length > 0) {
    const code = Array.from(aceLines)
      .map((line) => line.innerText.replace(/\u00a0/g, " "))
      .join("\n")
      .trim();
    if (code) return { code, extractor: "ace" };
  }

  // 4. CodeMirror 5 / 6
  const cmLines = document.querySelectorAll(".cm-content .cm-line, .CodeMirror-code .CodeMirror-line");
  if (cmLines.length > 0) {
    const code = Array.from(cmLines)
      .map((line) => line.innerText.replace(/\u00a0/g, " "))
      .join("\n")
      .trim();
    if (code) return { code, extractor: "codemirror" };
  }

  // 5. Fallback to textarea or pre/code blocks (GeeksforGeeks / tutorials)
  const codeTextarea = document.querySelector("textarea.editor, textarea[name='code'], pre code");
  const fallbackText = (codeTextarea?.value || codeTextarea?.innerText || "").trim();
  if (fallbackText) {
    return { code: fallbackText, extractor: "code-block" };
  }

  return { code: "", extractor: "none" };
}

function showNonBlockingToast(message) {
  requestAnimationFrame(() => {
    const existing = document.getElementById("algoviz-bridge-toast");
    if (existing) existing.remove();

    const toast = document.createElement("div");
    toast.id = "algoviz-bridge-toast";
    toast.setAttribute("role", "status");
    toast.setAttribute("aria-live", "polite");
    toast.textContent = `⚡ ${message}`;

    Object.assign(toast.style, {
      position: "fixed",
      bottom: "22px",
      right: "22px",
      zIndex: "2147483647",
      background: "#0B1120",
      color: "#F8FAFC",
      border: "1px solid #38BDF8",
      borderRadius: "10px",
      padding: "10px 16px",
      fontFamily: "ui-sans-serif, system-ui, -apple-system, sans-serif",
      fontSize: "13px",
      fontWeight: "600",
      boxShadow: "0 12px 28px rgba(2, 6, 23, 0.55)",
       pointerEvents: "none",
      transition: "opacity 0.2s ease"
    });

    document.body.appendChild(toast);
    setTimeout(() => {
      requestAnimationFrame(() => {
        toast.style.opacity = "0";
        setTimeout(() => toast.remove(), 220);
      });
    }, 2400);
  });
}

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message?.type === "ALGOVIZ_EXTRACT_CODE") {
    const { code, extractor } = extractEditorCode();
    const { problemTitle, problemId, customInput } = extractProblemMeta();
    const language = detectLanguageOnPage(code);
    if (code) {
      showNonBlockingToast(`Extracted ${language.toUpperCase()} code (${extractor}) to AlgoViz`);
    }
    sendResponse({
      code,
      extractor,
      language,
      problemTitle,
      problemId,
      customInput
    });
    return false;
  }

  if (message?.type === "ALGOVIZ_SHOW_TOAST") {
    showNonBlockingToast(message.message || "Sent to AlgoViz Side Panel");
    sendResponse({ ok: true });
    return false;
  }

  return false;
});
