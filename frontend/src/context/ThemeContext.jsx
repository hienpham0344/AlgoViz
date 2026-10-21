import { createContext, useContext, useEffect } from "react";

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
  useEffect(() => {
    try {
      localStorage.removeItem("algoviz-theme");
      localStorage.removeItem("algoviz-theme-preference");
    } catch {}

    // Ensure DOM root is always cleanly in light mode
    document.documentElement.setAttribute("data-theme", "light");
    document.documentElement.classList.remove("dark");
    document.documentElement.style.colorScheme = "light";
  }, []);

  const value = {
    theme: "light",
    resolvedTheme: "light",
    preference: "light",
    isDark: false,
    toggleTheme: () => {},
    setTheme: () => {},
    setPreference: () => {},
  };

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  return useContext(ThemeContext);
}
