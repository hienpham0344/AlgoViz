# Modern CSS Cheat Sheet & Implementation Guide

## 1. CSS Subgrid (Grid Level 2)
Align child items across nested grid components without flattening the DOM tree:

```css
.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 1.5rem;
}

.card {
  display: grid;
  grid-template-rows: subgrid;
  grid-row: span 3; /* Spans header, body, footer */
  gap: 0.75rem;
}
```

---

## 2. Container Queries (`@container`)
Style components based on the size of their parent container rather than the global browser viewport:

```css
.card-container {
  container-type: inline-size;
  container-name: product-card;
}

/* Switches from vertical stack to horizontal row when container is > 480px */
@container product-card (min-width: 480px) {
  .product-content {
    display: flex;
    flex-direction: row;
    gap: 1.5rem;
  }
  .product-image {
    width: 40%;
  }
}
```

---

## 3. Fluid Typography with `clamp()`
Smooth mathematical font scaling without media query breakpoints:

```css
:root {
  /* clamp(MIN, VAL, MAX) */
  --font-sm: clamp(0.8rem, 0.17vw + 0.76rem, 0.89rem);
  --font-base: clamp(1rem, 0.34vw + 0.91rem, 1.19rem);
  --font-lg: clamp(1.25rem, 0.61vw + 1.1rem, 1.58rem);
  --font-xl: clamp(1.56rem, 1vw + 1.31rem, 2.11rem);
  --font-2xl: clamp(1.95rem, 1.56vw + 1.56rem, 2.81rem);
}
```

---

## 4. Modern Viewport Units (`dvh`, `svh`, `lvh`)
Prevent mobile address bar jump layout shifts:

| Unit | Meaning | Use Case |
| :--- | :--- | :--- |
| `svh` (Small Viewport) | Height with mobile browser URL bar expanded | Modals, bottom navigation sheets |
| `lvh` (Large Viewport) | Height with mobile browser URL bar collapsed | Hero background images |
| `dvh` (Dynamic Viewport)| Adapts dynamically as URL bar slides in/out | Full-screen app viewports |

```css
.full-screen-app {
  height: 100dvh;
  min-height: -webkit-fill-available;
}
```

---

## 5. CSS Cascade Layers (`@layer`)
Control specificity deterministically regardless of selector weight:

```css
@layer reset, base, components, utilities;

@layer reset {
  *, *::before, *::after { box-sizing: border-box; margin: 0; }
}

@layer components {
  .btn { padding: 0.5rem 1rem; border-radius: 0.375rem; }
}

@layer utilities {
  .p-0 { padding: 0 !important; } /* Utilities always win */
}
```

---

## 6. View Transitions API
Smooth cross-fade and shared element transitions:

```css
::view-transition-old(root),
::view-transition-new(root) {
  animation-duration: 250ms;
  animation-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
}

.hero-thumbnail {
  view-transition-name: selected-item;
}
```
