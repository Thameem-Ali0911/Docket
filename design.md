# DESIGN.md — Visual Design System

**Project:** Docket

**Purpose:** Single source of truth for all visual decisions so the UI stays consistent across every page and every coding session. Do not introduce new colors, fonts, or spacing values outside this file — extend this file first if a genuine gap is found.

**Design direction:** Calm, trustworthy, "professional B2B fintech/legal-tech" — think a hybrid of Linear's cleanliness and a banking dashboard's seriousness. Avoid playful/consumer styling (no bright gradients, no rounded mascot-style illustration).

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
