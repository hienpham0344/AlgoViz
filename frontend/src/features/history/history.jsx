import { useEffect, useMemo, useState } from "react";
import { NavigationBar, CustomSelect } from "../../components/index.js";
import { api } from "../../services/api.js";
import "./history.css";

const HISTORY_LANG_OPTIONS = [
  { value: "All", label: "All Languages" },
  { value: "C++", label: "C++" },
  { value: "Java", label: "Java" },
  { value: "Python", label: "Python" },
];

const HISTORY_COMPLETION_OPTIONS = [
  { value: "All", label: "All Status" },
  { value: "completed", label: "Completed", dotColor: "#137333" },
  { value: "incomplete", label: "Incomplete", dotColor: "#b06000" },
];

const DEFAULT_HISTORY = [
  {
    id: "hist-1",
    algorithm: "Bubble Sort Visualizer",
    problemId: 912,
    topic: "Sorting Algorithms",
    complexity: "O(n²)",
    language: "C++",
    date: "Aug 20, 2025",
    completed: true,
  },
  {
    id: "hist-2",
    algorithm: "Binary Search Recursive",
    problemId: 704,
    topic: "Searching Arrays",
    complexity: "O(log n)",
    language: "Java",
    date: "Aug 19, 2025",
    completed: true,
  },
  {
    id: "hist-3",
    algorithm: "Quick Sort (Lomuto Partition)",
    problemId: 912,
    topic: "Divide & Conquer",
    complexity: "O(n log n)",
    language: "Python",
    date: "Aug 18, 2025",
    completed: true,
  },
  {
    id: "hist-4",
    algorithm: "Merge Sort Recursive",
    problemId: 912,
    topic: "Divide & Conquer",
    complexity: "O(n log n)",
    language: "C++",
    date: "Aug 16, 2025",
    completed: false,
  },
  {
    id: "hist-5",
    algorithm: "Two Sum Hash Map",
    problemId: 1,
    topic: "Hash Tables",
    complexity: "O(n)",
    language: "Python",
    date: "Aug 15, 2025",
    completed: true,
  },
  {
    id: "hist-6",
    algorithm: "3Sum Two Pointers",
    problemId: 15,
    topic: "Two Pointers",
    complexity: "O(n²)",
    language: "Python",
    date: "Aug 14, 2025",
    completed: true,
  },
  {
    id: "hist-7",
    algorithm: "Longest Substring Sliding Window",
    problemId: 3,
    topic: "Sliding Window",
    complexity: "O(n)",
    language: "Java",
    date: "Aug 12, 2025",
    completed: true,
  },
  {
    id: "hist-8",
    algorithm: "Valid Parentheses Stack",
    problemId: 20,
    topic: "Stack",
    complexity: "O(n)",
    language: "C++",
    date: "Aug 10, 2025",
    completed: true,
  },
  {
    id: "hist-9",
    algorithm: "LRU Cache Doubly Linked List",
    problemId: 146,
    topic: "Design",
    complexity: "O(1)",
    language: "Python",
    date: "Aug 08, 2025",
    completed: true,
  },
  {
    id: "hist-10",
    algorithm: "Climbing Stairs DP",
    problemId: 70,
    topic: "Dynamic Programming",
    complexity: "O(n)",
    language: "Python",
    date: "Aug 06, 2025",
    completed: false,
  },
  {
    id: "hist-11",
    algorithm: "Kth Largest Element (Min-Heap)",
    problemId: 215,
    topic: "Heap",
    complexity: "O(n log k)",
    language: "Java",
    date: "Aug 04, 2025",
    completed: true,
  },
  {
    id: "hist-12",
    algorithm: "Invert Binary Tree BFS",
    problemId: 226,
    topic: "Trees",
    complexity: "O(n)",
    language: "C++",
    date: "Aug 01, 2025",
    completed: true,
  },
];

function HistoryHero({ onNewAnalysis, onExport }) {
  return (
    <section className="history-hero">
      <div className="history-shell history-hero-content">
        <div className="history-hero-info">
          <nav className="history-breadcrumb" aria-label="Breadcrumb">
            <a href="#problems">AlgoViz</a>
            <span aria-hidden="true">/</span>
            <strong>Execution History</strong>
          </nav>
          <h1>Execution History</h1>
          <p>Review past algorithm runs, runtime complexity benchmarks, and visual execution traces.</p>
        </div>
        <div className="history-hero-actions">
          <button
            className="history-btn-secondary"
            type="button"
            onClick={onExport}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            <span>Export Data</span>
          </button>
          <button
            className="history-btn-primary"
            type="button"
            onClick={onNewAnalysis}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>New Analysis</span>
          </button>
        </div>
      </div>
    </section>
  );
}

