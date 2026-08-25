# Docket

**AI-powered document intelligence for contracts, invoices, and resumes.**

Docket reads your business documents in seconds — pulling out the key facts, summarizing the rest in plain English, and flagging the moment something deviates from your standard template. Skip the manual read-through; Docket's already done it.

> Final year engineering project — B2B SaaS-style document intelligence platform, inspired by real problem statements from banking/legal sectors (SIH-style) and existing tools like Docsumo and Rossum, scoped tightly to be genuinely buildable in one academic year.

---

## Table of Contents

- [What It Does](#what-it-does)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [Environment Variables](#environment-variables)
- [Core Features](#core-features)
- [API Overview](#api-overview)
- [Project Documentation](#project-documentation)
- [Build Phases](#build-phases)
- [Testing](#testing)
- [Known Limitations](#known-limitations)
- [License](#license)

---

## What It Does

A business uploads a document — a contract, invoice, or resume — and Docket returns:

1. **Structured data extraction** — key fields pulled out as a clean table / JSON (vendor name, invoice total, termination clause, candidate skills, etc.)
2. **Plain-English summary** — a 3–5 sentence summary of the document
3. **Anomaly / deviation flagging** — compares the document against a saved "standard template" for that document type and flags what's different (e.g., *"Termination notice is 15 days; your standard template specifies 30 days"*)

The MVP is intentionally scoped to **three document types**: Contracts, Invoices, and Resumes — not a generic "any document" tool — to keep extraction quality high and the project achievable.

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, JavaScript (JSX), Tailwind CSS, React Router, TanStack Query, Recharts |
| Backend | Java 17+, Spring Boot 3.x (Spring Web, Spring Data JPA, Spring Security, Flyway, Bean Validation, Jackson) |
| Database | PostgreSQL |
| OCR / PDF | Tess4J (Tesseract wrapper), Apache PDFBox |
| AI / LLM | Google Gemini API — called via a thin Spring `RestClient`/`WebClient` wrapper |
| Auth | Spring Security, JWT (jjwt), BCrypt |
| Build Tool | Maven |
| Deployment (demo) | Vercel/Netlify (frontend), Render/Railway (backend), Supabase/Neon (DB) |

Full rationale for each choice lives in [`architecture.md`](./architecture.md).

## Project Structure

```
docket/
├── prd.md              # What to build, target users, features
├── architecture.md      # Full architecture, stack, folder structure, prerequisites
├── rules.md              # Coding rules, approved libraries, AI/LLM boundaries
├── phases.md             # Step-by-step build plan (12 phases)
├── design.md             # Colors, fonts, typography, component conventions
├── memory.md              # Running project log / session history
├── README.md              # You are here
├── docker-compose.yml     # Runs db + backend + frontend together (`docker compose up --build`)
├── .env.example           # Compose-level env vars (Postgres creds, JWT secret, Gemini key)
│
├── frontend/
│   ├── Dockerfile         # Multi-stage: npm run build → served by nginx
│   ├── nginx.conf
│   └── src/
│       ├── pages/         # Login, Dashboard, DocumentDetail, UploadDocument, ...
│       ├── components/    # layout, documents, extraction, shared ui
│       ├── hooks/, lib/, types/, styles/
│
└── backend/
    ├── Dockerfile         # Multi-stage: Maven build → Temurin JRE + Tesseract runtime
    └── src/main/java/com/docket/
        ├── controller/    # REST controllers (Auth, Document, Template, Workspace)
        ├── entity/        # JPA entities
        ├── repository/    # Spring Data JPA repositories
        ├── dto/           # Request/response + LLM-output DTOs
        ├── service/       # OCR, extraction, summarize, anomaly, storage services
        ├── prompt/        # Gemini prompt builders, one per responsibility
        ├── security/      # JWT service + auth filter
        └── exception/     # Global exception handling
```

See [`architecture.md`](./architecture.md) for the complete, annotated folder tree and data model.

## Prerequisites

**Fastest path:** [Docker Desktop](https://www.docker.com/products/docker-desktop/) is the only thing you need — see [Option A](#getting-started) below. Everything past this section assumes you're running natively instead.

Install these before running the project locally without Docker:

- **Java 17+ (JDK)** — Temurin/Adoptium or Oracle JDK
- **Maven 3.9+**
- **Node.js 18+** and npm
- **PostgreSQL 14+** (local install, or a free-tier hosted DB — Supabase/Neon/Railway)
- **Tesseract OCR engine** + trained language data (`tessdata`), with `TESSDATA_PREFIX` set
  - Windows: UB-Mannheim Tesseract build
  - Mac: `brew install tesseract`
  - Linux: `sudo apt install tesseract-ocr libtesseract-dev`
- **Google Gemini API key** — from [aistudio.google.com/apikey](https://aistudio.google.com/apikey)
- **Git**

Optional: Docker Desktop (containerized Postgres), IntelliJ IDEA (recommended IDE for the backend), Postman/Thunder Client.

Full details and a sanity-check command block: see `architecture.md` §8 "Prerequisites & Local Setup."

## Getting Started

### Option A: Docker (recommended — one command, no local Java/Node/Postgres/Tesseract needed)

Requires [Docker Desktop](https://www.docker.com/products/docker-desktop/).

```bash
cp .env.example .env
# edit .env — set GEMINI_API_KEY at minimum; POSTGRES_PASSWORD/JWT_SECRET have dev defaults

docker compose up --build
```

This builds and starts three containers together:

| Service | URL | What it is |
|---|---|---|
| `frontend` | http://localhost:5173 | React app built with Vite, served by nginx |
| `backend` | http://localhost:8080 | Spring Boot API |
| `db` | localhost:5432 | PostgreSQL 16 (data persisted in a Docker volume) |

Flyway migrations run automatically on backend startup, same as running natively. Stop everything with `Ctrl+C` or `docker compose down` (add `-v` to also wipe the Postgres volume). Rebuild after changing backend or frontend code with `docker compose up --build`.

> ⚠️ **Don't mix Option A and Option B.** Docker's Postgres (in the `docket_pgdata` volume) and a native/local Postgres install are two completely separate databases, even though they can both listen on `localhost:5432`. If you alternate between `docker compose up` and `mvn spring-boot:run` against a locally installed Postgres, your data will appear to "disappear" — you're actually just landing on a different, empty database each time. Pick one and stick with it for local dev. The safest hybrid is: `docker compose up -d db` to start only the Postgres container, then run the backend natively with `mvn spring-boot:run` pointed at that same container on `localhost:5432` — that way both the containerized and native backend share the one persisted volume.

### Option B: Native (no Docker)

See [`architecture.md`](./architecture.md) §8 for full prerequisites (JDK, Maven, Node, Postgres, Tesseract).

### Backend Setup

```bash
cd backend

# Configure environment (see Environment Variables below)
cp src/main/resources/application-dev.yml.example src/main/resources/application-dev.yml
# edit application-dev.yml with your DB credentials and GEMINI_API_KEY

# Run (Flyway migrations run automatically on startup)
mvn spring-boot:run
```

Backend runs on `http://localhost:8080` by default.

### Frontend Setup

```bash
cd frontend

npm install

# Configure environment
cp .env.example .env
# edit .env: set VITE_API_BASE_URL=http://localhost:8080

npm run dev
```

Frontend runs on `http://localhost:5173` by default (Vite's default port).

## Environment Variables

**Backend** (`application-dev.yml` or environment variables):

| Variable | Description |
|---|---|
| `SPRING_DATASOURCE_URL` | PostgreSQL JDBC connection string |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |
| `GEMINI_API_KEY` | Your Google Gemini API key |
| `JWT_SECRET` | Secret used to sign JWTs |
| `TESSDATA_PREFIX` | Path to the folder containing `tessdata/` |

**Frontend** (`.env`):

| Variable | Description |
|---|---|
| `VITE_API_BASE_URL` | Base URL of the backend API (e.g., `http://localhost:8080`) |

⚠️ Never commit real values for any of the above — only commit `.example` files with placeholders (see `rules.md` §5).

## Quick Demo & Evaluation Walkthrough

Docket includes an automated seed migration (Flyway V9) that populates an instant evaluation environment:

| Access Point | Details |
|---|---|
| **App URL** | `http://localhost:5173` |
| **Demo User** | `demo@docket.ai` |
| **Demo Password** | `Demo1234!` |
| **Swagger UI** | `http://localhost:8080/swagger-ui.html` (Interactive API Docs) |
| **Health Check** | `http://localhost:8080/actuator/health` (DB & system health) |
| **Walkthrough** | See [`DEMO_SCRIPT.md`](./DEMO_SCRIPT.md) for the full 5-minute evaluation click-path |

## Core Features

- 🔐 **Auth & Workspace Isolation** — email/password signup, JWT sessions, multi-tenant workspace isolation on all data queries
- 📤 **Document Upload** — PDF/PNG/JPG up to 10MB, typed by Contract / Invoice / Resume
- 🔎 **OCR + Text Extraction** — digital PDF text extraction with automatic Tess4J OCR fallback for scans
- 🧠 **LLM Field Extraction** — structured JSON output per document type with Bean Validation schema grounding
- 📝 **Summarization** — plain-English executive summary for every processed document
- ⚠️ **Anomaly Flagging** — compares documents against a saved "standard template" and highlights policy deviations
- ✍️ **Human-in-the-Loop Field Correction** — inline JSON correction editor with zero-loss audit trails (`PATCH /api/documents/{id}/extraction`)
- 🛡️ **Denial-of-Wallet Budget Guard** — atomic per-workspace daily LLM invocation quotas and progress tracking
- 📊 **Neomorphic Dashboard** — filter by type/status/date, real-time status polling, single and bulk CSV/JSON export
- 📖 **Interactive OpenAPI / Swagger** — OpenAPI 3.0 specs and Swagger UI with Bearer JWT test harness

## API Overview

Interactive documentation with live request execution is available at **`/swagger-ui.html`**.

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| `POST` | `/api/auth/signup` | Register user & workspace | Public |
| `POST` | `/api/auth/login` | Login & receive JWT | Public |
| `GET`  | `/api/auth/me` | Current user & workspace profile | JWT |
| `GET`  | `/api/documents` | List workspace documents with anomaly count | JWT |
| `GET`  | `/api/documents/page` | Paginated workspace document list | JWT |
| `POST` | `/api/documents/upload` | Upload & async process document | JWT |
| `GET`  | `/api/documents/{id}` | Get single document enriched details | JWT |
| `GET`  | `/api/documents/{id}/file` | Stream secure authenticated file preview | JWT |
| `GET`  | `/api/documents/{id}/extraction` | Get Gemini extracted fields | JWT |
| `PATCH`| `/api/documents/{id}/extraction` | Save human field corrections | JWT |
| `GET`  | `/api/documents/{id}/summary` | Get document plain-English summary | JWT |
| `GET`  | `/api/documents/{id}/anomalies` | Get detected template anomalies | JWT |
| `POST` | `/api/documents/{id}/reprocess` | Manually re-trigger OCR/LLM pipeline | JWT |
| `GET`  | `/api/documents/usage` | Today's LLM usage count for workspace | JWT |
| `GET`  | `/api/documents/{id}/export` | Export single document (CSV / JSON) | JWT |
| `GET`  | `/api/documents/export` | Bulk export workspace dataset (CSV / JSON) | JWT |
| `POST` | `/api/templates` | Set standard baseline template | JWT |
| `GET`  | `/api/templates/{type}` | Get standard template for document type | JWT |
| `GET`  | `/actuator/health` | Spring Boot Actuator health status | Public |

## Project Documentation

This repo is built around living documentation files:

| File | Purpose |
|---|---|
| [`DEMO_SCRIPT.md`](./DEMO_SCRIPT.md) | 5-minute evaluator & judge click-path walkthrough |
| [`prd.md`](./prd.md) | What to build, target users, features, non-goals |
| [`architecture.md`](./architecture.md) | Stack, app flow, folder structure, data model, prerequisites |
| [`rules.md`](./rules.md) | Coding rules, approved libraries, AI/LLM boundaries |
| [`phases.md`](./phases.md) | The 12-phase build plan with Definition of Done per phase |
| [`design.md`](./design.md) | Neomorphic design tokens, color palette, component specs |
| [`memory.md`](./memory.md) | Complete running log of progress and session history |

## Build Phases

The project is structured in 12 phases (see `phases.md`):

0. Project Setup
1. Auth & Workspace
2. Document Upload (Invoice)
3. OCR + Text Extraction Pipeline
4. LLM Field Extraction (Invoice)
5. Summarization
6. Template Manager & Anomaly Flagging
7. Extend to Contract and Resume types
8. Dashboard Polish & Export
8.5 Modern 3D & Motion Visual Overhaul ("Aurora Obsidian")
9. Production Hardening & Evaluation Remediation
10. Advanced Polish, Human-in-the-Loop & API Governance
11. Deployment & Demo Readiness
12. *(Stretch)* Batch upload, confidence scores, job queue, billing simulation, trend anomaly detection

## Testing

- **Backend:** JUnit 5 + Spring Boot Test + Mockito (`cd backend && ./mvnw test`) — 25 unit/integration tests covering auth, tenant isolation, rate limiting, and LLM budget guards.
- **Frontend:** Vite production builds (`cd frontend && npm run build`).
- **CI Pipeline:** GitHub Actions (`.github/workflows/ci.yml`) runs full test suite and frontend compilation on every push/PR.

## Known Limitations

- English-language documents only (v1)
- Scoped to typed/clean documents — handwriting recognition is out of scope for MVP
- Single workspace role (Admin) — no granular permissions yet
- Synchronous processing — large batches may be slow until a background queue is introduced (Phase 12 stretch)

## License

Academic/educational project. Add a license (MIT recommended for a portfolio project) before making the repository public.
