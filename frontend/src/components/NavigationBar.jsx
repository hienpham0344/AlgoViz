import { useEffect, useState } from "react";
import { api, clearAuthData, getToken } from "../services/api.js";
import ThemeToggle from "./ThemeToggle.jsx";
import "./components.css";

const NAV_ITEMS = [
  { id: "problems", label: "Problems", href: "#problems" },
  { id: "visualize", label: "Visualize", href: "#visualize" },
  { id: "learn", label: "Learn", href: "#learn" },
];

function NavigationBar({
  active = "Problems",
  userName = "Your name",
  isAuthenticated: propAuth,
  className = "",
}) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);

  const isAuthenticated =
    propAuth !== undefined
      ? propAuth
      : Boolean(getToken()) && localStorage.getItem("algoviz-auth") === "true";

  useEffect(() => {
    if (!isMenuOpen && !isAccountMenuOpen) return undefined;

    const handleEscape = (event) => {
      if (event.key !== "Escape") return;
      setIsMenuOpen(false);
      setIsAccountMenuOpen(false);
    };

    window.addEventListener("keydown", handleEscape);
    return () => window.removeEventListener("keydown", handleEscape);
  }, [isAccountMenuOpen, isMenuOpen]);

  const closeMenu = () => {
    setIsMenuOpen(false);
    setIsAccountMenuOpen(false);
  };

  const initial = (userName && userName !== "Your name" ? userName.charAt(0) : "U").toUpperCase();

  return (
    <>
      <header className={`av-navigation-bar ${isMenuOpen ? "is-menu-open" : ""} ${className}`.trim()}>
      <div className="av-nav-left">
        <a className="av-brand" href="#problems" onClick={closeMenu}>
          <span className="av-brand-mark" aria-hidden="true">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="16 18 22 12 16 6" />
              <polyline points="8 6 2 12 8 18" />
              <line x1="12" y1="2" x2="12" y2="22" strokeDasharray="3 3" opacity="0.6" />
            </svg>
          </span>
          <span className="av-brand-title">Algo<strong>Viz</strong></span>
        </a>

        <nav className="av-nav-items" id="av-primary-navigation" aria-label="Primary navigation">
          {NAV_ITEMS.map((item) => {
            const normActive = (active || "").toLowerCase();
            const isActive =
              item.label.toLowerCase() === normActive ||
              item.id.toLowerCase() === normActive ||
              ((normActive === "analyze" || normActive === "visualize") &&
                (item.id === "visualize" || item.id === "analyze"));

            return (
              <a
                className={isActive ? "is-active" : ""}
                href={item.href}
                key={item.id}
                onClick={closeMenu}
              >
                {item.label}
              </a>
            );
          })}
        </nav>
      </div>

      <div className="av-nav-right">
        {isAuthenticated ? (
          <>
            <div className="av-user-stats" title="3-day solving streak">
              <span className="av-user-stats__streak">
                <span className="av-user-stats__icon" aria-label="Streak">🔥</span>
                <span className="av-user-stats__count">3</span>
              </span>
            </div>

            <div className={`av-user ${isAccountMenuOpen ? "is-account-menu-open" : ""}`}>
              <button
                className="av-user-avatar-button"
                type="button"
                aria-label="Open account menu"
                aria-expanded={isAccountMenuOpen}
                aria-controls="av-account-menu"
                onClick={() => setIsAccountMenuOpen((current) => !current)}
              >
                <span className="av-user-avatar-initial">{initial}</span>
              </button>
              <span className="av-user-name">{userName}</span>

              {isAccountMenuOpen && (
                <div className="av-account-menu" id="av-account-menu">
                  <div className="av-account-menu__header">
                    <span className="av-account-menu__name">{userName}</span>
                    <span className="av-account-menu__sub">AlgoViz Member</span>
                  </div>
                  <a href="#profile" onClick={closeMenu}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                      <circle cx="12" cy="7" r="4" />
                    </svg>
                    <span>Profile &amp; Solutions</span>
                  </a>
                  <a
                    href="#profile?tab=security"
                    onClick={(e) => {
                      e.preventDefault();
                      closeMenu();
                      if (window.location.hash.startsWith("#profile")) {
                        window.history.replaceState(null, "", "#profile?tab=security");
                      } else {
                        window.location.hash = "#profile?tab=security";
                      }
                      window.dispatchEvent(new CustomEvent("algoviz:open-security"));
                    }}
                  >
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                      <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    <span>Security</span>
                  </a>
                  <a href="#history" onClick={closeMenu}>
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                      <circle cx="12" cy="12" r="10" />
                      <polyline points="12 6 12 12 16 14" />
                    </svg>
                    <span>Submission History</span>
                  </a>
                  <button
                    type="button"
                    className="av-account-menu__logout"
                    onClick={() => {
                      closeMenu();
                      window.location.hash = "#landing";
                      api.logout();
                    }}
                  >
                    Logout
                  </button>
                </div>
              )}
            </div>
          </>
        ) : (
          <div className="av-auth-actions">
            <a href="#login" className="av-auth-btn av-auth-btn--login" onClick={closeMenu}>
              Sign In
            </a>
            <a href="#register" className="av-auth-btn av-auth-btn--register" onClick={closeMenu}>
              Register
            </a>
          </div>
        )}

        <button
          className="av-menu-toggle"
          type="button"
          aria-label={isMenuOpen ? "Close navigation menu" : "Open navigation menu"}
          aria-expanded={isMenuOpen}
          aria-controls="av-primary-navigation"
          onClick={() => setIsMenuOpen((current) => !current)}
        >
          <span aria-hidden="true" />
          <span aria-hidden="true" />
          <span aria-hidden="true" />
        </button>
      </div>
      </header>
      <div className="av-navigation-spacer" aria-hidden="true" />
    </>
  );
}

export default NavigationBar;
