import { useEffect, useRef, useState } from "react";
import "./CustomSelect.css";

export default function CustomSelect({
  value,
  onChange,
  options = [],
  placeholder = "Select...",
  ariaLabel,
  className = "",
  direction = "auto",
  align = "left",
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [computedDirection, setComputedDirection] = useState(
    direction === "up" ? "up" : "down"
  );
  const containerRef = useRef(null);

  // Normalize options array
  const normalizedOptions = options.map((opt) => {
    if (typeof opt === "object" && opt !== null) {
      return opt;
    }
    return { value: opt, label: opt };
  });

  const selectedOption = normalizedOptions.find((opt) => opt.value === value) || {
    value,
    label: value || placeholder,
  };

  useEffect(() => {
    if (isOpen) {
      if (direction === "auto") {
        if (containerRef.current) {
          const rect = containerRef.current.getBoundingClientRect();
          const spaceBelow = window.innerHeight - rect.bottom;
          const spaceAbove = rect.top;
          if (spaceBelow < 220 && spaceAbove > spaceBelow) {
            setComputedDirection("up");
          } else {
            setComputedDirection("down");
          }
        }
      } else {
        setComputedDirection(direction);
      }
    }
  }, [isOpen, direction]);

  useEffect(() => {
    function handleClickOutside(event) {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }

    function handleKeyDown(event) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }

    if (isOpen) {
      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("keydown", handleKeyDown);
    }
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen]);

  const handleSelect = (optValue) => {
    onChange?.(optValue);
    setIsOpen(false);
  };

  return (
    <div
      ref={containerRef}
      className={`custom-select-container ${isOpen ? "is-open" : ""} direction-${computedDirection} align-${align} ${className}`}
    >
      <button
        type="button"
        className="custom-select-trigger"
        onClick={() => setIsOpen((prev) => !prev)}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label={ariaLabel || selectedOption.label}
      >
        <div className="custom-select-trigger-content">
          {selectedOption.dotColor && (
            <span
              className="custom-select-dot"
              style={{ backgroundColor: selectedOption.dotColor }}
              aria-hidden="true"
            />
          )}
          {selectedOption.icon && (
            <span className="custom-select-icon" aria-hidden="true">
              {selectedOption.icon}
            </span>
          )}
          <span className="custom-select-label">{selectedOption.label}</span>
        </div>
        <svg
          className={`custom-select-chevron ${isOpen ? "is-rotated" : ""}`}
          width="14"
          height="14"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
          aria-hidden="true"
        >
          <polyline points="6 9 12 15 18 9" />
        </svg>
      </button>

      {isOpen && (
        <ul className="custom-select-menu" role="listbox" aria-label={ariaLabel || "Options"}>
          {normalizedOptions.map((opt) => {
            const isSelected = opt.value === value;
            return (
              <li
                key={opt.value}
                role="option"
                aria-selected={isSelected}
                tabIndex={0}
                className={`custom-select-item ${isSelected ? "is-selected" : ""}`}
                onClick={() => handleSelect(opt.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    handleSelect(opt.value);
                  }
                }}
              >
                <div className="custom-select-item-content">
                  {opt.dotColor && (
                    <span
                      className="custom-select-dot"
                      style={{ backgroundColor: opt.dotColor }}
                      aria-hidden="true"
                    />
                  )}
                  {opt.icon && (
                    <span className="custom-select-icon" aria-hidden="true">
                      {opt.icon}
                    </span>
                  )}
                  <span className="custom-select-item-label">{opt.label}</span>
                </div>
                {isSelected && (
                  <svg
                    className="custom-select-check"
                    width="14"
                    height="14"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                  >
                    <polyline points="20 6 9 17 4 12" />
                  </svg>
                )}
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
}
