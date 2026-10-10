import { useEffect, useMemo, useRef, useState } from "react";
import { BadgeProblemStatus, NavigationBar } from "../../components/index.js";
import { PROBLEMS } from "./problemData.js";
import "./problems.css";

const TOTAL_PROBLEMS = 125;
const PAGE_SIZE = 8;
const PAGE_COUNT = 5;

const DIFFICULTIES = ["All", "Easy", "Medium", "Hard"];
const STATUSES = ["All", "Solved", "To Do"];
const TOPICS = [
  { label: "Sorting", count: 18 },
  { label: "Searching", count: 14 },
  { label: "Graph Algorithms", count: 24 },
  { label: "Tree & DFS / BFS", count: 29 },
  { label: "Dynamic Programming", count: 32 },
];
const TECHNIQUES = [
  "Array",
  "Recursion",
  "Two Pointers",
  "Sliding Window",
  "Greedy",
  "Backtracking",
];
const QUICK_FILTERS = [
  { id: "all", label: "All Topics" },
  { id: "popular", label: "🔥 Popular" },
  { id: "interview", label: "🏆 Top Interview 150" },
  { id: "recent", label: "⏱ Recent" },
  { id: "editorial", label: "💡 Editorial Solutions" },
];
const SORT_OPTIONS = [
  { value: "difficulty", label: "Difficulty" },
  { value: "acceptance", label: "Acceptance" },
  { value: "id", label: "Problem ID" },
];

function getDifficultyOrder(difficulty) {
  return { Easy: 1, Medium: 2, Hard: 3 }[difficulty] ?? 0;
}

function filterProblems(problems, filters) {
  const normalizedQuery = filters.query.trim().toLowerCase();

  return problems.filter((problem) => {
    const matchesQuery = normalizedQuery
      ? [problem.id, problem.title, ...problem.topics]
          .join(" ")
          .toLowerCase()
          .includes(normalizedQuery)
      : true;
    const matchesDifficulty =
      filters.difficulty === "All" || problem.difficulty === filters.difficulty;
    const matchesStatus =
      filters.status === "All" || problem.status === filters.status;
    const matchesTopics =
      filters.topics.length === 0 ||
      filters.topics.some((topic) => problem.topics.includes(topic));
    const matchesQuickFilter =
      filters.quickFilter === "all" || Boolean(problem[filters.quickFilter]);

    return (
      matchesQuery &&
      matchesDifficulty &&
      matchesStatus &&
      matchesTopics &&
      matchesQuickFilter
    );
  });
}

function sortProblems(problems, sort) {
  return [...problems].sort((first, second) => {
    if (sort === "acceptance") return second.acceptance - first.acceptance;
    if (sort === "id") return first.id - second.id;
    return (
      getDifficultyOrder(first.difficulty) -
      getDifficultyOrder(second.difficulty)
    );
  });
}

function Icon({ name, className = "" }) {
  return (
    <span
      className={`problem-icon problem-icon--${name} ${className}`.trim()}
      aria-hidden="true"
    />
  );
}

function DifficultyFilters({ selectedDifficulty, onChange }) {
  const counts = { All: 125, Easy: 45, Medium: 58, Hard: 22 };

  return (
    <fieldset className="filter-section filter-section--difficulty">
      <legend>Difficulty</legend>
      <div className="filter-options">
        {DIFFICULTIES.map((difficulty) => (
          <label className="filter-option" key={difficulty}>
            <input
              type="radio"
              name="difficulty"
              value={difficulty}
              checked={selectedDifficulty === difficulty}
              onChange={() => onChange(difficulty)}
            />
            <span
              className={`radio-mark radio-mark--${difficulty.toLowerCase()}`}
              aria-hidden="true"
            />
            <span className="filter-option__label">
              {difficulty === "All" ? "All Difficulties" : difficulty}
            </span>
            <span
              className={`filter-option__count filter-option__count--${difficulty.toLowerCase()}`}
            >
              {counts[difficulty]}
            </span>
          </label>
        ))}
      </div>
    </fieldset>
  );
}

