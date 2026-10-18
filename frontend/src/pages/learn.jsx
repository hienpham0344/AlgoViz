import { useState } from "react";
import { NavigationBar } from "../components/index.js";
import ResizableColumns from "../components/ResizableColumns.jsx";
import "./learn.css";

const MODULES = [
  { title: "Bubble Sort", status: "Completed", quiz: "Quiz: 80%", description: "Iteratively steps through the list, compares adjacent items and swaps them if they are in the wrong order.", worst: "O(n²)", best: "O(n)", space: "O(1)", action: "Open Practice Quiz", icon: "sort" },
  { title: "Merge Sort", status: "In Progress", quiz: "Step 4 of 8", description: "Efficient, general-purpose, comparison-based divide-and-conquer algorithm with guaranteed logarithmic depth.", worst: "O(n log n)", best: "O(n log n)", space: "O(n)", action: "Browse Practice Problems", icon: "merge" },
  { title: "Quick Sort", status: "Not Started", quiz: "Est. 20 mins", description: "Selects a pivot and partitions the other elements into two sub-arrays, according to whether they are less or...", worst: "O(n log n)", best: "O(n²)", space: "O(log n)", action: "Browse Practice Problems", icon: "quick" },
];

const QUIZ_OPTIONS = [
  ["O(n)", "Best-case condition when array is already sorted with an early swap flag.", "Linear time"],
  ["O(log n)", "Requires only binary searches, not full iterative pairwise passes.", "Logarithmic time"],
  ["O(n²)", "Occurs when elements are in reverse order, necessitating n(n - 1) / 2 comparisons and swaps.", "Quadratic time"],
  ["O(n log n)", "A hypothetical lower bound for comparison sorting algorithms like Merge or Quick sort.", "Linearithmic time"],
];

function LearnIcon({ name }) {
  return <span className={`learn-icon learn-icon--${name}`} aria-hidden="true" />;
}

function ProgressTracker() {
  return <section className="learn-progress-card"><div className="learn-progress-top"><div><span className="learn-track-label">Overall Learning Track</span><strong>17 <small>of 25 Modules Completed</small></strong></div><div className="learn-progress-meta"><span>Current Topic: <b>Sorting Algorithms</b></span><span>Quiz Accuracy: <b>84%</b></span><span>XP Earned: <strong>2,450 XP</strong></span></div></div><div className="learn-progress-bar"><i /></div><div className="learn-progress-bottom"><span>Progress: Modules Track</span><b>68% Complete</b></div></section>;
}

function Complexity({ label, value, accent }) {
  return <div className="learn-complexity"><span>{label}</span><strong className={accent ? "is-accent" : ""}>{value}</strong></div>;
}

function ModuleCard({ module, onAction }) {
  return <article className={`learn-module-card learn-module-card--${module.status.toLowerCase().replace(" ", "-")}`}><div className="learn-module-top"><span className="learn-module-status"><i />{module.status}</span><span className="learn-module-quiz">{module.quiz}</span></div><h3>{module.title}</h3><p>{module.description}</p><div className="learn-complexity-row"><Complexity label="Worst" value={module.worst} /><Complexity label="Best" value={module.best} accent={module.status === "Completed"} /><Complexity label="Space" value={module.space} accent={module.status === "In Progress"} /></div><button type="button" onClick={onAction}>{module.status === "Completed" && <LearnIcon name="refresh" />}{module.action} <span>›</span></button></article>;
}

