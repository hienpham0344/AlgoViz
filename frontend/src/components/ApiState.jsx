import { useId } from "react";
import ProgressView from "./ProgressView.jsx";

export default function ApiState({ loading, error, onRetry, label = "Loading…", page = false }) {
  const statusId = useId();
  if (!loading && !error) return null;
  return (
    <div className={`api-state${page ? " api-state--page" : ""}`}
      role={loading ? undefined : "alert"} aria-busy={Boolean(loading)}
      aria-describedby={loading ? statusId : undefined}>
      {loading ? <ProgressView id={statusId} variant="spinner" size={page ? "lg" : "md"} label={label} /> : <>
        <p>{error}</p>
        {onRetry && <button type="button" onClick={onRetry}>Try again</button>}
      </>}
    </div>
  );
}
