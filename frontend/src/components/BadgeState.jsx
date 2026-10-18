import "./components.css";

const stateAssets = {
  Completed:
    "https://www.figma.com/api/mcp/asset/2ab381a4-23b1-40b7-90ce-382ff942b545.png",
  "In Progress":
    "https://www.figma.com/api/mcp/asset/1fbbbef5-614f-42a1-b247-e31ca4c2684d.png",
  "Not Started":
    "https://www.figma.com/api/mcp/asset/c1b28f97-526d-4146-a7f8-758c8329f1c8.png",
};

const stateLabels = {
  Completed: "Hoàn Thành",
  "In Progress": "Đang học",
  "Not Started": "Chưa học",
};

function BadgeState({ status = "Completed", className = "" }) {
  return (
    <span
      className={`av-badge-state av-state-${status.toLowerCase().replaceAll(" ", "-")} ${className}`.trim()}
    >
      <img src={stateAssets[status]} alt="" />
      <strong>{stateLabels[status]}</strong>
    </span>
  );
}

export default BadgeState;
