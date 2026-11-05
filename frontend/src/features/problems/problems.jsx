import { useEffect, useMemo, useRef, useState } from "react";
import { NavigationBar, CustomSelect, ProgressView } from "../../components/index.js";
import { api } from "../../services/api.js";
import { isAccepted, profileStats } from "../../services/submissions.js";
import ApiState from "../../components/ApiState.jsx";
import { COURSES } from "../learn/coursesData.js";
import "./problems.css";

const TOPIC_TAGS = [
  "All",
  "Array",
  "Hash Table",
  "Two Pointers",
  "Sliding Window",
  "Binary Search",
  "Sorting",
  "Stack",
  "Linked List",
  "Tree",
  "Graph",
  "Dynamic Programming",
];

function getTopicIcon(topic, size = 13) {
  switch (topic) {
    case "All":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="14" width="7" height="7" rx="1.5" />
          <rect x="3" y="14" width="7" height="7" rx="1.5" />
        </svg>
      );
    case "Array":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="2" y="6" width="20" height="12" rx="2" />
          <line x1="8" y1="6" x2="8" y2="18" />
          <line x1="16" y1="6" x2="16" y2="18" />
        </svg>
      );
    case "Hash Table":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round">
          <line x1="4" y1="9" x2="20" y2="9" />
          <line x1="4" y1="15" x2="20" y2="15" />
          <line x1="10" y1="3" x2="8" y2="21" />
          <line x1="16" y1="3" x2="14" y2="21" />
        </svg>
      );
    case "Two Pointers":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 8h12M12 4l4 4-4 4" />
          <path d="M20 16H8M12 12l-4 4 4 4" />
        </svg>
      );
    case "Sliding Window":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="5" y="4" width="14" height="16" rx="2" strokeDasharray="3 3" />
          <path d="M9 4v16M15 4v16" />
          <path d="M2 12h3M19 12h3" />
        </svg>
      );
    case "Binary Search":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="10" cy="10" r="7" />
          <line x1="21" y1="21" x2="15" y2="15" />
          <line x1="10" y1="6" x2="10" y2="14" strokeDasharray="2 2" />
        </svg>
      );
    case "Sorting":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="4" y1="19" x2="4" y2="13" />
          <line x1="10" y1="19" x2="10" y2="9" />
          <line x1="16" y1="19" x2="16" y2="5" />
          <path d="M14 5l4-2 4 2" />
        </svg>
      );
    case "Stack":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 6l8-3 8 3-8 3-8-3z" />
          <path d="M4 12l8 3 8-3" />
          <path d="M4 18l8 3 8-3" />
        </svg>
      );
    case "Linked List":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="5" cy="12" r="3" />
          <circle cx="19" cy="12" r="3" />
          <line x1="8" y1="12" x2="16" y2="12" />
          <polyline points="13 9 16 12 13 15" />
        </svg>
      );
    case "Tree":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="5" r="2.5" />
          <circle cx="6" cy="19" r="2.5" />
          <circle cx="18" cy="19" r="2.5" />
          <line x1="10.5" y1="7" x2="7.5" y2="16.5" />
          <line x1="13.5" y1="7" x2="16.5" y2="16.5" />
        </svg>
      );
    case "Graph":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="6" cy="6" r="2.5" />
          <circle cx="18" cy="6" r="2.5" />
          <circle cx="12" cy="18" r="2.5" />
          <line x1="8.5" y1="6" x2="15.5" y2="6" />
          <line x1="7.5" y1="8" x2="10.5" y2="16" />
          <line x1="16.5" y1="8" x2="13.5" y2="16" />
        </svg>
      );
    case "Dynamic Programming":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <line x1="3" y1="9" x2="21" y2="9" />
          <line x1="3" y1="15" x2="21" y2="15" />
          <line x1="9" y1="3" x2="9" y2="21" />
          <line x1="15" y1="3" x2="15" y2="21" />
          <rect x="9" y="9" width="6" height="6" fill="currentColor" opacity="0.3" />
        </svg>
      );
    case "Divide & Conquer":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M12 4v8M8 8l4-4 4 4" />
          <path d="M4 16l8-4 8 4" />
        </svg>
      );
    case "Recursion":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
          <polyline points="3 3 3 8 8 8" />
        </svg>
      );
    case "String":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 7V4h16v3M9 20h6M12 4v16" />
        </svg>
      );
    case "Math":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <path d="M4 19h16M7 5l6 7-6 7M17 5v14" />
        </svg>
      );
    case "Design":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="4" y1="21" x2="4" y2="14" />
          <line x1="4" y1="10" x2="4" y2="3" />
          <line x1="12" y1="21" x2="12" y2="12" />
          <line x1="12" y1="8" x2="12" y2="3" />
          <line x1="20" y1="21" x2="20" y2="16" />
          <line x1="20" y1="12" x2="20" y2="3" />
          <line x1="1" y1="14" x2="7" y2="14" />
          <line x1="9" y1="8" x2="15" y2="8" />
          <line x1="17" y1="16" x2="23" y2="16" />
        </svg>
      );
    case "Breadth-First Search":
    case "Depth-First Search":
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <circle cx="12" cy="5" r="2.5" />
          <circle cx="6" cy="18" r="2.5" />
          <circle cx="18" cy="18" r="2.5" />
          <path d="M10 7l-3 8M14 7l3 8" />
        </svg>
      );
    default:
      return (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
          <polyline points="16 18 22 12 16 6" />
          <polyline points="8 6 2 12 8 18" />
        </svg>
      );
  }
}

