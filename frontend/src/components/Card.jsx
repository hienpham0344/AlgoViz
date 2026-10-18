import "./components.css";

function Card({ title = "Thuật toán ABC", className = "" }) {
  return (
    <article className={`av-card ${className}`.trim()}>
      <div className="av-card-image" aria-hidden="true" />
      <h3>{title}</h3>
    </article>
  );
}

export default Card;
