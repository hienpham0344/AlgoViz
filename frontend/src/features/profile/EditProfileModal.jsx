import { useState, useEffect } from "react";
import { createPortal } from "react-dom";
import "./EditProfileModal.css";

export default function EditProfileModal({
  isOpen,
  onClose,
  initialTab = "profile",
  activeTab: controlledTab,
  onTabChange,
  profileData,
  onSave,
}) {
  const [internalTab, setInternalTab] = useState(initialTab);
  const activeTab = controlledTab !== undefined ? controlledTab : internalTab;
  const setActiveTab = onTabChange || setInternalTab;

  // Profile Form State initialized directly from props
  const [displayName, setDisplayName] = useState(() => profileData?.displayName || "");
  const [email, setEmail] = useState(() => profileData?.email || "");
  const [roleTitle, setRoleTitle] = useState(() => profileData?.roleTitle || "Pro Algorithmic Learner");
  const [bio, setBio] = useState(() => profileData?.bio || "");
  const [location, setLocation] = useState(() => profileData?.location || "Ho Chi Minh City");
  const [target, setTarget] = useState(() => profileData?.target || "Senior DSA Specialist");

  // Security Form State
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [twoFactorEnabled, setTwoFactorEnabled] = useState(() => Boolean(profileData?.twoFactorEnabled));
  const [loginAlerts, setLoginAlerts] = useState(() => profileData?.loginAlerts !== false);

  // Active Sessions
  const [otherSessionsRevoked, setOtherSessionsRevoked] = useState(false);

  // Feedback toast
  const [toast, setToast] = useState(null);

  // Handle ESC key to close
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === "Escape" && isOpen) {
        onClose();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const showToast = (text, type = "success") => {
    setToast({ text, type });
    setTimeout(() => setToast(null), 4000);
  };

  const handleSaveProfile = (e) => {
    e.preventDefault();
    if (!displayName.trim()) {
      showToast("Vui lòng nhập tên hiển thị.", "error");
      return;
    }

    const updated = {
      ...profileData,
      displayName: displayName.trim(),
      email: email.trim(),
      roleTitle: roleTitle.trim(),
      bio: bio.trim(),
      location: location.trim(),
      target: target.trim(),
      twoFactorEnabled,
      loginAlerts,
    };

    onSave(updated);
    showToast("Đã lưu thông tin hồ sơ thành công!");
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

    // Success simulated update
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    showToast("Mật khẩu của bạn đã được cập nhật thành công!");
  };

  const handleToggle2FA = () => {
    const nextVal = !twoFactorEnabled;
    setTwoFactorEnabled(nextVal);
    const updated = { ...profileData, twoFactorEnabled: nextVal };
    onSave(updated);
    showToast(
      nextVal
        ? "Đã kích hoạt xác thực 2 bước (2FA) cho tài khoản!"
        : "Đã tắt xác thực 2 bước."
    );
  };

  const handleRevokeSessions = () => {
    setOtherSessionsRevoked(true);
    showToast("Đã đăng xuất khỏi tất cả các thiết bị khác!");
  };

  const modalContent = (
    <div
      className="profile-modal-backdrop"
      onClick={onClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="profile-modal-title"
    >
      <div className="profile-modal-card" onClick={(e) => e.stopPropagation()}>
        {/* Header */}
        <header className="profile-modal-header">
          <div className="profile-modal-title-group">
            <h2 id="profile-modal-title">Account Settings &amp; Security</h2>
            <p className="profile-modal-sub">
              Manage your personal profile information and account security settings.
            </p>
          </div>
          <button
            type="button"
            className="profile-modal-close-btn"
            onClick={onClose}
            aria-label="Close dialog"
          >
            ✕
          </button>
        </header>

        {/* Tab Switcher */}
        <nav className="profile-modal-tabs" aria-label="Settings options">
          <button
            type="button"
            className={`profile-modal-tab-btn ${activeTab === "profile" ? "is-active" : ""}`}
            onClick={() => setActiveTab("profile")}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
            <span>Profile</span>
          </button>

          <button
            type="button"
            className={`profile-modal-tab-btn ${activeTab === "security" ? "is-active" : ""}`}
            onClick={() => setActiveTab("security")}
          >
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            </svg>
            <span>Security</span>
            {twoFactorEnabled && (
              <span className="profile-modal-pill-tag is-safe">2FA On</span>
            )}
          </button>
        </nav>

        {/* Toast Feedback */}
        {toast && (
          <div className={`profile-modal-toast profile-modal-toast--${toast.type}`} role="status">
            <span>{toast.type === "success" ? "✓" : "⚠️"}</span>
            <span>{toast.text}</span>
          </div>
        )}

        {/* Modal Body */}
        <div className="profile-modal-body">
          {/* TAB 1: General Profile */}
          {activeTab === "profile" && (
            <form onSubmit={handleSaveProfile} className="profile-form">
              <div className="profile-avatar-preview-row">
                <div className="profile-modal-avatar">
                  <span>{(displayName || "U").charAt(0).toUpperCase()}</span>
                </div>
                <div className="profile-avatar-info">
                  <strong>{displayName || "Người dùng AlgoViz"}</strong>
                  <p>Học viên &amp; Kỹ sư thuật toán</p>
                  <span className="profile-avatar-hint">Ảnh đại diện tự động đồng bộ theo tên hiển thị</span>
                </div>
              </div>

              <div className="profile-form-grid">
                <div className="profile-form-field">
                  <label htmlFor="pf-display-name">Tên hiển thị (Display Name)</label>
                  <input
                    id="pf-display-name"
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    placeholder="hienpham0344"
                    required
                  />
                </div>

                <div className="profile-form-field">
                  <label htmlFor="pf-email">Địa chỉ Email</label>
                  <input
                    id="pf-email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="hienpham0344@example.com"
                  />
                </div>

                <div className="profile-form-field">
                  <label htmlFor="pf-role">Danh hiệu / Vai trò</label>
                  <input
                    id="pf-role"
                    type="text"
                    value={roleTitle}
                    onChange={(e) => setRoleTitle(e.target.value)}
                    placeholder="Pro Algorithmic Learner"
                  />
                </div>

                <div className="profile-form-field">
                  <label htmlFor="pf-location">Vị trí (Location)</label>
                  <input
                    id="pf-location"
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    placeholder="Ho Chi Minh City"
                  />
                </div>

                <div className="profile-form-field profile-form-field--full">
                  <label htmlFor="pf-target">Mục tiêu phát triển (Target Goal)</label>
                  <input
                    id="pf-target"
                    type="text"
                    value={target}
                    onChange={(e) => setTarget(e.target.value)}
                    placeholder="Senior DSA Specialist"
                  />
                </div>

                <div className="profile-form-field profile-form-field--full">
                  <label htmlFor="pf-bio">Giới thiệu ngắn (Bio)</label>
                  <textarea
                    id="pf-bio"
                    rows={3}
                    value={bio}
                    onChange={(e) => setBio(e.target.value)}
                    placeholder="Chia sẻ về sở thích thuật toán, kỹ năng công nghệ hoặc dự án bạn quan tâm..."
                  />
                </div>
              </div>

              <div className="profile-modal-footer">
                <button
                  type="button"
                  className="profile-modal-btn profile-modal-btn--ghost"
                  onClick={onClose}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  className="profile-modal-btn profile-modal-btn--primary"
                >
                  Lưu thay đổi
                </button>
              </div>
            </form>
          )}

          {/* TAB 2: Security & Authentication */}
          {activeTab === "security" && (
            <div className="profile-security-content">
              {/* 1. Change Password */}
              <section className="profile-sec-card">
                <div className="profile-sec-header">
                  <div className="profile-sec-icon">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                  </div>
                  <div>
                    <h3 className="profile-sec-title">Đổi mật khẩu (Password)</h3>
                    <p className="profile-sec-desc">
                      Nên đặt mật khẩu mạnh kết hợp chữ hoa, chữ thường, chữ số và ký tự đặc biệt.
                    </p>
                  </div>
                </div>

                <form onSubmit={handleChangePassword} className="profile-password-form">
                  <div className="profile-form-field">
                    <label htmlFor="sec-current-pwd">Mật khẩu hiện tại</label>
                    <input
                      id="sec-current-pwd"
                      type={showPassword ? "text" : "password"}
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      placeholder="••••••••••••"
                    />
                  </div>

                  <div className="profile-form-row-2">
                    <div className="profile-form-field">
                      <label htmlFor="sec-new-pwd">Mật khẩu mới</label>
                      <input
                        id="sec-new-pwd"
                        type={showPassword ? "text" : "password"}
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Ít nhất 8 ký tự"
                      />
                    </div>

                    <div className="profile-form-field">
                      <label htmlFor="sec-confirm-pwd">Xác nhận mật khẩu mới</label>
                      <input
                        id="sec-confirm-pwd"
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

                    <button
                      type="submit"
                      className="profile-modal-btn profile-modal-btn--secondary"
                    >
                      Cập nhật mật khẩu
                    </button>
                  </div>
                </form>
              </section>

              {/* 2. Two-Factor Authentication (2FA) */}
              <section className="profile-sec-card">
                <div className="profile-sec-header">
                  <div className="profile-sec-icon is-emerald">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                      <path d="m9 12 2 2 4-4" />
                    </svg>
                  </div>
                  <div className="profile-sec-header-right">
                    <div>
                      <div className="profile-sec-title-line">
                        <h3 className="profile-sec-title">Xác thực 2 yếu tố (2FA)</h3>
                        <span
                          className={`profile-status-badge ${
                            twoFactorEnabled ? "is-active" : "is-inactive"
                          }`}
                        >
                          {twoFactorEnabled ? "Đang bảo vệ" : "Chưa kích hoạt"}
                        </span>
                      </div>
                      <p className="profile-sec-desc">
                        Yêu cầu mã 6 số từ Google Authenticator hoặc Authy khi đăng nhập từ thiết bị lạ.
                      </p>
                    </div>

                    <button
                      type="button"
                      onClick={handleToggle2FA}
                      className={`profile-toggle-btn ${twoFactorEnabled ? "is-on" : "is-off"}`}
                      aria-label="Bật/Tắt 2FA"
                    >
                      <span className="profile-toggle-thumb" />
                    </button>
                  </div>
                </div>
              </section>

              {/* 3. Active Sessions */}
              <section className="profile-sec-card">
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
                      Quản lý danh sách các trình duyệt và thiết bị đang đăng nhập tài khoản của bạn.
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
                        Ho Chi Minh City, VN • Địa chỉ IP: 118.69.182.xx • Hoạt động vừa xong
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
              </section>

              {/* 4. Login Notification Alerts */}
              <section className="profile-sec-card">
                <label className="profile-checkbox-row">
                  <input
                    type="checkbox"
                    checked={loginAlerts}
                    onChange={(e) => {
                      setLoginAlerts(e.target.checked);
                      showToast("Đã cập nhật cài đặt cảnh báo email.");
                    }}
                  />
                  <div>
                    <strong>Cảnh báo bảo mật qua Email</strong>
                    <p className="profile-sec-desc">
                      Gửi email thông báo ngay lập tức nếu phát hiện lượt đăng nhập từ thiết bị hoặc vị trí địa lý bất thường.
                    </p>
                  </div>
                </label>
              </section>

              <div className="profile-modal-footer">
                <button
                  type="button"
                  className="profile-modal-btn profile-modal-btn--ghost"
                  onClick={onClose}
                >
                  Đóng
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );

  return typeof document !== "undefined"
    ? createPortal(modalContent, document.body)
    : modalContent;
}
