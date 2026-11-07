import { lazy, Suspense, useEffect, useState } from "react";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
const AnalyzePage = lazy(() => import("./features/analyze/analyze.jsx"));
const AuthPage = lazy(() => import("./features/auth/auth.jsx"));
const HistoryPage = lazy(() => import("./features/history/history.jsx"));
const LearnPage = lazy(() => import("./features/learn/learn.jsx"));
const LessonPage = lazy(() => import("./features/learn/LessonPage.jsx"));
const LandingPage = lazy(() => import("./features/landing/landing.jsx"));
const ProfilePage = lazy(() => import("./features/profile/profile.jsx"));
const ProblemsPage = lazy(() => import("./features/problems/problems.jsx"));
const ProblemDetailsPage = lazy(() => import("./features/problems/problemDetails.jsx"));
import ApiState from "./components/ApiState.jsx";
import { NavigationBar } from "./components/index.js";

import { clearAuthData, getToken } from "./services/api.js";

const AUTH_STORAGE_KEY = "algoviz-auth";
const USER_NAME_STORAGE_KEY = "algoviz-user-name";
function getRoutePath() {
  const hash = window.location.hash || "";
  const pathname = window.location.pathname || "/";

  // If hash has an explicit route (not empty, not just '#' or '#top')
  if (hash && hash !== "#" && hash !== "#top") {
    const cleaned = hash.replace(/^#\/?/, "").split("?")[0].trim().toLowerCase();
    if (cleaned) {
      return cleaned;
    }
  }

  // Otherwise, use pathname: e.g. "/visualizer/sorting" -> "visualizer/sorting"
  return pathname.replace(/^\/+|\/+$/g, "").split("?")[0].trim().toLowerCase();
}

function getQueryParam(key) {
  const hash = window.location.hash || "";
  const searchSource = hash.includes("?")
    ? hash.slice(hash.indexOf("?"))
    : window.location.search || "";
  return new URLSearchParams(searchSource).get(key);
}

function getProblemId() {
  const hash = window.location.hash || "";
  const searchSource = hash.includes("?")
    ? hash.slice(hash.indexOf("?"))
    : window.location.search || "";
  const match = searchSource.match(/[?&]id=(\d+)/);
  if (match) return Number(match[1]);
  const pathMatch = (hash || window.location.pathname).match(/problem-details\/(\d+)/);
  if (pathMatch) return Number(pathMatch[1]);
  return 1;
}

function App() {
  const [, setLocationKey] = useState(
    () => window.location.pathname + window.location.search + window.location.hash,
  );
  const [isAuthenticated, setIsAuthenticated] = useState(
    () => localStorage.getItem(AUTH_STORAGE_KEY) === "true" && Boolean(getToken()),
  );
  const [userName, setUserName] = useState(
    () => getToken() ? localStorage.getItem(USER_NAME_STORAGE_KEY) || "Developer" : "Guest",
  );

  useEffect(() => {
    const handleLocationChange = () => {
      setLocationKey(window.location.pathname + window.location.search + window.location.hash);
    };
    const handleLogout = () => {
      clearAuthData();
      setIsAuthenticated(false);
      setUserName("Your name");
      if (window.location.pathname !== "/") {
        window.history.pushState(null, "", "/");
      }
      window.location.hash = "#landing";
      setLocationKey("/#landing");
    };
    window.addEventListener("hashchange", handleLocationChange);
    window.addEventListener("popstate", handleLocationChange);
    window.addEventListener("algoviz:logout", handleLogout);
    const handleProfileUpdate = () => setUserName(localStorage.getItem(USER_NAME_STORAGE_KEY) || "Your name");
    const handleStorage = (event) => {
      if (event.key === "algoviz-token" || event.key === AUTH_STORAGE_KEY || event.key === null) {
        setIsAuthenticated(Boolean(getToken()));
        handleProfileUpdate();
      }
    };
    window.addEventListener("algoviz:profile-updated", handleProfileUpdate);
    window.addEventListener("storage", handleStorage);
    return () => {
      window.removeEventListener("hashchange", handleLocationChange);
      window.removeEventListener("popstate", handleLocationChange);
      window.removeEventListener("algoviz:logout", handleLogout);
      window.removeEventListener("algoviz:profile-updated", handleProfileUpdate);
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  const handleAuthenticated = (name, token) => {
    if (!token || !getToken()) return;
    localStorage.setItem(AUTH_STORAGE_KEY, "true");
    localStorage.setItem(USER_NAME_STORAGE_KEY, name);
    setIsAuthenticated(true);
    setUserName(name);
    const requestedRoute = getRoutePath();
    const next = getQueryParam("next");
    const protectedDestination = requestedRoute === "profile" || requestedRoute.startsWith("profile/") || requestedRoute === "history" || requestedRoute.startsWith("history/");
    if (next && /^problem-details\?id=\d+(?:&tab=(?:submissions|visualizer))?$/.test(next)) {
      window.location.hash = `#${next}`;
    } else if (!protectedDestination) {
      if (window.location.pathname === "/login" || window.location.pathname === "/register") {
        window.history.pushState(null, "", "/problems");
      } else {
        window.location.hash = "#problems";
      }
    }
    setLocationKey(window.location.pathname + window.location.search + window.location.hash);
  };

  const handleOpenProblem = (problem) => {
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

  const route = getRoutePath();
  let pageContent;
  let pageKey;
  let activeNav = null;

  if (route === "login" || route === "register") {
    pageKey = route;
    pageContent = <AuthPage mode={route} onAuthenticated={handleAuthenticated} />;
  } else if (route === "landing" || (!route && !isAuthenticated)) {
    pageKey = "landing";
    pageContent = <LandingPage isAuthenticated={isAuthenticated} userName={userName} />;
  } else if (
    route === "visualize" ||
    route === "visualizer" ||
    route === "visualizer/sorting" ||
    route.startsWith("visualizer/") ||
    route === "analyze" ||
    route.startsWith("analyze/")
  ) {
    pageKey = "visualize";
    activeNav = "Visualize";
    pageContent = <AnalyzePage userName={userName} isAuthenticated={isAuthenticated} />;
  } else if (route === "problem-details" || route.startsWith("problem-details/")) {
      const pId = getProblemId();
      pageKey = `problem-details-${pId}`;
      pageContent = (
        <ProblemDetailsPage
          key={pId}
          userName={userName}
          problemId={pId}
          isAuthenticated={isAuthenticated}
        />
      );
  } else if (!isAuthenticated && (route === "history" || route.startsWith("history/") || route === "profile" || route.startsWith("profile/"))) {
    pageKey = "login";
    pageContent = <AuthPage mode="login" onAuthenticated={handleAuthenticated} />;
  } else if (route === "history" || route.startsWith("history/")) {
    pageKey = "history";
    activeNav = "History";
    pageContent = <HistoryPage userName={userName} isAuthenticated={isAuthenticated} />;
  } else if (route === "lesson" || route.startsWith("lesson/") || route.startsWith("learn-lesson")) {
    const courseId = getQueryParam("courseId") || "sorting-masterclass";
    const lessonId = parseInt(getQueryParam("lessonId") || "1", 10);
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
  } else if (route === "learn" || route.startsWith("learn/")) {
    pageKey = "learn";
    activeNav = "Learn";
    pageContent = <LearnPage userName={userName} isAuthenticated={isAuthenticated} />;
  } else if (route === "profile" || route.startsWith("profile/")) {
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
          if (!getRoutePath().startsWith("problem-details")) {
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
          <Suspense fallback={<ApiState loading page label="Loading page…" />}>{pageContent}</Suspense>
        </motion.div>
      </AnimatePresence>
    </div>
  );
}

export default App;
