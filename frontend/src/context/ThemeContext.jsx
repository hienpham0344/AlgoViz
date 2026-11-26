/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import "../dark-theme.css";

const THEME_STORAGE_KEY = "algoviz-theme";

function getInitialTheme() {
  try {
    if (typeof window !== "undefined" && window.localStorage) {
      const saved = window.localStorage.getItem(THEME_STORAGE_KEY);
      if (saved === "dark" || saved === "light") return saved;
    }
  } catch {
    // ignore storage access errors
  }
  return "light";
}

const DEFAULT_THEME_VALUE = {
  theme: "light",
  resolvedTheme: "light",
  preference: "light",
  isDark: false,
  toggleTheme: () => {},
  setTheme: () => {},
  setPreference: () => {},
};

const ThemeContext = createContext(DEFAULT_THEME_VALUE);

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(getInitialTheme);

  const setTheme = useCallback((nextTheme) => {
    setThemeState((current) => {
      const candidate = typeof nextTheme === "function" ? nextTheme(current) : nextTheme;
      return candidate === "dark" ? "dark" : "light";
    });
  }, []);

  const toggleTheme = useCallback(() => {
    setThemeState((current) => (current === "dark" ? "light" : "dark"));
  }, []);

  useEffect(() => {
    const resolved = theme === "dark" ? "dark" : "light";
    try {
      if (typeof window !== "undefined" && window.localStorage) {
        window.localStorage.setItem(THEME_STORAGE_KEY, resolved);
      }
    } catch {
      // ignore storage errors
    }
    if (typeof document !== "undefined" && document.documentElement) {
      document.documentElement.setAttribute("data-theme", resolved);
      document.documentElement.classList.toggle("dark", resolved === "dark");
      document.documentElement.style.colorScheme = resolved;
    }
  }, [theme]);

  const value = useMemo(
    () => ({
      theme,
      resolvedTheme: theme,
      preference: theme,
      isDark: theme === "dark",
      toggleTheme,
      setTheme,
      setPreference: setTheme,
    }),
    [theme, toggleTheme, setTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

// eslint-disable-next-line react-refresh/only-export-components
export function useTheme() {
  return useContext(ThemeContext);
}
