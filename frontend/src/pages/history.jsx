import { useEffect, useMemo, useState } from "react";
import { BadgeProblemStatus, NavigationBar } from "../components/index.js";
import { api } from "../services/api.js";
import "./history.css";

const PAGE_SIZE = 5;

const FEATURES = [
  [
    "trace",
    "Deterministic Traces",
    "Recorded step-by-step visual graph frames and heap layouts are immutable once benchmarked.",
  ],
  [
    "sync",
    "Sync to Sandbox",
    "Jump directly from any history run back into the live dynamic debugger with previous inputs loaded.",
  ],
  [
    "export",
    "CSV / JSON Export",
    "Export history benchmarks formatted for academic research or interview prep tracking sheets.",
  ],
];

function HistoryIcon({ name }) {
  return (
    <span className={`history-icon history-icon--${name}`} aria-hidden="true" />
  );
}

function HistoryHero({ onNewAnalysis, onDeleteSelected, selectedCount }) {
  return (
    <section className="history-hero">
      <div
        className="history-ambient history-ambient--violet"
        aria-hidden="true"
      />
      <div
        className="history-ambient history-ambient--mint"
        aria-hidden="true"
      />
      <div
        className="history-ambient history-ambient--amber"
        aria-hidden="true"
      />
      <div className="history-shell history-hero-content">
        <div>
          <div className="history-breadcrumb">
            <span>⌂ Home</span>
            <b>/</b>
            <strong>Analysis History</strong>
          </div>
          <span className="history-status">
            <i /> TRACE AUDIT LOGS ACTIVE
          </span>
          <h1>Analysis History</h1>
          <p>
            Review, inspect, or manage your previous code inspections, runtime
            benchmarks, and visual simulations.
          </p>
        </div>
        <div className="history-hero-actions">
          <button
            className="history-button history-button--primary"
            type="button"
            onClick={onNewAnalysis}
          >
            <HistoryIcon name="play" /> New Analysis
          </button>
          <button
            className="history-button history-button--muted"
            type="button"
            onClick={onDeleteSelected}
            disabled={selectedCount === 0}
          >
            <HistoryIcon name="delete" /> Delete Selected ({selectedCount})
          </button>
        </div>
      </div>
    </section>
  );
}

function StatCard({ label, icon, children, className = "" }) {
  return (
    <article className={`history-stat-card ${className}`.trim()}>
      <div className="history-stat-heading">
        <span>{label}</span>
        <span className="history-stat-icon">
          <HistoryIcon name={icon} />
        </span>
      </div>
      {children}
    </article>
  );
}

