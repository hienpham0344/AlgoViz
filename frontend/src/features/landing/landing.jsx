import { useState, useEffect, useRef } from "react";
import { api } from "../../services/api.js";
import ThemeToggle from "../../components/ThemeToggle.jsx";
import "./landing.css";

// Partner logos matching Kaggle's infinite marquee
function GoogleLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 85 28" fill="currentColor" aria-label="Google">
      <text x="0" y="21" fontFamily="'Google Sans', Inter, system-ui, sans-serif" fontSize="21" fontWeight="700" letterSpacing="-0.5">Google</text>
    </svg>
  );
}

function OpenAILogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 115 28" fill="currentColor" aria-label="OpenAI">
      <g transform="translate(2, 2)">
        <path d="M22.7 11.8a5.5 5.5 0 0 0-.5-4.3 5.6 5.6 0 0 0-5.5-2.8 5.6 5.6 0 0 0-4-1.8 5.6 5.6 0 0 0-5.3 3.7 5.6 5.6 0 0 0-3.5 2.5 5.6 5.6 0 0 0 .7 6 5.5 5.5 0 0 0 .5 4.3 5.6 5.6 0 0 0 5.5 2.8c.3.7.8 1.3 1.5 1.7a5.6 5.6 0 0 0 7.8-1.9 5.6 5.6 0 0 0 3.5-2.5 5.6 5.6 0 0 0-.7-6.1zm-8.2 9.8a4.1 4.1 0 0 1-2.5.4l.6-1.8 3.5-2 1.6.9a4.2 4.2 0 0 1-3.2 2.5zm-6.7-2.6a4.1 4.1 0 0 1-.9-2.4l1.8-.5 2 3.5-.8 1.6a4.2 4.2 0 0 1-2.1-2.2zm-1.8-6.9c.3-.8.8-1.5 1.6-2l1.2 1.5v4.1l-1.6.9a4.2 4.2 0 0 1-1.2-4.5zm9-4.2a4.1 4.1 0 0 1 2.5-.4l-.6 1.8-3.5 2-1.6-.9a4.2 4.2 0 0 1 3.2-2.5zm6.7 2.6c.5.7.8 1.5.9 2.4l-1.8.5-2-3.5.8-1.6a4.2 4.2 0 0 1 2.1 2.2zm1.8 6.9a4.1 4.1 0 0 1-1.6 2l-1.2-1.5v-4.1l1.6-.9a4.2 4.2 0 0 1 1.2 4.5z" />
        <text x="32" y="17" fontFamily="Inter, system-ui, sans-serif" fontSize="15" fontWeight="600" letterSpacing="-0.3">OpenAI</text>
      </g>
    </svg>
  );
}

function DeepMindLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 125 28" fill="currentColor" aria-label="DeepMind">
      <g transform="translate(4, 4)">
        <circle cx="5" cy="5" r="3" />
        <circle cx="13" cy="5" r="2" />
        <circle cx="5" cy="13" r="2" />
        <circle cx="13" cy="13" r="3" />
        <text x="24" y="14" fontFamily="Inter, system-ui, sans-serif" fontSize="15" fontWeight="600" letterSpacing="-0.2">DeepMind</text>
      </g>
    </svg>
  );
}

function ArcPrizeLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 100 28" fill="currentColor" aria-label="ARC Prize">
      <g transform="translate(2, 3)">
        <rect x="0" y="3" width="4" height="15" />
        <rect x="6" y="3" width="4" height="15" />
        <rect x="12" y="3" width="4" height="15" />
        <text x="22" y="11" fontFamily="Inter, system-ui, sans-serif" fontSize="11" fontWeight="800" letterSpacing="1">ARC</text>
        <text x="22" y="20" fontFamily="Inter, system-ui, sans-serif" fontSize="9" fontWeight="700" letterSpacing="2">PRIZE</text>
      </g>
    </svg>
  );
}

function LlamaIndexLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 120 28" fill="currentColor" aria-label="LlamaIndex">
      <g transform="translate(3, 4)">
        <path d="M3 16V8l3-4 3 4v8H3zm1.5-2h3V9.5L6 7.5l-1.5 2V14z" />
        <text x="17" y="14" fontFamily="Inter, system-ui, sans-serif" fontSize="14" fontWeight="600" letterSpacing="-0.3">LlamaIndex</text>
      </g>
    </svg>
  );
}

function StanfordLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 95 28" fill="currentColor" aria-label="Stanford">
      <text x="3" y="20" fontFamily="Georgia, 'Times New Roman', serif" fontSize="17" fontWeight="600" letterSpacing="0.2">Stanford</text>
    </svg>
  );
}

function CohereLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 90 28" fill="currentColor" aria-label="Cohere">
      <text x="3" y="19" fontFamily="'Courier New', Courier, monospace" fontSize="16" fontWeight="700" letterSpacing="-0.5">cohere</text>
    </svg>
  );
}

function JaneStreetLogo() {
  return (
    <svg className="kaggle-partner-logo" viewBox="0 0 115 28" fill="currentColor" aria-label="Jane Street">
      <g transform="translate(3, 4)">
        <circle cx="5" cy="9" r="4" fill="none" stroke="currentColor" strokeWidth="2" />
        <text x="15" y="13" fontFamily="Inter, system-ui, sans-serif" fontSize="13" fontWeight="600" letterSpacing="-0.2">Jane Street</text>
      </g>
    </svg>
  );
}

// 6 Cycling Featured Items for Floating Card (live algorithmic telemetry)
const FEATURED_ITEMS = [
  {
    id: 1,
    tag: "Interactive Trace",
    meta: "Live 2ms",
    typeClass: "tag-coral",
    title: "Dijkstra Shortest Path & Priority Queue",
    href: "#problem-details?id=1&tab=visualizer",
  },
  {
    id: 2,
    tag: "Benchmark",
    meta: "Live 60 FPS",
    typeClass: "tag-green",
    title: "QuickSort vs MergeSort Stress Benchmark (N=100K)",
    href: "#visualize",
  },
  {
    id: 3,
    tag: "State Machine",
    meta: "Step 14/42",
    typeClass: "tag-blue",
    title: "Binary Tree Level Order Traversal BFS",
    href: "#visualizer",
  },
  {
    id: 4,
    tag: "Dynamic Prog",
    meta: "Live 1ms",
    typeClass: "tag-amber",
    title: "Longest Increasing Subsequence O(N log N)",
    href: "#problem-details?id=2",
  },
  {
    id: 5,
    tag: "Graph Theory",
    meta: "Step 28/80",
    typeClass: "tag-blue",
    title: "Topological Sort & Cycle Detection (DAG)",
    href: "#problem-details?id=3&tab=visualizer",
  },
  {
    id: 6,
    tag: "Divide & Conquer",
    meta: "Live 120 FPS",
    typeClass: "tag-green",
    title: "Median of Two Sorted Arrays Binary Partition",
    href: "#problems",
  },
];

// Curated safe spawn zones across the mountain & sky canvas
const RANDOM_ZONES = [
  // 1. Bottom Right (Classic base)
  { top: "auto", bottom: "28px", left: "auto", right: "6%" },
  // 2. Bottom Left (Slate base)
  { top: "auto", bottom: "28px", left: "6%", right: "auto" },
  // 3. Top Right (Warm golden sky)
  { top: "24px", bottom: "auto", left: "auto", right: "7%" },
  // 4. Top Left (Cool slate sky)
  { top: "24px", bottom: "auto", left: "7%", right: "auto" },
  // 5. Mid Right (Golden mountain flank)
  { top: "40%", bottom: "auto", left: "auto", right: "5%" },
  // 6. Mid Left (Slate mountain flank)
  { top: "40%", bottom: "auto", left: "5%", right: "auto" },
];

// Rolling Kicker Titles (like Kaggle's Solution Seekers, Benchmark Climbers...)
const KICKER_WORDS = [
  "Algorithmic Thinkers",
  "Problem Solvers",
  "Competitive Programmers",
  "System Builders",
  "Standard Setters",
];

