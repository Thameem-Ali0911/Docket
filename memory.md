# MEMORY.md — Project Log

**Project:** Docket

**Purpose:** Running log of project state. Read this file FIRST at the start of every new session, before touching code. Update it LAST at the end of every session. This is the only thing that reliably carries context between chats/tools — do not rely on chat history.

---

## How to Use This File

- **At session start:** Read "Current Status" and "Next Steps" below. Cross-check against `phases.md` to confirm which phase is active.
- **At session end:** Update every section below with what actually happened — not what was planned. Be specific: which files were created/edited, what's tested and confirmed working vs. untested.
- **Never delete history** — append to the "Session Log" at the bottom instead of overwriting it, so the full project history stays visible.

---

## Current Status

- **Active Phase:** Phase 2 — Document Upload (Invoice type only)
- **Last Updated:** 2026-08-08
- **Overall Progress:** ~35% — Phase 1 complete and verified. Moving on to Document Uploads (Phase 2).

## Completed

- [x] Project scoped and defined (`prd.md`)
- [x] Architecture and tech stack decided (`architecture.md`)
- [x] Coding/AI rules established (`rules.md`)
- [x] Phased build plan created (`phases.md`)
- [x] Design system defined (`design.md`)
- [x] `frontend/` initialized (Vite + React, `App.jsx` calling backend health check)
- [x] `backend/` initialized (Spring Boot, Maven wrapper committed, `DocketApplication.java`)
- [x] `GET /api/health` endpoint built (`HealthController.java`)
- [x] First Flyway migration written (`V1__init_schema.sql` — `workspaces`, `users` tables)
- [x] Basic `SecurityConfig.java` written (permits `/api/health`, `/api/auth/**`; CORS fixed — see Known Issues)
- [x] Dockerized the whole stack: `backend/Dockerfile` (Maven build → Temurin 25 JRE runtime, native Tesseract installed), `frontend/Dockerfile` (npm build → nginx), root `docker-compose.yml` (db + backend + frontend), root `.env.example`
- [x] Backend Auth Implementation: `Workspace` and `User` entities/repos, JWT infrastructure, `GlobalExceptionHandler`, and `AuthController` (login, signup, /me).
- [x] Frontend Auth Implementation: Tailwind v4 config, `App.jsx` React Router setup, and UI pages (`Login`, `Signup`, `Dashboard`).
- [x] Phase 1: E2E Auth verified. User can successfully sign up, log in, and view dashboard against a running backend via browser.
- [x] Phase 2: Created `Document` entity, `StorageService`, `DocumentController`, and DB migration.
- [x] Phase 2: Added `UploadDocument.jsx` and updated `Dashboard.jsx` to list documents. Both backend and frontend compile successfully.

## In Progress

- Phase 2 E2E verification: Need to confirm a user can upload a PDF/Image, the file is saved to `/uploads` volume, and it appears on the dashboard.

## Next Steps (in order)

1. Verify Phase 2 Upload flow manually (start docker compose, log in, upload document).
2. Start Phase 3 (OCR Extraction) once Phase 2 is verified.

## Key Decisions & Why

| Decision | Reason | File Reference |
|---|---|---|
| MVP locked to 3 document types (Contract, Invoice, Resume) | Keeps extraction quality high and scope achievable in one academic year | prd.md §3, §7 |
| Java + Spring Boot for backend | Mature enterprise framework, strong typing, integrated ORM/security/migrations; natural fit for a student comfortable in Java | architecture.md §3.2, §7 |
| Postgres over MongoDB | Extracted fields/flags/templates are relational data | architecture.md §7 |
| Jackson + Bean Validation for both API and LLM JSON output validation | One validation pattern across the backend instead of a second library | architecture.md §3.6, §7 |
| Synchronous processing for MVP (no queue) | Avoids premature infra complexity; queue (Kafka/RabbitMQ) is a stretch goal only | architecture.md §3.4, phases.md Phase 10 |
| Tess4J over cloud OCR APIs | Free, no per-page cost, sufficient for MVP's "clean/typed documents" scope | architecture.md §7 |
| Switched LLM provider from Anthropic (Claude) to Google Gemini | User has a Gemini API key, not an Anthropic one; no LLM code was written yet (Phase 4 not started), so this was a docs/config-only rename with no migration cost | architecture.md §3.6, prd.md, rules.md §2, phases.md Phase 4 — see Session 6 |