function HistoryStats() {
  return (
    <section
      className="history-stats history-shell"
      aria-label="History overview"
    >
      <StatCard
        label="Total Analyses"
        icon="total"
        className="history-stat-card--total"
      >
        <div className="history-stat-value">
          <strong>128</strong>
          <span>+14 this week</span>
        </div>
        <div className="history-progress">
          <i style={{ width: "78%" }} />
        </div>
      </StatCard>
      <StatCard
        label="Top Algorithm"
        icon="bolt"
        className="history-stat-card--algorithm"
      >
        <strong className="history-stat-title">Bubble Sort</strong>
        <div className="history-stat-detail">
          <b>34 runs</b>
          <span>Sorting Array Engine</span>
        </div>
      </StatCard>
      <StatCard
        label="Success Rate"
        icon="success"
        className="history-stat-card--success"
      >
        <div className="history-stat-value">
          <strong>94.2%</strong>
          <span>121 passing</span>
        </div>
        <div className="history-progress">
          <i style={{ width: "94.2%" }} />
        </div>
      </StatCard>
      <StatCard
        label="Languages Used"
        icon="language"
        className="history-stat-card--languages"
      >
        <div className="history-stat-value">
          <strong>4</strong>
          <span>Runtimes active</span>
        </div>
        <div className="history-language-pills">
          <b>C++</b>
          <b>Java</b>
          <b>Python</b>
          <b>TS</b>
        </div>
      </StatCard>
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
  onExport,
  onClear,
}) {
  return (
    <section
      className="history-toolbar history-shell"
      aria-label="History controls"
    >
      <label className="history-search">
        <HistoryIcon name="search" />
        <span className="sr-only">Search history</span>
        <input
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="Search history..."
        />
        {query && (
          <button
            type="button"
            onClick={() => onQueryChange("")}
            aria-label="Clear search"
          >
            ×
          </button>
        )}
      </label>
      <div className="history-toolbar-controls">
        <select
          className="history-filter-button"
          value={language}
          onChange={(event) => onLanguageChange(event.target.value)}
          aria-label="Filter by language"
        >
          <option value="All">⚙ Filter</option>
          <option value="C++">C++</option>
          <option value="Java">Java</option>
          <option value="Python">Python</option>
        </select>
        <select
          className="history-filter-button"
          value={completion}
          onChange={(event) => onCompletionChange(event.target.value)}
          aria-label="Filter by completion status"
        >
          <option value="All">Completion: All</option>
          <option value="completed">Completed</option>
          <option value="incomplete">Not completed</option>
        </select>
        <button
          className="history-meta-filter"
          type="button"
          onClick={onSortChange}
        >
          Lang: {language} <i>|</i> Sort:{" "}
          <strong>{newestFirst ? "Newest First" : "Oldest First"}</strong>
        </button>
        <button
          className="history-button history-button--muted"
          type="button"
          onClick={onClear}
        >
          <HistoryIcon name="delete" /> Clear Selected
        </button>
        <button
          className="history-button history-button--export"
          type="button"
          onClick={onExport}
        >
          <HistoryIcon name="export" /> Export History
        </button>
      </div>
    </section>
  );
}

function LanguageBadge({ language }) {
  return (
    <span
      className={`history-language history-language--${language.toLowerCase().replace("++", "pp")}`}
    >
      <i />
      {language}
    </span>
  );
}

function HistoryTable({
  rows,
  selected,
  onSelect,
  onDelete,
  onInspect,
  page,
  pageCount,
  totalCount,
  onPageChange,
}) {
  const firstRow = totalCount === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
  const lastRow = Math.min(page * PAGE_SIZE, totalCount);

  return (
    <section
      className="history-table-card history-shell"
      aria-label="Analysis history table"
    >
      <div className="history-table-scroll">
        <div className="history-table-header">
          <span>Algorithm</span>
          <span>Language</span>
          <span>Date</span>
          <span>Status</span>
          <span>Action</span>
        </div>
        {rows.map((row) => (
          <article
            className={`history-table-row ${selected.has(row.id) ? "is-selected" : ""}`}
            key={row.id}
            onClick={() => onSelect(row.id)}
            aria-selected={selected.has(row.id)}
          >
            <div className="history-algorithm">
              <span
                className={`history-algorithm-icon history-algorithm-icon--${row.icon}`}
              >
                <HistoryIcon name={row.icon} />
              </span>
              <div>
                <strong>{row.algorithm}</strong>
                <small>
                  <em>{row.topic}</em>
                  <code>{row.complexity}</code>
                </small>
              </div>
            </div>
            <div>
              <LanguageBadge language={row.language} />
            </div>
            <div className="history-date">{row.date}</div>
            <div>
              <BadgeProblemStatus
                status={row.completed ? "completed" : "incomplete"}
              />
            </div>
            <div className="history-row-actions">
              <button
                type="button"
                aria-label={`Inspect ${row.algorithm}`}
                onClick={(event) => {
                  event.stopPropagation();
                  onInspect(row);
                }}
              >
                <HistoryIcon name="view" />
              </button>
              <button
                type="button"
                aria-label={`Delete ${row.algorithm}`}
                onClick={(event) => {
                  event.stopPropagation();
                  onDelete(row.id);
                }}
              >
                <HistoryIcon name="delete" />
              </button>
            </div>
          </article>
        ))}
      </div>
      <div className="history-table-footer">
        <span>
          Showing {firstRow}-{lastRow} of {totalCount} analyses
        </span>
        <nav aria-label="History pages">
          <button
            type="button"
            aria-label="Previous page"
            disabled={page === 1}
            onClick={() => onPageChange(page - 1)}
          >
            ‹
          </button>
          {Array.from({ length: pageCount }, (_, index) => index + 1).map(
            (pageNumber) => (
              <button
                className={pageNumber === page ? "is-current" : ""}
                type="button"
                key={pageNumber}
                onClick={() => onPageChange(pageNumber)}
              >
                {pageNumber}
              </button>
            ),
          )}
          <button
            type="button"
            aria-label="Next page"
            disabled={page === pageCount}
            onClick={() => onPageChange(page + 1)}
          >
            ›
          </button>
        </nav>
      </div>
    </section>
  );
}