function HistoryStats({
  totalAnalyses = 12,
  successRate = 92,
  topAlgo = "Bubble Sort Visualizer",
  topAlgoRuns = 2,
  topTopic = "Sorting Algorithms",
  topLang = "Python",
  topLangPercent = 42,
}) {
  return (
    <section className="history-stats history-shell" aria-label="History overview">
      <div className="history-stat-card">
        <div className="history-stat-header">
          <span className="history-stat-icon history-stat-icon--emerald">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <rect x="2" y="6" width="20" height="12" rx="2" />
              <line x1="8" y1="6" x2="8" y2="18" />
              <line x1="16" y1="6" x2="16" y2="18" />
            </svg>
          </span>
          <span className="history-stat-label">Total Analyses</span>
        </div>
        <div className="history-stat-val">
          <strong>{totalAnalyses}</strong>
          <span className="history-stat-pill history-stat-pill--emerald">Active Session</span>
        </div>
        <span className="history-stat-sub">Recorded algorithmic runs</span>
      </div>

      <div className="history-stat-card">
        <div className="history-stat-header">
          <span className="history-stat-icon history-stat-icon--cyan">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </span>
          <span className="history-stat-label">Success Rate</span>
        </div>
        <div className="history-stat-val">
          <strong>{successRate}%</strong>
          <span className="history-stat-pill history-stat-pill--cyan">Passing</span>
        </div>
        <span className="history-stat-sub">Completed assertion suites</span>
      </div>

      <div className="history-stat-card">
        <div className="history-stat-header">
          <span className="history-stat-icon history-stat-icon--indigo">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          </span>
          <span className="history-stat-label">Top Algorithm</span>
        </div>
        <div className="history-stat-val">
          <strong style={{ fontSize: "16px" }} title={topAlgo}>{topAlgo}</strong>
          <span className="history-stat-pill history-stat-pill--indigo">{topAlgoRuns} {topAlgoRuns === 1 ? "run" : "runs"}</span>
        </div>
        <span className="history-stat-sub">{topTopic || "Most analyzed algorithm"}</span>
      </div>

      <div className="history-stat-card">
        <div className="history-stat-header">
          <span className="history-stat-icon history-stat-icon--amber">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
            </svg>
          </span>
          <span className="history-stat-label">Primary Language</span>
        </div>
        <div className="history-stat-val">
          <strong>{topLang}</strong>
          <span className="history-stat-pill history-stat-pill--amber">{topLangPercent}% of runs</span>
        </div>
        <span className="history-stat-sub">Most practiced runtime environment</span>
      </div>
    </section>
  );
}

function HistoryToolbar({
  query,
  onQueryChange,
  language,
  onLanguageChange,
  completion,
  onCompletionChange,
  newestFirst,
  onSortChange,
  selectedCount,
  onDeleteSelected,
  onClearSelected,
}) {
  return (
    <section className="history-toolbar history-shell" aria-label="History controls">
      <div className="history-toolbar-search">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
        <input
          value={query}
          onChange={(e) => onQueryChange(e.target.value)}
          placeholder="Search by algorithm, topic, or language..."
          aria-label="Search history"
        />
        {query && (
          <button
            type="button"
            className="history-search-clear-btn"
            onClick={() => onQueryChange("")}
            aria-label="Clear search query"
          >
            ✕
          </button>
        )}
      </div>

      <div className="history-toolbar-controls">
        <CustomSelect
          value={language}
          onChange={onLanguageChange}
          options={HISTORY_LANG_OPTIONS}
          ariaLabel="Filter by language"
        />
        <CustomSelect
          value={completion}
          onChange={onCompletionChange}
          options={HISTORY_COMPLETION_OPTIONS}
          ariaLabel="Filter by completion status"
        />
        <button
          className="history-sort-btn"
          type="button"
          onClick={onSortChange}
          title="Toggle chronological order"
        >
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="12" y1="5" x2="12" y2="19" />
            <polyline points="19 12 12 19 5 12" />
          </svg>
          <span>{newestFirst ? "Newest First" : "Oldest First"}</span>
        </button>

        {selectedCount > 0 && (
          <div className="history-batch-actions">
            <button
              className="history-btn-danger"
              type="button"
              onClick={onDeleteSelected}
            >
              Delete ({selectedCount})
            </button>
            <button
              className="history-btn-ghost"
              type="button"
              onClick={onClearSelected}
            >
              Clear
            </button>
          </div>
        )}
      </div>
    </section>
  );
}

