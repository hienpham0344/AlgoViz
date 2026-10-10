import { useEffect, useState } from "react";
import AnalyzePage from "./pages/analyze.jsx";
import AuthPage from "./pages/auth.jsx";
import HistoryPage from "./pages/history.jsx";
import LearnPage from "./pages/learn.jsx";
import LandingPage from "./pages/landing.jsx";
import ProfilePage from "./pages/profile.jsx";
import ProblemsPage from "./pages/problem/problems.jsx";
import ProblemDetailsPage from "./pages/problem/problemDetails.jsx";

const AUTH_STORAGE_KEY = "algoviz-auth";
const USER_NAME_STORAGE_KEY = "algoviz-user-name";
const SELECTED_PROBLEM_STORAGE_KEY = "algoviz-selected-problem";

function App() {
  const [hash, setHash] = useState(() => window.location.hash);
  const [selectedProblem, setSelectedProblem] = useState(() => {
    try {
      return JSON.parse(
        sessionStorage.getItem(SELECTED_PROBLEM_STORAGE_KEY) || "null",
      );
    } catch {
      sessionStorage.removeItem(SELECTED_PROBLEM_STORAGE_KEY);
      return null;
    }
  });
  const [isAuthenticated, setIsAuthenticated] = useState(
    () => localStorage.getItem(AUTH_STORAGE_KEY) === "true",
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
    const handleSelectedProblem = (event) => {
      setSelectedProblem(event.detail);
    };
    window.addEventListener("hashchange", handleHashChange);
    window.addEventListener("algoviz:logout", handleLogout);
    window.addEventListener("algoviz:problem-selected", handleSelectedProblem);
    return () => {
      window.removeEventListener("hashchange", handleHashChange);
      window.removeEventListener("algoviz:logout", handleLogout);
      window.removeEventListener("algoviz:problem-selected", handleSelectedProblem);
    };
  }, []);

  const handleAuthenticated = (name) => {
    localStorage.setItem(AUTH_STORAGE_KEY, "true");
    localStorage.setItem(USER_NAME_STORAGE_KEY, name);
    setIsAuthenticated(true);
    setUserName(name);
    window.location.hash = "problems";
  };

  const openProblem = (problem) => {
    if (problem) {
      sessionStorage.setItem(SELECTED_PROBLEM_STORAGE_KEY, JSON.stringify(problem));
      setSelectedProblem(problem);
    }
    window.location.hash = "problem-details";
  };

  if (!isAuthenticated) {
    if (hash === "#login" || hash === "#register") {
      return <AuthPage mode={hash.slice(1)} onAuthenticated={handleAuthenticated} />;
    }

    return <LandingPage />;
  }

  if (hash === "#login" || hash === "#register" || hash === "" || hash === "#top") {
    return <ProblemsPage userName={userName} onOpenProblem={openProblem} />;
  }

  if (hash === "#analyze") {
    return <AnalyzePage userName={userName} />;
  }

  if (hash === "#problem-details") {
    return <ProblemDetailsPage userName={userName} problem={selectedProblem} />;
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

  return <ProblemsPage userName={userName} onOpenProblem={openProblem} />;
}

export default App;