function HistoryFeatures() {
  return (
    <section className="history-features history-shell">
      {FEATURES.map(([icon, title, copy]) => (
        <article key={title}>
          <HistoryIcon name={icon} />
          <div>
            <strong>{title}</strong>
            <p>{copy}</p>
          </div>
        </article>
      ))}
    </section>
  );
}

function HistoryFooter() {
  return (
    <footer className="history-footer">
      <div className="history-footer-grid history-shell">
        <div>
          <a className="history-footer-brand" href="#top">
            AlgoViz
          </a>
          <p>
            Visual algorithm practice and complexity breakdown engine designed
            for technical problem solvers and engineers.
          </p>
          <strong>INTERACTIVE VISUAL RUNTIME</strong>
        </div>
        <div>
          <h2>Curriculum</h2>
          <a href="#data-structures">Data Structures</a>
          <a href="#dynamic-programming">Dynamic Programming</a>
          <a href="#graph-traversals">Graph Traversals</a>
          <a href="#sorting">Sorting &amp; Searching</a>
        </div>
        <div>
          <h2>Platform</h2>
          <a href="#visualizer">Visual Complexity Inspector</a>
          <a href="#execution">Execution History</a>
          <a href="#sandboxes">Algorithm Sandboxes</a>
          <a href="#problems">Problem Catalog</a>
        </div>
        <div>
          <h2>Community</h2>
          <a href="#discord">Developer Discord</a>
          <a href="#submissions">Code Submissions</a>
          <a href="#benchmarks">Algorithm Benchmarks</a>
          <a href="#docs">Open Source Docs</a>
        </div>
      </div>
      <div className="history-footer-legal history-shell">
        <span>© 2025 AlgoMaster / AlgoViz. All rights reserved.</span>
        <nav>
          <a href="#privacy">Privacy Policy</a>
          <a href="#terms">Terms of Service</a>
          <a href="#security">Security Status</a>
        </nav>
      </div>
    </footer>
  );
}