export default function LandingPage({ isAuthenticated = false, userName = "Developer" }) {
  const [cookieDismissed, setCookieDismissed] = useState(() => {
    return localStorage.getItem("algoviz-cookie-accepted") === "true";
  });

  // Kicker Slot-machine rotator index
  const [kickerIndex, setKickerIndex] = useState(0);

  // Floating card rotating index, random spawn zone & appearance state
  const [cardIndex, setCardIndex] = useState(() => Math.floor(Math.random() * FEATURED_ITEMS.length));
  const [zoneIndex, setZoneIndex] = useState(() => Math.floor(Math.random() * RANDOM_ZONES.length));
  const [jitter, setJitter] = useState({ x: 0, y: 0 });
  const [isSpawning, setIsSpawning] = useState(false);
  const [isHovered, setIsHovered] = useState(false);
  const [spawnCount, setSpawnCount] = useState(0);

  // Parallax container ref (uses CSS variables for 120 FPS performance without React re-render lags)
  const containerRef = useRef(null);

  // Kicker rotating timer
  useEffect(() => {
    const timer = setInterval(() => {
      setKickerIndex((prev) => (prev + 1) % KICKER_WORDS.length);
    }, 2800);
    return () => clearInterval(timer);
  }, []);

  // Floating card auto-cycler timer: stays visible for full duration, fades out, hops to a random new zone, and fades in
  useEffect(() => {
    let isCancelled = false;
    let cycleTimer = null;
    let fadeOutTimer = null;
    let fadeInTimer = null;

    const runCycle = () => {
      if (isCancelled) return;

      // 1. Stay visible for full 4.5s countdown
      cycleTimer = setTimeout(() => {
        if (isCancelled) return;

        // 2. Smoothly fade out current card
        setIsSpawning(true);

        // 3. After fade-out animation completes (300ms), reposition & change item while invisible
        fadeOutTimer = setTimeout(() => {
          if (isCancelled) return;

          setZoneIndex((prev) => {
            let next;
            do {
              next = Math.floor(Math.random() * RANDOM_ZONES.length);
            } while (next === prev && RANDOM_ZONES.length > 1);
            return next;
          });

          setCardIndex((prev) => {
            let next;
            do {
              next = Math.floor(Math.random() * FEATURED_ITEMS.length);
            } while (next === prev && FEATURED_ITEMS.length > 1);
            return next;
          });

          setJitter({
            x: Math.round((Math.random() - 0.5) * 20),
            y: Math.round((Math.random() - 0.5) * 14),
          });

          setSpawnCount((c) => c + 1);

          // 4. Brief pause while invisible (80ms), then fade in smoothly at the new spot
          fadeInTimer = setTimeout(() => {
            if (isCancelled) return;
            setIsSpawning(false);

            // 5. Schedule the next cycle recursively
            runCycle();
          }, 80);
        }, 300);
      }, 4500);
    };

    if (!isHovered) {
      runCycle();
    } else {
      setIsSpawning(false);
    }

    return () => {
      isCancelled = true;
      if (cycleTimer) clearTimeout(cycleTimer);
      if (fadeOutTimer) clearTimeout(fadeOutTimer);
      if (fadeInTimer) clearTimeout(fadeInTimer);
    };
  }, [isHovered]);

  useEffect(() => {
    window.scrollTo(0, 0);
  }, []);

  // High-performance hardware-accelerated mouse parallax
  const handleMouseMove = (e) => {
    if (!containerRef.current) return;
    const rect = containerRef.current.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width - 0.5) * 2;
    const y = ((e.clientY - rect.top) / rect.height - 0.5) * 2;
    containerRef.current.style.setProperty("--mouse-x", x.toFixed(3));
    containerRef.current.style.setProperty("--mouse-y", y.toFixed(3));
  };

  const handleMouseLeave = () => {
    if (!containerRef.current) return;
    containerRef.current.style.setProperty("--mouse-x", "0");
    containerRef.current.style.setProperty("--mouse-y", "0");
  };

  const handleDismissCookie = () => {
    localStorage.setItem("algoviz-cookie-accepted", "true");
    setCookieDismissed(true);
  };

  const currentItem = FEATURED_ITEMS[cardIndex] || FEATURED_ITEMS[0];
  const currentZone = RANDOM_ZONES[zoneIndex] || RANDOM_ZONES[0];
  const initial = (userName && userName !== "Your name" ? userName.charAt(0) : "U").toUpperCase();

  return (
    <div className="kaggle-landing">
      {/* 1. TOP NAVBAR */}
      <header className="kaggle-header" role="banner">
        <div className="kaggle-header-inner">
          <div className="kaggle-header-left">
            <a className="kaggle-brand" href="#landing" aria-label="AlgoViz Home">
              algoviz
            </a>

            <nav className="kaggle-nav-menu" aria-label="Main Navigation">
              <a className="kaggle-nav-link" href="#problems">
                Problems
              </a>
              <a className="kaggle-nav-link" href="#visualize">
                Visualize
              </a>
              <a className="kaggle-nav-link" href="#learn">
                Learn
              </a>
              <button 
                className="kaggle-nav-more" 
                aria-label="More options"
                onClick={() => { window.location.hash = "problems"; }}
              >
                ···
              </button>
            </nav>
          </div>

          <div className="kaggle-header-right">
            <button
              className="kaggle-search-btn"
              aria-label="Search algorithms and problems"
              onClick={() => { window.location.hash = "problems"; }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                <circle cx="11" cy="11" r="8" />
                <line x1="21" y1="21" x2="16.65" y2="16.65" />
              </svg>
            </button>

            {isAuthenticated ? (
              <div style={{ display: "flex", alignItems: "center", gap: "10px" }}>
                <a
                  href="#profile"
                  style={{
                    display: "inline-flex",
                    alignItems: "center",
                    justifyContent: "center",
                    width: "36px",
                    height: "36px",
                    borderRadius: "50%",
                    background: "#202124",
                    color: "#ffffff",
                    fontWeight: 700,
                    fontSize: "14px",
                    textDecoration: "none",
                  }}
                  title={userName}
                >
                  {initial}
                </a>
                <button
                  type="button"
                  className="kaggle-login-btn"
                  onClick={() => {
                    window.location.hash = "#landing";
                    api.logout();
                  }}
                  style={{ cursor: "pointer", border: "1px solid #dadce0" }}
                >
                  Logout
                </button>
              </div>
            ) : (
              <>
                <a className="kaggle-login-btn" href="#login">
                  Login
                </a>

                <a className="kaggle-signup-btn" href="#register">
                  Sign Up
                </a>
              </>
            )}
          </div>
        </div>
      </header>

      {/* 2. HERO DISPLAY SECTION */}
      <section className="kaggle-hero-section" aria-labelledby="kaggle-hero-heading">
        <div className="kaggle-hero-content">
          {/* Animated Kicker with rolling slot-machine text */}
          <div className="kaggle-hero-kicker">
            <span className="kaggle-kicker-prefix">AlgoViz is for </span>
            <span className="kaggle-kicker-rotator" aria-live="polite">
              <span
                className="kaggle-kicker-track"
                style={{ transform: `translateY(-${kickerIndex * 1.5}em)` }}
              >
                {KICKER_WORDS.map((word, i) => (
                  <span
                    key={word}
                    className={`kaggle-kicker-item ${i === kickerIndex ? "is-active" : ""}`}
                  >
                    {word}
                  </span>
                ))}
              </span>
            </span>
          </div>

          <h1 id="kaggle-hero-heading" className="kaggle-hero-title">
            Evaluating what works at the algorithmic frontier
          </h1>

          <p className="kaggle-hero-subtitle">
            Interactive code visualizers, execution traces, and benchmark sandboxes where the community builds deep algorithmic intuition.
          </p>
        </div>

        {/* 3. INTERACTIVE PANORAMIC MOUNTAIN & ANIMATED CONSTELLATION MESH */}
        <div
          ref={containerRef}
          className="kaggle-panorama-container"
          role="img"
          aria-label="Interactive geometric mountain landscape with animated glowing algorithmic network constellation"
          onMouseMove={handleMouseMove}
          onMouseLeave={handleMouseLeave}
        >
          <svg
            className="kaggle-panorama-svg"
            viewBox="0 0 1600 460"
            preserveAspectRatio="xMidYMid slice"
            xmlns="http://www.w3.org/2000/svg"
          >
            <defs>
              {/* Sky Background Gradient */}
              <linearGradient id="kaggleSkyGradient" x1="0%" y1="30%" x2="100%" y2="50%">
                <stop offset="0%" stopColor="#9C968B" />
                <stop offset="28%" stopColor="#BAB4A8" />
                <stop offset="55%" stopColor="#D5CFBE" />
                <stop offset="78%" stopColor="#EAB841" />
                <stop offset="100%" stopColor="#F5A31B" />
              </linearGradient>

              {/* Sun Flare radial glow with animated breathing shimmer */}
              <radialGradient id="kaggleSunGlow" cx="90%" cy="40%" r="65%">
                <stop offset="0%" stopColor="#FFF2A3" stopOpacity="0.9" />
                <stop offset="35%" stopColor="#F5B229" stopOpacity="0.45" />
                <stop offset="100%" stopColor="#EAB841" stopOpacity="0" />
              </radialGradient>

              {/* Shading gradients for mountain facets */}
              <linearGradient id="goldFacet1" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#FFE066" />
                <stop offset="100%" stopColor="#F5A623" />
              </linearGradient>
              <linearGradient id="goldFacet2" x1="0%" y1="0%" x2="100%" y2="100%">
                <stop offset="0%" stopColor="#F7C948" />
                <stop offset="100%" stopColor="#E69500" />
              </linearGradient>
              <linearGradient id="snowFacet" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#FFFFFF" />
                <stop offset="100%" stopColor="#EDF2F7" />
              </linearGradient>
              <linearGradient id="slateFacet" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#CBD5E1" />
                <stop offset="100%" stopColor="#94A3B8" />
              </linearGradient>
              <linearGradient id="shadowFacet" x1="100%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stopColor="#8295A8" />
                <stop offset="100%" stopColor="#627D98" />
              </linearGradient>

              {/* Glow filter for constellation pulses */}
              <filter id="nodeGlow" x="-50%" y="-50%" width="200%" height="200%">
                <feGaussianBlur stdDeviation="3" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>

            {/* Sky backdrop */}
            <rect width="1600" height="460" fill="url(#kaggleSkyGradient)" />
            <rect className="kaggle-sun-glow-animated" width="1600" height="460" fill="url(#kaggleSunGlow)" />

            {/* Distant foothills / haze horizon */}
            <path d="M0 410 Q 400 370 800 395 T 1600 410 L 1600 460 L 0 460 Z" fill="rgba(255, 255, 255, 0.18)" />

            {/* === CENTRAL MT. FUJI STYLE POLYGONAL MOUNTAIN (Parallax layer 1) === */}
            <g id="kaggle-mountain-polygons" className="kaggle-parallax-layer-mountain">
              {/* Left Shadow Slopes (Cool Slate & Blue-Grey) */}
              <polygon points="800,120 730,195 640,240 540,290 320,380 180,440 800,440" fill="url(#shadowFacet)" opacity="0.9" />
              <polygon points="800,120 740,210 680,260 520,370 410,440 650,440" fill="url(#slateFacet)" opacity="0.85" />
              <polygon points="730,195 650,270 570,320 480,410 400,440 540,440" fill="#718096" opacity="0.6" />

              {/* Central Snow Ridges & White Facets */}
              <polygon points="800,120 780,180 750,225 790,260 815,220 800,120" fill="url(#snowFacet)" />
              <polygon points="800,120 815,220 840,240 870,185 800,120" fill="#FFFFFF" />
              <polygon points="780,180 710,250 745,290 790,260" fill="#E2E8F0" />
              <polygon points="750,225 680,310 725,350 760,305" fill="#CBD5E1" />
              <polygon points="725,350 630,410 670,440 760,440" fill="#A0AEC0" />

              {/* Eastern / Sunlit Golden Facets (Catching Dawn Sunlight) */}
              <polygon points="800,120 870,185 910,215 885,270 830,230" fill="url(#goldFacet1)" />
              <polygon points="870,185 950,225 990,275 920,295 885,270" fill="url(#goldFacet2)" />
              <polygon points="910,215 1010,270 1060,330 980,350 920,295" fill="url(#goldFacet1)" />
              <polygon points="950,225 1070,305 1140,365 1040,390 980,350" fill="url(#goldFacet2)" />
              <polygon points="1010,270 1150,350 1260,405 1140,435 1040,390" fill="url(#goldFacet1)" />
              <polygon points="1150,350 1290,410 1440,440 1260,440" fill="#F6AD55" opacity="0.8" />

              {/* Ridge highlights in snow white & bright gold */}
              <polygon points="815,220 860,280 840,340 790,310 790,260" fill="#FFFFFF" opacity="0.95" />
              <polygon points="860,280 900,345 870,390 820,360 840,340" fill="url(#goldFacet1)" />
              <polygon points="840,340 880,410 840,440 790,440 810,380" fill="#F7FAFC" />
              <polygon points="900,345 960,405 920,440 870,440 870,390" fill="url(#goldFacet2)" />
            </g>

            {/* === ALGORITHMIC CONSTELLATION GRAPH LINES & ANIMATED PULSING NODES (Parallax layer 2) === */}
            <g
              id="kaggle-algorithmic-constellations"
              className="kaggle-parallax-layer-constellation"
              stroke="rgba(255, 255, 255, 0.85)"
              strokeWidth="1.6"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              {/* Static constellation wireframe */}
              <line x1="600" y1="210" x2="690" y2="170" />
              <line x1="690" y1="170" x2="800" y2="120" />
              <line x1="800" y1="120" x2="890" y2="150" />
              <line x1="890" y1="150" x2="1020" y2="210" />
              <line x1="1020" y1="210" x2="1120" y2="280" />

              <line x1="600" y1="210" x2="710" y2="250" />
              <line x1="690" y1="170" x2="780" y2="180" />
              <line x1="800" y1="120" x2="840" y2="240" />
              <line x1="890" y1="150" x2="885" y2="270" />
              <line x1="1020" y1="210" x2="980" y2="350" />

              {/* Sky branches */}
              <line x1="690" y1="170" x2="610" y2="120" opacity="0.75" />
              <line x1="800" y1="120" x2="800" y2="60" opacity="0.8" />
              <line x1="890" y1="150" x2="960" y2="90" opacity="0.75" />
              <line x1="1020" y1="210" x2="1080" y2="160" opacity="0.7" />

              {/* Animated Glowing Execution Wave Traces (Simulating algorithm traversal flow) */}
              <line className="kaggle-constellation-flow flow-1" x1="690" y1="170" x2="800" y2="120" stroke="#FFE066" strokeWidth="2.2" />
              <line className="kaggle-constellation-flow flow-2" x1="800" y1="120" x2="890" y2="150" stroke="#FFFFFF" strokeWidth="2.2" />
              <line className="kaggle-constellation-flow flow-3" x1="890" y1="150" x2="1020" y2="210" stroke="#FFE066" strokeWidth="2.2" />
              <line className="kaggle-constellation-flow flow-4" x1="800" y1="120" x2="840" y2="240" stroke="#FFFFFF" strokeWidth="2" strokeDasharray="6 4" />

              {/* Constellation Nodes with Staggered Pulse Animations */}
              <circle className="kaggle-pulse-node node-1" cx="690" cy="170" r="4.5" fill="#FFFFFF" filter="url(#nodeGlow)" />
              <circle className="kaggle-pulse-node node-peak" cx="800" cy="120" r="6" fill="#FFFFFF" filter="url(#nodeGlow)" />
              <circle className="kaggle-pulse-node node-2" cx="890" cy="150" r="4.5" fill="#FFFFFF" filter="url(#nodeGlow)" />
              <circle className="kaggle-pulse-node node-3" cx="1020" cy="210" r="4.5" fill="#FFFFFF" filter="url(#nodeGlow)" />
              <circle className="kaggle-pulse-node node-4" cx="600" cy="210" r="3.5" fill="#FFFFFF" />
              <circle className="kaggle-pulse-node node-5" cx="1120" cy="280" r="4" fill="#FFFFFF" />

              {/* Sky floating data vertices */}
              <circle className="kaggle-pulse-node node-6" cx="800" cy="60" r="3" fill="#FFFFFF" opacity="0.9" />
              <circle className="kaggle-pulse-node node-7" cx="610" cy="120" r="3" fill="#FFFFFF" opacity="0.8" />
              <circle className="kaggle-pulse-node node-gold-1" cx="960" cy="90" r="3.8" fill="#FFD363" filter="url(#nodeGlow)" />
              <circle className="kaggle-pulse-node node-gold-2" cx="1080" cy="160" r="3.2" fill="#FFE066" />

              {/* Animated direction arrow heads */}
              <polygon className="kaggle-vector-arrow arrow-1" points="798,62 800,53 802,62" fill="#FFFFFF" stroke="none" />
              <polygon className="kaggle-vector-arrow arrow-2" points="958,92 964,84 965,93" fill="#FFE066" stroke="none" />
            </g>
          </svg>

          {/* FLOATING INTERACTIVE PREVIEW CARD WITH RANDOMIZED SPAWN POSITION ACROSS PANORAMA */}
          <a
            className={`kaggle-floating-card ${isSpawning ? "is-spawning" : "has-spawned"}`}
            href={currentItem.href}
            aria-label={`View live trace: ${currentItem.title}`}
            style={{
              ...currentZone,
              "--jitter-x": `${jitter.x}px`,
              "--jitter-y": `${jitter.y}px`,
            }}
            onMouseEnter={() => {
              setIsHovered(true);
              setIsSpawning(false);
            }}
            onMouseLeave={() => setIsHovered(false)}
          >
            <div className="kaggle-card-inner">
              <div className="kaggle-card-header">
                <div className="kaggle-card-tag-wrapper">
                  <span className={`kaggle-card-tag ${currentItem.typeClass}`}>
                    <span className="kaggle-live-dot" aria-hidden="true" />
                    {currentItem.tag}
                  </span>
                </div>
                <span className="kaggle-card-meta">{currentItem.meta}</span>
              </div>
              <div className="kaggle-card-title">
                {currentItem.title}
              </div>
            </div>
            <div className="kaggle-card-progress-bar" aria-hidden="true">
              <div
                className="kaggle-card-progress-fill"
                key={`progress-${spawnCount}`}
                style={{ animationPlayState: isHovered ? "paused" : "running" }}
              />
            </div>
          </a>
        </div>
      </section>

      {/* 4. "TRUSTED BY TEAMS AT" INFINITE CONTINUOUS MARQUEE BAR */}
      <section className="kaggle-trusted-bar" aria-label="Trusted by companies and universities">
        <div className="kaggle-trusted-caption">
          Trusted by teams at
        </div>
        <div className="kaggle-marquee-wrapper">
          <div className="kaggle-marquee-track">
            {/* Set 1 */}
            <div className="kaggle-partner-set">
              <GoogleLogo />
              <OpenAILogo />
              <DeepMindLogo />
              <ArcPrizeLogo />
              <LlamaIndexLogo />
              <StanfordLogo />
              <CohereLogo />
              <JaneStreetLogo />
            </div>
            {/* Set 2 (Duplicate for seamless infinite loop) */}
            <div className="kaggle-partner-set" aria-hidden="true">
              <GoogleLogo />
              <OpenAILogo />
              <DeepMindLogo />
              <ArcPrizeLogo />
              <LlamaIndexLogo />
              <StanfordLogo />
              <CohereLogo />
              <JaneStreetLogo />
            </div>
            {/* Set 3 (Triplicate to ensure ultra-wide screen coverage) */}
            <div className="kaggle-partner-set" aria-hidden="true">
              <GoogleLogo />
              <OpenAILogo />
              <DeepMindLogo />
              <ArcPrizeLogo />
              <LlamaIndexLogo />
              <StanfordLogo />
              <CohereLogo />
              <JaneStreetLogo />
            </div>
          </div>
        </div>
      </section>

      {/* 5. THREE PILLARS / FEATURES */}
      <section className="kaggle-pillars-section" aria-labelledby="pillars-heading">
        <div className="kaggle-section-container">
          <div className="kaggle-pillars-grid">
            {/* Pillar 1 */}
            <article className="kaggle-pillar-card">
              <div className="kaggle-pillar-icon-box kaggle-pillar-icon--visualizer">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="5 3 19 12 5 21 5 3" />
                </svg>
              </div>
              <h2 className="kaggle-pillar-title">Interactive Visualizers</h2>
              <p className="kaggle-pillar-desc">
                Step frame-by-frame through memory buffers, pointer states, dynamic programming tables, and recursion graphs at 60 FPS.
              </p>
              <a className="kaggle-pillar-link" href="#visualizer">
                Launch Visualizer
                <span className="kaggle-arrow" aria-hidden="true">→</span>
              </a>
            </article>

            {/* Pillar 2 */}
            <article className="kaggle-pillar-card">
              <div className="kaggle-pillar-icon-box kaggle-pillar-icon--benchmark">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="20" x2="18" y2="10" />
                  <line x1="12" y1="20" x2="12" y2="4" />
                  <line x1="6" y1="20" x2="6" y2="14" />
                </svg>
              </div>
              <h2 className="kaggle-pillar-title">Big-O Telemetry Sandbox</h2>
              <p className="kaggle-pillar-desc">
                Benchmark execution times across large datasets, plot empirical scaling curves, and analyze hardware CPU instruction cycles.
              </p>
              <a className="kaggle-pillar-link" href="#visualize">
                Open Visualize Studio
                <span className="kaggle-arrow" aria-hidden="true">→</span>
              </a>
            </article>

            {/* Pillar 3 */}
            <article className="kaggle-pillar-card">
              <div className="kaggle-pillar-icon-box kaggle-pillar-icon--learn">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20" />
                  <path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z" />
                </svg>
              </div>
              <h2 className="kaggle-pillar-title">Structured Learning Paths</h2>
              <p className="kaggle-pillar-desc">
                12 comprehensive curriculums spanning Sorting, Trees, Graphs, Greedy, and Dynamic Programming with hands-on practice.
              </p>
              <a className="kaggle-pillar-link" href="#learn">
                Browse All Courses
                <span className="kaggle-arrow" aria-hidden="true">→</span>
              </a>
            </article>
          </div>
        </div>
      </section>

      {/* 6. CALL TO ACTION BANNER */}
      <section className="kaggle-cta-section">
        <div className="kaggle-cta-card">
          <h2 className="kaggle-cta-heading">
            Join the community shaping algorithmic mastery
          </h2>
          <div className="kaggle-cta-actions">
            <a className="kaggle-btn-dark" href="#register">
              Get Started for Free
            </a>
            <a className="kaggle-btn-outline" href="#problems">
              Explore Problem Set
            </a>
          </div>
        </div>
      </section>

      {/* 7. FOOTER */}
      <footer className="kaggle-footer">
        <div className="kaggle-footer-inner">
          <div className="kaggle-footer-brand">
            <span className="kaggle-footer-logo">algoviz</span>
            <span className="kaggle-footer-copy">© 2026 AlgoViz, Inc. All rights reserved.</span>
          </div>
          <div className="kaggle-footer-links">
            <a href="#problems">Problems</a>
            <a href="#visualize">Visualize</a>
            <a href="#learn">Learn</a>
            <a href="#profile">Profile</a>
          </div>
        </div>
      </footer>

      {/* 8. KAGGLE COOKIE & TELEMETRY BANNER */}
      {!cookieDismissed && (
        <aside className="kaggle-cookie-banner" role="region" aria-label="Cookie consent">
          <div className="kaggle-cookie-content">
            <p className="kaggle-cookie-text">
              AlgoViz uses cookies from Google to deliver and enhance the quality of its services and to analyze traffic.
            </p>
            <div className="kaggle-cookie-actions">
              <a className="kaggle-cookie-link" href="#problems">
                Learn more
              </a>
              <button
                className="kaggle-cookie-dismiss"
                onClick={handleDismissCookie}
              >
                OK, Got it.
              </button>
            </div>
          </div>
        </aside>
      )}
    </div>
  );
}
