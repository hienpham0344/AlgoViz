import "./components.css";

const links = ["Dashboard", "Problems", "Analyze", "Learn", "History"];

function NavBarItem({ className = "" }) {
  return (
    <aside className={`av-sidebar ${className}`.trim()}>
      <nav aria-label="Dashboard navigation">
        {links.map((link) => (
          <a href={`#${link.toLowerCase()}`} key={link}>
            {link}
          </a>
        ))}
      </nav>
      <nav aria-label="Account navigation">
        <a href="#profile">Profile</a>
        <a href="#settings">Settings</a>
      </nav>
    </aside>
  );
}

export default NavBarItem;
