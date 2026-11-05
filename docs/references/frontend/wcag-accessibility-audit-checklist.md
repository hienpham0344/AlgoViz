# WCAG 2.2 AA Accessibility & ARIA Audit Checklist

## 1. Perceivable
- [ ] **Color Contrast**: Normal text has at least 4.5:1 contrast ratio against background; large text (18pt+) has at least 3:1.
- [ ] **Non-Text Content**: All `<img>` tags have descriptive `alt` text or `alt=""` for purely decorative images.
- [ ] **Form Labels**: Every `<input>`, `<select>`, `<textarea>` has an associated `<label htmlFor="...">` or `aria-label`.
- [ ] **Information & Relationships**: Semantic headings (`<h1>` through `<h6>`) are strictly sequential without skipped levels.

---

## 2. Operable
- [ ] **Keyboard Navigation**: All interactive elements (buttons, links, dialogs, dropdowns) are reachable and operable via `Tab`, `Enter`, `Space`, and `Arrow` keys.
- [ ] **No Keyboard Trap**: Focus is never trapped in a subcomponent unless inside an active modal dialog (which must release focus on `Escape`).
- [ ] **Visible Focus Indicator**: Focus rings are distinct and never suppressed (`outline: none` without replacement is banned).
- [ ] **Target Size**: Touch/click targets are at least 24x24 CSS pixels (WCAG 2.2 AA Level).

---

## 3. Understandable
- [ ] **Language of Page**: Root `<html>` element specifies valid `lang` attribute (e.g., `<html lang="en">`).
- [ ] **Error Identification & Suggestion**: Form validation errors describe the specific problem and how to correct it.
- [ ] **Predictable Navigation**: Navigation menus appear in the same order across all pages.

---

## 4. Robust (ARIA 1.2 Patterns)
- [ ] **Live Regions**: Dynamic updates (cart items, live notifications) announced via `aria-live="polite"` or `role="status"`.
- [ ] **Expanded State**: Accordions, dropdowns, and disclosures use `aria-expanded="true|false"` and `aria-controls="..."`.
- [ ] **Modal Roles**: Dialogs use `role="dialog"`, `aria-modal="true"`, and `aria-labelledby="..."`.
