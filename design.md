# DESIGN.md — Visual Design System

**Project:** Docket

**Purpose:** Single source of truth for all visual decisions so the UI stays consistent across every page and every coding session. Do not introduce new colors, fonts, or spacing values outside this file — extend this file first if a genuine gap is found.

**Design direction (v1, Phases 0–8):** Calm, trustworthy, "professional B2B fintech/legal-tech" — think a hybrid of Linear's cleanliness and a banking dashboard's seriousness. Avoid playful/consumer styling (no bright gradients, no rounded mascot-style illustration).

**Superseded by v2 as of Phase 8.5 — see §8.** §§1–7 below stay in the file as the historical record of the v1 tokens and remain the source of truth for anyone reading this doc before Phase 8.5 is reached; the moment Phase 8.5 starts, §8 tokens/rules take priority everywhere they overlap with §§1–7 (color, radii, shadows, "avoid bright gradients"). Typography (§2), spacing base unit (§3), tone of voice (§6), and the workspace-scoping/security rules elsewhere in the repo are unaffected and still apply.

---

## 1. Color Palette

### Brand / Primary
| Token | Hex | Usage |
|---|---|---|
| `--color-primary` | `#1E3A5F` (deep navy-blue) | Primary buttons, nav, links, headers |
| `--color-primary-hover` | `#16293F` | Hover state for primary elements |
| `--color-primary-light` | `#E8EEF4` | Selected/active backgrounds, subtle highlights |

### Accent
| Token | Hex | Usage |
|---|---|---|
| `--color-accent` | `#0F9D8C` (teal) | Success states, "Matches standard" flags, positive actions |
| `--color-accent-light` | `#E4F5F2` | Success background chips |

### Semantic / Status
| Token | Hex | Usage |
|---|---|---|
| `--color-warning` | `#B7791F` (amber) | Anomaly/deviation flags |
| `--color-warning-light` | `#FBF0DD` | Warning background chips |
| `--color-danger` | `#C0392B` | Failed processing, destructive actions |
| `--color-danger-light` | `#FBE9E7` | Error background chips |
| `--color-info` | `#2E6BAA` | Informational badges |

### Neutrals (grayscale)
| Token | Hex | Usage |
|---|---|---|
| `--color-bg` | `#F7F8FA` | App background |
| `--color-surface` | `#FFFFFF` | Cards, panels, modals |
| `--color-border` | `#E2E5EA` | Dividers, input borders |
| `--color-text-primary` | `#1A1D23` | Headings, primary body text |
| `--color-text-secondary` | `#5B6270` | Secondary/muted text, captions |
| `--color-text-disabled` | `#A0A5AD` | Disabled states, placeholders |

**Rule:** Never use raw hex values directly in component code — always reference the CSS variable/Tailwind token so a future palette tweak is a one-file change.

## 2. Typography

### Font Families
- **Headings:** `Manrope` (geometric, modern, slightly distinctive — avoids the "default SaaS" feel of Inter alone)
- **Body/UI text:** `Inter` (highly legible at small sizes, standard for dense data tables)
- **Monospace (for JSON/export previews):** `JetBrains Mono`

Load via Google Fonts or self-hosted; fallback stack:
```css
--font-heading: 'Manrope', 'Segoe UI', sans-serif;
--font-body: 'Inter', 'Segoe UI', sans-serif;
--font-mono: 'JetBrains Mono', 'Courier New', monospace;
```

### Type Scale
| Token | Size / Line-height | Weight | Usage |
|---|---|---|---|
| `--text-h1` | 32px / 40px | 700 | Page titles ("Dashboard", "Document Detail") |
| `--text-h2` | 24px / 32px | 700 | Section headers ("Extracted Fields", "Summary") |
| `--text-h3` | 18px / 26px | 600 | Card titles |
| `--text-body` | 15px / 22px | 400 | Default body text |
| `--text-small` | 13px / 18px | 400 | Captions, timestamps, helper text |
| `--text-label` | 12px / 16px | 600, uppercase, letter-spacing 0.04em | Field labels, table headers |

## 3. Spacing & Layout

- Base spacing unit: **4px**. All margins/paddings are multiples of 4 (4, 8, 12, 16, 24, 32, 48, 64).
- Page content max-width: **1200px**, centered, with 24px side padding on smaller viewports.
- Cards/panels: 8px corner radius, 1px `--color-border`, subtle shadow (`0 1px 2px rgba(0,0,0,0.04)`) — no heavy drop shadows.
- Standard grid: 12-column, 24px gutters, using Tailwind's default grid utilities.

