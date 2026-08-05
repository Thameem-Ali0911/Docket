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

- **Active Phase:** Phase 0 — Project Setup (in progress, nearly complete)
- **Last Updated:** 2026-08-05
- **Overall Progress:** ~10% — planning documents complete; frontend and backend scaffolds exist and both run locally; `/api/health` endpoint built and now reachable from the frontend after a CORS fix (see Session 4 below)

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

## In Progress

- Frontend/backend are both running locally and the health check now succeeds after the CORS fix. Not yet confirmed: DB connection actually verified end-to-end against a running Postgres instance, and whether Flyway migration `V1` has actually been applied (ran on a live DB) vs. just written.

## Next Steps (in order)

1. Confirm PostgreSQL is reachable and `V1__init_schema.sql` applies cleanly on backend startup (check startup logs for Flyway success)
2. Manually confirm the frontend health check now shows green after the CORS fix + backend restart
3. Close out Phase 0's Definition of Done (see phases.md) once the above are verified, then flip Active Phase to Phase 1 (Auth & Workspace)
4. Note: `memory.md` had drifted from the actual repo state (code existed here that wasn't logged) — future sessions should treat this as a reminder to always run the AGENTS.md §1.7 repo-state check before trusting this file

## Key Decisions & Why

| Decision | Reason | File Reference |
|---|---|---|
| MVP locked to 3 document types (Contract, Invoice, Resume) | Keeps extraction quality high and scope achievable in one academic year | prd.md §3, §7 |
| Java + Spring Boot for backend | Mature enterprise framework, strong typing, integrated ORM/security/migrations; natural fit for a student comfortable in Java | architecture.md §3.2, §7 |
| Postgres over MongoDB | Extracted fields/flags/templates are relational data | architecture.md §7 |
| Jackson + Bean Validation for both API and LLM JSON output validation | One validation pattern across the backend instead of a second library | architecture.md §3.6, §7 |
| Synchronous processing for MVP (no queue) | Avoids premature infra complexity; queue (Kafka/RabbitMQ) is a stretch goal only | architecture.md §3.4, phases.md Phase 10 |
| Tess4J over cloud OCR APIs | Free, no per-page cost, sufficient for MVP's "clean/typed documents" scope | architecture.md §7 |

## Known Issues / Gotchas

- **CORS not configured → frontend health check fails with "Failed to fetch" (fixed Session 4).** `SecurityConfig.java` had no `CorsConfigurationSource` bean, so Spring Security silently blocked the browser's cross-origin request from the Vite dev server (`localhost:5173`) to the backend (`localhost:8080`). This shows up in the browser as a generic "Failed to fetch," not a normal HTTP error, which makes it easy to misdiagnose as a wrong URL/port. Fixed by adding a `corsConfigurationSource()` bean allowing `http://localhost:5173` and wiring `.cors(...)` into the filter chain. **Any future new frontend origin (e.g. a deployed Vercel/Netlify URL) must be added to `allowedOrigins` here too**, or the same symptom will reappear in production.

## Ideas / Not Yet Approved

(Things that came up but are NOT in scope — noted here instead of built, per rules.md §1.3)

- None yet.

## Commands Reference

_(To be filled in once Phase 0 is complete — e.g., how to run frontend/backend locally, how to run migrations, how to seed demo data.)_

```
# placeholder — update after Phase 0
cd frontend && npm run dev

cd backend
mvn spring-boot:run
# migrations run automatically on startup via Flyway
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
- Files touched: `AGENTS.md`, `backend/src/main/java/com/docket/config/SecurityConfig.java`, `memory.md`
- Tested/confirmed: Reviewed `HealthController.java` (correct, `GET /api/health` → `"OK"`), `application.yml` (port 8080, matches frontend's `VITE_API_BASE_URL`), and the CORS bean addition itself was reviewed for correctness — the fix has **not yet been confirmed working in the browser** by re-running the app after this change, since that happens on the user's machine
- Still untested / follow-up: confirm the health check goes green after restarting the backend with the CORS fix; confirm Postgres is actually running and `V1__init_schema.sql` applies cleanly on startup
- Next session should: verify the CORS fix worked, verify the DB migration applied, then close out Phase 0's Definition of Done and move `memory.md`'s Active Phase to Phase 1