## Known Issues / Gotchas

- **Flyway warning on Postgres 18.4.** The backend startup log shows `PostgreSQL 18.4` is newer than the version tested by Flyway, but `V1__init_schema.sql` was still validated successfully and the schema is up to date.
- **Docker Compose build not yet run/verified this session.** `docker-compose.yml`, `backend/Dockerfile`, and `frontend/Dockerfile` were written and the compose YAML was syntax-checked, but `docker compose up --build` has NOT actually been executed against Docker Desktop yet (no Docker daemon available in this session's environment). Next session with Docker Desktop running should do `docker compose up --build` and confirm all three containers come up healthy and the frontend can reach the backend health check.
- **`TESSDATA_PREFIX` path in `backend/Dockerfile` (`/usr/share/tesseract-ocr/5/tessdata`) is a best guess** based on the Debian `tesseract-ocr` apt package layout — not yet verified since OCR (Phase 3) isn't built yet. Confirm/fix this path when Phase 3 lands and Tess4J is actually wired up, by shelling into the built container (`docker compose exec backend sh` — note the run-stage image doesn't have a shell by default beyond what's in Temurin's base) or checking `dpkg -L tesseract-ocr` during the image build.
- **Frontend's `VITE_API_BASE_URL` is baked in at Docker build time**, not read at container runtime (Vite env vars are compile-time). If the backend's externally-reachable URL changes, the frontend image must be rebuilt (`docker compose up --build frontend`), not just restarted.

## Ideas / Not Yet Approved

(Things that came up but are NOT in scope — noted here instead of built, per rules.md §1.3)

- None yet.

## Commands Reference

```bash
# --- Docker (recommended) ---
cp .env.example .env    # set GEMINI_API_KEY at minimum
docker compose up --build
# frontend: http://localhost:5173, backend: http://localhost:8080, db: localhost:5432

# --- Native (no Docker) ---
# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm run dev

# Use the backend health check to verify connectivity
# Frontend should show "Backend health check: OK"
```

---


## Session Log

### Session 1 — 2026-07-31
- Created all 6 lifecycle documents: prd.md, architecture.md, rules.md, phases.md, design.md, memory.md
- No code written yet
- Next session should start Phase 0 per phases.md

### Session 2 — 2026-07-31
- Renamed project from "DocuMind" to **Docket** across all 6 files
- Switched backend stack from Node.js/Express/Prisma to **Python 3.11+ / FastAPI / SQLAlchemy (async) / Alembic / Pydantic v2**
- Updated architecture.md: tech stack (§3.2–3.9), folder structure, rationale (§7)
- Updated rules.md: approved libraries, error handling, code style conventions for Python/FastAPI
- Updated phases.md: all backend file references now point to `app/services/*.py`, `app/prompts/*.py` etc. instead of `.ts` files
- Updated memory.md: next steps and commands reference now assume a Python virtualenv + Alembic + Uvicorn workflow
- Still no code written — still Phase 0, not yet started

### Session 3 — 2026-07-31
- Switched backend stack again, from Python/FastAPI to **Java 17+ / Spring Boot 3.x / Spring Data JPA / Spring Security / Flyway**
- Updated architecture.md: tech stack (§3.2–3.9), folder structure (`src/main/java/com/docket/...`), Prerequisites & Local Setup (§8, now JDK/Maven/Tess4J/TESSDATA_PREFIX instead of Python venv/poppler), rationale (§9)
- Updated rules.md: approved libraries, Bean Validation for LLM output DTOs, `@ControllerAdvice` error handling, Java code style conventions
- Updated phases.md: all backend file references now point to `.java` classes (`controller/`, `service/`, `prompt/`, `security/`) instead of `.py` files
- Updated memory.md: next steps and commands reference now assume Spring Initializr + Maven + Flyway workflow
- Still no code written — still Phase 0, not yet started