## 4. Components — Key Conventions

- **Buttons:** Primary = filled `--color-primary`, white text, 8px radius. Secondary = outline, `--color-border`, `--color-text-primary` text. Destructive = filled `--color-danger`.
- **Status badges:** Small pill, 4px radius less than cards (rounder), background = `-light` variant of the relevant semantic color, text = the solid variant. Used for document status (Processed/Pending/Failed) and anomaly flags (Matches/Deviation).
- **Tables (extracted fields):** Zebra-free, single `--color-border` row dividers, label column uses `--text-label` style, value column uses `--text-body`.
- **Forms:** Labels above inputs (never placeholder-only labels), 1px border inputs with `--color-border`, focus state = 2px `--color-primary` outline.
- **Empty states:** Centered icon + one-line message + primary CTA button (e.g., "No documents yet → Upload your first document").

## 5. Iconography

- Use a single consistent icon set throughout: **Lucide icons** (clean line-style, matches the calm/professional direction).
- Icon sizing: 16px inline with text, 20px in buttons, 24px standalone/empty-states.

## 6. Tone of Voice (UI Copy)

- Direct and professional, never cutesy. "No documents uploaded yet" not "Whoops, nothing here!"
- Anomaly explanations should read like a colleague's note: "Termination notice is 15 days; your standard template specifies 30 days" — specific and factual, not alarmist.

## 7. Do / Avoid

**Do:**
- Keep every page's background `--color-bg`, every card `--color-surface` — consistent visual rhythm.
- Reuse the same badge/chip component for every status type across the app.

**Avoid:**
- Introducing a second accent color "just for this one page."
- Using pure black (`#000`) or pure white shadows — use the defined neutral tokens.
- Rounded/playful illustration styles — this is a B2B trust-driven product, not a consumer app.

---

## 8. v2 — "Aurora Obsidian" (Phase 8.5 Visual Overhaul)

**Why a new direction:** Docket's pitch is "an AI second pair of eyes" — something actively scanning, reading, and illuminating your documents. The v1 palette (navy/teal, flat cards) is safe but generic — it's the same navy-on-white every B2B SaaS dashboard reaches for. v2 keeps the trust and legibility v1 earned (still a document-intelligence tool people upload financial/legal data to — never sacrifice contrast or clarity for spectacle) but makes the "AI is actively illuminating this document" idea visible: a dark, glass-like surface with a signature aurora-gradient light source, instead of a flat light-mode dashboard. This is deliberately *not* the two most common AI-generated defaults (warm cream + terracotta; or near-black + single acid-green accent) — it uses a cooler obsidian-plum base with a two-hue violet→cyan aurora, which ties to the product's "scanning light" concept rather than being a generic dark-mode reskin.

### 8.1 Color Palette (v2 — supersedes §1 for Phase 8.5 onward)

#### Base surfaces
| Token | Hex | Usage |
|---|---|---|
| `--color-bg` | `#12101B` (obsidian plum, near-black with a violet undertone) | App background |
| `--color-surface` | `#1B1830` | Base card/panel fill (before glass treatment) |
| `--color-surface-raised` | `#242040` | Modals, dropdowns, elevated panels |
| `--color-border` | `rgba(196, 181, 253, 0.14)` | Hairline borders on glass panels |
| `--color-text-primary` | `#F3F1FA` | Headings, primary body text |
| `--color-text-secondary` | `#A8A2C4` | Secondary/muted text, captions |
| `--color-text-disabled` | `#645E82` | Disabled states, placeholders |

#### Signature accent — the "Aurora" gradient
| Token | Hex | Usage |
|---|---|---|
| `--color-aurora-start` | `#7C5CFC` (electric violet) | Gradient start — primary buttons, active nav, focus rings |
| `--color-aurora-end` | `#22D3EE` (bright cyan) | Gradient end — paired with `-start` in `linear-gradient(135deg, var(--color-aurora-start), var(--color-aurora-end))` |
| `--color-aurora-glow` | `rgba(124, 92, 252, 0.35)` | Box-shadow glow behind primary CTAs and the hero 3D element only |