function FilterSidebar({
  filters,
  onDifficultyChange,
  onStatusChange,
  onTopicToggle,
  onReset,
}) {
  return (
    <aside className="filter-sidebar" aria-label="Problem filters">
      <div className="filter-sidebar__header">
        <h2>
          <Icon name="filter" /> Filter
        </h2>
        <button type="button" className="text-button" onClick={onReset}>
          Reset All
        </button>
      </div>

      <DifficultyFilters
        selectedDifficulty={filters.difficulty}
        onChange={onDifficultyChange}
      />

      <section
        className="filter-section filter-section--status"
        aria-labelledby="status-filter-title"
      >
        <h3 id="status-filter-title">Status</h3>
        <div
          className="status-switcher"
          role="group"
          aria-label="Filter by completion status"
        >
          {STATUSES.map((status) => (
            <button
              key={status}
              type="button"
              className={filters.status === status ? "is-selected" : ""}
              aria-pressed={filters.status === status}
              onClick={() => onStatusChange(status)}
            >
              {status}
            </button>
          ))}
        </div>
      </section>

      <fieldset className="filter-section filter-section--topics">
        <legend>
          <span>Topics</span>
          <small>Selected ({filters.topics.length})</small>
        </legend>
        <div className="topic-options">
          {TOPICS.map(({ label, count }) => (
            <label className="topic-option" key={label}>
              <input
                type="checkbox"
                checked={filters.topics.includes(label)}
                onChange={() => onTopicToggle(label)}
              />
              <span className="checkbox-mark" aria-hidden="true" />
              <span>{label}</span>
              <small>{count}</small>
            </label>
          ))}
        </div>
      </fieldset>

      <section
        className="filter-section filter-section--techniques"
        aria-labelledby="techniques-title"
      >
        <h3 id="techniques-title">Techniques &amp; Tags</h3>
        <div className="technique-list">
          {TECHNIQUES.map((technique) => (
            <span className="technique-tag" key={technique}>
              {technique}
            </span>
          ))}
        </div>
      </section>
    </aside>
  );
}