function HistoryPage({ userName = "Your name" }) {
  const [query, setQuery] = useState("");
  const [language, setLanguage] = useState("All");
  const [completion, setCompletion] = useState("All");
  const [newestFirst, setNewestFirst] = useState(true);
  const [selected, setSelected] = useState(new Set());
  const [announcement, setAnnouncement] = useState("");
  const [historyList, setHistoryList] = useState([]);
  const [page, setPage] = useState(1);

  useEffect(() => {
    async function loadHistory() {
      try {
        const historyData = await api.getSubmissionHistory();
        if (Array.isArray(historyData)) {
          const mapped = historyData.map((item) => ({
            id: item.id,
            problemId: item.problemId,
            persisted: true,
            algorithm: item.problemTitle || "Problem #" + item.problemId,
            topic: item.status || "Evaluated",
            complexity: item.executionTimeMs ? `${item.executionTimeMs}ms` : "O(N)",
            language: ({ java: "Java", python: "Python", py: "Python", cpp: "C++", "c++": "C++", javascript: "JavaScript", js: "JavaScript" })[item.language?.toLowerCase()] || item.language || "Unknown",
            date: item.submittedAt ? new Date(item.submittedAt).toLocaleDateString() : "Today",
            icon: "sort",
            completed: ["accepted", "completed", "success"].includes(
              String(item.status || "").toLowerCase(),
            ),
          }));
          setHistoryList(mapped);
        }
      } catch (err) {
        console.warn("Could not load backend submission history:", err);
      }
    }
    loadHistory();
  }, []);

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
  const pageCount = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
  const currentPage = Math.min(page, pageCount);
  const visibleRows = rows.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );
  const toggleSelected = (id) =>
    setSelected((current) => {
      const next = new Set(current);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  const clearSelected = () => {
    setSelected(new Set());
    setAnnouncement("Selected analyses cleared.");
  };

  const deleteRows = async (ids) => {
    const idSet = new Set(ids);
    if (idSet.size === 0) {
      setAnnouncement("Select at least one analysis to delete.");
      return;
    }

    const persistedIds = historyList
      .filter((row) => idSet.has(row.id) && row.persisted)
      .map((row) => row.id);

    try {
      await Promise.all(persistedIds.map((id) => api.deleteSubmission(id)));
      setHistoryList((current) => current.filter((row) => !idSet.has(row.id)));
      setSelected(
        (current) => new Set([...current].filter((id) => !idSet.has(id))),
      );
      setAnnouncement(
        `${idSet.size} analysis record${idSet.size === 1 ? "" : "s"} deleted.`,
      );
    } catch (error) {
      setAnnouncement(`Delete failed: ${error.message}`);
    }
  };

  const exportHistory = () => {
    const escapeCsv = (value) =>
      `"${String(value ?? "").replaceAll('"', '""')}"`;
    const csvRows = [
      ["Algorithm", "Topic", "Status", "Complexity", "Language", "Date"],
      ...rows.map((row) => [
        row.algorithm,
        row.topic,
        row.completed ? "Completed" : "Not completed",
        row.complexity,
        row.language,
        row.date,
      ]),
    ];
    const blob = new Blob(
      [csvRows.map((row) => row.map(escapeCsv).join(",")).join("\n")],
      { type: "text/csv;charset=utf-8" },
    );
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "algoviz-history.csv";
    link.click();
    URL.revokeObjectURL(url);
    setAnnouncement(`${rows.length} analysis records exported.`);
  };

  return (
    <main className="history-page" id="top">
      <div className="history-nav-shell">
        <NavigationBar
          active="History"
          userName={userName}
          className="history-navigation"
        />
      </div>
      <HistoryHero
        onNewAnalysis={() => {
          window.location.hash = "analyze";
        }}
        onDeleteSelected={() => deleteRows([...selected])}
        selectedCount={selected.size}
      />
      <HistoryStats />
      <HistoryToolbar
        query={query}
        onQueryChange={(value) => {
          setQuery(value);
          setPage(1);
        }}
        language={language}
        onLanguageChange={(value) => {
          setLanguage(value);
          setPage(1);
        }}
        completion={completion}
        onCompletionChange={(value) => {
          setCompletion(value);
          setPage(1);
        }}
        newestFirst={newestFirst}
        onSortChange={() => {
          setNewestFirst((current) => !current);
          setPage(1);
        }}
        onClear={clearSelected}
        onExport={exportHistory}
      />
      <HistoryTable
        rows={visibleRows}
        selected={selected}
        onSelect={toggleSelected}
        onDelete={(id) => deleteRows([id])}
        onInspect={(row) => {
          window.location.hash = row.problemId
            ? `problem-details?id=${row.problemId}`
            : "analyze";
        }}
        page={currentPage}
        pageCount={pageCount}
        totalCount={rows.length}
        onPageChange={setPage}
      />
      <HistoryFeatures />
      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>
      <HistoryFooter />
    </main>
  );
}

export default HistoryPage;
