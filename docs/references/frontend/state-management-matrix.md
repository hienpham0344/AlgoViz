# Frontend State Management Decision Matrix

## Decision Framework

| Criterion | React Context | Zustand | Jotai | Signals (Preact/Solid) | TanStack Query |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Primary Domain** | Static Theme / Auth | Global Client State | Atomic / Micro State | Fine-grained reactivity | Server State & Cache |
| **Re-render Optimization** | Coarse (Entire subtree) | Fine (Selector-based) | Fine (Atom dependency) | Surgical (DOM-level) | Cache key subscribers |
| **Boilerplate** | Low | Very Low | Minimal | Minimal | Minimal |
| **Async Support** | Manual | Middleware / Actions | Async Atoms | Resource primitives | Built-in (Suspense/Retry) |
| **Bundle Size** | 0 KB (Built-in) | ~1.1 KB | ~2.5 KB | ~1.5 KB | ~12 KB |

---

## When to Choose What

1. **Server State (API / DB)**: **ALWAYS** use **TanStack Query** (or SWR / RTK Query). Never replicate server state in client stores.
2. **Global Client State (UI Panels, Player State, Multi-step Drafts)**: Choose **Zustand**. Simple slices, middleware support, zero context wrapper overhead.
3. **Atomic Interdependent UI (Spreadsheets, Canvases, Graph Nodes)**: Choose **Jotai**. Bottom-up composition prevents parent re-renders.
4. **App-wide Theme / Locale**: Choose native **React Context** (changes rarely).