### Session 4 — 2026-08-05
- Reviewed `AGENTS.md` and strengthened the loop protocol itself: added a repo-state verification step (§1.7), an anti-overclaiming rule and clean-stop-on-budget rule (§2), a Git & Commit Discipline section (§4.5), and a final "Definition of Production-Ready" exit checklist (§8)
- Discovered `memory.md` had drifted significantly from the actual repo: frontend (Vite/React) and backend (Spring Boot, `HealthController`, `SecurityConfig`, Maven wrapper) had already been scaffolded and a first Flyway migration (`V1__init_schema.sql`) written, none of which was reflected here — corrected Current Status/Completed/Next Steps to match reality
- Diagnosed and fixed: frontend showed "Backend health check: error — Failed to fetch." Root cause was a missing CORS configuration in `SecurityConfig.java` — Spring Security was blocking the cross-origin request from the Vite dev server, and the browser reported it as a generic fetch failure rather than a clear HTTP error. Added a `CorsConfigurationSource` bean allowing `http://localhost:5173` and wired `.cors(...)` into the filter chain.
- Confirmed backend startup logs show successful PostgreSQL connection and Flyway validation of `V1__init_schema.sql` with `Schema "public" is up to date. No migration necessary.`
- Files touched: `AGENTS.md`, `backend/src/main/java/com/docket/config/SecurityConfig.java`, `memory.md`
- Tested/confirmed: Backend health endpoint correctness; CORS fix reviewed; PostgreSQL connection and Flyway migration verification from startup logs. Frontend browser check already showed `Backend health check: OK`.
- Still untested / follow-up: none for Phase 0; Phase 1 work begins next.
- Next session should: implement auth and workspace support, starting with the backend signup/login flow and frontend login/signup pages.

### Session 5 — 2026-08-05
- User requested: dockerize the project so backend and frontend don't need to be run/managed separately, given Docker Desktop 29.5.3 is installed locally.
- Followed AGENTS.md Session Start Protocol: read memory.md, phases.md (implicitly — no phase conflict, this is infra/tooling not app scope), rules.md, architecture.md before making changes; sanity-checked the actual repo (cloned fresh from `main`) against memory.md's claims and found them accurate.
- Added: `backend/Dockerfile` (multi-stage — Maven+Temurin 25 JDK build stage, Temurin 25 JRE run stage with native `tesseract-ocr` installed for future Phase 3 OCR work, non-root user), `backend/.dockerignore`; `frontend/Dockerfile` (multi-stage — Node 20 build stage running `npm run build` with `VITE_API_BASE_URL` as a build arg, nginx 1.27 run stage), `frontend/nginx.conf` (SPA fallback routing), `frontend/.dockerignore`; root `docker-compose.yml` (services: `db` Postgres 16 with healthcheck + named volume, `backend`, `frontend`; backend waits on db healthcheck); root `.env.example`.
- Parameterized `backend/src/main/resources/application.yml` datasource/jwt/anthropic values with `${VAR:default}` env-var syntax so the same jar/image works both natively (`mvn spring-boot:run`, defaults unchanged) and in Docker Compose (env vars injected by compose) — no more need for a separate `application-dev.yml` copy step when using Docker.
- Confirmed no code changes needed to `SecurityConfig.java` — its CORS allow-list already targets `http://localhost:5173`, which is exactly where the frontend container's nginx is published.
- Updated docs per AGENTS.md §6.2: `architecture.md` §8.1 (new — Docker Compose usage), `README.md` (Getting Started now has Option A Docker / Option B native, Prerequisites section points to Docker as the fast path, Project Structure tree lists the new Docker files), `rules.md` §2 (Docker/Compose added to approved tooling list, explicitly scoped as tooling not runtime stack), `memory.md` (this entry + Completed + Known Issues + Commands Reference).
- Tested/confirmed: `docker-compose.yml` is valid YAML (parsed with PyYAML). Dockerfiles were written carefully against the actual `pom.xml` (Java 25), `package.json` (Vite 8), and existing config files, but **not built or run** — no Docker daemon available in this session's sandboxed environment.
- Still untested / follow-up: actually run `docker compose up --build` on the user's machine (Docker Desktop 29.5.3) and confirm all three services come up and the frontend reaches the backend; verify the `TESSDATA_PREFIX` path guess in `backend/Dockerfile` once Phase 3 (OCR) is built.
- Next session should: continue Phase 1 (auth/workspace) as previously planned — Docker setup was a tooling request, not a phase-scope change. If the user reports a Docker Compose issue first, debug that before resuming Phase 1.

