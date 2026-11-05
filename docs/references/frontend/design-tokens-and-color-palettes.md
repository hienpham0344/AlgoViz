# Design Tokens, Oklch Color Scales & Typography

## 1. Perceptually Uniform Oklch Color Palette
Oklch guarantees consistent lightness across hues, eliminating muddy tints in dark/light mode:

```css
:root {
  /* Primary Brand Scales: oklch(Lightness Chroma Hue) */
  --color-primary-50:  oklch(0.97 0.02 250);
  --color-primary-100: oklch(0.93 0.05 250);
  --color-primary-200: oklch(0.85 0.09 250);
  --color-primary-300: oklch(0.76 0.14 250);
  --color-primary-400: oklch(0.67 0.19 250);
  --color-primary-500: oklch(0.58 0.22 250); /* Brand Base */
  --color-primary-600: oklch(0.49 0.20 250);
  --color-primary-700: oklch(0.40 0.17 250);
  --color-primary-800: oklch(0.31 0.13 250);
  --color-primary-900: oklch(0.22 0.08 250);
  --color-primary-950: oklch(0.14 0.04 250);

  /* Semantic Neutral Grays */
  --color-neutral-0:   oklch(1.0 0 0);
  --color-neutral-50:  oklch(0.98 0.005 260);
  --color-neutral-100: oklch(0.95 0.008 260);
  --color-neutral-200: oklch(0.89 0.012 260);
  --color-neutral-700: oklch(0.35 0.015 260);
  --color-neutral-900: oklch(0.18 0.018 260);
  --color-neutral-950: oklch(0.12 0.015 260);
}
```

---

## 2. 8pt Grid Spacing Scale
```css
:root {
  --space-1: 0.25rem; /* 4px */
  --space-2: 0.5rem;  /* 8px  (Base step) */
  --space-3: 0.75rem; /* 12px */
  --space-4: 1.0rem;  /* 16px */
  --space-6: 1.5rem;  /* 24px */
  --space-8: 2.0rem;  /* 32px */
  --space-12: 3.0rem; /* 48px */
  --space-16: 4.0rem; /* 64px */
  --space-24: 6.0rem; /* 96px */
}
```

---

## 3. Elevation & Layering Shadows
```css
:root {
  --shadow-sm: 0 1px 2px 0 rgb(0 0 0 / 0.05);
  --shadow-md: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
  --shadow-lg: 0 10px 15px -3px rgb(0 0 0 / 0.1), 0 4px 6px -4px rgb(0 0 0 / 0.1);
  --shadow-xl: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
}
```
