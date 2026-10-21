import { useEffect, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import AnalyzePage from "./features/analyze/analyze.jsx";
import AuthPage from "./features/auth/auth.jsx";
import HistoryPage from "./features/history/history.jsx";
import LearnPage from "./features/learn/learn.jsx";
import LessonPage from "./features/learn/LessonPage.jsx";
import LandingPage from "./features/landing/landing.jsx";
import ProfilePage from "./features/profile/profile.jsx";
import ProblemsPage from "./features/problems/problems.jsx";
import ProblemDetailsPage from "./features/problems/problemDetails.jsx";
import { NavigationBar } from "./components/index.js";

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
      localStorage.removeItem("algoviz-token");
      localStorage.removeItem("algoviz-user");
      localStorage.setItem(AUTH_STORAGE_KEY, "false");
      localStorage.removeItem(USER_NAME_STORAGE_KEY);
      setIsAuthenticated(false);
      setUserName("Your name");
      window.location.hash = "#landing";
      setHash("#landing");
    };
    window.addEventListener("hashchange", handleHashChange);
    window.addEventListener("algoviz:logout", handleLogout);
    return () => {
      window.removeEventListener("hashchange", handleHashChange);
      window.removeEventListener("algoviz:logout", handleLogout);
    };
  }, []);

  const handleAuthenticated = (name, token) => {
    localStorage.setItem(AUTH_STORAGE_KEY, "true");
    localStorage.setItem(USER_NAME_STORAGE_KEY, name);
    if (!getToken()) {
      localStorage.setItem("algoviz-token", token || ("offline-session-" + Date.now()));
    }
    setIsAuthenticated(true);
    setUserName(name);
    window.location.hash = "#problems";
  };

  const getProblemIdFromHash = () => {
    const match = hash.match(/[?&]id=(\d+)/);
    return match ? Number(match[1]) : 1;
  };

  const getHashParam = (key) => {
    const match = hash.match(new RegExp(`[?&]${key}=([^&#]+)`));
    return match ? decodeURIComponent(match[1]) : null;
  };

  const handleOpenProblem = (problem) => {
    if (!isAuthenticated) {
      window.location.hash = "#login";
      return;
    }
    window.location.hash = `problem-details?id=${problem?.id || 1}`;
  };

  const shouldReduceMotion = useReducedMotion();

  const pageVariants = shouldReduceMotion
    ? {
        initial: { opacity: 0 },
        animate: { opacity: 1, transition: { duration: 0.05 } },
        exit: { opacity: 0, transition: { duration: 0.05 } },
      }
    : {
        initial: { opacity: 0, y: 6 },
        animate: {
          opacity: 1,
          y: 0,
          transition: {
            duration: 0.22,
            ease: [0.16, 1, 0.3, 1], // fluid easeOutExpo
          },
          transitionEnd: {
            transform: "none",
          },
        },
        exit: {
          opacity: 0,
          y: -4,
          transition: {
            duration: 0.12,
            ease: [0.4, 0, 1, 1],
          },
        },
      };

  let pageContent;
  let pageKey;
  let activeNav = null;

  if (hash === "#login" || hash === "#register") {
    pageKey = hash;
    pageContent = <AuthPage mode={hash.slice(1)} onAuthenticated={handleAuthenticated} />;
  } else if (hash === "#landing" || (!hash && !isAuthenticated) || (hash === "#" && !isAuthenticated)) {
    pageKey = "landing";
    pageContent = <LandingPage isAuthenticated={isAuthenticated} userName={userName} />;
  } else if (hash.startsWith("#visualize") || hash.startsWith("#analyze")) {
    pageKey = "visualize";
    activeNav = "Visualize";
    pageContent = <AnalyzePage userName={userName} isAuthenticated={isAuthenticated} />;
  } else if (hash.startsWith("#problem-details") || hash === "#visualizer") {
    if (!isAuthenticated) {
      pageKey = "login";
      pageContent = <AuthPage mode="login" onAuthenticated={handleAuthenticated} />;
    } else {
      const pId = hash === "#visualizer" ? 1 : getProblemIdFromHash();
      pageKey = `problem-details-${pId}`;
      pageContent = (
        <ProblemDetailsPage
          key={pId}
          userName={userName}
          problemId={pId}
          isAuthenticated={isAuthenticated}
        />
      );
    }
  } else if (hash.startsWith("#history")) {
    pageKey = "history";
    activeNav = "History";
    pageContent = <HistoryPage userName={userName} isAuthenticated={isAuthenticated} />;
  } else if (hash.startsWith("#lesson") || hash.startsWith("#learn-lesson")) {
    const courseId = getHashParam("courseId") || "sorting-masterclass";
    const lessonId = parseInt(getHashParam("lessonId") || "1", 10);
    pageKey = `lesson-${courseId}-${lessonId}`;
    activeNav = "Learn";
    pageContent = (
      <LessonPage
        courseId={courseId}
        lessonId={lessonId}
        userName={userName}
        isAuthenticated={isAuthenticated}
      />
    );
  } else if (hash.startsWith("#learn")) {
    pageKey = "learn";
    activeNav = "Learn";
    pageContent = <LearnPage userName={userName} isAuthenticated={isAuthenticated} />;
  } else if (hash.startsWith("#profile")) {
    pageKey = "profile";
    activeNav = "Profile";
    pageContent = <ProfilePage userName={userName} isAuthenticated={isAuthenticated} />;
  } else {
    pageKey = "problems";
    activeNav = "Problems";
    pageContent = (
      <ProblemsPage
        userName={userName}
        isAuthenticated={isAuthenticated}
        onOpenProblem={handleOpenProblem}
      />
    );
  }

  return (
    <div className="algoviz-app-shell">
      {activeNav && (
        <NavigationBar
          active={activeNav}
          userName={userName}
          isAuthenticated={isAuthenticated}
        />
      )}
      <AnimatePresence
        mode="wait"
        initial={false}
        onExitComplete={() => {
          if (!window.location.hash.startsWith("#problem-details")) {
            window.scrollTo({ top: 0, left: 0, behavior: "instant" });
          }
        }}
      >
        <motion.div
          key={pageKey}
          className="algoviz-page-transition"
          variants={pageVariants}
          initial="initial"
          animate="animate"
          exit="exit"
        >
          {pageContent}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}

export default App;