### Session 6 — 2026-08-05
- User doesn't have an Anthropic API key, has a Google Gemini one instead — requested the LLM provider be swapped from Anthropic (Claude) to Google Gemini throughout the whole project.
- Confirmed via search that **no application code referenced Anthropic/Claude yet** — Phase 4 (LLM Field Extraction) hasn't started, so this was a pure docs/config rename, not a code migration. No `.java` files touched.
- Renamed `ANTHROPIC_API_KEY` → `GEMINI_API_KEY` everywhere it appeared: `backend/src/main/resources/application.yml` (now a `gemini:` config block, `${GEMINI_API_KEY:}`), `backend/src/main/resources/application-dev.yml.example`, `docker-compose.yml`, root `.env.example` (comment now points to aistudio.google.com/apikey instead of console.anthropic.com).
- Updated prose/planning docs: `README.md` (tech stack table, prerequisites, env var table, setup comments), `architecture.md` (system diagram, app flow steps, §3.6 LLM integration section, folder tree comments — `AnthropicClient.java` renamed to `GeminiClient.java` in the planned layout, prerequisites §8, rationale §9), `rules.md` §2 approved stack and §3 data-sharing rule, `prd.md` §technical notes, `phases.md` Phase 4 goal + task (planned class name `AnthropicClient` → `GeminiClient`), `AGENTS.md` §4 approved stack line.
- **Deliberately left untouched:** every reference to "Claude" meaning the AI coding assistant itself (e.g. `AGENTS.md`'s audience line, `rules.md`'s "every AI coding session (Claude Code, Cursor, ChatGPT...)" line) — those describe the tool being used to work on this repo, not the app's LLM provider, and swapping those would be a mistake.
- Since no backend code exists yet for the LLM client, there's nothing to functionally test this session — when Phase 4 starts, the actual Gemini REST API shape (endpoint, auth header, request/response JSON) will need to be looked up fresh rather than assumed, since it differs from Anthropic's `/v1/messages` shape referenced in the old architecture.md text.
- Files touched: `README.md`, `architecture.md`, `rules.md`, `prd.md`, `phases.md`, `AGENTS.md`, `memory.md`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-dev.yml.example`, `docker-compose.yml`, `.env.example`.
- Next session should: continue Phase 1 (auth/workspace) — the provider swap doesn't change phase scope. When Phase 4 (LLM extraction) actually starts, write `GeminiClient.java` against Gemini's real REST API (`generativelanguage.googleapis.com`), not by assuming it mirrors Anthropic's request/response shape.

### Session 7 — 2026-08-08
- Implemented backend Phase 1: JPA entities (`Workspace`, `User`), repositories, JWT generation and filter (`JwtService`, `JwtAuthFilter`), global error handler, and `AuthController` (signup, login, me).
- Updated `SecurityConfig` to be stateless and integrated `JwtAuthFilter`.
- Implemented frontend Phase 1: Installed React Router and Tailwind v4, configured CSS tokens per `design.md`, created `api.js` wrapper, `ProtectedRoute.jsx`, and UI pages (`Login.jsx`, `Signup.jsx`, `Dashboard.jsx`). Rewrote `App.jsx` with routes.
- Built both frontend and backend to verify compilation; both build cleanly without errors.
- Files touched: `backend/src/main/java/com/docket/...`, `frontend/src/...`, `frontend/index.html`, `frontend/vite.config.js`.
- Tested/confirmed: Backend compiles successfully (`mvn clean compile`). Frontend builds successfully (`npm run build`).
- Still untested / follow-up: End-to-end auth flow needs to be run locally (start DB + backend + frontend and manually sign up).
- Next session should: Boot the stack (via Docker Compose or native) and verify Phase 1 E2E flow.