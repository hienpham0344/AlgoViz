import ProgressView from "../../components/ProgressView.jsx";
import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { api } from "../../services/api.js";
import "./EditProfileModal.css";

export default function EditProfileModal({ isOpen, onClose, initialTab = "profile", profileData, onSave }) {
  const [tab, setTab] = useState(initialTab);
  const [form, setForm] = useState(() => ({
    displayName: profileData?.displayName || "", email: profileData?.email || "",
    roleTitle: profileData?.roleTitle || "", bio: profileData?.bio || "",
    location: profileData?.location || "", target: profileData?.target || "",
  }));
  const [passwords, setPasswords] = useState({ currentPassword: "", newPassword: "", confirmPassword: "" });
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const dialogRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialogRef.current?.querySelector("button")?.focus();
    const handleKey = (event) => {
      if (event.key === "Escape" && !busy) onClose();
      if (event.key !== "Tab") return;
      const items = [...dialogRef.current.querySelectorAll("button:not(:disabled), input, textarea")]
        .filter((element) => element.getClientRects().length);
      const first = items[0];
      const last = items.at(-1);
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    };
    document.addEventListener("keydown", handleKey);
    return () => {
      document.removeEventListener("keydown", handleKey);
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus();
    };
  }, [isOpen, onClose, busy]);
  if (!isOpen) return null;

  const saveProfile = async (event) => {
    event.preventDefault();
    if (busy) return;
    if (!form.displayName.trim()) { setFeedback({ error: true, text: "Vui lòng nhập tên hiển thị." }); return; }
    setBusy(true);
    setFeedback(null);
    try {
      await onSave({ ...form, displayName: form.displayName.trim() });
      setFeedback({ text: "Đã lưu hồ sơ." });
    } catch (error) { setFeedback({ error: true, text: error.message }); }
    finally { setBusy(false); }
  };
  const changePassword = async (event) => {
    event.preventDefault();
    if (busy) return;
    if (passwords.newPassword !== passwords.confirmPassword) {
      setFeedback({ error: true, text: "Mật khẩu xác nhận không khớp." }); return;
    }
    setBusy(true);
    setFeedback(null);
    try {
      await api.changePassword(passwords.currentPassword, passwords.newPassword);
      setPasswords({ currentPassword: "", newPassword: "", confirmPassword: "" });
      setFeedback({ text: "Đã đổi mật khẩu. Vui lòng đăng nhập lại." });
      await api.logout().catch(() => {});
    } catch (error) { setFeedback({ error: true, text: error.message }); }
    finally { setBusy(false); }
  };
  const fields = [
    ["displayName", "Tên hiển thị", 100], ["email", "Email", 254], ["roleTitle", "Vai trò", 100],
    ["location", "Địa điểm", 100], ["target", "Mục tiêu học tập", 200], ["bio", "Giới thiệu", 2000],
  ];
  return createPortal(
    <div className="profile-modal-backdrop" onClick={() => { if (!busy) onClose(); }}>
      <section ref={dialogRef} className="profile-modal-card" role="dialog" aria-modal="true"
        aria-labelledby="profile-modal-title" onClick={(event) => event.stopPropagation()}>
        <header className="profile-modal-header">
          <div className="profile-modal-title-group">
            <h2 id="profile-modal-title">Account Settings &amp; Security</h2>
            <p className="profile-modal-sub">Quản lý hồ sơ và mật khẩu của bạn.</p>
          </div>
          <button type="button" className="profile-modal-close-btn" onClick={onClose} disabled={busy} aria-label="Close dialog">✕</button>
        </header>
        <nav className="profile-modal-tabs" aria-label="Settings options">
          <button type="button" className={`profile-modal-tab-btn ${tab === "profile" ? "is-active" : ""}`}
            disabled={busy} onClick={() => { setTab("profile"); setFeedback(null); }}>Profile</button>
          <button type="button" className={`profile-modal-tab-btn ${tab === "security" ? "is-active" : ""}`}
            disabled={busy} onClick={() => { setTab("security"); setFeedback(null); }}>Security</button>
        </nav>
        {feedback && <div className={`profile-modal-toast profile-modal-toast--${feedback.error ? "error" : "success"}`}
          role={feedback.error ? "alert" : "status"}>{feedback.text}</div>}
        <div className="profile-modal-body">
          {tab === "profile" ? (
            <form onSubmit={saveProfile} aria-busy={busy}>
              <div className="profile-form-grid">
                {fields.map(([key, label, maxLength]) => (
                  <div className={`profile-form-field ${key === "bio" ? "profile-form-field--full" : ""}`} key={key}>
                    <label htmlFor={`profile-${key}`}>{label}</label>
                    {key === "bio" ? <textarea id={`profile-${key}`} rows={3} maxLength={maxLength} value={form[key]}
                      disabled={busy} onChange={(event) => setForm({ ...form, [key]: event.target.value })} />
                      : <input id={`profile-${key}`} type={key === "email" ? "email" : "text"} required={key === "email" || key === "displayName"}
                        maxLength={maxLength} value={form[key]} disabled={busy}
                        onChange={(event) => setForm({ ...form, [key]: event.target.value })} />}
                  </div>
                ))}
              </div>
              <div className="profile-modal-footer">
                <button type="button" className="profile-modal-btn profile-modal-btn--ghost" disabled={busy} onClick={onClose}>Hủy</button>
                <button type="submit" className="profile-modal-btn profile-modal-btn--primary" disabled={busy}>{busy ? <ProgressView size="sm" label="Đang lưu…" /> : "Lưu thay đổi"}</button>
              </div>
            </form>
          ) : (
            <form onSubmit={changePassword} className="profile-sec-card" aria-busy={busy}>
              <h3 className="profile-sec-title">Đổi mật khẩu</h3>
              <p className="profile-sec-desc">Sau khi đổi mật khẩu, bạn cần đăng nhập lại. Các refresh token hiện có sẽ được thu hồi.</p>
              {[["currentPassword", "Mật khẩu hiện tại"], ["newPassword", "Mật khẩu mới"], ["confirmPassword", "Xác nhận mật khẩu mới"]].map(([key, label]) => (
                <div className="profile-form-field" key={key}>
                  <label htmlFor={key}>{label}</label>
                  <input id={key} type="password" autoComplete={key === "currentPassword" ? "current-password" : "new-password"}
                    required minLength={key === "currentPassword" ? undefined : 8} maxLength={100}
                    disabled={busy} value={passwords[key]} onChange={(event) => setPasswords({ ...passwords, [key]: event.target.value })} />
                </div>
              ))}
              <div className="profile-modal-footer"><button type="submit" className="profile-modal-btn profile-modal-btn--primary" disabled={busy}>
                {busy ? <ProgressView size="sm" label="Đang cập nhật…" /> : "Cập nhật mật khẩu"}</button></div>
            </form>
          )}
        </div>
      </section>
    </div>, document.body,
  );
}
