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
├── phases.md             # Step-by-step build plan (10 phases)
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

## Core Features

- 🔐 **Auth & Workspaces** — email/password signup, JWT sessions, one workspace per business
- 📤 **Document Upload** — PDF/PNG/JPG up to 10MB, tagged by type (Contract / Invoice / Resume)
- 🔎 **OCR + Text Extraction** — digital PDF text extraction with automatic OCR fallback for scans
- 🧠 **LLM Field Extraction** — structured JSON output per document type, grounded in the source text (no hallucinated fields)
- 📝 **Summarization** — plain-English summary for every processed document
- ⚠️ **Anomaly Flagging** — compares new documents against a saved "standard template" and explains deviations
- 📊 **Dashboard** — filter by type/status/date, view extracted fields + summary + flags side-by-side with the original file
- 📥 **Export** — per-document or bulk CSV/JSON export

Full feature spec: [`prd.md`](./prd.md).

## API Overview

Representative endpoints (see backend controllers for full detail):

```
POST   /api/auth/signup
POST   /api/auth/login
GET    /api/documents
POST   /api/documents/upload
GET    /api/documents/{id}
POST   /api/templates
GET    /api/templates/{type}
GET    /api/documents/{id}/export
```

Consider adding `springdoc-openapi` for an auto-generated Swagger UI at `/swagger-ui.html` during development.

## Project Documentation

This repo is built around six living documents that stay in sync with the codebase throughout the project's lifecycle:

| File | Purpose |
|---|---|
| [`prd.md`](./prd.md) | What to build, target users, features, non-goals |
| [`architecture.md`](./architecture.md) | Stack, app flow, folder structure, data model, prerequisites |
| [`rules.md`](./rules.md) | Approved libraries, error handling, AI/LLM boundaries, what to avoid |
| [`phases.md`](./phases.md) | The 10-phase build plan with a Definition of Done per phase |
| [`design.md`](./design.md) | Color palette, typography, component conventions |
| [`memory.md`](./memory.md) | Running log of progress, decisions, and session history |

**If you're picking this project back up (or handing it to an AI coding assistant):** read `memory.md` first to see current status, then `phases.md` to confirm the active phase, before writing any code.

## Build Phases

The project is broken into 10 phases (see `phases.md` for full detail):

0. Project Setup
1. Auth & Workspace
2. Document Upload (Invoice, first vertical slice)
3. OCR + Text Extraction Pipeline
4. LLM Field Extraction (Invoice)
5. Summarization
6. Template Manager & Anomaly Flagging
7. Extend to Contract and Resume types
8. Dashboard Polish & Export
9. Deployment & Demo Readiness
10. *(Stretch)* Batch upload, confidence scores, job queue, billing simulation, 4th document type

Current status: see [`memory.md`](./memory.md).

## Testing

- **Backend:** JUnit 5 + Spring Boot Test + Mockito
- **Frontend:** (add Vitest + React Testing Library as the project matures)
- Sample/test documents live in `docs/sample-documents/` — synthetic or anonymized public templates only, never real client data (see `rules.md` §5)

## Known Limitations

- English-language documents only (v1)
- Scoped to typed/clean documents — handwriting recognition is out of scope for MVP
- Single workspace role (Admin) — no granular permissions yet
- Synchronous processing — large batches may be slow until a background queue is introduced (Phase 10 stretch)

## License

Academic/educational project. Add a license (MIT recommended for a portfolio project) before making the repository public.
