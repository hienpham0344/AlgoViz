const STATUS_PRESENTATION = {
  completed: { label: "Done", marker: "✓" },
  incomplete: { label: "Incomplete", marker: "✗" },
  "not-started": { label: "Not Started", marker: "○" },
};

function BadgeProblemStatus({ status = "not-started", className = "" }) {
  const presentation =
    STATUS_PRESENTATION[status] ?? STATUS_PRESENTATION["not-started"];

  return (
    <span
      className={`problem-progress-badge problem-progress-badge--${status} ${className}`.trim()}
      aria-label={presentation.label}
    >
      <span className="problem-progress-badge__marker" aria-hidden="true">
        {presentation.marker}
      </span>
      <strong>{presentation.label}</strong>
    </span>
  );
}

export default BadgeProblemStatus;
