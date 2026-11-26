function BadgeDifficulty({ state = "Easy", className = "" }) {
  const raw = String(state || "Easy").trim() || "Easy";
  const normalized = raw.charAt(0).toUpperCase() + raw.slice(1).toLowerCase();
  
  return (
    <span
      className={`av-badge-difficulty av-difficulty-${normalized.toLowerCase()} ${className}`.trim()}
      data-difficulty={normalized}
    >
      <span className="difficulty-indicator-dot" aria-hidden="true" />
      <span>{normalized}</span>
    </span>
  );
}

export default BadgeDifficulty;
