import "./components.css";

const difficultyAssets = {
  Easy: "https://www.figma.com/api/mcp/asset/bb066b73-1519-4266-80bc-d4896b59d007.png",
  Medium:
    "https://www.figma.com/api/mcp/asset/e21fc4ed-60a2-4a6e-b477-206e108d85e9.png",
  Hard: "https://www.figma.com/api/mcp/asset/4d7dcf31-5a8f-490d-88ef-8fee6043ef89.png",
};

function BadgeDifficulty({ state = "Easy", className = "" }) {
  return (
    <span
      className={`av-badge-difficulty av-difficulty-${state.toLowerCase()} ${className}`.trim()}
    >
      <img src={difficultyAssets[state]} alt="" />
      <strong>{state}</strong>
    </span>
  );
}

export default BadgeDifficulty;
