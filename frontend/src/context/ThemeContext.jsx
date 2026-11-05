import { createContext, useContext, useEffect, useState } from "react";

const ThemeContext = createContext({
  theme: "light",
  resolvedTheme: "light",
  preference: "light",
  isDark: false,
  toggleTheme: () => {},
  setTheme: () => {},
  setPreference: () => {},
});

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(() => {
    try {
      const saved = localStorage.getItem("algoviz-theme");
      if (saved === "light") return "light";
      if (saved === "dark") {
        localStorage.setItem("algoviz-theme", "light");
        return "light";
      }
    } catch {}
    return "light";
  });

  useEffect(() => {
    try {
      localStorage.setItem("algoviz-theme", theme);
    } catch {}

    document.documentElement.setAttribute("data-theme", theme);
    if (theme === "dark") {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
    document.documentElement.style.colorScheme = theme;
  }, [theme]);

  const toggleTheme = () => {
    setThemeState((current) => (current === "dark" ? "light" : "dark"));
  };

  const setTheme = (nextTheme) => {
    if (nextTheme === "dark" || nextTheme === "light") {
      setThemeState(nextTheme);
    }
  };

  const isDark = theme === "dark";

  const value = {
    theme,
    resolvedTheme: theme,
    preference: theme,
    isDark,
    toggleTheme,
    setTheme,
    setPreference: setTheme,
  };

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
