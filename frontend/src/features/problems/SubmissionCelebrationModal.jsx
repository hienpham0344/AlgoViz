import { useEffect, useRef, useState } from "react";
import "./SubmissionCelebrationModal.css";

export default function SubmissionCelebrationModal({
  isOpen,
  onClose,
  data,
  onViewSubmissions,
  onViewSolution,
}) {
  const canvasRef = useRef(null);
  const [showContent, setShowContent] = useState(false);

  useEffect(() => {
    if (!isOpen) {
      setShowContent(false);
      return;
    }

    const timer = setTimeout(() => setShowContent(true), 50);

    // Canvas Confetti Animation
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    let animId;

    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener("resize", resize);

    const colors = [
      "#10b981", // emerald
      "#20beff", // cyan
      "#f59e0b", // gold/amber
      "#8b5cf6", // violet
      "#ec4899", // pink
      "#3b82f6", // blue
    ];

    const particleCount = 80;
    const particles = [];
    const originX = window.innerWidth / 2;
    const originY = window.innerHeight * 0.35;

    for (let i = 0; i < particleCount; i++) {
      const angle = (Math.PI * 2 * i) / particleCount + (Math.random() - 0.5) * 0.5;
      const speed = Math.random() * 12 + 6;
      particles.push({
        x: originX,
        y: originY,
        vx: Math.cos(angle) * speed * (Math.random() * 1.5 + 0.8),
        vy: Math.sin(angle) * speed - Math.random() * 6 - 4, // burst upward
        size: Math.random() * 8 + 5,
        color: colors[Math.floor(Math.random() * colors.length)],
        rotation: Math.random() * 360,
        rotationSpeed: (Math.random() - 0.5) * 14,
        alpha: 1,
        shape: Math.random() > 0.4 ? "rect" : "circle",
        drag: 0.95,
        gravity: 0.32,
      });
    }

    let startTime = Date.now();
    const duration = 3800; // 3.8s confetti

    const render = () => {
      const elapsed = Date.now() - startTime;
      ctx.clearRect(0, 0, canvas.width, canvas.height);

      for (let i = 0; i < particles.length; i++) {
        const p = particles[i];
        p.vx *= p.drag;
        p.vy = p.vy * p.drag + p.gravity;
        p.x += p.vx;
        p.y += p.vy;
        p.rotation += p.rotationSpeed;

        if (elapsed > 2000) {
          p.alpha = Math.max(0, 1 - (elapsed - 2000) / 1800);
        }

        ctx.save();
        ctx.globalAlpha = p.alpha;
        ctx.translate(p.x, p.y);
        ctx.rotate((p.rotation * Math.PI) / 180);
        ctx.fillStyle = p.color;

        if (p.shape === "rect") {
          ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
        } else {
          ctx.beginPath();
          ctx.arc(0, 0, p.size / 2, 0, Math.PI * 2);
          ctx.fill();
        }

        ctx.restore();
      }

      if (elapsed < duration) {
        animId = requestAnimationFrame(render);
      } else {
        ctx.clearRect(0, 0, canvas.width, canvas.height);
      }
    };

    animId = requestAnimationFrame(render);

    return () => {
      clearTimeout(timer);
      cancelAnimationFrame(animId);
      window.removeEventListener("resize", resize);
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const runtimeMs = data?.runtimeMs ?? 24;
  const memoryMb = data?.memoryMb ?? 14.8;
  const runtimePercentile = data?.runtimePercentile ?? "88.6%";
  const memoryPercentile = data?.memoryPercentile ?? "84.2%";
  const problemTitle = data?.problemTitle ?? "Two Sum";

  return (
    <div
      className={`celebration-overlay ${showContent ? "is-visible" : ""}`}
      role="dialog"
      aria-modal="true"
      aria-labelledby="celebration-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <canvas ref={canvasRef} className="celebration-canvas" aria-hidden="true" />

      <div className={`celebration-card ${showContent ? "is-entered" : ""}`}>
        <button
          type="button"
          className="celebration-close-btn"
          onClick={onClose}
          aria-label="Close celebration dialog"
        >
          ✕
        </button>

        {/* Animated Checkmark Icon */}
        <div className="celebration-hero-icon">
          <div className="celebration-glow-ring" />
          <div className="celebration-icon-circle">
            <svg
              className="celebration-svg-check"
              width="36"
              height="36"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#ffffff"
              strokeWidth="3.2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <polyline points="20 6 9 17 4 12" />
            </svg>
          </div>
        </div>

        <div className="celebration-badge-pill">
          <span className="sparkle-glyph">✨</span>
          <span>ACCEPTED</span>
          <span className="sparkle-glyph">✨</span>
        </div>

        <h2 id="celebration-title" className="celebration-heading">
          Congratulations! 🎉
        </h2>
        <p className="celebration-subtext">
          All test cases passed successfully for <strong>{problemTitle}</strong>!
        </p>

        {/* Performance Benchmark Cards */}
        <div className="celebration-metrics-grid">
          {/* Runtime Metric */}
          <div className="celebration-metric-box celebration-metric-box--runtime">
            <div className="metric-header">
              <div className="metric-label-group">
                <span className="metric-icon">⏱️</span>
                <span className="metric-title">Runtime</span>
              </div>
              <span className="metric-val"><strong>{runtimeMs}</strong> ms</span>
            </div>
            <div className="metric-progress-wrapper">
              <div
                className="metric-progress-fill metric-progress-fill--runtime"
                style={{ width: runtimePercentile }}
              />
            </div>
            <span className="metric-footer-text">
              Beats <strong>{runtimePercentile}</strong> of {data?.language || "Python"} submissions
            </span>
          </div>

          {/* Memory Metric */}
          <div className="celebration-metric-box celebration-metric-box--memory">
            <div className="metric-header">
              <div className="metric-label-group">
                <span className="metric-icon">🧠</span>
                <span className="metric-title">Memory</span>
              </div>
              <span className="metric-val"><strong>{memoryMb}</strong> MB</span>
            </div>
            <div className="metric-progress-wrapper">
              <div
                className="metric-progress-fill metric-progress-fill--memory"
                style={{ width: memoryPercentile }}
              />
            </div>
            <span className="metric-footer-text">
              Beats <strong>{memoryPercentile}</strong> of submissions
            </span>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="celebration-actions-row">
          <button
            type="button"
            className="celebration-btn celebration-btn--solution"
            onClick={() => {
              onClose();
              if (onViewSolution) onViewSolution();
            }}
          >
            <span className="btn-icon">⚡</span>
            <span>Explore Solution & Visualizer</span>
          </button>

          <button
            type="button"
            className="celebration-btn celebration-btn--submissions"
            onClick={() => {
              onClose();
              if (onViewSubmissions) onViewSubmissions();
            }}
          >
            <span className="btn-icon">📋</span>
            <span>View Submissions Tab</span>
          </button>
        </div>

        <button
          type="button"
          className="celebration-continue-btn"
          onClick={onClose}
        >
          Continue Coding
        </button>
      </div>
    </div>
  );
}
