const stateLabels = {
  Completed: "Solved",
  "In Progress": "Attempted",
  "Not Started": "Todo",
};

function StatusIcon({ status }) {
  if (status === "Completed") {
    return (
      <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.5" />
        <path d="M5 8.2L7 10.2L11 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
    );
  }
  if (status === "In Progress") {
    return (
      <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.5" strokeDasharray="3 3" />
        <circle cx="8" cy="8" r="3" fill="currentColor" />
      </svg>
    );
  }
  return (
    <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.2" opacity="0.4" />
    </svg>
  );
}

function BadgeState({ status = "Completed", className = "" }) {
  const label = stateLabels[status] || status;
  const statusSlug = status.toLowerCase().replace(/\s+/g, "-");

  return (
    <span className={`av-badge-state av-state-${statusSlug} ${className}`.trim()}>
      <StatusIcon status={status} />
      <span>{label}</span>
    </span>
  );
}

export default BadgeState;
