import "./components.css";

function LandingBar({ className = "" }) {
  return (
    <header className={`av-landing-bar ${className}`.trim()}>
      <a className="av-brand" href="#top">
        AlgoViz
      </a>
      <div className="av-auth-links">
        <a href="#login">Login</a>
        <a className="av-register" href="#register">
          Register
        </a>
      </div>
    </header>
  );
}

export default LandingBar;
