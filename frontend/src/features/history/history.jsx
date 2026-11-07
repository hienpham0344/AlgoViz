import { useEffect, useMemo, useState } from "react";
import { CustomSelect } from "../../components/index.js";
import { api } from "../../services/api.js";
import { paginationItems } from "../../services/pagination.js";
import { mapSubmission } from "../../services/submissions.js";
import ApiState from "../../components/ApiState.jsx";
import "./history.css";

const HISTORY_LANG_OPTIONS = [
  { value: "All", label: "All Languages" },
  { value: "C++", label: "C++" },
  { value: "JavaScript", label: "JavaScript" },
  { value: "Java", label: "Java" },
  { value: "Python", label: "Python" },
];

const HISTORY_COMPLETION_OPTIONS = [
  { value: "All", label: "All Status" },
  { value: "completed", label: "Completed", dotColor: "#137333" },
  { value: "incomplete", label: "Incomplete", dotColor: "#b06000" },
];



function HistoryHero({ onOpenStudio, onExport }) {
  return (
    <section className="history-hero">
      <div className="history-shell history-hero-content">
        <div className="history-hero-info">
          <nav className="history-breadcrumb" aria-label="Breadcrumb">
            <a href="#problems">AlgoViz</a>
            <span aria-hidden="true">/</span>
            <strong>Submission History</strong>
          </nav>
          <h1>Submission History</h1>
          <p>Review your judged solutions, verdicts, runtime, and language.</p>
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
            onClick={onOpenStudio}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
              <line x1="12" y1="5" x2="12" y2="19" />
              <line x1="5" y1="12" x2="19" y2="12" />
            </svg>
            <span>Open Code Studio</span>
          </button>
        </div>
      </div>
    </section>
  );
}