function HistoryTable({
  rows,
  totalCount,
  page = 1,
  pageSize = 5,
  pageCount = 1,
  onPageChange,
  onPageSizeChange,
  selected,
  onSelect,
  onSelectAll,
  isAllSelected,
  onDelete,
  onInspect,
}) {
  return (
    <section className="history-table-card history-shell" aria-label="Analysis history table">
      <div className="history-table-wrapper">
        <table className="history-table">
          <thead>
            <tr>
              <th style={{ width: "42px", textAlign: "center" }}>
                <input
                  type="checkbox"
                  checked={isAllSelected}
                  onChange={onSelectAll}
                  aria-label="Select all rows"
                />
              </th>
              <th>Algorithm</th>
              <th>Complexity</th>
              <th>Language</th>
              <th>Date</th>
              <th>Status</th>
              <th style={{ textAlign: "right" }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 ? (
              <tr>
                <td colSpan={7} className="history-empty-td">
                  <p>No matching analyses found</p>
                  <small>Try clearing your search query or selecting a different filter.</small>
                </td>
              </tr>
            ) : (
              rows.map((row) => (
                <tr
                  key={row.id}
                  className={selected.has(row.id) ? "is-row-selected" : ""}
                  onClick={() => onSelect(row.id)}
                >
                  <td style={{ textAlign: "center" }} onClick={(e) => e.stopPropagation()}>
                    <input
                      type="checkbox"
                      checked={selected.has(row.id)}
                      onChange={() => onSelect(row.id)}
                      aria-label={`Select ${row.algorithm}`}
                    />
                  </td>
                  <td>
                    <div className="algo-cell">
                      <strong className="algo-title">{row.algorithm}</strong>
                      <span className="algo-topic">{row.topic}</span>
                    </div>
                  </td>
                  <td>
                    <code className="algo-complexity">{row.complexity}</code>
                  </td>
                  <td>
                    <span className={`algo-lang algo-lang--${row.language.toLowerCase().replace("++", "pp")}`}>
                      {row.language}
                    </span>
                  </td>
                  <td className="algo-date">{row.date}</td>
                  <td>
                    <span className={`algo-status algo-status--${row.completed ? "done" : "incomplete"}`}>
                      {row.completed ? "✓ Done" : "● Incomplete"}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }} onClick={(e) => e.stopPropagation()}>
                    <div className="algo-actions">
                      <button
                        type="button"
                        className="algo-btn-trace"
                        title="Open interactive trace in Visualizer"
                        onClick={() => onInspect(row)}
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                        </svg>
                        <span>Trace</span>
                      </button>
                      <button
                        type="button"
                        className="algo-btn-delete"
                        title="Delete from history"
                        onClick={() => onDelete(row.id)}
                        aria-label={`Delete ${row.algorithm}`}
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="3 6 5 6 21 6" />
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="history-table-footer">
        <div className="history-footer-left">
          <span>
            Showing {totalCount > 0 ? (page - 1) * pageSize + 1 : 0}–
            {Math.min(page * pageSize, totalCount)} of {totalCount} analyses
          </span>
          <div className="history-page-size-selector">
            <label htmlFor="history-page-size">Per page:</label>
            <select
              id="history-page-size"
              value={pageSize}
              onChange={(e) => onPageSizeChange(Number(e.target.value))}
              aria-label="Items per page"
            >
              <option value={5}>5</option>
              <option value={10}>10</option>
              <option value={20}>20</option>
            </select>
          </div>
        </div>

        <nav aria-label="History pages" className="history-pagination">
          <button
            type="button"
            className="history-nav-btn"
            disabled={page <= 1}
            onClick={() => onPageChange(page - 1)}
            aria-label="Previous page"
            title="Previous page (Đi lùi)"
          >
            ‹ Prev
          </button>
          {Array.from({ length: pageCount }, (_, i) => i + 1).map((pNum) => (
            <button
              key={pNum}
              type="button"
              className={page === pNum ? "is-current" : ""}
              onClick={() => onPageChange(pNum)}
              aria-label={`Go to page ${pNum}`}
              aria-current={page === pNum ? "page" : undefined}
            >
              {pNum}
            </button>
          ))}
          <button
            type="button"
            className="history-nav-btn"
            disabled={page >= pageCount}
            onClick={() => onPageChange(page + 1)}
            aria-label="Next page"
            title="Next page (Đi tiếp)"
          >
            Next ›
          </button>
        </nav>
      </div>
    </section>
  );
}

function HistoryFooter() {
  return (
    <footer className="history-simple-footer history-shell">
      <span>© 2025 AlgoViz. All rights reserved.</span>
      <div className="history-footer-links">
        <a href="#problems">Problems</a>
        <a href="#visualize">Visualize</a>
        <a href="#learn">Learn</a>
      </div>
    </footer>
  );
}

function HistoryPage({ userName = "Developer", isAuthenticated = false }) {
  const [query, setQuery] = useState("");
  const [language, setLanguage] = useState("All");
  const [completion, setCompletion] = useState("All");
  const [newestFirst, setNewestFirst] = useState(true);
  const [selected, setSelected] = useState(new Set());
  const [announcement, setAnnouncement] = useState("");
  const [historyList, setHistoryList] = useState(DEFAULT_HISTORY);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);

  useEffect(() => {
    async function loadHistory() {
      try {
        const historyData = await api.getSubmissionHistory();
        if (Array.isArray(historyData) && historyData.length > 0) {
          const formatLanguage = (lang) => {
            const l = (lang || "").toLowerCase();
            if (l.includes("py")) return "Python";
            if (l.includes("js") || l.includes("javascript")) return "JavaScript";
            if (l.includes("cpp") || l.includes("c++")) return "C++";
            if (l.includes("java")) return "Java";
            return lang || "Python";
          };

          const formatDate = (isoStr) => {
            if (!isoStr) return "Just now";
            try {
              return new Date(isoStr).toLocaleDateString("en-US", {
                month: "short",
                day: "2-digit",
                year: "numeric",
              });
            } catch {
              return "Recent";
            }
          };

          const mapped = historyData.map((item, idx) => ({
            id: item.id || `sub-${idx}`,
            algorithm: item.problemTitle || `Problem #${item.problemId || 1}`,
            problemId: item.problemId || 1,
            topic: item.topic || "Algorithm & Logic",
            complexity: item.executionTimeMs != null ? `${item.executionTimeMs.toFixed(1)}ms` : "O(n)",
            language: formatLanguage(item.language),
            date: formatDate(item.submittedAt),
            completed: item.status === "Accepted" || item.status === "ACCEPTED",
            rawStatus: item.status,
          }));
          setHistoryList(mapped);
        }
      } catch (err) {
        console.warn("Using default history", err);
      }
    }
    loadHistory();
  }, []);

  // Reset to page 1 whenever search, language, completion, or order changes
  useEffect(() => {
    setPage(1);
  }, [query, language, completion, newestFirst]);

  const rows = useMemo(() => {
    const filtered = historyList.filter(
      (row) =>
        `${row.algorithm} ${row.topic} ${row.language}`
          .toLowerCase()
          .includes(query.toLowerCase()) &&
        (language === "All" || row.language === language) &&
        (completion === "All" ||
          (completion === "completed" ? row.completed : !row.completed)),
    );
    return newestFirst ? filtered : [...filtered].reverse();
  }, [completion, historyList, language, newestFirst, query]);

  const totalCount = rows.length;
  const pageCount = Math.max(1, Math.ceil(totalCount / pageSize));

  // Auto-clamp page if it exceeds pageCount (e.g. after deleting items)
  useEffect(() => {
    if (page > pageCount) {
      setPage(pageCount);
    }
  }, [page, pageCount]);

  // Current page's sliced slice
  const pagedRows = useMemo(() => {
    const start = (page - 1) * pageSize;
    return rows.slice(start, start + pageSize);
  }, [rows, page, pageSize]);

  const isAllSelected =
    pagedRows.length > 0 && pagedRows.every((r) => selected.has(r.id));

  const toggleSelected = (id) => {
    setSelected((current) => {
      const next = new Set(current);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const handleSelectAll = () => {
    if (isAllSelected) {
      setSelected((prev) => {
        const next = new Set(prev);
        pagedRows.forEach((r) => next.delete(r.id));
        return next;
      });
    } else {
      setSelected((prev) => {
        const next = new Set(prev);
        pagedRows.forEach((r) => next.add(r.id));
        return next;
      });
    }
  };

  const removeRow = (id) => {
    setHistoryList((prev) => prev.filter((item) => item.id !== id));
    setSelected((current) => new Set([...current].filter((item) => item !== id)));
    setAnnouncement("Analysis removed from the selected history.");
  };

  const handleDeleteSelected = () => {
    if (selected.size === 0) return;
    setHistoryList((prev) => prev.filter((item) => !selected.has(item.id)));
    setSelected(new Set());
    setAnnouncement("Selected analyses deleted.");
  };

  const clearSelected = () => {
    setSelected(new Set());
    setAnnouncement("Selected analyses cleared.");
  };

  const handleExport = () => {
    const dataStr =
      "data:text/json;charset=utf-8," +
      encodeURIComponent(JSON.stringify(historyList, null, 2));
    const downloadAnchor = document.createElement("a");
    downloadAnchor.setAttribute("href", dataStr);
    downloadAnchor.setAttribute(
      "download",
      `algoviz_history_${new Date().toISOString().slice(0, 10)}.json`,
    );
    document.body.appendChild(downloadAnchor);
    downloadAnchor.click();
    downloadAnchor.remove();
    setAnnouncement("History exported as JSON.");
  };

  const handleInspectTrace = (row) => {
    window.location.hash = `problem-details?id=${row.problemId || 1}&tab=visualizer`;
  };

  // Dynamic calculations for Hero stats
  const stats = useMemo(() => {
    if (!historyList || historyList.length === 0) {
      return {
        total: 0,
        successRate: 100,
        topAlgo: "None",
        topAlgoRuns: 0,
        topTopic: "None",
        topLang: "Python",
        topLangPercent: 0,
      };
    }

    const total = historyList.length;
    const completedCount = historyList.filter((h) => h.completed).length;
    const successRate = Math.round((completedCount / total) * 100);

    const algoCounts = {};
    const algoTopics = {};
    const langCounts = {};
    historyList.forEach((h) => {
      algoCounts[h.algorithm] = (algoCounts[h.algorithm] || 0) + 1;
      algoTopics[h.algorithm] = h.topic;
      langCounts[h.language] = (langCounts[h.language] || 0) + 1;
    });

    let topAlgo = historyList[0].algorithm;
    let maxAlgoCount = 0;
    Object.entries(algoCounts).forEach(([algo, count]) => {
      if (count > maxAlgoCount) {
        topAlgo = algo;
        maxAlgoCount = count;
      }
    });

    let topLang = historyList[0].language;
    let maxLangCount = 0;
    Object.entries(langCounts).forEach(([lang, count]) => {
      if (count > maxLangCount) {
        topLang = lang;
        maxLangCount = count;
      }
    });
    const topLangPercent = Math.round((maxLangCount / total) * 100);

    return {
      total,
      successRate,
      topAlgo,
      topAlgoRuns: maxAlgoCount,
      topTopic: algoTopics[topAlgo] || "Sorting Algorithms",
      topLang,
      topLangPercent,
    };
  }, [historyList]);

  return (
    <main className="history-page" id="top">
      <HistoryHero
        onNewAnalysis={() => {
          window.location.hash = "visualize";
        }}
        onExport={handleExport}
      />

      <HistoryStats
        totalAnalyses={stats.total}
        successRate={stats.successRate}
        topAlgo={stats.topAlgo}
        topAlgoRuns={stats.topAlgoRuns}
        topTopic={stats.topTopic}
        topLang={stats.topLang}
        topLangPercent={stats.topLangPercent}
      />

      <HistoryToolbar
        query={query}
        onQueryChange={setQuery}
        language={language}
        onLanguageChange={setLanguage}
        completion={completion}
        onCompletionChange={setCompletion}
        newestFirst={newestFirst}
        onSortChange={() => setNewestFirst((current) => !current)}
        selectedCount={selected.size}
        onDeleteSelected={handleDeleteSelected}
        onClearSelected={clearSelected}
      />

      <HistoryTable
        rows={pagedRows}
        totalCount={totalCount}
        page={page}
        pageSize={pageSize}
        pageCount={pageCount}
        onPageChange={(newPage) => setPage(newPage)}
        onPageSizeChange={(newSize) => {
          setPageSize(newSize);
          setPage(1);
        }}
        selected={selected}
        onSelect={toggleSelected}
        onSelectAll={handleSelectAll}
        isAllSelected={isAllSelected}
        onDelete={removeRow}
        onInspect={handleInspectTrace}
      />

      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>

      <HistoryFooter />
    </main>
  );
}

export default HistoryPage;