function QuizPanel({ selectedAnswer, onSelect, onCheck, onContinue, onReview }) {
  return <ResizableColumns as="section" className="learn-quiz-grid" initialPrimarySize="1.55fr" minPrimaryWidth={320} minSecondaryWidth={260} resizeLabel="Đổi độ rộng câu hỏi và kết quả"><article className="learn-quiz-card independent-scroll-region" id="practice-quiz" tabIndex={0} aria-label="Assessment question"><div className="learn-quiz-header"><span>Practice - Bubble Sort</span><small>Available question</small><b>Untimed</b></div><div className="learn-quiz-content"><span className="learn-quiz-topic">Topic: Complexity Analysis</span><h2>What is the worst-case time complexity of Bubble Sort?</h2><div className="learn-quiz-options">{QUIZ_OPTIONS.map(([answer, copy, note]) => <button className={selectedAnswer === answer ? "is-selected" : ""} type="button" key={answer} onClick={() => onSelect(answer)}><i /> <span><strong>{answer}</strong>{selectedAnswer === answer && <em>Your Selection</em>}<small>{copy}</small></span><label>{note}</label></button>)}</div></div><div className="learn-quiz-footer"><button type="button" disabled title="Only one practice question is available">‹ Previous Question</button><span>Saved for this visit</span><button className="is-primary" type="button" onClick={onCheck}>Check Answer →</button></div></article><article className="learn-result-card independent-scroll-region" id="practice-result" tabIndex={0} aria-label="Assessment results"><div className="learn-result-heading"><h2>🎉 Practice Result</h2><span>Bubble Sort</span></div><p>Select an answer and check it to review your understanding of Bubble Sort complexity.</p><div className="learn-score"><small>Reference Result</small><strong>O(n²)</strong><b>Worst-case complexity</b><div><span>Complexity Review</span><i /></div></div><div className="learn-mastery"><b>↗</b><p><strong>Keep practicing!</strong><br />Use the problem catalog to apply algorithm concepts in the runnable workspace.</p></div><button className="is-primary" type="button" onClick={onContinue}>Continue to Problems →</button><button type="button" onClick={onReview}>◉ Review Answer &amp; Rationale</button></article></ResizableColumns>;
}

function LearnFooter() {
  return <footer className="learn-footer"><div className="learn-footer-inner"><div><a className="learn-footer-brand" href="#top"><span>AV</span> AlgoViz</a><p>AlgoViz Interactive Learning Platform</p><small>© 2025 AlgoViz. Designed with clarity for algorithm mastery.</small></div><nav><a href="#curriculum">Curriculum Roadmap</a><a href="#cheatsheet">Complexity Cheatsheet</a><a href="#help">Help &amp; Docs</a></nav></div></footer>;
}

function LearnPage({ userName = "Your name" }) {
  const [selectedAnswer, setSelectedAnswer] = useState("");
  const [announcement, setAnnouncement] = useState("");
  const scrollTo = (id) => document.getElementById(id)?.scrollIntoView({ behavior: "smooth", block: "start" });
  const handleModule = (module) => {
    if (module.title === "Bubble Sort") {
      scrollTo("practice-quiz");
      setAnnouncement("Bubble Sort practice opened.");
      return;
    }
    window.location.assign("#problems");
  };
  const handleCheck = () => {
    if (!selectedAnswer) {
      setAnnouncement("Choose an answer before checking it.");
      return;
    }
    setAnnouncement(selectedAnswer === "O(n²)" ? "Correct. Bubble Sort is O(n squared) in the worst case." : "Not quite. Review why Bubble Sort is O(n squared) in the worst case.");
    scrollTo("practice-result");
  };
  return <main className="learn-page" id="top"><div className="learn-nav-shell"><NavigationBar active="Learn" userName={userName} className="learn-navigation" /></div><div className="learn-main learn-shell"><header className="learn-header"><div><div className="learn-breadcrumb"><a href="#top">Home</a><span>/</span><a href="#learn">Learn</a><span>/</span><strong>Sorting Algorithms</strong></div><h1>Learn <span>Algorithms</span></h1><p>Master fundamental computer science algorithms through interactive visualizations,<br /> step-by-step intuition builders, and retention mastery quizzes.</p></div><div className="learn-track-pill"><LearnIcon name="track" /><span>Active Track<strong>Algorithm Foundations v2.4</strong></span></div></header><ProgressTracker /><section className="learn-track-section"><div className="learn-section-heading"><div><span>Track 01</span><h2>Sorting Algorithms</h2><p>Understand comparison vs non-comparison based sorting routines, stable properties, and in-place space bounds.</p></div><a href="#problems">Browse sorting practice →</a></div><div className="learn-module-grid">{MODULES.map((module) => <ModuleCard key={module.title} module={module} onAction={() => handleModule(module)} />)}</div><div className="learn-mini-modules"><span>04. Insertion Sort <b>Adaptive</b></span><span>05. Heap Sort <b>In-place</b></span><span>06. Counting Sort <b>Non-comparison</b></span></div></section><section className="learn-assessment"><div className="learn-assessment-heading"><h2><i />Interactive Assessment Lab</h2><span>Module: Bubble Sort Mastery</span></div><QuizPanel selectedAnswer={selectedAnswer} onSelect={setSelectedAnswer} onCheck={handleCheck} onContinue={() => { window.location.assign("#problems"); }} onReview={() => { scrollTo("practice-quiz"); setAnnouncement("Review the selected answer and its rationale."); }} /></section></div><p className="sr-only" aria-live="polite">{announcement}</p><LearnFooter /></main>;
}

export default LearnPage;