function HistoryStats({ totalSubmissions, successRate, topAlgo, topAlgoRuns, topLang, topLangPercent }) {
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
          <span className="history-stat-label">Total Submissions</span>
        </div>
        <div className="history-stat-val">
          <strong>{totalSubmissions}</strong>
          <span className="history-stat-pill history-stat-pill--emerald">Recorded</span>
        </div>
        <span className="history-stat-sub">Judged code submissions</span>
      </div>

      <div className="history-stat-card">
        <div className="history-stat-header">
          <span className="history-stat-icon history-stat-icon--cyan">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
              <polyline points="22 4 12 14.01 9 11.01" />
            </svg>
          </span>
          <span className="history-stat-label">Acceptance Rate</span>
        </div>
        <div className="history-stat-val">
          <strong>{successRate}%</strong>
          <span className="history-stat-pill history-stat-pill--cyan">Passing</span>
        </div>
        <span className="history-stat-sub">Accepted submissions</span>
      </div>

      <div className="history-stat-card">
        <div className="history-stat-header">
          <span className="history-stat-icon history-stat-icon--indigo">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          </span>
          <span className="history-stat-label">Most Submitted Problem</span>
        </div>
        <div className="history-stat-val">
          <strong style={{ fontSize: "16px" }} title={topAlgo}>{topAlgo}</strong>
          <span className="history-stat-pill history-stat-pill--indigo">{topAlgoRuns} {topAlgoRuns === 1 ? "submission" : "submissions"}</span>
        </div>
        <span className="history-stat-sub">Most frequently submitted</span>
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
          <span className="history-stat-pill history-stat-pill--amber">{topLangPercent}% of submissions</span>
        </div>
        <span className="history-stat-sub">Most used language</span>
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
          placeholder="Search by problem or language..."
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
  hasHistory,
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
  onViewProblem,
}) {
  return (
    <section className="history-table-card history-shell" aria-label="Submission history table">
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
              <th>Problem</th>
              <th>Runtime</th>
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
                  <p>{hasHistory ? "No matching submissions found" : "No submissions yet"}</p>
                  <small>{hasHistory ? "Try clearing your search query or selecting a different filter." : "Open a problem and submit a solution to build your history."}</small>
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
                      {row.topic && <span className="algo-topic">{row.topic}</span>}
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
                      {row.rawStatus}
                    </span>
                  </td>
                  <td style={{ textAlign: "right" }} onClick={(e) => e.stopPropagation()}>
                    <div className="algo-actions">
                      <button
                        type="button"
                        className="algo-btn-trace"
                        title="View this problem and its submissions"
                        onClick={() => onViewProblem(row)}
                      >
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                          <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                        </svg>
                        <span>View</span>
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
            {Math.min(page * pageSize, totalCount)} of {totalCount} submissions
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
          {paginationItems(page, pageCount).map((pNum, index) => pNum === "ellipsis" ? (
            <span key={`ellipsis-${index}`} aria-hidden="true">…</span>
          ) : (
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
      <span>© {new Date().getFullYear()} AlgoViz. All rights reserved.</span>
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
  const [historyList, setHistoryList] = useState([]);
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(5);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    let active = true;
    async function loadHistory() {
      setIsLoading(true);
      setLoadError("");
      try {
        const data = await api.getSubmissionHistory();
        if (!Array.isArray(data)) throw new Error("Invalid history response.");
        if (active) setHistoryList(data.map(mapSubmission));
      } catch (error) { if (active) setLoadError(error.message); }
      finally { if (active) setIsLoading(false); }
    }
    loadHistory();
    return () => { active = false; };
  }, [reloadKey]);

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

  const currentPage = Math.min(page, pageCount);

  // Current page's sliced slice
  const pagedRows = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return rows.slice(start, start + pageSize);
  }, [rows, currentPage, pageSize]);

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

  const removeRow = async (id) => {
    try { await api.deleteSubmission(id); }
    catch (error) { setLoadError(error.message); return; }
    setHistoryList((prev) => prev.filter((item) => item.id !== id));
    setSelected((current) => new Set([...current].filter((item) => item !== id)));
    setAnnouncement("Submission removed from history.");
  };

  const handleDeleteSelected = async () => {
    if (!selected.size) return;
    const ids = [...selected];
    const results = await Promise.allSettled(ids.map((id) => api.deleteSubmission(id)));
    const deleted = new Set(ids.filter((_, index) => results[index].status === "fulfilled"));
    setHistoryList((prev) => prev.filter((item) => !deleted.has(item.id)));
    setSelected((prev) => new Set([...prev].filter((id) => !deleted.has(id))));
    const failed = results.find((result) => result.status === "rejected");
    if (failed) setLoadError(failed.reason.message);
    setAnnouncement(`${deleted.size} submissions deleted.`);
  };

  const clearSelected = () => {
    setSelected(new Set());
    setAnnouncement("Selection cleared.");
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

  const handleViewProblem = (row) => {
    window.location.hash = `problem-details?id=${row.problemId}&tab=submissions`;
  };

  // Dynamic calculations for Hero stats
  const stats = useMemo(() => {
    if (!historyList || historyList.length === 0) {
      return {
        total: 0,
        successRate: 0,
        topAlgo: "None",
        topAlgoRuns: 0,
        topLang: "None",
        topLangPercent: 0,
      };
    }

    const total = historyList.length;
    const completedCount = historyList.filter((h) => h.completed).length;
    const successRate = Math.round((completedCount / total) * 100);

    const algoCounts = {};
    const langCounts = {};
    historyList.forEach((h) => {
      algoCounts[h.algorithm] = (algoCounts[h.algorithm] || 0) + 1;
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
      topLang,
      topLangPercent,
    };
  }, [historyList]);

  if (isLoading || (loadError && historyList.length === 0)) return (
    <main className="history-page" aria-busy={isLoading}>
      <ApiState loading={isLoading} error={loadError} onRetry={() => setReloadKey((key) => key + 1)} label="Loading submission history…" page />
    </main>
  );

  return (
    <main className="history-page" id="top">
      <HistoryHero
        onOpenStudio={() => {
          window.location.hash = "visualize";
        }}
        onExport={handleExport}
      />

      <ApiState loading={isLoading} error={loadError} onRetry={() => setReloadKey((key) => key + 1)} />
      <HistoryStats
        totalSubmissions={stats.total}
        successRate={stats.successRate}
        topAlgo={stats.topAlgo}
        topAlgoRuns={stats.topAlgoRuns}
        topLang={stats.topLang}
        topLangPercent={stats.topLangPercent}
      />

      <HistoryToolbar
        query={query}
        onQueryChange={(value) => { setQuery(value); setPage(1); }}
        language={language}
        onLanguageChange={(value) => { setLanguage(value); setPage(1); }}
        completion={completion}
        onCompletionChange={(value) => { setCompletion(value); setPage(1); }}
        newestFirst={newestFirst}
        onSortChange={() => { setNewestFirst((current) => !current); setPage(1); }}
        selectedCount={selected.size}
        onDeleteSelected={handleDeleteSelected}
        onClearSelected={clearSelected}
      />

      <HistoryTable
        rows={pagedRows}
        totalCount={totalCount}
        hasHistory={historyList.length > 0}
        page={currentPage}
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
        onViewProblem={handleViewProblem}
      />

      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>

      <HistoryFooter />
    </main>
  );
}

export default HistoryPage;