const DIFFICULTY_OPTIONS = [
  { value: "All", label: "All Difficulties" },
  { value: "Easy", label: "Easy", dotColor: "#137333" },
  { value: "Medium", label: "Medium", dotColor: "#b06000" },
  { value: "Hard", label: "Hard", dotColor: "#c5221f" },
];

const STATUS_OPTIONS = [
  { value: "All", label: "All Status" },
  { value: "Attempted", label: "Attempted" },
  {
    value: "Solved",
    label: "Solved",
    icon: (
      <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#137333" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
        <polyline points="20 6 9 17 4 12" />
      </svg>
    ),
  },
  {
    value: "To Do",
    label: "To Do",
    icon: (
      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#80868b" strokeWidth="2.5">
        <circle cx="12" cy="12" r="10" />
      </svg>
    ),
  },
];

const SORT_OPTIONS = [
  { value: "id-asc", label: "ID (1 → N)" },
  { value: "id-desc", label: "ID (N → 1)" },
  { value: "difficulty-asc", label: "Difficulty (Easy → Hard)" },
  { value: "difficulty-desc", label: "Difficulty (Hard → Easy)" },
  { value: "acceptance-desc", label: "Acceptance (High → Low)" },
  { value: "acceptance-asc", label: "Acceptance (Low → High)" },
  { value: "title-asc", label: "Title (A → Z)" },
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
    const matchesStatus = filters.status === "All" || problem.status === filters.status;
    const matchesTopic =
      filters.topic === "All" ||
      problem.topics.some((topic) => topic.toLowerCase().includes(filters.topic.toLowerCase()));

    return matchesQuery && matchesDifficulty && matchesStatus && matchesTopic;
  });
}

function sortProblems(problems, sort) {
  return [...problems].sort((first, second) => {
    switch (sort) {
      case "id-desc":
        return second.id - first.id;
      case "id-asc":
        return first.id - second.id;
      case "difficulty-desc": {
        const diff = getDifficultyOrder(second.difficulty) - getDifficultyOrder(first.difficulty);
        return diff !== 0 ? diff : first.id - second.id;
      }
      case "difficulty-asc": {
        const diff = getDifficultyOrder(first.difficulty) - getDifficultyOrder(second.difficulty);
        return diff !== 0 ? diff : first.id - second.id;
      }
      case "acceptance-asc":
        return (first.acceptance ?? 0) - (second.acceptance ?? 0);
      case "acceptance-desc":
        return (second.acceptance ?? 0) - (first.acceptance ?? 0);
      case "title-asc":
        return first.title.localeCompare(second.title);
      default:
        return first.id - second.id;
    }
  });
}

function DifficultyBadge({ difficulty }) {
  return <span className={`diff-badge diff-badge--${difficulty.toLowerCase()}`}>{difficulty}</span>;
}