#### Semantic / Status (kept legible on the dark base — do not reuse v1's light-mode values)
| Token | Hex | Usage |
|---|---|---|
| `--color-accent` | `#34D399` (emerald) | Success, "Matches standard" flags |
| `--color-accent-light` | `rgba(52, 211, 153, 0.12)` | Success background chips |
| `--color-warning` | `#F5A524` (amber) | Anomaly/deviation flags |
| `--color-warning-light` | `rgba(245, 165, 36, 0.12)` | Warning background chips |
| `--color-danger` | `#F65A5A` | Failed processing, destructive actions |
| `--color-danger-light` | `rgba(246, 90, 90, 0.12)` | Error background chips |
| `--color-info` | `#22D3EE` | Informational badges (reuses aurora-end) |

**Contrast rule (non-negotiable, carried over from v1 + ui-ux-pro-max priority-1 check):** every text/background pairing above must hit ≥4.5:1. `--color-text-secondary` on `--color-bg` and all status colors on their `-light` chip backgrounds have been chosen to clear this — do not substitute lighter/dimmer variants for "mood" without re-checking contrast.

### 8.2 Materials — Glassmorphism scoping

- Cards/panels: `background: var(--color-surface)` at ~70% opacity + `backdrop-filter: blur(16px)` + 1px gradient border (subtle aurora-tinted, not full-strength) + soft outer glow only on hover.
- **Glass is for containers, never for content.** Data tables, form inputs, and any text-bearing row stay on a flat, fully-opaque `--color-surface-raised` — glass blur behind dense text or financial figures hurts legibility and directly conflicts with rules.md's error/data-integrity intent. This is a hard boundary, not a style preference.
- Corner radius: 12px for cards (up from v1's 8px, reads more "3D object" than "flat panel"), 999px (pill) for badges/buttons unchanged from v1.

### 8.3 The 3D Signature Element

- One ambient element, reused consistently: a low-poly or particle-based aurora gradient mesh (React Three Fiber `<Canvas>`), placed behind the dashboard hero and the login/signup panels only. It should feel like ambient light/depth, not a mascot or toy — no spinning logos, no literal document icons in 3D.
- Never place a live 3D canvas behind or inside a data table, form, or anything the user reads/edits closely — motion competing with financial/legal data is both a UX and trust problem.
- Must degrade gracefully: static CSS aurora-gradient fallback (no WebGL, no JS) for `prefers-reduced-motion`, low-end devices, and as the initial paint before the canvas mounts (avoid layout shift — reserve the space up front).

### 8.4 Motion Rules

- Durations: 150–300ms for UI micro-interactions (hover, focus, badge state change); up to 600ms for page-level transitions/staggered reveals — never longer, never used for anything but conveying state change (per ui-ux-pro-max priority-7 checks).
- Animate `opacity`/`transform` only — never `width`/`height`/`top`/`left` (layout-thrashing risk).
- Card hover = subtle lift (`translateY(-2px)` + soft aurora-tinted shadow), not a scale-jump.
- Dashboard list items stagger in on load (~40ms delay between items), not all at once and not individually re-triggered on every re-render.
- `processing` status badges get a slow (~2s) opacity pulse — not a spinner racing — to signal "in progress" without feeling frantic.
- Respect `prefers-reduced-motion: reduce` globally: disable staggers/pulses/3D motion, keep instant or near-instant state changes with no user-facing loss of information.

### 8.5 Typography, Iconography, Tone

- Typography (§2), Lucide iconography (§5), and tone of voice (§6) are unchanged in v2 — do not introduce a new type system or icon set alongside the new palette; consistency there is what keeps this from feeling like a random reskin.
- Icon color: default to `--color-text-secondary`, switch to the aurora gradient (via `currentColor` + gradient text/fill trick) only for the small set of "active/selected" icon states — not applied everywhere or it stops being a signature.

### 8.6 Do / Avoid (v2 additions)

**Do:**
- Keep the aurora gradient to *one* signature use per screen (hero glow, primary CTA, or active-state icon) — repeating it everywhere dilutes it into "just another gradient."
- Ship the reduced-motion/no-WebGL fallback in the same PR as the 3D element, not as a follow-up.

**Avoid:**
- Using the aurora gradient as a text-highlight/background behind dense paragraphs — reserve it for large, sparse surfaces (buttons, hero, active icons).
- Adding a second 3D element "since we already have Three.js in the bundle" — one signature element, per the frontend-design principle of spending boldness in one place.
- Rebuilding data tables, forms, or the extracted-fields view in glass/dark-only styling in a way that drops contrast below 4.5:1 — re-run the accessibility check per component, don't assume the palette swap is automatically compliant.
