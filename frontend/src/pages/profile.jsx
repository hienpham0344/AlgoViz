import { useState } from "react";
import { NavigationBar } from "../components/index.js";
import "./profile.css";

const PROFILE_PORTRAIT =
  "https://www.figma.com/api/mcp/asset/796666dd-b9fb-4805-8cf7-5c9095179f81.png";
const ACTIVITY = Array.from({ length: 28 }, (_, index) => (index * 7) % 5);
const HISTORY = [
  ["Bubble Sort Analysis", "C++", "Aug 20, 2025", "94%", "O(n²) Verified"],
  [
    "Binary Search Recursive",
    "Java",
    "Aug 19, 2025",
    "100%",
    "O(log n) Passed",
  ],
  ["Quick Sort (Lomuto Partition)", "Py", "Aug 18, 2025", "88%", "O(n log n)"],
];

function ProfileIcon({ name }) {
  return (
    <span className={`profile-icon profile-icon--${name}`} aria-hidden="true" />
  );
}

function ProfileHeader({ onEdit }) {
  return (
    <section className="profile-card profile-header-card">
      <div className="profile-glow profile-glow--violet" aria-hidden="true" />
      <div className="profile-glow profile-glow--cyan" aria-hidden="true" />
      <div className="profile-identity">
        <div className="profile-portrait-wrap">
          <img src={PROFILE_PORTRAIT} alt="Nguyen Ai My" />
          <i />
        </div>
        <div className="profile-details">
          <div className="profile-name-line">
            <h1>Nguyễn Ái My</h1>
            <span className="profile-role">
              <i /> Pro Algorithmic Learner
            </span>
          </div>
          <a className="profile-email" href="mailto:amy@example.com">
            amy@example.com
          </a>
          <p>
            Computer Science student at DevHub Academy. Specializing in
            algorithm optimization, complexity analysis, and competitive
            programming visualization.
          </p>
          <div className="profile-badges">
            <span>📍 Ho Chi Minh City</span>
            <span>⚡ Top 5% Rank</span>
            <span>🎯 Target: Senior DSA Specialist</span>
          </div>
        </div>
      </div>
      <div className="profile-actions">
        <button
          className="profile-button profile-button--primary"
          type="button"
          onClick={onEdit}
        >
          <ProfileIcon name="edit" /> Edit Profile
        </button>
        <button
          className="profile-icon-button"
          type="button"
          aria-label="Share profile"
        >
          <ProfileIcon name="share" />
        </button>
      </div>
    </section>
  );
}

function MetricCard({ label, value, detail, note, icon, accent = "" }) {
  return (
    <article className={`profile-metric ${accent}`}>
      <div className="profile-metric-top">
        <div>
          <span>{label}</span>
          <strong>{value}</strong>
        </div>
        <i className="profile-metric-icon">
          <ProfileIcon name={icon} />
        </i>
      </div>
      <div className="profile-metric-bottom">
        <span>{note}</span>
        <small>{detail}</small>
      </div>
    </article>
  );
}

function Statistics() {
  return (
    <section className="profile-section">
      <div className="profile-section-heading">
        <div>
          <h2>
            <ProfileIcon name="stats" /> Statistics
          </h2>
          <p>
            Real-time compilation of algorithmic problem executions and focus
            time
          </p>
        </div>
        <span className="profile-updated">Updated Today</span>
      </div>
      <div className="profile-metrics">
        <MetricCard
          label="Study Time"
          value="12h 30m"
          note="↗ +2.4 hrs vs last week"
          detail="Daily avg: 1.8h"
          icon="clock"
        />
        <MetricCard
          label="Attempts"
          value="48 Attempts"
          note="+12 this week"
          detail="87.5% success rate"
          icon="bolt"
          accent="profile-metric--purple"
        />
        <MetricCard
          label="Problems Solved"
          value="18 Completed"
          note="40% of Total Set"
          detail="Goal: 45 problems"
          icon="check"
          accent="profile-metric--mint"
        />
      </div>
    </section>
  );
}

function Activity() {
  return (
    <section className="profile-card profile-activity">
      <div className="profile-panel-heading">
        <h2>
          📊 Learning Activity <small>Last 12 Weeks</small>
        </h2>
        <div>
          <span>
            Current Streak: <b>7 Days</b>
          </span>
          <span>
            Max Streak: <b>21 Days</b>
          </span>
          <span>
            Active Days: <b>84%</b>
          </span>
        </div>
      </div>
      <div className="profile-heatmap">
        <div className="profile-heat-labels">
          <span>Mon</span>
          <span>Wed</span>
          <span>Fri</span>
          <span>Sun</span>
        </div>
        <div className="profile-heat-grid">
          {ACTIVITY.map((level, index) => (
            <i className={`heat-level-${level}`} key={index} />
          ))}
        </div>
        <div className="profile-heat-legend">
          <span>Less</span>
          <i className="heat-level-0" />
          <i className="heat-level-1" />
          <i className="heat-level-2" />
          <i className="heat-level-3" />
          <i className="heat-level-4" />
          <span>More</span>
        </div>
      </div>
    </section>
  );
}