function ProblemHeroBanner({
  history,
  userName,
  totalCount,
  solvedCount,
  easySolved = 5,
  easyTotal = 8,
  medSolved = 3,
  medTotal = 8,
  hardSolved = 1,
  hardTotal = 4,
  onPickRandom,
}) {
  const [showStats, setShowStats] = useState(true);
  const percent = totalCount > 0 ? Math.round((solvedCount / totalCount) * 100) : 0;
  const displayName = userName && userName !== "Your name" ? userName : "Guest";

  // Circular progress ring calculation (radius 22, circumference ~138.23)
  const radius = 22;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (percent / 100) * circumference;

  const activity = profileStats(history, []);
  const weekDays = ["S", "M", "T", "W", "T", "F", "S"];
  const recentDays = activity.cells.slice(-21);
  const activityWeeks = [0, 1, 2].map((week) => recentDays.slice(week * 7, week * 7 + 7)
    .map((day) => ({ active: day.count > 0, label: `${day.date}: ${day.count} submissions` })));
  const totalActiveDays = recentDays.filter((day) => day.count > 0).length;

  return (
    <section className="problemset-banner" aria-label="Welcome Overview">
      <div className="problemset-banner__top">
        <div className="problemset-banner__left">
          <h1 className="problemset-banner__title">Hi, {displayName}!</h1>
          <p className="problemset-banner__desc">Jump back in, or start something new.</p>
        </div>

        <div className="problemset-banner__kpi-group">
          {/* 1. Login streak */}
          <div className="kaggle-kpi-col">
            <span className="kaggle-kpi-label">Practice streak</span>
            <div className="kaggle-kpi-streak-val">
              <span className="kaggle-streak-num">{activity.currentStreak}</span>
            </div>
            <span className="kaggle-kpi-sub">days of consecutive practice</span>
          </div>

          {/* 2. Problem progress */}
          <div className="kaggle-kpi-col kaggle-kpi-col--center">
            <span className="kaggle-kpi-label">Problem progress</span>
            <div className="kaggle-ring-box">
              <ProgressView
                variant="ring"
                value={percent}
                size={56}
                strokeWidth={3.5}
                color="var(--accent-primary-hover)"
                trackColor="var(--border-muted)"
                showValue
                label="Problem progress"
              />
            </div>
            <span className="kaggle-kpi-sub">of the problem catalog</span>
          </div>

          {/* 3. Recent activity (Kaggle 3-week dot matrix) */}
          <div className="kaggle-kpi-col">
            <span className="kaggle-kpi-label">Recent activity</span>
            <div className="kaggle-activity-grid">
              <div className="kaggle-grid-header">
                {weekDays.map((d, i) => (
                  <span key={i} className="kaggle-grid-day-label">{d}</span>
                ))}
              </div>
              <div className="kaggle-grid-matrix">
                {activityWeeks.map((week, wIdx) => (
                  <div key={wIdx} className="kaggle-grid-row">
                    {week.map((day, dIdx) => (
                      <div key={dIdx} className="kaggle-grid-cell">
                        <span
                          className={`kaggle-grid-dot ${day.active ? "is-active" : "is-inactive"} ${day.isToday ? "is-today" : ""}`}
                          title={day.label}
                        />
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </div>
            <span className="kaggle-kpi-sub">{totalActiveDays} active days</span>
          </div>
        </div>
      </div>

      {/* Redesigned Lower Metric Shelf */}
      {showStats && (
        <div className="problemset-banner__bottom">
          <div className="banner-stats-grid">
            {/* Stat 1: Solved Problems */}
            <div className="banner-stat-card">
              <div className="banner-stat-header">
                <span className="banner-stat-icon banner-stat-icon--emerald">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14" />
                    <polyline points="22 4 12 14.01 9 11.01" />
                  </svg>
                </span>
                <span className="banner-stat-label">Problems Solved</span>
              </div>
              <div className="banner-stat-value">
                <strong>{solvedCount}</strong>
                <span className="banner-stat-denom">/ {totalCount}</span>
                <span className="banner-stat-rate-pill">{percent}%</span>
              </div>
              <div className="banner-stat-diff-row">
                <span className="diff-dot diff-dot--easy" title={`Easy: ${easySolved}/${easyTotal}`}>E: {easySolved}/{easyTotal}</span>
                <span className="diff-dot diff-dot--med" title={`Medium: ${medSolved}/${medTotal}`}>M: {medSolved}/{medTotal}</span>
                <span className="diff-dot diff-dot--hard" title={`Hard: ${hardSolved}/${hardTotal}`}>H: {hardSolved}/{hardTotal}</span>
              </div>
            </div>

            {/* Stat 2: Interactive Visualizers */}
            <div className="banner-stat-card">
              <div className="banner-stat-header">
                <span className="banner-stat-icon banner-stat-icon--cyan">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
                  </svg>
                </span>
                <span className="banner-stat-label">Visualizer Traces</span>
              </div>
              <div className="banner-stat-value">
                <strong>8</strong>
                <span className="banner-stat-badge">Live Traces</span>
              </div>
              <span className="banner-stat-sub">Step-by-step internal states</span>
            </div>

            {/* Stat 3: Study Blueprints & Patterns */}
            <div className="banner-stat-card">
              <div className="banner-stat-header">
                <span className="banner-stat-icon banner-stat-icon--indigo">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z" />
                    <path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z" />
                  </svg>
                </span>
                <span className="banner-stat-label">Study Blueprints</span>
              </div>
              <div className="banner-stat-value">
                <strong>6</strong>
                <span className="banner-stat-badge banner-stat-badge--indigo">Guides</span>
              </div>
              <span className="banner-stat-sub">Big-O & interview cheatsheets</span>
            </div>

            {/* Stat 4: Mastery Tracks */}
            <div className="banner-stat-card">
              <div className="banner-stat-header">
                <span className="banner-stat-icon banner-stat-icon--amber">
                  <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                    <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                  </svg>
                </span>
                <span className="banner-stat-label">Curriculum Tracks</span>
              </div>
              <div className="banner-stat-value">
                <strong>{COURSES.length}</strong>
                <span className="banner-stat-badge banner-stat-badge--amber">Courses</span>
              </div>
              <span className="banner-stat-sub">Foundations to advanced prep</span>
            </div>
          </div>
        </div>
      )}

      {/* Action Row with perfectly centered Show / Hide Toggle Button */}
      <div className="problemset-banner__action-row">
        <div className="action-row__spacer" aria-hidden="true" />
        <div className="action-row__center">
          <button
            type="button"
            className={`banner-stats-toggle-btn ${showStats ? "banner-stats-toggle-btn--hide" : "banner-stats-toggle-btn--show"}`}
            onClick={() => setShowStats((prev) => !prev)}
            aria-label={showStats ? "Hide platform summary" : "Show platform summary"}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" />
              <line x1="3" y1="9" x2="21" y2="9" />
              <line x1="9" y1="21" x2="9" y2="9" />
            </svg>
            <span>{showStats ? "Hide Platform Summary" : "Show Platform Summary"}</span>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              {showStats ? (
                <polyline points="18 15 12 9 6 15" />
              ) : (
                <polyline points="6 9 12 15 18 9" />
              )}
            </svg>
          </button>
        </div>
        <div className="action-row__right">
          <button type="button" className="banner-random-btn banner-random-btn--sm" onClick={onPickRandom}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="16 3 21 3 21 8" />
              <line x1="4" y1="20" x2="21" y2="3" />
            </svg>
            <span>Pick Random</span>
          </button>
        </div>
      </div>

    </section>
  );
}

function FilterBar({
  topics = TOPIC_TAGS,
  filters,
  onSearchChange,
  onDifficultyChange,
  onStatusChange,
  onSortChange,
  onTopicChange,
  onReset,
  inputRef,
}) {
  return (
    <div className="problemset-filter-bar">
      <div className="filter-bar__top">
        <div className="filter-search-box">
          <svg className="filter-search-icon" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="11" cy="11" r="8" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            ref={inputRef}
            type="search"
            value={filters.query}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Search problem title, topic, or #ID..."
            aria-label="Search problems"
          />
          <kbd className="filter-search-kbd">Ctrl+K</kbd>
        </div>

        <div className="filter-selects">
          <CustomSelect
            value={filters.difficulty}
            onChange={onDifficultyChange}
            options={DIFFICULTY_OPTIONS}
            ariaLabel="Filter by difficulty"
          />

          <CustomSelect
            value={filters.status}
            onChange={onStatusChange}
            options={STATUS_OPTIONS}
            ariaLabel="Filter by status"
          />

          <CustomSelect
            value={filters.sort}
            onChange={onSortChange}
            options={SORT_OPTIONS}
            ariaLabel="Sort problems"
          />

          {(filters.query || filters.difficulty !== "All" || filters.status !== "All" || filters.topic !== "All") && (
            <button type="button" className="filter-reset-btn" onClick={onReset}>
              Reset
            </button>
          )}
        </div>
      </div>

      <div className="filter-topic-strip" role="tablist" aria-label="Topic categories">
        {topics.map((topic) => (
          <button
            type="button"
            key={topic}
            role="tab"
            aria-selected={filters.topic === topic}
            className={`topic-pill ${filters.topic === topic ? "is-selected" : ""}`}
            onClick={() => onTopicChange(topic)}
          >
            <span className="topic-pill-icon" aria-hidden="true">
              {getTopicIcon(topic)}
            </span>
            <span>{topic === "All" ? "All Topics" : topic}</span>
          </button>
        ))}
      </div>
    </div>
  );
}

function ProblemRow({ problem, onSolve, onVisualizer }) {
  const isSolved = problem.status === "Solved";

  return (
    <div className="problem-table-row">
      <div className="col-status">
        {isSolved ? (
          <span className="status-dot status-dot--solved" title="Solved" aria-label="Solved">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </span>
        ) : (
          <span className="status-dot status-dot--todo" title="To Do" aria-label="To Do" />
        )}
      </div>

      <div className="col-title">
        <div className="title-container">
          <span className="problem-num">#{problem.id}</span>
          <button
            type="button"
            className="problem-name-btn"
            onClick={() => onSolve(problem)}
          >
            {problem.title}
          </button>
        </div>
        <div className="problem-topics-list">
          {problem.topics.map((t) => (
            <span className="topic-chip" key={t}>
              <span className="topic-chip-icon" aria-hidden="true">
                {getTopicIcon(t, 11)}
              </span>
              <span>{t}</span>
            </span>
          ))}
        </div>
      </div>

      <div className="col-visualizer">
        {problem.hasVisualizer ? (
          <button
            type="button"
            className="visualizer-btn"
            onClick={() => onVisualizer(problem)}
            title="Open interactive step-by-step trace"
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
            <span>Trace</span>
          </button>
        ) : (
          <span className="visualizer-placeholder">—</span>
        )}
      </div>

      <div className="col-difficulty">
        <DifficultyBadge difficulty={problem.difficulty} />
      </div>

      <div className="col-acceptance">
        <span className="acceptance-pct">{problem.acceptance == null ? "—" : `${problem.acceptance.toFixed(1)}%`}</span>
        <div className="acceptance-bar" aria-hidden="true">
          <div
            className={`acceptance-fill ${
              problem.acceptance >= 55 ? "is-high" : problem.acceptance >= 40 ? "is-med" : "is-low"
            }`}
            style={{ width: `${Math.min(100, Math.max(0, problem.acceptance ?? 0))}%` }}
          />
        </div>
      </div>

      <div className="col-action">
        <button
          type="button"
          className="solve-link-btn"
          onClick={() => onSolve(problem)}
        >
          {problem.action || "Solve"}
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
            <line x1="5" y1="12" x2="19" y2="12" />
            <polyline points="12 5 19 12 12 19" />
          </svg>
        </button>
      </div>
    </div>
  );
}

function Pagination({ page, pageCount, onPageChange }) {
  const safePageCount = Math.max(1, pageCount);

  return (
    <nav className="problemset-pagination" aria-label="Pagination">
      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(1)}
        className="pagination-btn pagination-btn--nav"
        title="First page"
        aria-label="First page"
      >
        «
      </button>

      <button
        type="button"
        disabled={page <= 1}
        onClick={() => onPageChange(page - 1)}
        className="pagination-btn pagination-btn--nav"
        title="Previous page"
        aria-label="Previous page"
      >
        ‹ Prev
      </button>

      <div className="pagination-numbers">
        {Array.from({ length: safePageCount }, (_, i) => i + 1).map((num) => (
          <button
            type="button"
            key={num}
            className={`pagination-num ${page === num ? "is-active" : ""}`}
            onClick={() => onPageChange(num)}
            aria-current={page === num ? "page" : undefined}
          >
            {num}
          </button>
        ))}
      </div>

      <button
        type="button"
        disabled={page >= safePageCount}
        onClick={() => onPageChange(page + 1)}
        className="pagination-btn pagination-btn--nav"
        title="Next page"
        aria-label="Next page"
      >
        Next ›
      </button>

      <button
        type="button"
        disabled={page >= safePageCount}
        onClick={() => onPageChange(safePageCount)}
        className="pagination-btn pagination-btn--nav"
        title="Last page"
        aria-label="Last page"
      >
        »
      </button>
    </nav>
  );
}

function ProblemsSidebar({
  problems,
  history,
  totalCount,
  solvedCount,
  easySolved,
  easyTotal,
  medSolved,
  medTotal,
  hardSolved,
  hardTotal,
  onSolve,
  onVisualizer,
}) {
  const percent = totalCount > 0 ? Math.round((solvedCount / totalCount) * 100) : 0;
  const dailyProblem = problems.length ? problems[0] : null;
  const activity = profileStats(history, []);
  if (!dailyProblem) return null;

  return (
    <div className="problems-sidebar">
      <div className="sidebar-card sidebar-card--streak">
        <div className="streak-minimal-header">
          <div className="streak-stat-headline"><strong>{activity.currentStreak}</strong><span>Day Streak</span></div>
          <span className="streak-best-label">{history.length} submissions</span>
        </div>
        <p className="streak-minimal-subtext">Practice regularly to build your streak.</p>
      </div>

      {/* 2. Daily Algorithm Challenge Card */}
      <div className="sidebar-card sidebar-card--daily">
        <div className="sidebar-card__header">
          <div className="daily-badge-wrap">
            <span className="daily-pill">Daily Challenge</span>
            <span className="xp-tag">+10 points on first solve</span>
          </div>

        </div>

        <div className="daily-card-body">
          <h4 className="daily-problem-title">
            #{dailyProblem.id} {dailyProblem.title}
          </h4>
          <div className="daily-problem-meta">
            <span className={`diff-badge diff-badge--${dailyProblem.difficulty.toLowerCase()}`}>
              {dailyProblem.difficulty}
            </span>
            <span className="daily-meta-tag">{dailyProblem.topics[0]}</span>
            <span className="daily-meta-rate">{dailyProblem.acceptance == null ? "No submissions yet" : `${dailyProblem.acceptance}% pass`}</span>
          </div>
          <p className="daily-problem-hint">
            {dailyProblem.shortDescription || "Practice this problem and explore its reference solutions."}
          </p>

          <div className="daily-card-actions">
            <button
              type="button"
              className="daily-btn-solve"
              onClick={() => onSolve(dailyProblem)}
            >
              Solve Challenge →
            </button>
            <button
              type="button"
              className="daily-btn-trace"
              onClick={() => onVisualizer(dailyProblem)}
              title="Open Interactive Visualizer"
            >
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
              Trace
            </button>
          </div>
        </div>
      </div>

      {/* 3. Interactive Visualizer Showcase */}
      <div className="sidebar-card sidebar-card--visualizers">
        <div className="sidebar-card__header">
          <span className="sidebar-card__title">Interactive Visualizers</span>
          <span className="sidebar-card__badge-cyan">8 Ready</span>
        </div>
        <p className="sidebar-card__caption">
          Inspect algorithmic internal states, memory pointers, and recursion trees step-by-step.
        </p>

        <div className="sidebar-vis-list">
          <button
            type="button"
            className="sidebar-vis-item"
            onClick={() => onVisualizer({ id: 1 })}
          >
            <div className="vis-item-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
              </svg>
            </div>
            <div className="vis-item-info">
              <strong>Two Sum</strong>
              <small>Hash Map Lookup Trace</small>
            </div>
            <span className="vis-item-arrow">→</span>
          </button>

          <button
            type="button"
            className="sidebar-vis-item"
            onClick={() => onVisualizer({ id: 912 })}
          >
            <div className="vis-item-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </div>
            <div className="vis-item-info">
              <strong>Sort an Array</strong>
              <small>Merge Sort Recursion Tree</small>
            </div>
            <span className="vis-item-arrow">→</span>
          </button>

          <button
            type="button"
            className="sidebar-vis-item"
            onClick={() => onVisualizer({ id: 704 })}
          >
            <div className="vis-item-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </div>
            <div className="vis-item-info">
              <strong>Binary Search</strong>
              <small>Pointers & Halving Intervals</small>
            </div>
            <span className="vis-item-arrow">→</span>
          </button>
        </div>
      </div>

      {/* 4. Curated Tracks */}
      <div className="sidebar-card sidebar-card--roadmap">
        <div className="sidebar-card__header">
          <span className="sidebar-card__title">Curated Tracks</span>
        </div>
        <div className="roadmap-mini-list"><a href="#learn">Browse courses and track your lesson progress →</a></div>
      </div>
    </div>
  );
}

function ProblemsPage({ userName = "Your name", isAuthenticated = false, onOpenProblem }) {
  const [filters, setFilters] = useState({
    query: "",
    difficulty: "All",
    status: "All",
    topic: "All",
    sort: "id-asc",
  });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);
  const [problemsList, setProblemsList] = useState([]);
  const [history, setHistory] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const searchInputRef = useRef(null);

  useEffect(() => {
    let active = true;
    async function loadProblems() {
      setIsLoading(true);
      setLoadError("");
      try {
        const [catalog, submissions] = await Promise.allSettled([
          api.getProblems(), isAuthenticated ? api.getSubmissionHistory() : Promise.resolve([]),
        ]);
        if (!active) return;
        if (catalog.status === "rejected") throw catalog.reason;
        if (!Array.isArray(catalog.value)) throw new Error("Invalid problems response.");
        const records = submissions.status === "fulfilled" && Array.isArray(submissions.value) ? submissions.value : [];
        setHistory(records);
        if (submissions.status === "rejected") setLoadError("Could not load your progress. Please try again.");
        const solvedIds = new Set(records.filter((item) => isAccepted(item.status)).map((item) => item.problemId));
        const attemptedIds = new Set(records.map((item) => item.problemId));
        setProblemsList(catalog.value.map((p) => ({
          ...p, difficulty: p.difficulty || "Easy",
          acceptance: typeof p.acceptanceRate === "number" ? Math.round(p.acceptanceRate * 10) / 10 : null,
          topics: p.category ? p.category.split(/,\s*/) : ["Algorithm"],
          status: solvedIds.has(p.id) ? "Solved" : attemptedIds.has(p.id) ? "Attempted" : "To Do",
          action: solvedIds.has(p.id) ? "Review" : "Solve",
          hasVisualizer: [1, 912, 704, 3, 42, 207, 20, 53, 206, 15, 200].includes(p.id),
        })));
      } catch (error) {
        if (active) { setProblemsList([]); setLoadError(error.message); }
      } finally { if (active) setIsLoading(false); }
    }
    loadProblems();
    return () => { active = false; };
  }, [isAuthenticated, reloadKey]);

  useEffect(() => {
    const handleShortcut = (e) => {
      if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === "k") {
        e.preventDefault();
        searchInputRef.current?.focus();
      }
    };
    window.addEventListener("keydown", handleShortcut);
    return () => window.removeEventListener("keydown", handleShortcut);
  }, []);

  const totalProblemsCount = problemsList.length;
  const solvedCount = useMemo(() => {
    return problemsList.filter((p) => p.status === "Solved").length;
  }, [problemsList]);

  const easyTotal = useMemo(() => problemsList.filter((p) => p.difficulty === "Easy").length, [problemsList]);
  const easySolved = useMemo(() => problemsList.filter((p) => p.difficulty === "Easy" && p.status === "Solved").length, [problemsList]);

  const medTotal = useMemo(() => problemsList.filter((p) => p.difficulty === "Medium").length, [problemsList]);
  const medSolved = useMemo(() => problemsList.filter((p) => p.difficulty === "Medium" && p.status === "Solved").length, [problemsList]);

  const hardTotal = useMemo(() => problemsList.filter((p) => p.difficulty === "Hard").length, [problemsList]);
  const hardSolved = useMemo(() => problemsList.filter((p) => p.difficulty === "Hard" && p.status === "Solved").length, [problemsList]);

  const filteredProblems = useMemo(() => {
    return sortProblems(filterProblems(problemsList, filters), filters.sort);
  }, [filters, problemsList]);

  const pageCount = Math.max(1, Math.ceil(filteredProblems.length / pageSize));

  const visibleProblems = useMemo(() => {
    const start = (page - 1) * pageSize;
    return filteredProblems.slice(start, start + pageSize);
  }, [filteredProblems, page, pageSize]);

  const handleSearchChange = (query) => {
    setPage(1);
    setFilters((c) => ({ ...c, query }));
  };

  const handleDifficultyChange = (difficulty) => {
    setPage(1);
    setFilters((c) => ({ ...c, difficulty }));
  };

  const handleStatusChange = (status) => {
    setPage(1);
    setFilters((c) => ({ ...c, status }));
  };

  const handleSortChange = (sort) => {
    setFilters((c) => ({ ...c, sort }));
  };

  const handleTopicChange = (topic) => {
    setPage(1);
    setFilters((c) => ({ ...c, topic }));
  };

  const handleResetFilters = () => {
    setPage(1);
    setFilters({ query: "", difficulty: "All", status: "All", topic: "All", sort: "id-asc" });
  };

  const handleSolve = (problem) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    if (onOpenProblem) {
      onOpenProblem(problem);
      return;
    }
    window.location.hash = `problem-details?id=${problem?.id || 1}`;
  };

  const handleOpenVisualizer = (problem) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    window.location.hash = `problem-details?id=${problem?.id || 1}&tab=visualizer`;
  };

  const handlePickRandom = () => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    const candidates = filteredProblems.length > 0 ? filteredProblems : problemsList;
    if (candidates.length === 0) return;
    const selected = candidates[Math.floor(Math.random() * candidates.length)];
    handleSolve(selected);
  };

  const startIndex = filteredProblems.length === 0 ? 0 : (page - 1) * pageSize + 1;
  const endIndex =
    filteredProblems.length === 0
      ? 0
      : Math.min(filteredProblems.length, startIndex + visibleProblems.length - 1);

  if (isLoading || (loadError && problemsList.length === 0)) return (
    <main className="problems-page" aria-busy={isLoading}>
      <div className="problems-container">
        <ApiState loading={isLoading} error={loadError} onRetry={() => setReloadKey((key) => key + 1)} label="Loading problems…" page />
      </div>
    </main>
  );

  return (
    <main className="problems-page" id="top">
      <div className="problems-container">
        <ProblemHeroBanner
          history={history}
          userName={userName}
          totalCount={totalProblemsCount}
          solvedCount={solvedCount}
          easySolved={easySolved}
          easyTotal={easyTotal}
          medSolved={medSolved}
          medTotal={medTotal}
          hardSolved={hardSolved}
          hardTotal={hardTotal}
          onPickRandom={handlePickRandom}
        />

        <ApiState loading={isLoading} error={loadError} onRetry={() => setReloadKey((key) => key + 1)} />
        <div className="problems-main-layout">
          <div className="problems-primary-column">
            <FilterBar
              topics={["All", ...new Set(problemsList.flatMap((problem) => problem.topics))]}
              filters={filters}
              onSearchChange={handleSearchChange}
              onDifficultyChange={handleDifficultyChange}
              onStatusChange={handleStatusChange}
              onSortChange={handleSortChange}
              onTopicChange={handleTopicChange}
              onReset={handleResetFilters}
              inputRef={searchInputRef}
            />

            <section className="problem-table-container" aria-label="Problems list">
              <div className="problem-table-header" aria-hidden="true">
                <span className="col-status">Status</span>
                <span className="col-title">Title &amp; Tags</span>
                <span className="col-visualizer">Trace</span>
                <span className="col-difficulty">Difficulty</span>
                <span className="col-acceptance">Acceptance</span>
                <span className="col-action">Action</span>
              </div>

              <div className="problem-table-body">
                {visibleProblems.length > 0 ? (
                  visibleProblems.map((prob) => (
                    <ProblemRow
                      key={prob.id}
                      problem={prob}
                      onSolve={handleSolve}
                      onVisualizer={handleOpenVisualizer}
                    />
                  ))
                ) : (
                  <div className="problem-table-empty">
                    <p>{isLoading ? "Loading problems…" : loadError ? "Problem data is unavailable." : "No problems match the current filter."}</p>
                    <button type="button" className="filter-reset-btn" onClick={handleResetFilters}>
                      Clear all filters
                    </button>
                  </div>
                )}
              </div>

              <div className="problem-table-footer">
                <div className="problem-footer-left">
                  <span className="problem-count-info">
                    Showing <strong>{startIndex}</strong> – <strong>{endIndex}</strong> of <strong>{filteredProblems.length}</strong> problems
                  </span>
                </div>

                <div className="problem-footer-center">
                  <Pagination page={page} pageCount={pageCount} onPageChange={setPage} />
                </div>

                <div className="problem-footer-right">
                  <span className="page-size-label">Rows per page:</span>
                  <CustomSelect
                    value={pageSize}
                    onChange={(val) => {
                      setPageSize(Number(val));
                      setPage(1);
                    }}
                    options={[
                      { value: 5, label: "5 / page" },
                      { value: 10, label: "10 / page" },
                      { value: 20, label: "20 / page" },
                      { value: 50, label: "50 / page" },
                    ]}
                    direction="up"
                    align="right"
                    ariaLabel="Select items per page"
                    className="page-size-select"
                  />
                </div>
              </div>
            </section>
          </div>

          <aside className="problems-sidebar-column">
            <ProblemsSidebar
              problems={problemsList}
              history={history}
              totalCount={totalProblemsCount}
              solvedCount={solvedCount}
              easySolved={easySolved}
              easyTotal={easyTotal}
              medSolved={medSolved}
              medTotal={medTotal}
              hardSolved={hardSolved}
              hardTotal={hardTotal}
              onSolve={handleSolve}
              onVisualizer={handleOpenVisualizer}
            />
          </aside>
        </div>
      </div>

      <footer className="problems-minimal-footer">
        <span>AlgoViz Studio • Interactive Algorithmic Engineering</span>
      </footer>
    </main>
  );
}

export default ProblemsPage;
