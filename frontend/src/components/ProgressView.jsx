import React from "react";
import "./ProgressView.css";

/**
 * ProgressView
 * 
 * Standard progress and loading indicator inspired by SwiftUI's ProgressView
 * and WAI-ARIA 1.2 progressbar guidelines.
 * 
 * Supports:
 * 1. Indeterminate Spinner: when remaining work is unknown (role="status", aria-live="polite").
 * 2. Determinate Progress Ring: compact circular arc filling toward completion (role="progressbar", aria-valuenow).
 * 3. HTML <progress> Linear Bar: track for horizontal linear completion.
 */
export default function ProgressView({
  value,
  max = 100,
  min = 0,
  variant, // "spinner" | "ring" | "linear"
  size = "md", // for spinner: "sm" | "md" | "lg" (or number px); for ring: diameter in px (e.g. 56)
  strokeWidth = 3.5, // for ring
  color = "#0083bf",
  trackColor = "#dadce0",
  label,
  showLabel = false,
  showValue = false,
  className = "",
  style = {},
  ...props
}) {
  // Determine variant automatically if not specified
  const resolvedVariant = variant || (value == null ? "spinner" : "linear");

  // Normalized percentage calculation
  const numValue = typeof value === "number" ? value : 0;
  const range = max - min > 0 ? max - min : 100;
  const percent = Math.min(100, Math.max(0, Math.round(((numValue - min) / range) * 100)));

  // -------------------------------------------------------------------------
  // 1. INDETERMINATE SPINNER (work remaining unknown)
  // -------------------------------------------------------------------------
  if (resolvedVariant === "spinner") {
    const sizeMap = { sm: 16, md: 22, lg: 32 };
    const pixelSize = typeof size === "number" ? size : sizeMap[size] || 22;
    const sizeClass = typeof size === "string" ? `progress-view-spinner--${size}` : "";

    return (
      <span
        className={`progress-view-spinner ${sizeClass} ${className}`.trim()}
        role="status"
        aria-live="polite"
        aria-label={label || "Loading"}
        style={style}
        {...props}
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="progress-spinner-svg"
          width={pixelSize}
          height={pixelSize}
          aria-hidden="true"
        >
          <circle
            cx="12"
            cy="12"
            r="9.5"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeOpacity="0.2"
          />
          <path
            d="M12 2.5C17.2467 2.5 21.5 6.75329 21.5 12"
            stroke="currentColor"
            strokeWidth="2.5"
            strokeLinecap="round"
          />
        </svg>
        {label && <span className="progress-spinner-label">{label}</span>}
        <span className="sr-only">{label || "Loading..."}</span>
      </span>
    );
  }

  // -------------------------------------------------------------------------
  // 2. DETERMINATE PROGRESS RING (compact circular arc)
  // -------------------------------------------------------------------------
  if (resolvedVariant === "ring") {
    const diameter = typeof size === "number" ? size : 56;
    const radius = (diameter - strokeWidth) / 2;
    const center = diameter / 2;
    const circumference = 2 * Math.PI * radius;
    const strokeDashoffset = circumference - (percent / 100) * circumference;

    return (
      <div
        className={`progress-view-ring ${className}`.trim()}
        role="progressbar"
        aria-valuenow={percent}
        aria-valuemin={min}
        aria-valuemax={max}
        aria-valuetext={`${percent}%`}
        aria-label={label || "Progress ring"}
        style={{ width: diameter, height: diameter, ...style }}
        {...props}
      >
        <svg
          width={diameter}
          height={diameter}
          viewBox={`0 0 ${diameter} ${diameter}`}
          className="progress-ring-svg"
          aria-hidden="true"
        >
          {/* Background track circle */}
          <circle
            cx={center}
            cy={center}
            r={radius}
            fill="none"
            stroke={trackColor}
            strokeWidth={strokeWidth}
            className="progress-ring-track"
          />
          {/* Filling progress arc */}
          <circle
            cx={center}
            cy={center}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            transform={`rotate(-90 ${center} ${center})`}
            className="progress-ring-fill"
          />
          {/* Centered visible percentage */}
          {showValue && (
            <text
              x={center}
              y={center + 4}
              textAnchor="middle"
              className="progress-ring-text"
            >
              {percent}%
            </text>
          )}
        </svg>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // 3. HTML <progress> LINEAR BAR (readable track)
  // -------------------------------------------------------------------------
  return (
    <div className={`progress-view-linear-wrap ${className}`.trim()} style={style}>
      {((label && showLabel) || showValue) && (
        <div className="progress-view-linear-header">
          {label && showLabel && <span className="progress-view-linear-label">{label}</span>}
          {showValue && (
            <span className="progress-view-linear-value" aria-hidden="true">
              {percent}%
            </span>
          )}
        </div>
      )}
      <progress
        className="progress-view-bar"
        value={numValue}
        max={max}
        aria-valuenow={percent}
        aria-valuemin={min}
        aria-valuemax={max}
        aria-label={label || `${percent}% progress`}
        {...props}
      >
        {percent}%
      </progress>
    </div>
  );
}
