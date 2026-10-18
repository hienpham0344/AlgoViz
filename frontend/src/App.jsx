import { useEffect, useState } from "react";
import AnalyzePage from "./pages/analyze.jsx";
import AuthPage from "./pages/auth.jsx";
import HistoryPage from "./pages/history.jsx";
import LearnPage from "./pages/learn.jsx";
import LandingPage from "./pages/landing.jsx";
import ProfilePage from "./pages/profile.jsx";
import ProblemsPage from "./pages/problem/problems.jsx";
import ProblemDetailsPage from "./pages/problem/problemDetails.jsx";

import { getToken } from "./services/api.js";

const AUTH_STORAGE_KEY = "algoviz-auth";
const USER_NAME_STORAGE_KEY = "algoviz-user-name";
function App() {
  const [hash, setHash] = useState(() => window.location.hash);
  const [isAuthenticated, setIsAuthenticated] = useState(
    () => localStorage.getItem(AUTH_STORAGE_KEY) === "true" && Boolean(getToken()),
  );
  const [userName, setUserName] = useState(
    () => localStorage.getItem(USER_NAME_STORAGE_KEY) || "Your name",
  );

  useEffect(() => {
    const handleHashChange = () => setHash(window.location.hash);
    const handleLogout = () => {
      setIsAuthenticated(false);
      setUserName("Your name");
    };
    window.addEventListener("hashchange", handleHashChange);
    window.addEventListener("algoviz:logout", handleLogout);
    return () => {
      window.removeEventListener("hashchange", handleHashChange);
      window.removeEventListener("algoviz:logout", handleLogout);
    };
  }, []);

  const handleAuthenticated = (name) => {
    localStorage.setItem(AUTH_STORAGE_KEY, "true");
    localStorage.setItem(USER_NAME_STORAGE_KEY, name);
    setIsAuthenticated(true);
    setUserName(name);
    window.location.hash = "problems";
  };

  if (!isAuthenticated) {
    if (hash === "#login" || hash === "#register") {
      return <AuthPage mode={hash.slice(1)} onAuthenticated={handleAuthenticated} />;
    }

    return <LandingPage />;
  }

  const getProblemIdFromHash = () => {
    const match = hash.match(/[?&]id=(\d+)/);
    return match ? Number(match[1]) : 1;
  };

  const handleOpenProblem = (problem) => {
    window.location.hash = `problem-details?id=${problem?.id || 1}`;
  };

  if (hash === "#login" || hash === "#register" || hash === "" || hash === "#top" || hash === "#problems") {
    return <ProblemsPage userName={userName} onOpenProblem={handleOpenProblem} />;
  }

  if (hash === "#analyze") {
    return <AnalyzePage userName={userName} />;
  }

  if (hash.startsWith("#problem-details")) {
    return <ProblemDetailsPage userName={userName} problemId={getProblemIdFromHash()} />;
  }

  if (hash === "#history") {
    return <HistoryPage userName={userName} />;
  }

  if (hash === "#learn") {
    return <LearnPage userName={userName} />;
  }

  if (hash === "#profile") {
    return <ProfilePage userName={userName} />;
  }

  return <ProblemsPage userName={userName} onOpenProblem={handleOpenProblem} />;
}

export default App;
