import { useState } from "react";
import "./auth.css";

function AuthPage({ mode = "login", onAuthenticated }) {
  const isRegistering = mode === "register";
  const [form, setForm] = useState({ name: "", email: "", password: "" });
  const [error, setError] = useState("");

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setError("");
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!form.email.trim() || !form.password.trim() || (isRegistering && !form.name.trim())) {
      setError("Please complete all required fields.");
      return;
    }
    const name = form.name.trim() || form.email.split("@")[0];
    onAuthenticated(name);
  };

  return (
    <main className="auth-page">
      <section className="auth-panel" aria-labelledby="auth-title">
        <a className="auth-brand" href="#top">AlgoViz</a>
        <div className="auth-intro">
          <span className="auth-eyebrow">Algorithmic learning studio</span>
          <h1 id="auth-title">{isRegistering ? "Start your journey." : "Welcome back."}</h1>
          <p>{isRegistering ? "Create an account and make every algorithm visible." : "Continue turning complex algorithms into clear visual steps."}</p>
        </div>
        <form className="auth-form" onSubmit={handleSubmit}>
          {isRegistering && (
            <label>
              Name
              <input name="name" value={form.name} onChange={updateField} autoComplete="name" required />
            </label>
          )}
          <label>
            Email
            <input name="email" type="email" value={form.email} onChange={updateField} autoComplete="email" required />
          </label>
          <label>
            Password
            <input name="password" type="password" value={form.password} onChange={updateField} autoComplete={isRegistering ? "new-password" : "current-password"} required />
          </label>
          {error && <p className="auth-error" role="alert">{error}</p>}
          <button type="submit">{isRegistering ? "Create account" : "Log in"}</button>
        </form>
        <p className="auth-switch">
          {isRegistering ? "Already have an account?" : "New to AlgoViz?"}{" "}
          <a href={isRegistering ? "#login" : "#register"}>{isRegistering ? "Log in" : "Create an account"}</a>
        </p>
        <a className="auth-back" href="#top">Back to landing page</a>
      </section>
      <aside className="auth-aside" aria-label="AlgoViz introduction">
        <span className="auth-orbit auth-orbit--one" aria-hidden="true" />
        <span className="auth-orbit auth-orbit--two" aria-hidden="true" />
        <div>
          <span className="auth-aside-label">Visualize the invisible</span>
          <strong>Learn algorithms<br />by seeing them move.</strong>
        </div>
      </aside>
    </main>
  );
}

export default AuthPage;