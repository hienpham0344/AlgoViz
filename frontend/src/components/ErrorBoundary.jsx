import { Component } from "react";

export default class ErrorBoundary extends Component {
  state = { failed: false };
  static getDerivedStateFromError() { return { failed: true }; }
  render() {
    if (this.state.failed) return (
      <main className="api-state" role="alert">
        <h1>Không thể tải trang</h1>
        <p>Vui lòng tải lại để thử lại.</p>
        <button type="button" onClick={() => window.location.reload()}>Tải lại trang</button>
      </main>
    );
    return this.props.children;
  }
}
