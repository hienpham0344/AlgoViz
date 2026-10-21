import { useState, useEffect } from "react";
import { NavigationBar, ProgressView } from "../../components/index.js";
import { api } from "../../services/api.js";
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

const WEEKS_COUNT = 52;
const DAYS_PER_WEEK = 7;

const MONTH_LABELS = [
  { name: "Nov", colSpan: 4 },
  { name: "Dec", colSpan: 5 },
  { name: "Jan", colSpan: 4 },
  { name: "Feb", colSpan: 4 },
  { name: "Mar", colSpan: 5 },
  { name: "Apr", colSpan: 4 },
  { name: "May", colSpan: 5 },
  { name: "Jun", colSpan: 4 },
  { name: "Jul", colSpan: 4 },
  { name: "Aug", colSpan: 5 },
  { name: "Sep", colSpan: 4 },
  { name: "Oct", colSpan: 4 },
];

const WEEKDAY_LABELS = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

// 364 days with realistic coding pattern and active 7-day streak at the end
const ACTIVITY_52_WEEKS = Array.from({ length: WEEKS_COUNT * DAYS_PER_WEEK }, (_, i) => {
  // Last 7 days are active streak
  if (i >= WEEKS_COUNT * DAYS_PER_WEEK - 7) {
    return [2, 3, 4, 3, 4, 3, 4][(i - (WEEKS_COUNT * DAYS_PER_WEEK - 7)) % 7];
  }
  const dayOfWeek = i % 7;
  const weekIndex = Math.floor(i / 7);
  const seed = (weekIndex * 13 + dayOfWeek * 7 + (weekIndex % 5) * 3) % 17;
  if (seed < 4) return 0;
  if (seed < 8) return 1;
  if (seed < 12) return 2;
  if (seed < 15) return 3;
  return 4;
});
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

