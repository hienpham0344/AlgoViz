import "./ProgressView.css";

/** Unknown work uses a spinner; known completion uses a ring or native track. */
export default function ProgressView({
  value,
  max = 100,
  min = 0,
  variant,
  size = "md",
  strokeWidth = 3.5,
  color,
  trackColor,
  label,
  showLabel = false,
  showValue = false,
  className = "",
  style = {},
  ...props
}) {
  const range = max - min;
  const determinate = Number.isFinite(value) && Number.isFinite(min) &&
    Number.isFinite(max) && Number.isFinite(range) && range > 0;
  const resolvedVariant = variant === "spinner" || !determinate ? "spinner" : variant || "linear";
  const percent = determinate
    ? Math.round(((Math.min(max, Math.max(min, value)) - min) / range) * 100)
    : undefined;
  const accessibleLabel = props["aria-label"] || label;

  if (resolvedVariant === "spinner") {
    const sizeMap = { sm: 16, md: 22, lg: 32 };
    const pixelSize = Number.isFinite(size) && size > 0 ? size : sizeMap[size] || 22;
    return (
      <span
        {...props}
        className={`progress-view-spinner ${className}`.trim()}
        role="progressbar"
        aria-label={accessibleLabel || "Loading"}
        aria-live="polite"
        aria-valuenow={undefined}
        aria-valuemin={undefined}
        aria-valuemax={undefined}
        aria-valuetext={undefined}
        style={{ ...(color ? { color } : {}), ...style }}
      >
        <svg viewBox="0 0 24 24" fill="none" className="progress-spinner-svg"
          width={pixelSize} height={pixelSize} aria-hidden="true">
          <circle cx="12" cy="12" r="9.5" stroke="currentColor" strokeWidth="2.5" strokeOpacity="0.2" />
          <path d="M12 2.5C17.2467 2.5 21.5 6.75329 21.5 12"
            stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
        {label && <span className="progress-spinner-label" aria-hidden="true">{label}</span>}
      </span>
    );
  }

  if (resolvedVariant === "ring") {
    const diameter = Number.isFinite(size) && size > 0 ? size : 56;
    const stroke = Number.isFinite(strokeWidth) && strokeWidth > 0
      ? Math.min(strokeWidth, diameter / 2) : Math.min(3.5, diameter / 2);
    const radius = (diameter - stroke) / 2;
    const center = diameter / 2;
    const circumference = 2 * Math.PI * radius;
    return (
      <div
        {...props}
        className={`progress-view-ring ${className}`.trim()}
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuetext={`${percent}%`}
        aria-label={accessibleLabel || "Progress"}
        style={{ width: diameter, height: diameter, ...style }}
      >
        <svg width={diameter} height={diameter} viewBox={`0 0 ${diameter} ${diameter}`}
          className="progress-ring-svg" aria-hidden="true">
          <circle cx={center} cy={center} r={radius} fill="none"
            stroke={trackColor || "var(--border-muted)"} strokeWidth={stroke} className="progress-ring-track" />
          <circle cx={center} cy={center} r={radius} fill="none"
            stroke={color || "var(--accent-primary-hover)"} strokeWidth={stroke}
            strokeDasharray={circumference} strokeDashoffset={circumference * (1 - percent / 100)}
            strokeLinecap="round" transform={`rotate(-90 ${center} ${center})`} className="progress-ring-fill" />
          {showValue && <text x={center} y={center + 4} textAnchor="middle" className="progress-ring-text">{percent}%</text>}
        </svg>
      </div>
    );
  }

  return (
    <div className={`progress-view-linear-wrap ${className}`.trim()}
      style={{ ...(color ? { "--progress-fill": color } : {}), ...(trackColor ? { "--progress-track": trackColor } : {}), ...style }}>
      {((label && showLabel) || showValue) && (
        <div className="progress-view-linear-header" aria-hidden="true">
          {label && showLabel && <span className="progress-view-linear-label">{label}</span>}
          {showValue && <span className="progress-view-linear-value">{percent}%</span>}
        </div>
      )}
      <progress
        {...props}
        className="progress-view-bar"
        role="progressbar"
        value={percent}
        max={100}
        aria-valuenow={percent}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuetext={`${percent}%`}
        aria-label={accessibleLabel || "Progress"}
      >{percent}%</progress>
    </div>
  );
}
