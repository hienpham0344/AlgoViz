# AI Agent Rules: Coding & UI Guidelines

Follow these mandatory rules when generating UI and writing code for this project.

---

## 1. Color System & Design Tokens (`src/index.css`)

- **No Hardcoded Colors:** DO NOT hardcode HEX, RGB, or HSL color values (e.g., `#FFFFFF`, `rgba(0,0,0,0.5)`) anywhere in UI components.
- **Read `src/index.css` First:** Always reference CSS variables defined in `src/index.css` (e.g., `var(--color-white-20)`, `var(--primary)`).
- **Usage:** Use defined CSS variables directly or via Tailwind classes mapped to them (e.g., `bg-[var(--color-name)]`). If a Figma design uses a new color, declare the new variable in `src/index.css` first.

---

## 2. Component Reuse (`src/components/`)

- **Check Existing Components First:** Before coding any Figma frame or UI section, inspect `src/components/` to see what is already built.
- **Maximize Reusability:** Always reuse existing UI components (Button, Input, Modal, Card, Header, etc.). If an existing component misses a feature or variation from Figma, extend it with new `props` or `variants` instead of creating duplicate files.
- **Create New Components Only When Necessary:** Only create a new component if no matching element exists in `src/components/` and extending existing ones is not feasible.

---

## 3. Standard Workflow

1. **Read `src/index.css`** to get all valid design tokens/colors.
2. **Scan `src/components/`** to index all reusable UI elements.
3. **Compare Figma against existing code:**
   - **Exact/Near match:** Import existing component and pass relevant props.
   - **Needs variation:** Refactor/extend existing component (add props/variants).
   - **Missing entirely:** Build a new component under `src/components/` using tokens from `src/index.css`.
