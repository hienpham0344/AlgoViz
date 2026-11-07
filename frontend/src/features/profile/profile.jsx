import { useCallback, useEffect, useState } from "react";
import { ProgressView } from "../../components/index.js";
import ApiState from "../../components/ApiState.jsx";
import { api, getUserData } from "../../services/api.js";
import { mapSubmission, profileStats } from "../../services/submissions.js";
import EditProfileModal from "./EditProfileModal.jsx";
import "./profile.css";

function ProfileAvatar() {
  return (
    <div className="profile-developer-avatar" aria-hidden="true">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75">
        <circle cx="12" cy="8" r="4" />
        <path d="M6 20v-2a6 6 0 0 1 12 0v2" />
      </svg>
    </div>
  );
}

function ProfileIcon({ name }) {
  return (
    <span className={`profile-icon profile-icon--${name}`} aria-hidden="true" />
  );
}

function ProfileHeader({ profile, onEdit }) {
  const displayName = profile?.displayName || "Developer";
  const email = profile?.email;

  return (
    <section className="profile-card profile-header-card">
      <div className="profile-identity">
        <div className="profile-portrait-wrap">
          <ProfileAvatar />
          <i className="profile-status-dot" aria-hidden="true" />
        </div>
        <div className="profile-details">
          <div className="profile-name-line">
            <h1>{displayName}</h1>
            <span className="profile-role">
              <i className="profile-role-dot" aria-hidden="true" /> {profile?.roleTitle || "Learner"}
            </span>
          </div>
          {email ? <a className="profile-email" href={`mailto:${email}`}>{email}</a> : <span className="profile-email">Email unavailable</span>}
          <p>
            {profile?.bio ||
              "Chưa có giới thiệu."}
          </p>
          <div className="profile-badges">
            <span>📍 {profile?.location || "Chưa cập nhật"}</span>
            <span>⚡ {`${profile?.points ?? 0} points`}</span>
            <span>🎯 Target: {profile?.target || "Chưa cập nhật"}</span>
          </div>
        </div>
      </div>
      <div className="profile-actions">
        <button
          className="profile-button profile-button--primary"
          type="button"
          onClick={onEdit}
          title="Chỉnh sửa thông tin hồ sơ cá nhân"
        >
          <ProfileIcon name="edit" /> Edit Profile
        </button>
        <button
          className="profile-button profile-button--danger"
          type="button"
          onClick={() => {
            api.logout().catch(() => {});
          }}
          title="Sign out of your account"
        >
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <polyline points="16 17 21 12 16 7" />
            <line x1="21" y1="12" x2="9" y2="12" />
          </svg>
          Logout
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

function Statistics({ stats, points }) {
  return <section className="profile-section">
    <div className="profile-section-heading"><h2>Statistics</h2><span className="profile-updated">From your submissions</span></div>
    <div className="profile-metrics">
      <MetricCard label="Points" value={points ?? 0} note="Earned from accepted solutions" detail="Awarded for first solves" icon="clock" />
      <MetricCard label="Attempts" value={stats.attempts} note={`${stats.successRate}% accepted`} detail="All submissions" icon="bolt" accent="profile-metric--purple" />
      <MetricCard label="Problems Solved" value={stats.solved} note={`${stats.inProgress} in progress`} detail={`${stats.total} problems available`} icon="check" accent="profile-metric--mint" />
    </div>
  </section>;
}
function Activity({ stats }) {
  const [hovered, setHovered] = useState("");
  const total = stats.cells.reduce((sum, cell) => sum + cell.count, 0);
  return <section className="profile-card profile-activity" aria-labelledby="activity-heading">
    <div className="profile-activity-header"><h2 id="activity-heading">Learning Activity</h2><span>Past 52 weeks</span></div>
    <div className="profile-activity-kpis">
      {[["Current streak", `${stats.currentStreak} ${stats.currentStreak === 1 ? "day" : "days"}`], ["Active days", stats.cells.filter((cell) => cell.count).length],
        ["Submissions", total], ["Acceptance", `${stats.successRate}%`]].map(([label, value]) => (
        <div className="activity-kpi-item" key={label}><div className="activity-kpi-text"><span>{label}</span><strong>{value}</strong></div></div>
      ))}
    </div>
    <div className="profile-heatmap-box"><div className="profile-heatmap-scroll">
      <div className="profile-heat-body"><div className="profile-heat-weekdays" aria-hidden="true">
        <span>Sun</span><span>Mon</span><span>Tue</span><span>Wed</span><span>Thu</span><span>Fri</span><span>Sat</span>
      </div><div className="profile-heat-grid" role="img" aria-label={`${total} submissions over the past 52 weeks`}>
        {stats.cells.map((cell) => {
          const label = `${cell.date}: ${cell.count} submissions`;
          return <i key={cell.date} className={`heat-level-${Math.min(4, cell.count)}`} title={label}
            onMouseEnter={() => setHovered(label)} onMouseLeave={() => setHovered("")} />;
        })}
      </div></div>
    </div><div className="profile-heat-footer"><span>{hovered || `${total} submissions in the past 52 weeks`}</span>
      <div className="profile-heat-legend"><span>Less</span>{[0, 1, 2, 3, 4].map((level) => <i key={level} className={`heat-level-${level}`} />)}<span>More</span></div>
    </div></div>
  </section>;
}
function LearningProgress({ stats }) {
  const percent = stats.total ? Math.round(stats.solved / stats.total * 100) : 0;
  return <section className="profile-card profile-learning">
    <div className="profile-panel-heading"><h2>Learning Progress</h2><b>{stats.solved} / {stats.total} Problems Solved <em>{percent}% Total</em></b></div>
    <div className="profile-total-progress-wrap"><ProgressView variant="linear" value={percent} max={100} label="Problems solved" className="profile-total-progress-el" /></div>
    <div className="profile-progress-stats">
      {[["Completed", stats.solved], ["In Progress", stats.inProgress], ["Not Started", Math.max(0, stats.total - stats.solved - stats.inProgress)]].map(([label, count]) => (
        <div key={label}><b>{label}</b><strong>{count}</strong></div>
      ))}
    </div>
    <div className="profile-topic-heading">Topic Progress</div><div className="profile-topics">
      {stats.topics.map((topic) => <div key={topic.name}><span>{topic.name}<b>{topic.solved} / {topic.total}</b></span>
        <i style={{ width: `${topic.total ? topic.solved / topic.total * 100 : 0}%` }} /></div>)}
    </div>
  </section>;
}
function AnalysisHistory({ submissions }) {
  return <section className="profile-card profile-history">
    <div className="profile-panel-heading"><h2>Recent Submissions</h2><a href="#history">View History ›</a></div>
    {!submissions.length && <p>No submissions yet. <a href="#problems">Try a problem</a>.</p>}
    <div className="profile-history-list">{submissions.slice(0, 5).map((item) => {
      const row = mapSubmission(item);
      return <article key={row.id}><span className="profile-language">{row.language}</span><div><h3>{row.algorithm}</h3><p>{row.date}</p></div>
        <span>{row.rawStatus}</span><a href={`#problem-details?id=${row.problemId}&tab=submissions`}>View →</a></article>;
    })}</div>
  </section>;
}
export default function ProfilePage() {
  const [profile, setProfile] = useState(() => getUserData());
  const [submissions, setSubmissions] = useState([]);
  const [problems, setProblems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadKey, setReloadKey] = useState(0);
  const [modalTab, setModalTab] = useState("profile");
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const closeModal = useCallback(() => {
    setIsEditModalOpen(false);
    if (window.location.hash.includes("tab=")) window.history.replaceState(null, "", "#profile");
  }, []);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    Promise.all([api.getProfile(), api.getSubmissionHistory(), api.getProblems()]).then(([user, history, catalog]) => {
      if (!user?.id || !Array.isArray(history) || !Array.isArray(catalog)) throw new Error("Invalid profile response.");
      if (active) { setProfile({ ...user, displayName: user.fullName || user.username }); setSubmissions(history); setProblems(catalog); }
    }).catch((cause) => { if (active) setError(cause.message); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reloadKey]);
  useEffect(() => {
    const openSecurity = () => { setModalTab("security"); setIsEditModalOpen(true); };
    const checkHash = () => {
      if (window.location.hash.includes("tab=security")) openSecurity();
      else if (/tab=edit|action=edit/.test(window.location.hash)) { setModalTab("profile"); setIsEditModalOpen(true); }
    };
    checkHash();
    window.addEventListener("hashchange", checkHash);
    window.addEventListener("algoviz:open-security", openSecurity);
    return () => { window.removeEventListener("hashchange", checkHash); window.removeEventListener("algoviz:open-security", openSecurity); };
  }, []);
  const saveProfile = async (updated) => {
    const user = await api.updateProfile({ ...updated, fullName: updated.displayName });
    setProfile({ ...user, displayName: user.fullName || user.username });
  };
  const stats = profileStats(submissions, problems);
  return <main className="profile-page" id="top"><div className="profile-main profile-shell">
    <div className="profile-breadcrumb"><a href="#problems">Home</a><span>/</span><strong>Profile</strong></div>
    <ApiState loading={loading} error={error} onRetry={() => setReloadKey((key) => key + 1)} label="Loading profile…" page />
    {!loading && !error && <>
      <ProfileHeader profile={profile} onEdit={() => { setModalTab("profile"); setIsEditModalOpen(true); }} />
      <Statistics stats={stats} points={profile.points} /><Activity stats={stats} /><LearningProgress stats={stats} /><AnalysisHistory submissions={submissions} />
      <EditProfileModal key={isEditModalOpen ? modalTab : "closed"} isOpen={isEditModalOpen} onClose={closeModal}
        initialTab={modalTab} profileData={profile} onSave={saveProfile} />
    </>}
  </div></main>;
}
