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

- **Active Phase:** Phase 1 — Auth & Workspace (in progress)
- **Last Updated:** 2026-08-05
- **Overall Progress:** ~15% — Phase 0 complete; frontend/backend integration verified, PostgreSQL connection confirmed, and Flyway migration applied cleanly

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

- Phase 1 work begins next: auth and workspace support for signup/login, JWT sessions, and dashboard routing. Frontend and backend scaffolds are stable and database connectivity has been verified.

## Next Steps (in order)

1. Implement backend auth endpoints and JWT issuance, including password hashing and workspace creation on signup
2. Add frontend Login and Signup pages plus a protected route wrapper for the dashboard
3. Verify a new user can sign up, log in, and refresh the dashboard page without losing session
4. Keep `rules.md` security guidance in mind for auth, password handling, and workspace-scoped data access

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

- **Flyway warning on Postgres 18.4.** The backend startup log shows `PostgreSQL 18.4` is newer than the version tested by Flyway, but `V1__init_schema.sql` was still validated successfully and the schema is up to date.

## Ideas / Not Yet Approved

(Things that came up but are NOT in scope — noted here instead of built, per rules.md §1.3)

- None yet.

## Commands Reference

```bash
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