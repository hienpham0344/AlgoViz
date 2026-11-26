/**
 * AlgoViz Side Panel & Code Bridge — Manifest V3 Service Worker
 *
 * Follows all Mandatory Manifest V3 rules:
 * - Uses chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true })
 * - Stores zero state in global variables (persists everything in chrome.storage.local)
 * - Uses async/await exclusively (no .then() chains)
 * - Provides visual confirmation (badge + toast) on context menu actions
 */

const DEFAULT_API_BASE_URL = "http://localhost:8080";
const CONTEXT_MENU_ID = "algoviz-trace-selection";

async function configureSidePanelBehavior() {
  try {
    await chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });
  } catch (error) {
    console.error("Failed to configure side panel behavior:", error);
  }
}

configureSidePanelBehavior();

export function detectLanguageFromCode(code = "") {
  const source = String(code);
  if (/public\s+class\s+|System\.out\.print|public\s+static\s+void\s+main|Arrays\.|List<|Map</.test(source)) {
    return "java";
  }
  if (/#include\s*<|std::|cout\s*<<|vector<\s*\w+\s*>/.test(source)) {
    return "cpp";
  }
  if (/def\s+\w+\s*\(|elif\s+|print\s*\(|import\s+collections/.test(source)) {
    return "python";
  }
  if (/function\s+\w+\s*\(|const\s+\w+\s*=\s*\(|console\.log\s*\(/.test(source)) {
    return "javascript";
  }
  return "java";
}

export function inferProblemIdFromUrl(url = "") {
  if (!url) return "";
  const leetcodeMatch = url.match(/leetcode\.com\/problems\/([a-z0-9-]+)/i);
  if (leetcodeMatch) {
    const slug = leetcodeMatch[1].toLowerCase();
    const knownSlugs = {
      "two-sum": "1",
      "valid-anagram": "242",
      "binary-search": "704"
    };
    return knownSlugs[slug] || slug;
  }
  const idParamMatch = url.match(/[?&](?:id|problem_id)=(\d+)/i);
  if (idParamMatch) {
    return idParamMatch[1];
  }
  return "";
}

chrome.runtime.onInstalled.addListener(async () => {
  await configureSidePanelBehavior();

  try {
    await chrome.contextMenus.removeAll();
    chrome.contextMenus.create({
      id: CONTEXT_MENU_ID,
      title: "Trace Selected Code in AlgoViz Side Panel",
      contexts: ["selection"]
    });
  } catch (error) {
    console.error("Failed to register context menu:", error);
  }

  try {
    const stored = await chrome.storage.local.get(["apiBaseUrl", "theme"]);
    const updates = {};
    if (!stored.apiBaseUrl) updates.apiBaseUrl = DEFAULT_API_BASE_URL;
    if (!stored.theme) updates.theme = "dark";
    if (Object.keys(updates).length > 0) {
      await chrome.storage.local.set(updates);
    }
  } catch (error) {
    console.error("Failed to initialize default storage:", error);
  }
});

chrome.contextMenus.onClicked.addListener(async (info, tab) => {
  if (info.menuItemId !== CONTEXT_MENU_ID || !info.selectionText || !tab?.id) {
    return;
  }

  try {
    // Invoke sidePanel.open immediately within the user gesture turn before awaiting storage
    const openPanelPromise = chrome.sidePanel.open({ windowId: tab.windowId });

    const code = info.selectionText.trim();
    const language = detectLanguageFromCode(code);
    const problemId = inferProblemIdFromUrl(tab.url || "");

    await chrome.storage.local.set({
      pendingTrace: {
        code,
        language,
        customInput: "",
        sourceUrl: tab.url || "",
        sourceTitle: tab.title || "Selected Code Snippet",
        problemId,
        extractor: "context-menu",
        timestamp: Date.now()
      }
    });

    await openPanelPromise;

    await chrome.action.setBadgeText({ text: "✓", tabId: tab.id });
    await chrome.action.setBadgeBackgroundColor({ color: "#10B981", tabId: tab.id });

    try {
      await chrome.tabs.sendMessage(tab.id, {
        type: "ALGOVIZ_SHOW_TOAST",
        message: `Loaded ${language.toUpperCase()} snippet into AlgoViz Side Panel`
      });
    } catch {
      // Content script may not be loaded on arbitrary pages; badge still confirms action
    }
  } catch (error) {
    console.error("Context menu trace action failed:", error);
  }
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  (async () => {
    try {
      if (message?.type === "ALGOVIZ_EXTRACT_FROM_ACTIVE_TAB") {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (!tab?.id) {
          sendResponse({ ok: false, error: "No active browser tab found." });
          return;
        }

        const problemId = inferProblemIdFromUrl(tab.url || "");

        try {
          const extracted = await chrome.tabs.sendMessage(tab.id, {
            type: "ALGOVIZ_EXTRACT_CODE"
          });
          if (extracted?.code) {
            const language = extracted.language || detectLanguageFromCode(extracted.code);
            await chrome.action.setBadgeText({ text: "✓", tabId: tab.id });
            await chrome.action.setBadgeBackgroundColor({ color: "#0EA5E9", tabId: tab.id });
            sendResponse({
              ok: true,
              code: extracted.code,
              language,
              customInput: extracted.customInput || "",
              problemTitle: extracted.problemTitle || tab.title || "",
              problemId: extracted.problemId || problemId,
              extractor: extracted.extractor || "content-script",
              sourceUrl: tab.url || ""
            });
            return;
          }
        } catch {
          // Fallback to programmatic DOM extraction if content script is not attached
        }

        const results = await chrome.scripting.executeScript({
          target: { tabId: tab.id },
          func: () => {
            const selection = window.getSelection()?.toString()?.trim();
            if (selection) return { code: selection, extractor: "selection" };

            const monacoLines = document.querySelectorAll(".monaco-editor .view-lines .view-line");
            if (monacoLines.length > 0) {
              return {
                code: Array.from(monacoLines)
                  .map((el) => el.innerText.replace(/\u00a0/g, " "))
                  .join("\n"),
                extractor: "monaco"
              };
            }

            const aceLines = document.querySelectorAll(".ace_editor .ace_text-layer .ace_line");
            if (aceLines.length > 0) {
              return {
                code: Array.from(aceLines)
                  .map((el) => el.innerText.replace(/\u00a0/g, " "))
                  .join("\n"),
                extractor: "ace"
              };
            }

            const preBlock = document.querySelector("pre code, pre");
            if (preBlock?.innerText?.trim()) {
              return { code: preBlock.innerText.trim(), extractor: "pre" };
            }

            return { code: "", extractor: "none" };
          }
        });

        const fallback = results?.[0]?.result || { code: "", extractor: "none" };
        if (!fallback.code) {
          sendResponse({
            ok: false,
            error: "No code detected in active tab. Highlight code or open a Wecode/LeetCode editor."
          });
          return;
        }

        const language = detectLanguageFromCode(fallback.code);
        sendResponse({
          ok: true,
          code: fallback.code,
          language,
          customInput: "",
          problemTitle: tab.title || "",
          problemId,
          extractor: fallback.extractor,
          sourceUrl: tab.url || ""
        });
        return;
      }

      if (message?.type === "ALGOVIZ_CLEAR_BADGE") {
        const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
        if (tab?.id) {
          await chrome.action.setBadgeText({ text: "", tabId: tab.id });
        }
        sendResponse({ ok: true });
        return;
      }

      sendResponse({ ok: false, error: "Unsupported message type." });
    } catch (error) {
      sendResponse({ ok: false, error: error?.message || "Service worker operation failed." });
    }
  })();

  return true;
});
