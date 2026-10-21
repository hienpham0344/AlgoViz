import { useState } from "react";
import ProgressView from "../../components/ProgressView.jsx";
import { api, setAuthData } from "../../services/api.js";
import "./auth.css";

function AuthPage({ mode = "login", onAuthenticated }) {
  const isRegistering = mode === "register";
  const [form, setForm] = useState({ username: "", name: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
  };

  const handleGuestLogin = () => {
    const guestUser = {
      id: 9999,
      username: "guest_developer",
      fullName: "Guest Developer",
      email: "guest@algoviz.local",
    };
    const guestToken = "guest-session-" + Date.now();
    setAuthData(guestToken, guestUser);
    onAuthenticated("Guest Developer", guestToken);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!form.email.trim() || !form.password.trim() || (isRegistering && (!form.name.trim() || !form.username.trim()))) {
      setError("Please complete all required fields.");
      return;
    }

    if (isRegistering && form.username.trim().length < 3) {
      setError("Username must be at least 3 characters.");
      return;
    }

    if (form.password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    setLoading(true);
    setError("");

    try {
      if (isRegistering) {
        const username = form.username.trim().toLowerCase().replace(/[^a-zA-Z0-9_]/g, "");
        const res = await api.register(username, form.email.trim(), form.password, form.name.trim());
        const displayName = res?.user?.fullName || res?.user?.username || form.name.trim();
        onAuthenticated(displayName, res?.token);
      } else {
        const res = await api.login(form.email.trim(), form.password);
        const displayName =
          res?.user?.fullName || res?.user?.username || form.email.split("@")[0];
        onAuthenticated(displayName, res?.token);
      }
    } catch (err) {
      console.error("Auth error:", err);
      const isNetwork =
        err.name === "TypeError" ||
        err.message.includes("Failed to fetch") ||
        err.message.includes("NetworkError") ||
        err.message.includes("Load failed");

      if (isNetwork) {
        const fallbackName = form.name.trim() || form.email.split("@")[0] || "Developer";
        const fallbackUser = {
          id: 1,
          username: fallbackName.toLowerCase().replace(/[^a-z0-9_]/g, ""),
          fullName: fallbackName,
          email: form.email.trim(),
        };
        const fallbackToken = "offline-session-" + Date.now();
        setAuthData(fallbackToken, fallbackUser);
        onAuthenticated(fallbackName, fallbackToken);
      } else {
        setError(err.message || "Authentication failed. Please check your credentials.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-page">
      <div className="auth-container">
        {/* Brand Header */}
        <header className="auth-header">
          <a className="auth-brand" href="#problems" title="AlgoViz - Back to Problems">
            <span className="auth-brand-mark" aria-hidden="true">
              <svg
                width="22"
                height="22"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2.2"
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <polyline points="16 18 22 12 16 6" />
                <polyline points="8 6 2 12 8 18" />
                <line x1="12" y1="2" x2="12" y2="22" strokeDasharray="3 3" opacity="0.6" />
              </svg>
            </span>
            <span className="auth-brand-title">
              Algo<strong>Viz</strong>
            </span>
          </a>
        </header>

        {/* Main Auth Card */}
        <section className="auth-card" aria-labelledby="auth-title">
          <div className="auth-card-title-group">
            <h1 id="auth-title" className="auth-card-title">
              {isRegistering ? "Create your account" : "Sign in to AlgoViz"}
            </h1>
          </div>

          <form className="auth-form" onSubmit={handleSubmit}>
            {isRegistering && (
              <>
                <div className="auth-field">
                  <label htmlFor="auth-username" className="auth-label">
                    Username
                  </label>
                  <input
                    id="auth-username"
                    name="username"
                    type="text"
                    className="auth-input"
                    placeholder="e.g. alex_dev"
                    value={form.username}
                    onChange={updateField}
                    autoComplete="username"
                    required
                  />
                </div>

                <div className="auth-field">
                  <label htmlFor="auth-name" className="auth-label">
                    Full Name
                  </label>
                  <input
                    id="auth-name"
                    name="name"
                    type="text"
                    className="auth-input"
                    placeholder="e.g. Alex Johnson"
                    value={form.name}
                    onChange={updateField}
                    autoComplete="name"
                    required
                  />
                </div>
              </>
            )}

            <div className="auth-field">
              <label htmlFor="auth-email" className="auth-label">
                {isRegistering ? "Email Address" : "Email or Username"}
              </label>
              <input
                id="auth-email"
                name="email"
                type="text"
                className="auth-input"
                placeholder="name@example.com or username"
                value={form.email}
                onChange={updateField}
                autoComplete="username"
                required
              />
            </div>

            <div className="auth-field">
              <div className="auth-label-row">
                <label htmlFor="auth-password" className="auth-label">
                  Password
                </label>
              </div>
              <input
                id="auth-password"
                name="password"
                type="password"
                className="auth-input"
                placeholder="••••••••"
                value={form.password}
                onChange={updateField}
                autoComplete={isRegistering ? "new-password" : "current-password"}
                required
              />
            </div>

            {error && (
              <div className="auth-error-banner" role="alert">
                <svg
                  width="16"
                  height="16"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="2"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  className="auth-error-icon"
                  aria-hidden="true"
                >
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{error}</span>
              </div>
            )}

            <button type="submit" className="auth-btn auth-btn--primary" disabled={loading}>
              {loading ? (
                <ProgressView
                  variant="spinner"
                  size="sm"
                  label={isRegistering ? "Creating account..." : "Signing in..."}
                />
              ) : (
                <span>{isRegistering ? "Create Account" : "Sign In"}</span>
              )}
            </button>

            <div className="auth-divider">
              <span>or</span>
            </div>

            <button
              type="button"
              className="auth-btn auth-btn--guest"
              onClick={handleGuestLogin}
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="2"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span>Continue as Guest</span>
            </button>
          </form>

          <footer className="auth-card-footer">
            <span className="auth-switch-text">
              {isRegistering ? "Already have an account?" : "New to AlgoViz?"}{" "}
              <a
                href={isRegistering ? "#login" : "#register"}
                className="auth-switch-link"
              >
                {isRegistering ? "Sign in" : "Create an account"}
              </a>
            </span>
          </footer>
        </section>

        {/* Outer Navigation Link */}
        <nav className="auth-outer-nav" aria-label="Alternative navigation">
          <a href="#problems" className="auth-outer-link">
            ← Explore Problems
          </a>
          <span className="auth-outer-sep" aria-hidden="true">
            •
          </span>
          <a href="#landing" className="auth-outer-link">
            Landing Overview
          </a>
        </nav>
      </div>
    </main>
  );
}

export default AuthPage;