function LearningProgress() {
  return (
    <section className="profile-card profile-learning">
      <div className="profile-panel-heading">
        <h2>
          📚 Learning Progress{" "}
          <small>
            Track your curriculum progress across standard algorithm topics
          </small>
        </h2>
        <b>
          18 / 45 Problems Solved <em>40% Total</em>
        </b>
      </div>
      <div className="profile-total-progress">
        <i />
      </div>
      <div className="profile-progress-stats">
        <div>
          <b>
            <i /> Completed
          </b>
          <strong>18</strong>
          <span>Fully tested &amp; analyzed</span>
        </div>
        <div>
          <b>
            <i /> In Progress
          </b>
          <strong>5</strong>
          <span>Under active code review</span>
        </div>
        <div>
          <b>
            <i /> Not Started
          </b>
          <strong>22</strong>
          <span>Remaining algorithm modules</span>
        </div>
      </div>
      <div className="profile-topic-heading">Topic Mastery Breakdown</div>
      <div className="profile-topics">
        <div>
          <span>
            Sorting <b>8 / 10</b>
          </span>
          <i style={{ width: "80%" }} />
        </div>
        <div>
          <span>
            Searching <b>5 / 5</b>
          </span>
          <i style={{ width: "100%" }} />
        </div>
        <div>
          <span>
            Dynamic Prog. <b>3 / 15</b>
          </span>
          <i style={{ width: "20%" }} />
        </div>
        <div>
          <span>
            Graphs &amp; Trees <b>2 / 15</b>
          </span>
          <i style={{ width: "14%" }} />
        </div>
      </div>
    </section>
  );
}

function AnalysisHistory() {
  const [message, setMessage] = useState("");
  return (
    <section className="profile-card profile-history">
      <div className="profile-panel-heading">
        <h2>
          ◷ Analysis History{" "}
          <small>
            Recent visual algorithmic problem executions &amp; benchmark logs
          </small>
        </h2>
        <button
          type="button"
          onClick={() => {
            window.location.hash = "history";
          }}
        >
          View History ›
        </button>
      </div>
      <div className="profile-history-list">
        {HISTORY.map(([title, language, date, score, badge]) => (
          <article key={title}>
            <span
              className={`profile-language profile-language--${language.toLowerCase()}`}
            >
              {language}
            </span>
            <div>
              <strong>{title}</strong>
              <small>{date} · Runtime: 14ms · Memory: 1.8 MB</small>
              <em className="profile-history-badge">{badge}</em>
            </div>
            <div className="profile-history-score">
              <b>{score}</b>
              <small>Efficiency</small>
            </div>
            <button
              type="button"
              onClick={() => setMessage(`${title} details opened.`)}
            >
              View Details
            </button>
          </article>
        ))}
      </div>
      <p className="sr-only" aria-live="polite">
        {message}
      </p>
    </section>
  );
}

function ProfileFooter() {
  return (
    <footer className="profile-footer">
      <div>
        <strong>AlgoViz</strong>
        <small>Interactive Algorithm Learning &amp; Visualization Engine</small>
      </div>
      <nav>
        <a href="#curriculum">Curriculum</a>
        <a href="#visualizer">Visualizer</a>
        <a href="#history">History</a>
        <a href="#docs">Documentation</a>
        <a href="#community">Community</a>
        <a href="#privacy">Privacy Policy</a>
      </nav>
      <small>© 2025 AlgoMaster / AlgoViz. All rights reserved.</small>
    </footer>
  );
}

function ProfilePage({ userName = "Your name" }) {
  const [message, setMessage] = useState("");
  return (
    <main className="profile-page" id="top">
      <div className="profile-nav-shell">
        <NavigationBar
          active=""
          userName={userName}
          className="profile-navigation"
        />
      </div>
      <div className="profile-main profile-shell">
        <div className="profile-breadcrumb">
          <a href="#top">Home</a>
          <span>/</span>
          <strong>Profile</strong>
        </div>
        <ProfileHeader onEdit={() => setMessage("Profile editor opened.")} />
        <Statistics />
        <Activity />
        <LearningProgress />
        <AnalysisHistory />
        <p className="sr-only" aria-live="polite">
          {message}
        </p>
      </div>
      <ProfileFooter />
    </main>
  );
}

export default ProfilePage;