function SearchToolbar({ query, sort, onQueryChange, onSortChange, inputRef }) {
  return (
    <div className="search-toolbar">
      <label className="search-field">
        <Icon name="search" />
        <span className="sr-only">Search problems</span>
        <input
          ref={inputRef}
          type="search"
          value={query}
          onChange={(event) => onQueryChange(event.target.value)}
          placeholder="Search algorithms, topics, or ID..."
        />
        <kbd>Ctrl+K</kbd>
      </label>
      <label className="sort-field">
        <span>Sort by:</span>
        <select
          value={sort}
          onChange={(event) => onSortChange(event.target.value)}
        >
          {SORT_OPTIONS.map((option) => (
            <option value={option.value} key={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <Icon name="chevron-down" />
      </label>
    </div>
  );
}

function QuickFilters({ activeFilter, onChange }) {
  return (
    <div
      className="quick-filters"
      aria-label="Quick problem filters"
      role="group"
    >
      {QUICK_FILTERS.map((filter) => (
        <button
          type="button"
          key={filter.id}
          className={activeFilter === filter.id ? "is-selected" : ""}
          aria-pressed={activeFilter === filter.id}
          onClick={() => onChange(filter.id)}
        >
          {filter.label}
        </button>
      ))}
    </div>
  );
}

function DifficultyBadge({ difficulty }) {
  return (
    <span
      className={`difficulty-badge difficulty-badge--${difficulty.toLowerCase()}`}
    >
      {difficulty}
    </span>
  );
}

function ProblemRow({ problem, onAction }) {
  const titleId = `problem-${problem.id}-title`;
  return (
    <article className="problem-row" aria-labelledby={titleId}>
      <div className="problem-row__status">
        <BadgeProblemStatus status={problem.progressStatus} />
      </div>
      <div className="problem-row__title">
        <div className="problem-title-line">
          <span className="problem-id">#{problem.id}</span>
          <h3 id={titleId}>{problem.title}</h3>
        </div>
        <div className="problem-tags">
          {problem.topics.map((topic) => (
            <span key={topic}>{topic}</span>
          ))}
        </div>
      </div>
      <div className="problem-row__difficulty">
        <DifficultyBadge difficulty={problem.difficulty} />
      </div>
      <div className="problem-row__acceptance">
        <span>{problem.acceptance.toFixed(1)}%</span>
        <span className="acceptance-track" aria-hidden="true">
          <span style={{ width: `${problem.acceptance}%` }} />
        </span>
      </div>
      <button
        className="problem-action"
        type="button"
        onClick={() => onAction(problem)}
        aria-label={`${problem.action} ${problem.title}`}
      >
        {problem.action}
        <Icon
          name={problem.action === "Review" ? "review-refresh" : "solve-arrow"}
        />
      </button>
    </article>
  );
}

function Pagination({ page, pageCount, onPageChange }) {
  return (
    <nav className="pagination" aria-label="Problem pages">
      <button
        type="button"
        className="pagination__arrow"
        disabled={page === 1}
        onClick={() => onPageChange(page - 1)}
        aria-label="Previous page"
      >
        <Icon name="chevron-left" />
      </button>
      {Array.from({ length: pageCount }, (_, index) => index + 1).map(
        (pageNumber) => (
          <button
            type="button"
            className={page === pageNumber ? "is-current" : ""}
            aria-current={page === pageNumber ? "page" : undefined}
            key={pageNumber}
            onClick={() => onPageChange(pageNumber)}
          >
            {pageNumber}
          </button>
        ),
      )}
      <button
        type="button"
        className="pagination__arrow"
        disabled={page === pageCount}
        onClick={() => onPageChange(page + 1)}
        aria-label="Next page"
      >
        <Icon name="chevron-right" />
      </button>
    </nav>
  );
}

function ProblemDirectory({
  filters,
  rows,
  page,
  pageCount,
  inputRef,
  onQueryChange,
  onSortChange,
  onQuickFilterChange,
  onPageChange,
  onAction,
}) {
  const start = rows.length === 0 ? 0 : (page - 1) * PAGE_SIZE + 1;
  const end = rows.length === 0 ? 0 : start + rows.length - 1;

  return (
    <section className="problem-directory" aria-labelledby="directory-title">
      <h2 id="directory-title" className="sr-only">
        Problem directory
      </h2>
      <SearchToolbar
        query={filters.query}
        sort={filters.sort}
        onQueryChange={onQueryChange}
        onSortChange={onSortChange}
        inputRef={inputRef}
      />
      <QuickFilters
        activeFilter={filters.quickFilter}
        onChange={onQuickFilterChange}
      />

      <div className="problem-list-card">
        <div className="problem-list-scroll">
          <div className="problem-list-header" aria-hidden="true">
            <span>Status</span>
            <span>Title &amp; Tags</span>
            <span>Difficulty</span>
            <span>Acceptance</span>
            <span>Action</span>
          </div>
          <div className="problem-rows">
            {rows.length > 0 ? (
              rows.map((problem) => (
                <ProblemRow
                  key={problem.id}
                  problem={problem}
                  onAction={onAction}
                />
              ))
            ) : (
              <div className="empty-problems">
                No problems match the current filters.
              </div>
            )}
          </div>
        </div>
        <div className="problem-list-footer">
          <p>
            Showing {start} - {end} of {TOTAL_PROBLEMS} algorithmic problems
          </p>
          <Pagination
            page={page}
            pageCount={pageCount}
            onPageChange={onPageChange}
          />
        </div>
      </div>
    </section>
  );
}

const FOOTER_COLUMNS = [
  {
    title: "Curriculum",
    links: [
      "Dynamic Programming",
      "Trees & Graph Algorithms",
      "Binary Search & Pointers",
      "Bit Manipulation",
      "System Design Foundations",
    ],
  },
  {
    title: "Platform",
    links: [
      "Problem Directory",
      "Algorithm Visualizer",
      "Weekly Coding Battles",
      "Company Tagged Lists",
      "Leaderboard",
    ],
  },
  {
    title: "Community",
    links: [
      "Discussion Forums",
      "Interview Experiences",
      "Discord Community",
      "Editorial Solutions",
      "FAQ & Help Center",
    ],
  },
];

function ProblemsFooter() {
  return (
    <footer className="problems-footer">
      <div className="problems-footer__grid">
        <div className="footer-brand-column">
          <a className="footer-brand" href="#top">
            <span className="footer-brand__mark">
              <Icon name="footer-brand" />
            </span>
            <span>
              Algo<span>Master</span>
            </span>
          </a>
          <p>
            Elevate your algorithmic problem solving through immersive
            visualizations, real-time code sandboxes, and structured learning
            tracks.
          </p>
          <div className="footer-socials" aria-label="Community links">
            <a
              className="footer-social footer-social--code"
              href="#community"
              aria-label="Code community"
            />
            <a
              className="footer-social footer-social--chat"
              href="#community"
              aria-label="Chat community"
            />
            <a
              className="footer-social footer-social--network"
              href="#community"
              aria-label="Network community"
            />
          </div>
        </div>
        {FOOTER_COLUMNS.map((column) => (
          <div className="footer-link-column" key={column.title}>
            <h2>{column.title}</h2>
            <nav aria-label={`${column.title} links`}>
              {column.links.map((link) => (
                <a
                  href={`#${link.toLowerCase().replaceAll(" ", "-")}`}
                  key={link}
                >
                  {link}
                </a>
              ))}
            </nav>
          </div>
        ))}
      </div>
      <div className="problems-footer__legal">
        <p>© 2025 AlgoMaster Inc. Engineered for algorithmic mastery.</p>
        <nav aria-label="Legal links">
          <a href="#privacy">Privacy Policy</a>
          <a href="#terms">Terms of Service</a>
          <a href="#conduct">Code of Conduct</a>
        </nav>
      </div>
    </footer>
  );
}

function ProblemsPage({ userName = "Your name", onOpenProblem }) {
  const [filters, setFilters] = useState({
    query: "",
    difficulty: "All",
    status: "All",
    topics: [],
    quickFilter: "all",
    sort: "difficulty",
  });
  const [page, setPage] = useState(1);
  const [announcement, setAnnouncement] = useState("");
  const searchInputRef = useRef(null);

  useEffect(() => {
    const handleShortcut = (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        searchInputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, []);

  const filteredProblems = useMemo(() => {
    return sortProblems(filterProblems(PROBLEMS, filters), filters.sort);
  }, [filters]);

  const visibleProblems = useMemo(() => {
    const start = (page - 1) * PAGE_SIZE;
    return filteredProblems.slice(start, start + PAGE_SIZE);
  }, [filteredProblems, page]);

  const updateFilter = (key, value) => {
    setPage(1);
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const handleTopicToggle = (topic) => {
    setPage(1);
    setFilters((current) => ({
      ...current,
      topics: current.topics.includes(topic)
        ? current.topics.filter((selectedTopic) => selectedTopic !== topic)
        : [...current.topics, topic],
    }));
  };

  const resetFilters = () => {
    setPage(1);
    setFilters({
      query: "",
      difficulty: "All",
      status: "All",
      topics: [],
      quickFilter: "all",
      sort: "difficulty",
    });
    setAnnouncement("Filters reset.");
  };

  const pickRandomProblem = () => {
    const candidates =
      filteredProblems.length > 0 ? filteredProblems : PROBLEMS;
    const selected = candidates[Math.floor(Math.random() * candidates.length)];
    setAnnouncement(`Random problem selected: ${selected.title}.`);
    onOpenProblem?.(selected);
  };

  const handleProblemAction = (problem) => {
    if (onOpenProblem) {
      onOpenProblem(problem);
      return;
    }
    window.location.hash = "problem-details";
  };

  return (
    <main className="problems-page" id="top">
      <div className="problems-shell">
        <NavigationBar
          active="Problems"
          userName={userName}
          className="problems-navigation"
          onPickRandomProblem={pickRandomProblem}
        />
      </div>
      <p className="sr-only" aria-live="polite">
        {announcement}
      </p>
      <section
        className="problems-workspace"
        aria-label="Browse algorithm problems"
      >
        <div className="problems-workspace__grid">
          <FilterSidebar
            filters={filters}
            onDifficultyChange={(difficulty) =>
              updateFilter("difficulty", difficulty)
            }
            onStatusChange={(status) => updateFilter("status", status)}
            onTopicToggle={handleTopicToggle}
            onReset={resetFilters}
          />
          <ProblemDirectory
            filters={filters}
            rows={visibleProblems}
            page={page}
            pageCount={PAGE_COUNT}
            inputRef={searchInputRef}
            onQueryChange={(query) => updateFilter("query", query)}
            onSortChange={(sort) => updateFilter("sort", sort)}
            onQuickFilterChange={(quickFilter) =>
              updateFilter("quickFilter", quickFilter)
            }
            onPageChange={setPage}
            onAction={handleProblemAction}
          />
        </div>
      </section>
      <ProblemsFooter />
    </main>
  );
}

export default ProblemsPage;