function ProfileHeader({ profile, onEdit }) {
  const displayName = profile?.displayName || "Developer";
  const email = profile?.email || `${displayName.toLowerCase().replace(/[^a-z0-9]/g, "") || "developer"}@example.com`;

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
              <i className="profile-role-dot" aria-hidden="true" /> {profile?.roleTitle || "Pro Algorithmic Learner"}
            </span>
          </div>
          <a className="profile-email" href={`mailto:${email}`}>
            {email}
          </a>
          <p>
            {profile?.bio ||
              "Computer Science student at DevHub Academy. Specializing in algorithm optimization, complexity analysis, and competitive programming visualization."}
          </p>
          <div className="profile-badges">
            <span>📍 {profile?.location || "Ho Chi Minh City"}</span>
            <span>⚡ {profile?.rank || "Top 5% Rank"}</span>
            <span>🎯 Target: {profile?.target || "Senior DSA Specialist"}</span>
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
          className="profile-icon-button"
          type="button"
          aria-label="Share profile"
        >
          <ProfileIcon name="share" />
        </button>
        <button
          className="profile-button profile-button--danger"
          type="button"
          onClick={() => {
            window.location.hash = "#landing";
            api.logout();
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

function Statistics() {
  return (
    <section className="profile-section">
      <div className="profile-section-heading">
        <div>
          <h2>
            <ProfileIcon name="stats" /> Statistics
          </h2>
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
  const [selectedYear, setSelectedYear] = useState("Past 12 Months");
  const [hoveredCell, setHoveredCell] = useState(null);

  return (
    <section className="profile-card profile-activity" aria-labelledby="activity-heading">
      <div className="profile-activity-header">
        <div>
          <h2 id="activity-heading">
            <span className="profile-activity-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <line x1="18" y1="20" x2="18" y2="10" />
                <line x1="12" y1="20" x2="12" y2="4" />
                <line x1="6" y1="20" x2="6" y2="14" />
              </svg>
            </span>
            Learning Activity
          </h2>
        </div>

        <div className="profile-activity-years" role="tablist" aria-label="Activity period">
          {["Past 12 Months", "2026", "2025"].map((yr) => (
            <button
              key={yr}
              type="button"
              role="tab"
              aria-selected={selectedYear === yr}
              className={`activity-year-btn ${selectedYear === yr ? "is-active" : ""}`}
              onClick={() => setSelectedYear(yr)}
            >
              {yr}
            </button>
          ))}
        </div>
      </div>

      {/* 4-Card Balanced KPI Strip */}
      <div className="profile-activity-kpis">
        <div className="activity-kpi-item">
          <div className="activity-kpi-icon is-flame" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.153.433-2.294 1-3a2.5 2.5 0 0 0 2.5 2.5z" />
            </svg>
          </div>
          <div className="activity-kpi-text">
            <span>Current Streak</span>
            <strong>7 Days</strong>
            <small>Active Today 🔥</small>
          </div>
        </div>

        <div className="activity-kpi-item">
          <div className="activity-kpi-icon is-thunder" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
            </svg>
          </div>
          <div className="activity-kpi-text">
            <span>Max Streak</span>
            <strong>21 Days</strong>
            <small>Personal Best ⚡</small>
          </div>
        </div>

        <div className="activity-kpi-item">
          <div className="activity-kpi-icon is-calendar" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
          </div>
          <div className="activity-kpi-text">
            <span>Active Days</span>
            <strong>168 Days</strong>
            <small>84% Consistency</small>
          </div>
        </div>

        <div className="activity-kpi-item">
          <div className="activity-kpi-icon is-check" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
          <div className="activity-kpi-text">
            <span>Total Submissions</span>
            <strong>348 Execs</strong>
            <small>Past 12 Months</small>
          </div>
        </div>
      </div>

      {/* Full 52-Week Balanced Heatmap Container */}
      <div className="profile-heatmap-box">
        <div className="profile-heatmap-scroll">
          {/* Months header */}
          <div className="profile-heat-months" aria-hidden="true">
            <span className="profile-heat-month-spacer" />
            <div className="profile-heat-months-grid">
              {MONTH_LABELS.map((m) => (
                <span key={m.name} style={{ gridColumn: `span ${m.colSpan}` }}>
                  {m.name}
                </span>
              ))}
            </div>
          </div>

          {/* Grid body with day labels and cells */}
          <div className="profile-heat-body">
            <div className="profile-heat-weekdays" aria-hidden="true">
              <span></span>
              <span>Mon</span>
              <span></span>
              <span>Wed</span>
              <span></span>
              <span>Fri</span>
              <span></span>
            </div>

            <div className="profile-heat-grid" role="grid" aria-label="Annual activity heatmap">
              {ACTIVITY_52_WEEKS.map((level, index) => {
                const weekIndex = Math.floor(index / 7);
                const dayIndex = index % 7;
                const submissionCount =
                  level === 0 ? "No" : level === 1 ? "1" : level === 2 ? "2-3" : level === 3 ? "4-5" : "6+";
                const label = `${submissionCount} submissions on ${WEEKDAY_LABELS[dayIndex]}, Week ${weekIndex + 1}`;
                return (
                  <i
                    key={index}
                    className={`heat-level-${level}`}
                    title={label}
                    tabIndex={0}
                    role="gridcell"
                    aria-label={label}
                    onMouseEnter={() => setHoveredCell(label)}
                    onMouseLeave={() => setHoveredCell(null)}
                  />
                );
              })}
            </div>
          </div>
        </div>

        {/* Heatmap Footer */}
        <div className="profile-heat-footer">
          <span className="profile-heat-status">
            {hoveredCell || "348 submissions in the last year • 7-day streak active"}
          </span>
          <div className="profile-heat-legend">
            <span>Less</span>
            <i className="heat-level-0" title="0 submissions" />
            <i className="heat-level-1" title="1-2 submissions" />
            <i className="heat-level-2" title="3-4 submissions" />
            <i className="heat-level-3" title="5-6 submissions" />
            <i className="heat-level-4" title="7+ submissions" />
            <span>More</span>
          </div>
        </div>
      </div>
    </section>
  );
}

function LearningProgress() {
  return (
    <section className="profile-card profile-learning">
      <div className="profile-panel-heading">
        <h2>📚 Learning Progress</h2>
        <b>
          18 / 45 Problems Solved <em>40% Total</em>
        </b>
      </div>
      <div className="profile-total-progress-wrap">
        <ProgressView
          variant="linear"
          value={40}
          max={100}
          label="Total Problems Solved Progress"
          className="profile-total-progress-el"
        />
      </div>
      <div className="profile-progress-stats">
        <div>
          <b>
            <i /> Completed
          </b>
          <strong>18</strong>
        </div>
        <div>
          <b>
            <i /> In Progress
          </b>
          <strong>5</strong>
        </div>
        <div>
          <b>
            <i /> Not Started
          </b>
          <strong>22</strong>
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
        <h2>◷ Analysis History</h2>
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

function SecuritySection({ profile, onSave }) {
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(() => Boolean(profile?.twoFactorEnabled));
  const [loginAlerts, setLoginAlerts] = useState(() => profile?.loginAlerts !== false);
  const [otherSessionsRevoked, setOtherSessionsRevoked] = useState(false);
  const [toast, setToast] = useState(null);

  const showToast = (text, type = "success") => {
    setToast({ text, type });
    setTimeout(() => setToast(null), 3500);
  };

  const handleChangePassword = (e) => {
    e.preventDefault();
    if (!currentPassword) {
      showToast("Vui lòng nhập mật khẩu hiện tại.", "error");
      return;
    }
    if (newPassword.length < 8) {
      showToast("Mật khẩu mới phải có ít nhất 8 ký tự.", "error");
      return;
    }
    if (newPassword !== confirmPassword) {
      showToast("Mật khẩu xác nhận không khớp.", "error");
      return;
    }
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    showToast("Mật khẩu của bạn đã được cập nhật thành công!");
  };

  const handleToggle2FA = () => {
    const nextVal = !twoFactorEnabled;
    setTwoFactorEnabled(nextVal);
    onSave({ ...profile, twoFactorEnabled: nextVal });
    showToast(nextVal ? "Đã bật xác thực 2 bước (2FA)!" : "Đã tắt xác thực 2 bước.");
  };

  const handleRevokeSessions = () => {
    setOtherSessionsRevoked(true);
    showToast("Đã đăng xuất khỏi tất cả các thiết bị khác!");
  };

  return (
    <section className="profile-inpage-security" aria-label="Cài đặt bảo mật">
      {toast && (
        <div className={`profile-modal-toast profile-modal-toast--${toast.type}`} role="status">
          <span>{toast.type === "success" ? "✓" : "⚠️"}</span>
          <span>{toast.text}</span>
        </div>
      )}

      {/* 1. Change Password */}
      <div className="profile-sec-card">
        <div className="profile-sec-header">
          <div className="profile-sec-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
              <path d="M7 11V7a5 5 0 0 1 10 0v4" />
            </svg>
          </div>
          <div>
            <h3 className="profile-sec-title">Đổi mật khẩu tài khoản</h3>
            <p className="profile-sec-desc">
              Cập nhật mật khẩu thường xuyên giúp bảo vệ an toàn cho tài khoản và các giải thuật của bạn.
            </p>
          </div>
        </div>

        <form onSubmit={handleChangePassword} className="profile-password-form">
          <div className="profile-form-field">
            <label htmlFor="inpage-curr-pwd">Mật khẩu hiện tại</label>
            <input
              id="inpage-curr-pwd"
              type={showPassword ? "text" : "password"}
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              placeholder="••••••••••••"
            />
          </div>

          <div className="profile-form-row-2">
            <div className="profile-form-field">
              <label htmlFor="inpage-new-pwd">Mật khẩu mới</label>
              <input
                id="inpage-new-pwd"
                type={showPassword ? "text" : "password"}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="Ít nhất 8 ký tự"
              />
            </div>

            <div className="profile-form-field">
              <label htmlFor="inpage-conf-pwd">Xác nhận mật khẩu</label>
              <input
                id="inpage-conf-pwd"
                type={showPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="Nhập lại mật khẩu mới"
              />
            </div>
          </div>

          <div className="profile-pwd-actions">
            <label className="profile-checkbox-label">
              <input
                type="checkbox"
                checked={showPassword}
                onChange={(e) => setShowPassword(e.target.checked)}
              />
              <span>Hiển thị mật khẩu</span>
            </label>

            <button type="submit" className="profile-modal-btn profile-modal-btn--secondary">
              Cập nhật mật khẩu
            </button>
          </div>
        </form>
      </div>

      {/* 2. 2FA */}
      <div className="profile-sec-card">
        <div className="profile-sec-header">
          <div className="profile-sec-icon is-emerald">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
              <path d="m9 12 2 2 4-4" />
            </svg>
          </div>
          <div className="profile-sec-header-right" onClick={handleToggle2FA} style={{ cursor: "pointer" }}>
            <div>
              <div className="profile-sec-title-line">
                <h3 className="profile-sec-title">Xác thực 2 yếu tố (Two-Factor Authentication)</h3>
                <span className={`profile-status-badge ${twoFactorEnabled ? "is-active" : "is-inactive"}`}>
                  {twoFactorEnabled ? "Đang bảo vệ" : "Chưa kích hoạt"}
                </span>
              </div>
              <p className="profile-sec-desc">
                Bảo vệ tài khoản bằng mã bảo mật 6 số khi đăng nhập từ thiết bị lạ qua Google Authenticator hoặc Authy.
              </p>
            </div>

            <button
              type="button"
              className={`profile-toggle-btn ${twoFactorEnabled ? "is-on" : "is-off"}`}
              aria-label="Bật/Tắt 2FA"
            >
              <span className="profile-toggle-thumb" />
            </button>
          </div>
        </div>
      </div>

      {/* 3. Active Sessions */}
      <div className="profile-sec-card">
        <div className="profile-sec-header">
          <div className="profile-sec-icon is-slate">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
              <line x1="8" y1="21" x2="16" y2="21" />
              <line x1="12" y1="17" x2="12" y2="21" />
            </svg>
          </div>
          <div>
            <h3 className="profile-sec-title">Phiên đăng nhập &amp; Thiết bị</h3>
            <p className="profile-sec-desc">
              Các thiết bị đang đăng nhập và có quyền truy cập vào tài khoản AlgoViz của bạn.
            </p>
          </div>
        </div>

        <div className="profile-session-list">
          <div className="profile-session-item is-current">
            <div className="profile-session-details">
              <div className="profile-session-name">
                <strong>Chrome trên Linux (Thiết bị này)</strong>
                <span className="profile-session-tag">Phiên hiện tại</span>
              </div>
              <span className="profile-session-meta">
                Ho Chi Minh City, VN • IP: 118.69.182.xx • Hoạt động vừa xong
              </span>
            </div>
          </div>

          {!otherSessionsRevoked && (
            <div className="profile-session-item">
              <div className="profile-session-details">
                <div className="profile-session-name">
                  <strong>Safari trên Apple iPhone 15</strong>
                </div>
                <span className="profile-session-meta">
                  Ho Chi Minh City, VN • Hoạt động 2 ngày trước
                </span>
              </div>
              <button
                type="button"
                className="profile-session-revoke-btn"
                onClick={handleRevokeSessions}
                title="Đăng xuất thiết bị này"
              >
                Đăng xuất
              </button>
            </div>
          )}
        </div>

        {!otherSessionsRevoked && (
          <div className="profile-session-actions">
            <button
              type="button"
              className="profile-modal-btn profile-modal-btn--danger-outline"
              onClick={handleRevokeSessions}
            >
              Đăng xuất khỏi tất cả các thiết bị khác
            </button>
          </div>
        )}
      </div>

      {/* 4. Login Notification Alerts */}
      <div className="profile-sec-card">
        <label className="profile-checkbox-row">
          <input
            type="checkbox"
            checked={loginAlerts}
            onChange={(e) => {
              const val = e.target.checked;
              setLoginAlerts(val);
              onSave({ ...profile, loginAlerts: val });
              showToast("Đã lưu thiết lập cảnh báo email.");
            }}
          />
          <div>
            <strong>Gửi thông báo bảo mật qua Email</strong>
            <p className="profile-sec-desc">
              Tự động cảnh báo ngay khi phát hiện lượt đăng nhập từ địa chỉ IP hoặc trình duyệt mới.
            </p>
          </div>
        </label>
      </div>
    </section>
  );
}

function ProfileFooter() {
  return (
    <footer className="profile-footer">
      <div>
        <strong>AlgoViz</strong>
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

function ProfilePage({ userName = "Developer", isAuthenticated = false }) {
  const [profile, setProfile] = useState(() => {
    try {
      const saved = localStorage.getItem("algoviz-user-profile");
      if (saved) return JSON.parse(saved);
    } catch {
      // ignore
    }
    const cleanName = userName && userName !== "Developer" && userName !== "Your name" ? userName : "hienpham0344";
    const userHandle = cleanName.toLowerCase().replace(/[^a-z0-9]/g, "") || "hienpham0344";
    return {
      displayName: cleanName,
      email: `${userHandle}@example.com`,
      roleTitle: "Pro Algorithmic Learner",
      bio: "Computer Science student at DevHub Academy. Specializing in algorithm optimization, complexity analysis, and competitive programming visualization.",
      location: "Ho Chi Minh City",
      rank: "Top 5% Rank",
      target: "Senior DSA Specialist",
      twoFactorEnabled: false,
      loginAlerts: true,
    };
  });

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [modalTab, setModalTab] = useState("profile");
  const [statusMessage, setStatusMessage] = useState("");

  // Sync hash changes (e.g. #profile?tab=security or #profile?tab=edit)
  useEffect(() => {
    const checkHash = () => {
      const hash = window.location.hash || "";
      if (hash.includes("tab=security") || hash.includes("security")) {
        setModalTab("security");
        setIsEditModalOpen(true);
      } else if (hash.includes("tab=edit") || hash.includes("action=edit")) {
        setModalTab("profile");
        setIsEditModalOpen(true);
      }
    };
    checkHash();
    window.addEventListener("hashchange", checkHash);
    return () => window.removeEventListener("hashchange", checkHash);
  }, []);

  // Listen for custom event from NavigationBar
  useEffect(() => {
    const handleOpenSecurity = () => {
      setModalTab("security");
      setIsEditModalOpen(true);
    };
    window.addEventListener("algoviz:open-security", handleOpenSecurity);
    return () => window.removeEventListener("algoviz:open-security", handleOpenSecurity);
  }, []);

  const handleSaveProfile = (updated) => {
    setProfile(updated);
    try {
      localStorage.setItem("algoviz-user-profile", JSON.stringify(updated));
    } catch {
      // ignore
    }
    setStatusMessage("Đã lưu hồ sơ thành công!");
  };

  const handleCloseModal = () => {
    setIsEditModalOpen(false);
    if (window.location.hash.includes("tab=")) {
      window.history.replaceState(null, "", "#profile");
    }
  };

  return (
    <main className="profile-page" id="top">
      <div className="profile-main profile-shell">
        <div className="profile-breadcrumb">
          <a href="#problems">Home</a>
          <span>/</span>
          <strong>Profile</strong>
        </div>

        <ProfileHeader
          profile={profile}
          onEdit={() => {
            setModalTab("profile");
            setIsEditModalOpen(true);
          }}
        />

        <Statistics />
        <Activity />
        <LearningProgress />
        <AnalysisHistory />

        <p className="sr-only" aria-live="polite">
          {statusMessage}
        </p>

        {/* Edit Profile & Security Modal */}
        <EditProfileModal
          key={isEditModalOpen ? `${modalTab}-${isEditModalOpen}` : "closed"}
          isOpen={isEditModalOpen}
          onClose={handleCloseModal}
          initialTab={modalTab}
          activeTab={modalTab}
          onTabChange={setModalTab}
          profileData={profile}
          onSave={handleSaveProfile}
        />
      </div>
      <ProfileFooter />
    </main>
  );
}

export default ProfilePage;
