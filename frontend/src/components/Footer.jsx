import "./components.css";

function Footer({ className = "" }) {
  return <footer className={`av-footer ${className}`.trim()} />;
}

export default Footer;
