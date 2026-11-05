import { useEffect, useState } from "react";
import { fetchAlgorithmCatalog } from "../../services/astApi.js";

export function hintLabel(type) {
  return String(type || "").split("-").filter(Boolean).map(word => word[0].toUpperCase() + word.slice(1)).join(" ");
}

/** Lists algorithms the AST recognizer found in the traced source. Static analysis, not a correctness verdict. */
export default function AlgorithmHints({ hints }) {
  const [families, setFamilies] = useState({});
  useEffect(() => {
    let active = true;
    fetchAlgorithmCatalog()
      .then(catalog => { if (active) setFamilies(Object.fromEntries(catalog.algorithms.map(a => [a.type, a.family]))); })
      .catch(() => {});
    return () => { active = false; };
  }, []);
  if (!Array.isArray(hints) || !hints.length) return null;
  return (
    <section className="algorithm-hints" aria-label="Recognized algorithms">
      <span className="algorithm-hints__title">Detected by static analysis</span>
      <ul className="algorithm-hints__list">
        {hints.map((hint, index) => (
          <li key={`${hint.type}-${hint.startLine}-${index}`} className="algorithm-hint"
            title={(hint.evidence || []).join("\n")}>
            <strong>{hintLabel(hint.type)}</strong>
            {families[hint.type] && <span className="algorithm-hint__family">{families[hint.type]}</span>}
            <span className="algorithm-hint__meta">
              lines {hint.startLine}–{hint.endLine} · {Math.round((hint.confidence ?? 0) * 100)}%
            </span>
          </li>
        ))}
      </ul>
    </section>
  );
}
