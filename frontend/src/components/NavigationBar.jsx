import { useEffect, useState } from "react";
import { PROBLEMS } from "../pages/problem/problemData.js";
import "./components.css";

const NAV_ITEMS = ["Problems", "Analyze", "History", "Learn"];

function NavigationBar({
  active = "Problems",
  userName = "Your name",
  className = "",
  onPickRandomProblem,
}) {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const [isAccountMenuOpen, setIsAccountMenuOpen] = useState(false);

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

  return (
    <header
      className={`av-navigation-bar ${isMenuOpen ? "is-menu-open" : ""} ${className}`.trim()}
    >
      <a className="av-brand" href="#top" onClick={closeMenu}>
        AlgoViz
      </a>
      <nav
        className="av-nav-items"
        id="av-primary-navigation"
        aria-label="Primary navigation"
      >
        {NAV_ITEMS.map((item) => (
          <a
            className={item === active ? "is-active" : ""}
            href={`#${item.toLowerCase()}`}
            key={item}
            onClick={closeMenu}
          >
            {item}
          </a>
        ))}
      </nav>
      <div className="av-nav-tools" aria-label="Problem progress and actions">
        <button
          className="av-nav-metric av-nav-progress"
          type="button"
          aria-label="View analysis history: 42 of 125 problems solved"
          onClick={() => {
            window.location.hash = "history";
            closeMenu();
          }}
        >
          <span className="av-nav-progress__copy">
            <span>Solved</span>
            <strong>
              42 <small>/ 125</small>
            </strong>
          </span>
          <span className="av-nav-progress__ring" aria-hidden="true">
            34%
          </span>
        </button>
        <a
          className="av-nav-metric av-nav-challenge"
          href="#problem-details"
          aria-label="Daily Challenge: Course Schedule"
          onClick={closeMenu}
        >
          <span className="av-nav-challenge__icon" aria-hidden="true" />
          <span className="av-nav-challenge__copy">
            <strong>Daily Challenge</strong>
            <small>Course Schedule</small>
          </span>
        </a>
        <button
          className="av-nav-random"
          type="button"
          onClick={() => {
            if (onPickRandomProblem) {
              onPickRandomProblem();
            } else {
              const selected = PROBLEMS[Math.floor(Math.random() * PROBLEMS.length)];
              sessionStorage.setItem(
                "algoviz-selected-problem",
                JSON.stringify(selected),
              );
              window.dispatchEvent(
                new CustomEvent("algoviz:problem-selected", { detail: selected }),
              );
              window.location.hash = "problem-details";
            }
            closeMenu();
          }}
        >
          <span className="av-nav-random__icon" aria-hidden="true" />
          <span>Pick Random</span>
        </button>
      </div>
      <div
        className={`av-user ${isAccountMenuOpen ? "is-account-menu-open" : ""}`}
      >
        <button
          className="av-user-avatar-button"
          type="button"
          aria-label="Open account menu"
          aria-expanded={isAccountMenuOpen}
          aria-controls="av-account-menu"
          onClick={() => setIsAccountMenuOpen((current) => !current)}
        >
          <span className="av-user-avatar" aria-hidden="true" />
        </button>
        <span className="av-user-name">{userName}</span>
        <div className="av-account-menu" id="av-account-menu">
          <a href="#profile" onClick={closeMenu}>
            Profile
          </a>
          <a
            href="#top"
            onClick={() => {
              localStorage.removeItem("algoviz-auth");
              localStorage.removeItem("algoviz-user-name");
              window.dispatchEvent(new Event("algoviz:logout"));
              closeMenu();
            }}
          >
            Logout
          </a>
        </div>
      </div>
      <button
        className="av-menu-toggle"
        type="button"
        aria-label={
          isMenuOpen ? "Close navigation menu" : "Open navigation menu"
        }
        aria-expanded={isMenuOpen}
        aria-controls="av-primary-navigation"
        onClick={() => setIsMenuOpen((current) => !current)}
      >
        <span aria-hidden="true" />
        <span aria-hidden="true" />
        <span aria-hidden="true" />
      </button>
    </header>
  );
}

export default NavigationBar;
