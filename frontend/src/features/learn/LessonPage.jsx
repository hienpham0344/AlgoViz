import { useState, useEffect, useMemo } from "react";
import { api } from "../../services/api.js";
import { languageKey } from "../../services/submissions.js";
import { COURSES } from "./coursesData.js";
import { getLessonContent } from "./lessonContent.js";
import "./LessonPage.css";
import { useLearningProgress } from "../../services/useLearningProgress.js";
import { withLearningProgress } from "../../services/learning.js";
import ProgressView from "../../components/ProgressView.jsx";
import ApiState from "../../components/ApiState.jsx";

function formatMath(expression) {
  return expression
    .replace(/\\frac\{([^{}]+)\}\{([^{}]+)\}/g, "($1)/($2)")
    .replace(/\\text\{([^{}]+)\}/g, "$1")
    .replace(/\\(le|ge|dots|lfloor|rfloor|Theta|quad|log)\b/g, (_, symbol) => ({
      le: "≤", ge: "≥", dots: "…", lfloor: "⌊", rfloor: "⌋", Theta: "Θ", quad: " ", log: "log",
    })[symbol]);
}

// Format the small set of Markdown and math notation used by local lessons.
function formatInline(text) {
  if (typeof text !== "string") return text;
  const parts = text.split(/(`[^`]+`|\*\*[^*]+\*\*|\$\$[^$]+\$\$|\$[^$]+\$)/g);
  return parts.map((part, i) => {
    if (part.startsWith("`") && part.endsWith("`")) {
      return (
        <code key={i} className="inline-code">
          {part.slice(1, -1)}
        </code>
      );
    }
    if (part.startsWith("**") && part.endsWith("**")) {
      return <strong key={i}>{formatInline(part.slice(2, -2))}</strong>;
    }
    if (part.startsWith("$") && part.endsWith("$")) {
      const display = part.startsWith("$$");
      return <span key={i} className={`lesson-math${display ? " lesson-math--display" : ""}`}>
        {formatMath(part.slice(display ? 2 : 1, display ? -2 : -1))}
      </span>;
    }
    return part;
  });
}

export default function LessonPage({
  courseId = "sorting-masterclass",
  lessonId = 1,
  userName = "Developer",
  isAuthenticated = true,
}) {
  const { progress, loading: progressLoading, error: progressError, save, retry } = useLearningProgress(isAuthenticated);
  const [savingProgress, setSavingProgress] = useState(false);
  const [progressSaveError, setProgressSaveError] = useState("");
  // Current course metadata
  const currentCourse = useMemo(() => {
    return (
      withLearningProgress(COURSES, progress).find((c) => c.id === courseId) || {
        id: courseId,
        title: "Sorting Algorithms Masterclass",
        category: "Sorting & Searching",
        difficulty: "Beginner",
        duration: "4 hours",
        targetProblemId: 912,
        lessons: [],
      }
    );
  }, [courseId, progress]);

  // Current lesson content (theory, code, quizzes)
  const lessonData = useMemo(() => {
    return getLessonContent(courseId, lessonId, COURSES.find((course) => course.id === courseId));
  }, [courseId, lessonId]);

  // Code editor state
  const [userCode, setUserCode] = useState(
    lessonData?.codeSandbox?.starterCode || ""
  );
  const [outputLog, setOutputLog] = useState(
    "Click Run to execute on the server."
  );
  const [isRunning, setIsRunning] = useState(false);
  const [execTime, setExecTime] = useState("—");
  const [hasRun, setHasRun] = useState(false);

  // Sync code editor state when lesson changes
  useEffect(() => {
    if (lessonData?.codeSandbox) {
      const id = setTimeout(() => {
        setUserCode(lessonData.codeSandbox.starterCode);
        setOutputLog("Click Run to execute on the server.");
        setHasRun(false);
        setExecTime("—");
      }, 0);
      return () => clearTimeout(id);
    }
  }, [lessonData]);

  // Quiz interactive state: { [quizId]: selectedOptionIndex }
  const [selectedAnswers, setSelectedAnswers] = useState({});

  // Reset quiz answers when lesson changes
  useEffect(() => {
    const id = setTimeout(() => setSelectedAnswers({}), 0);
    return () => clearTimeout(id);
  }, [courseId, lessonId]);

  const isCompleted = progress.some((item) => item.courseId === courseId && item.lessonId === Number(lessonId));
  const toggleCompleted = async () => {
    if (!isAuthenticated) { window.location.hash = "#login"; return; }
    if (savingProgress || progressLoading) return;
    setSavingProgress(true);
    setProgressSaveError("");
    try { await save(courseId, Number(lessonId), !isCompleted); }
    catch (error) { setProgressSaveError(error.message); }
    finally { setSavingProgress(false); }
  };

  // Find previous and next lessons
  const allLessons = currentCourse.lessons || [];
  const currentIndex = allLessons.findIndex((l) => l.id === Number(lessonId));
  const prevLesson = currentIndex > 0 ? allLessons[currentIndex - 1] : null;
  const nextLesson =
    currentIndex >= 0 && currentIndex < allLessons.length - 1
      ? allLessons[currentIndex + 1]
      : null;

  // Navigation handlers
  const navigateToLesson = (targetId) => {
    window.location.hash = `#lesson?courseId=${courseId}&lessonId=${targetId}`;
  };

  const handleBackToCurriculum = () => {
    window.location.hash = "#learn";
  };

  // Execute the lesson source using the shared backend sandbox.
  const handleRunCode = async () => {
    if (isRunning) return;
    setIsRunning(true);
    setOutputLog("Running on the server…");
    setExecTime("—");
    try {
      const result = await api.runCode(languageKey(lessonData.codeSandbox.language || "python"), userCode,
        lessonData.codeSandbox.sampleInput || "");
      if (!result?.status) throw new Error("The server returned no execution result.");
      setExecTime(result.executionTimeMs == null ? "—" : `${result.executionTimeMs.toFixed(1)} ms`);
      setOutputLog([result.status, result.errorMessage || result.output || "(No output)"].join("\n"));
      setHasRun(true);
    } catch (error) {
      setOutputLog(error.message);
      setHasRun(true);
    } finally { setIsRunning(false); }
  };

  const handleResetCode = () => {
    if (lessonData?.codeSandbox) {
      setUserCode(lessonData.codeSandbox.starterCode);
      setOutputLog("Click Run to execute on the server.");
      setHasRun(false);
      setExecTime("—");
    }
  };

  if (!COURSES.some((course) => course.id === courseId && course.lessons.some((lesson) => lesson.id === Number(lessonId)))) {
    return <main className="api-state"><h1>Lesson not found</h1><a href="#learn">Back to courses</a></main>;
  }

  // Line count for code gutter
  const lineCount = userCode.split("\n").length;

  if (progressLoading || progressError) return (
    <main className="algoviz-lesson-page" aria-busy={progressLoading}>
      <ApiState loading={progressLoading} error={progressError} onRetry={retry} label="Loading lesson progress…" page />
    </main>
  );

  return (
    <div className="algoviz-lesson-page">
      {/* 1. TOP BREADCRUMB & CONTEXT TOOLBAR */}
      <header className="lesson-top-bar">
        <div className="lesson-top-container">
          <nav className="lesson-breadcrumb" aria-label="Breadcrumb">
            <a href="#problems" className="lesson-crumb-link">
              Problems
            </a>
            <span className="crumb-sep">/</span>
            <a href="#learn" className="lesson-crumb-link">
              Learn
            </a>
            <span className="crumb-sep">/</span>
            <span className="lesson-crumb-current">{currentCourse.title}</span>
            <span className="crumb-sep">/</span>
            <span className="lesson-crumb-badge">Lesson {lessonId}</span>
          </nav>

          <div className="lesson-header-row">
            <div className="lesson-header-left">
              <span className="lesson-badge-category">
                {currentCourse.category}
              </span>
              <span className={`lesson-badge-diff diff--${currentCourse.difficulty.toLowerCase()}`}>
                {currentCourse.difficulty}
              </span>
              <span className="lesson-badge-meta">⏱ {lessonData.duration}</span>
              {isCompleted ? (
                <span className="lesson-status-tag is-done">
                  ✓ Completed
                </span>
              ) : (
                <span className="lesson-status-tag is-pending">
                  {isAuthenticated ? "In Progress" : "Preview"}
                </span>
              )}
            </div>

            <div className="lesson-header-actions">
              <button
                type="button"
                className="lesson-btn-secondary"
                onClick={handleBackToCurriculum}
              >
                ← Back to Courses
              </button>
              <button
                type="button"
                className={`lesson-btn-check ${isCompleted ? "is-active" : ""}`}
                onClick={toggleCompleted}
                disabled={savingProgress || progressLoading || Boolean(progressError)}
              >
                {savingProgress ? <ProgressView size="sm" label="Saving…" /> : !isAuthenticated ? "Sign in to save progress" : isCompleted ? "✓ Completed" : "Mark as Complete"}
              </button>
            </div>
          </div>

          <ApiState error={progressSaveError} />
          <h1 className="lesson-main-title">
            Lesson {lessonId}: {lessonData.title}
          </h1>
          <p className="lesson-overview-lead">{lessonData.overview}</p>
        </div>
      </header>

      {/* 2. MAIN NOTEBOOK CANVAS */}
      <main className="lesson-canvas-container">
        <div className="lesson-canvas-grid">
          {/* LEFT: KAGGLE NOTEBOOK BODY */}
          <article className="kaggle-notebook">
            {/* CELL 1: MARKDOWN SECTIONS */}
            {lessonData.markdownSections?.map((sec, idx) => (
              <section key={sec.id || idx} className="notebook-cell notebook-markdown-cell">
                <div className="notebook-cell-gutter">
                  <span className="gutter-icon">¶</span>
                </div>
                <div className="notebook-cell-content">
                  <h2 className="markdown-cell-heading">{sec.title}</h2>
                  <div className="markdown-prose">
                    {sec.content.split("\n\n").map((para, pIdx) => {
                      if (para.startsWith(">")) {
                        return (
                          <blockquote key={pIdx} className="markdown-callout">
                            <span className="callout-symbol">💡</span>
                            <div>
                              {para
                                .replace(/^>\s*/, "")
                                .split("\n")
                                .map((line, lIdx) => (
                                  <p key={lIdx}>{formatInline(line.replace(/^>\s*/, ""))}</p>
                                ))}
                            </div>
                          </blockquote>
                        );
                      }
                      if (para.startsWith("|")) {
                        // Render table
                        const rows = para.trim().split("\n");
                        const headers = rows[0]
                          .split("|")
                          .map((c) => c.trim())
                          .filter(Boolean);
                        const dataRows = rows
                          .slice(2)
                          .map((r) =>
                            r
                              .split("|")
                              .map((c) => c.trim())
                              .filter(Boolean)
                          );
                        return (
                          <div key={pIdx} className="markdown-table-wrapper">
                            <table className="markdown-table">
                              <thead>
                                <tr>
                                  {headers.map((h, hIdx) => (
                                    <th key={hIdx}>{formatInline(h)}</th>
                                  ))}
                                </tr>
                              </thead>
                              <tbody>
                                {dataRows.map((row, rIdx) => (
                                  <tr key={rIdx}>
                                    {row.map((cell, cIdx) => (
                                      <td key={cIdx}>{formatInline(cell)}</td>
                                    ))}
                                  </tr>
                                ))}
                              </tbody>
                            </table>
                          </div>
                        );
                      }
                      if (para.startsWith("- ") || para.startsWith("* ")) {
                        const items = para.split("\n").filter((l) => l.trim().length > 0);
                        return (
                          <ul key={pIdx} className="markdown-bullet-list">
                            {items.map((it, iIdx) => (
                              <li key={iIdx}>{formatInline(it.replace(/^[-*]\s+/, ""))}</li>
                            ))}
                          </ul>
                        );
                      }
                      if (para.startsWith("```")) {
                        const codeLines = para
                          .replace(/^```[a-z]*\n?/, "")
                          .replace(/```$/, "");
                        return (
                          <pre key={pIdx} className="markdown-code-block">
                            <code>{codeLines}</code>
                          </pre>
                        );
                      }
                      return <p key={pIdx}>{formatInline(para)}</p>;
                    })}
                  </div>
                </div>
              </section>
            ))}

            {/* CELL 2: KAGGLE INTERACTIVE CODE RUNNER CELL */}
            {lessonData.codeSandbox && (
              <section className="notebook-cell notebook-code-cell">
                {/* IN PROMPT GUTTER */}
                <div className="notebook-cell-gutter">
                  <span className="in-prompt-label">[In 1]</span>
                </div>

                <div className="notebook-cell-content">
                  {/* Code Cell Header */}
                  <div className="code-cell-toolbar">
                    <div className="code-cell-tag-group">
                      <span className="code-lang-pill">
                        <svg
                          width="14"
                          height="14"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <polyline points="16 18 22 12 16 6" />
                          <polyline points="8 6 2 12 8 18" />
                        </svg>
                        Python 3.12
                      </span>
                      <span className="code-sandbox-desc">
                        {lessonData.codeSandbox.title}
                      </span>
                    </div>

                    <div className="code-cell-btn-group">
                      <button
                        type="button"
                        className="btn-code-reset"
                        onClick={handleResetCode}
                        title="Reset code to original starter"
                      >
                        <svg
                          width="13"
                          height="13"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                        >
                          <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8" />
                          <path d="M3 3v5h5" />
                        </svg>
                        <span>Reset</span>
                      </button>

                      <button
                        type="button"
                        className={`btn-code-run ${isRunning ? "is-running" : ""}`}
                        onClick={handleRunCode}
                        disabled={isRunning}
                        title="Run this cell (Ctrl+Enter)"
                      >
                        {isRunning ? (
                          <>
                            <ProgressView size="sm" label="Running…" />
                          </>
                        ) : (
                          <>
                            <svg
                              width="13"
                              height="13"
                              viewBox="0 0 24 24"
                              fill="currentColor"
                            >
                              <polygon points="5 3 19 12 5 21 5 3" />
                            </svg>
                            <span>Run Cell</span>
                          </>
                        )}
                      </button>
                    </div>
                  </div>

                  {/* Code Editor Box */}
                  <div className="code-editor-surface">
                    <div className="code-gutter-numbers" aria-hidden="true">
                      {Array.from({ length: lineCount }).map((_, i) => (
                        <div key={i} className="gutter-num">
                          {i + 1}
                        </div>
                      ))}
                    </div>
                    <textarea
                      className="code-textarea"
                      value={userCode}
                      onChange={(e) => setUserCode(e.target.value)}
                      spellCheck="false"
                      aria-label="Interactive Python code editor"
                    />
                  </div>

                  {/* OUT PROMPT: OUTPUT SECTION */}
                  <div className="notebook-output-container" aria-busy={isRunning}>
                    <div className="output-header-bar">
                      <div className="output-header-left">
                        <span className="out-prompt-label">[Out 1]</span>
                        <span className="output-status-pill">
                          {hasRun ? `Executed in ${execTime}` : "Output"}
                        </span>
                      </div>
                      <span className="output-tip">stdout stream</span>
                    </div>
                    {isRunning ? <ApiState loading label="Running lesson code…" /> : (
                      <pre className="output-terminal-display"><code>{outputLog}</code></pre>
                    )}
                  </div>
                </div>
              </section>
            )}

            {/* CELL 3: INTERACTIVE CHECKPOINT QUIZZES */}
            {lessonData.quizzes && lessonData.quizzes.length > 0 && (
              <section className="notebook-cell notebook-quiz-cell" id="quiz">
                <div className="notebook-cell-gutter">
                  <span className="gutter-icon">🎯</span>
                </div>

                <div className="notebook-cell-content">
                  <div className="quiz-cell-header">
                    <span className="quiz-badge">Checkpoint Assessment</span>
                    <h2 className="quiz-cell-title">Knowledge Verification Lab</h2>
                    <p className="quiz-cell-subtitle">
                      Test your understanding of the algorithmic invariants covered in this lesson.
                    </p>
                  </div>

                  <div className="quiz-questions-stack">
                    {lessonData.quizzes.map((quiz, qIdx) => {
                      const selectedIdx = selectedAnswers[quiz.id];
                      const isAnswered = selectedIdx !== undefined;
                      const isCorrect = selectedIdx === quiz.correctIndex;

                      return (
                        <div key={quiz.id} className="quiz-card-item">
                          <div className="quiz-question-header">
                            <span className="quiz-q-num">Q{qIdx + 1}</span>
                            <h3 className="quiz-question-text">{quiz.question}</h3>
                          </div>

                          <div className="quiz-options-list" role="radiogroup">
                            {quiz.options.map((option, optIdx) => {
                              const isThisSelected = selectedIdx === optIdx;
                              let optionStateClass = "";
                              if (isAnswered) {
                                if (optIdx === quiz.correctIndex) {
                                  optionStateClass = "is-correct-target";
                                } else if (isThisSelected) {
                                  optionStateClass = "is-wrong-selection";
                                }
                              }

                              return (
                                <button
                                  key={optIdx}
                                  type="button"
                                  className={`quiz-option-btn ${isThisSelected ? "is-selected" : ""} ${optionStateClass}`}
                                  onClick={() =>
                                    setSelectedAnswers((prev) => ({
                                      ...prev,
                                      [quiz.id]: optIdx,
                                    }))
                                  }
                                >
                                  <span className="quiz-option-radio">
                                    {String.fromCharCode(65 + optIdx)}
                                  </span>
                                  <span className="quiz-option-label">{option}</span>
                                  {isAnswered && optIdx === quiz.correctIndex && (
                                    <span className="quiz-feedback-glyph is-correct">✓</span>
                                  )}
                                  {isAnswered && isThisSelected && !isCorrect && (
                                    <span className="quiz-feedback-glyph is-wrong">✕</span>
                                  )}
                                </button>
                              );
                            })}
                          </div>

                          {isAnswered && (
                            <div
                              className={`quiz-explanation-box ${isCorrect ? "is-correct" : "is-wrong"}`}
                            >
                              <div className="explanation-title">
                                {isCorrect ? "✓ Correct!" : "⚠️ Not quite — Try again:"}
                              </div>
                              <p className="explanation-text">{quiz.explanation}</p>
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              </section>
            )}

            {/* CELL 4: VISUALIZER BRIDGE (IF APPLICABLE) */}
            {lessonData.targetProblemId && (
              <section className="notebook-cell notebook-visualizer-bridge">
                <div className="notebook-cell-gutter">
                  <span className="gutter-icon">⚡</span>
                </div>
                <div className="notebook-cell-content">
                  <div className="visualizer-bridge-card">
                    <div className="bridge-card-text">
                      <span className="bridge-badge">Interactive Visualizer</span>
                      <h3 className="bridge-title">
                        Watch this algorithm run with live execution traces
                      </h3>
                      <p className="bridge-desc">
                        Step through pointer movements, array swaps, and state transitions
                        frame-by-frame in the AlgoViz Visualizer Studio.
                      </p>
                    </div>
                    <a
                      href={`#problem-details?id=${lessonData.targetProblemId}&tab=visualizer`}
                      className="bridge-action-btn"
                    >
                      <span>Launch Visualizer Studio →</span>
                    </a>
                  </div>
                </div>
              </section>
            )}

            {/* CELL 5: SUMMARY & KEY TAKEAWAYS */}
            {lessonData.keyTakeaways && (
              <section className="notebook-cell notebook-summary-cell">
                <div className="notebook-cell-gutter">
                  <span className="gutter-icon">📌</span>
                </div>
                <div className="notebook-cell-content">
                  <h3 className="summary-heading">Summary &amp; Key Invariants</h3>
                  <ul className="summary-list">
                    {lessonData.keyTakeaways.map((item, idx) => (
                      <li key={idx} className="summary-item">
                        <span className="summary-bullet">✦</span>
                        <span>{item}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </section>
            )}

            {/* BOTTOM NAV BAR */}
            <nav className="notebook-bottom-nav">
              <div className="bottom-nav-left">
                {prevLesson ? (
                  <button
                    type="button"
                    className="nav-lesson-btn"
                    onClick={() => navigateToLesson(prevLesson.id)}
                  >
                    <span className="nav-arrow">←</span>
                    <div className="nav-text-col">
                      <span className="nav-sub">Previous Lesson</span>
                      <span className="nav-title">{prevLesson.title}</span>
                    </div>
                  </button>
                ) : (
                  <button
                    type="button"
                    className="nav-lesson-btn"
                    onClick={handleBackToCurriculum}
                  >
                    <span className="nav-arrow">←</span>
                    <div className="nav-text-col">
                      <span className="nav-sub">Back</span>
                      <span className="nav-title">Course Overview</span>
                    </div>
                  </button>
                )}
              </div>

              <div className="bottom-nav-center">
                <button
                  type="button"
                  className={`btn-complete-bottom ${isCompleted ? "is-finished" : ""}`}
                  onClick={toggleCompleted}
                disabled={savingProgress || progressLoading || Boolean(progressError)}
                >
                  {savingProgress ? <ProgressView size="sm" label="Saving…" /> : !isAuthenticated ? "Sign in to save progress" : isCompleted ? "✓ Completed" : "Mark Lesson Completed"}
                </button>
              </div>

              <div className="bottom-nav-right">
                {nextLesson ? (
                  <button
                    type="button"
                    className="nav-lesson-btn is-next"
                    onClick={() => navigateToLesson(nextLesson.id)}
                  >
                    <div className="nav-text-col align-right">
                      <span className="nav-sub">Next Lesson</span>
                      <span className="nav-title">{nextLesson.title}</span>
                    </div>
                    <span className="nav-arrow">→</span>
                  </button>
                ) : (
                  <button
                    type="button"
                    className="nav-lesson-btn is-next"
                    onClick={handleBackToCurriculum}
                  >
                    <div className="nav-text-col align-right">
                      <span className="nav-sub">{currentCourse.progress === 100 ? "Course Complete!" : "End of course"}</span>
                      <span className="nav-title">View All Courses</span>
                    </div>
                    <span className="nav-arrow">✓</span>
                  </button>
                )}
              </div>
            </nav>
          </article>

          {/* RIGHT: STICKY SYLLABUS SIDEBAR */}
          <aside className="lesson-syllabus-sidebar">
            <div className="sidebar-sticky-panel">
              <div className="sidebar-header">
                <span className="sidebar-tag">Course Syllabus</span>
                <h3 className="sidebar-course-title">{currentCourse.title}</h3>
                <ProgressView value={currentCourse.progress} label="Course progress" showLabel showValue />
              </div>

              <div className="sidebar-lessons-nav">
                <div className="sidebar-nav-title">Lessons in this track:</div>
                <div className="sidebar-lessons-list">
                  {allLessons.map((les, idx) => {
                    const isCurrent = les.id === Number(lessonId);
                    const lesCompleted = les.status === "completed";

                    return (
                      <button
                        key={les.id}
                        type="button"
                        className={`sidebar-lesson-item ${isCurrent ? "is-current" : ""} ${lesCompleted ? "is-completed" : ""}`}
                        onClick={() => navigateToLesson(les.id)}
                      >
                        <span className="sidebar-item-index">
                          {lesCompleted ? (
                            <span className="done-check">✓</span>
                          ) : (
                            idx + 1
                          )}
                        </span>
                        <div className="sidebar-item-info">
                          <span className="sidebar-item-name">{les.title}</span>
                          <span className="sidebar-item-duration">{les.duration}</span>
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="sidebar-footer-card">
                <div className="sidebar-footer-heading">Need help?</div>
                <p className="sidebar-footer-copy">
                  Review the lesson code, run live tests, or jump to the step visualizer.
                </p>
                <a href="#learn" className="sidebar-footer-link">
                  Browse All Tracks →
                </a>
              </div>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
}
