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

- **Active Phase:** Phase 7 — Extend to Contract and Resume Types, not yet started
- **Last Updated:** 2026-08-16
- **Overall Progress:** ~85% — Phases 0-6 complete. Template Manager and Anomaly Flagging built. Phase 6's Definition of Done is met pending the user's manual E2E check.

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
- [x] Phase 2: E2E Auth verified. User can successfully sign up, log in, upload document, and view dashboard against a running backend via browser.
- [x] Phase 3: OCR + text extraction pipeline (`OcrService`, PDFBox + Tess4J fallback, confidence-based failure detection). Verified working end-to-end in Docker (Session 12) and confirmed via a real `mvn clean compile` build (Session 13).
- [x] Phase 4 (backend, complete): `extractions` table/migration, `Extraction` entity, `InvoiceExtractionDto`, `ExtractInvoicePrompt`, `GeminiClient`, `ExtractionService`, wired into the async OCR pipeline, `GET /api/documents/{id}/extraction` endpoint. **Verified end-to-end against the live Gemini API (`gemini-3.5-flash-lite`)** — real invoice produced correct structured fields (Session 16).
- [x] Phase 4 (frontend, complete): `DocumentDetail.jsx` — fetches the document + its extraction, renders vendor/invoice#/dates/total fields and a line-items table next to an `<iframe>` preview of the original file (with an "open in new tab" fallback link); handles PENDING/failed/no-extraction states. `Dashboard.jsx`'s "View" button now links to `/documents/:id`, route added in `App.jsx`. Currency values render through normal React text rendering (no manual encoding/escaping), so the ₹ symbol renders as plain UTF-8 — the Session 16 `psql` terminal glitch does not apply to the browser. Frontend builds cleanly (`npm run build`, Session 18).
- [x] Phase 5 (backend, complete): Created `V5__create_summaries_table.sql`, `Summary.java`, `SummaryRepository.java`, `SummaryResponseDto.java`, `SummarizePrompt.java`, `SummarizeService.java`. Wired into `DocumentProcessingService.java` to run for all document types. Added `GET /api/documents/{id}/summary` endpoint.
- [x] Phase 5 (frontend, complete): Updated `DocumentDetail.jsx` to fetch and render the summary alongside the document fields.
- [x] Phase 6 (backend, complete): Created `V6__create_templates_and_anomalies_tables.sql`, `Template.java`, `AnomalyFlag.java` entities and repositories. Built `AnomalyCheckPrompt.java` and `AnomalyService.java` to compare documents against templates using Gemini. Added `TemplateController` and updated `DocumentController`.
- [x] Phase 6 (frontend, complete): Built `TemplateManager.jsx` to allow designating a document as a standard template. Added `AnomalyFlag.jsx` and updated `DocumentDetail.jsx` to display anomaly warnings.

## In Progress

- None — Phase 6 is code-complete. Awaiting the user's manual check of Template Manager and Anomaly Flagging through the full UI to formally close out the Definition of Done before starting Phase 7.

## Next Steps (in order)

1. User to run `docker compose up -d --build` (to rebuild backend and frontend) and manually verify the new Template Manager. Upload a baseline invoice, set it as template, then upload an altered invoice and verify anomalies appear in `DocumentDetail.jsx`.
2. Once confirmed, start Phase 7 (Extend to Contract and Resume Types): Add prompts and DTOs for Contract and Resume types, and enable them in the UI.

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
- **Docker Postgres host port changed from 5432 -> 5433** (`docker-compose.yml`) to avoid colliding with a locally-installed Postgres on the same dev machine (used when running the backend via `mvn` directly). Both Postgres instances were binding `0.0.0.0:5432` on the host; whichever process claimed the port first silently "won," so a DBeaver connection to `localhost:5432` was non-deterministically hitting the wrong database (usually the local one), making Docker-side rows/tables look empty or stale. Fix was host-port-only — the container-internal port and `SPRING_DATASOURCE_URL` (`jdbc:postgresql://db:5432/docket`) are untouched since that connection happens over the Docker network, not the host. **Anyone connecting via DBeaver/psql from the host must now use port 5433 for the Docker DB**, and keep a separate connection on 5432 for the local `mvn`-side Postgres. See Session 12.
- **BUG (fixed Session 20): Documents can get permanently stuck at `PENDING` if a PDF's extracted text contains a NUL byte (`0x00`).** PostgreSQL's `text`/UTF8 columns reject NUL bytes outright under any encoding — this is a hard Postgres limitation, not a config/driver issue. Some PDFs (seen with a ReportLab-generated invoice using an unusual `FirstChar 0` / `ToUnicode`-only embedded TrueType font encoding) yield a `PDFBox`-extracted string that includes one. In the *original* `DocumentProcessingService.processDocumentAsync`, `documentRepository.save(doc)` was called **outside** the method's `try/catch` entirely, so the resulting `DataIntegrityViolationException` (`ERROR: invalid byte sequence for encoding "UTF8": 0x00`) had zero error handling around it — the async thread died silently, the row was never saved, and the document stayed `PENDING` forever with no trace in the logs. Two things fixed this: (1) wrapping the save call in its own try/catch with logging, so this class of failure is now always visible (`Failed to persist status for document id=... - it may remain stuck as PENDING.`), which is what actually surfaced the real cause; (2) the real fix — `DocumentProcessingService` and `ExtractionService` now both strip NUL bytes (`stripNulBytes()`) from any text before persisting (`extracted_text`, Gemini's `fields_json`, and `failed_reason`). **If a document is ever seen stuck at `PENDING` again, check the backend logs first for a `DataIntegrityViolationException`/`invalid byte sequence` before assuming it's an OCR/Tesseract problem** — that was the wrong initial hypothesis this session and cost significant time (see Session 20 for the full investigation trail: Tesseract binary, tessdata, and native `libtesseract.so` binding were all independently verified healthy and were red herrings).
- **`@Async` methods must never let *any* `Throwable` (not just `Exception`) escape.** A void `@Async` method has no `Future` for the caller to inspect, so any uncaught exception *or* Error (native JNI failures, `StackOverflowError`, etc.) simply vanishes — Spring's default behavior logs it via an internal logger with minimal context, easy to miss, and the DB row associated with that task is left in whatever state it was in before the async call (here: `PENDING` forever). `DocumentProcessingService.processDocumentAsync` and `ExtractionService.extractInvoiceFields` now both catch `Throwable` at every stage and always leave the document/extraction in a terminal, visible state. A global `AsyncUncaughtExceptionHandler` (new `config/AsyncConfig.java`) is also registered as a last-resort net for anything that still somehow escapes. **Apply this same "catch Throwable, always reach a terminal state, always log" pattern to any future `@Async` method added to this codebase** (e.g. Phase 5's summarization job).

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

### Session 8 — 2026-08-08
- Diagnosed and fixed a bug where the frontend login flow would get "stuck on 'Signing in...'". The root cause was that `JwtAuthFilter` injected the `userId` as an `Integer` into the Spring Security principal, but `DocumentController` mistakenly cast `authentication.getName()` to a `String email`. This caused `DocumentService` to look up the user by email using the `userId` string (e.g., `"3"`), resulting in a `RuntimeException` (500). The frontend `/dashboard` immediately caught this 500 error from `/api/documents`, cleared the token, and redirected back to `/login` too fast for the user to notice.
- Modified `DocumentController.java` to extract `Integer userId = (Integer) authentication.getPrincipal()` and pass it to `DocumentService`.
- Modified `DocumentService.java` to accept `Integer userId` instead of `String email`, and to use `userRepository.findById(userId)`.
- Changed `DocumentService` to throw an `ApiException` (404) instead of a generic `RuntimeException` if the user is somehow not found.
- Re-compiled the backend and rebuilt the `backend` Docker image.
- Files touched: `backend/src/main/java/com/docket/controller/DocumentController.java`, `backend/src/main/java/com/docket/service/DocumentService.java`.
- Tested/confirmed: Created a new user via `/api/auth/signup` and logged in via `/api/auth/login`. Verified that calling `/api/documents` with the resulting JWT now returns a `200 OK` with an empty array `[]` instead of throwing a 500. 
- Still untested / follow-up: Manually verify the file upload functionality via the frontend UI since it's the core of Phase 2.
- Next session should: Verify Phase 2 file upload from the browser, and then move on to Phase 3 (OCR Extraction) if successful.

### Session 9 — 2026-08-08
- Addressed a bug where the dashboard entered an infinite redirect loop after a successful document upload. The upload actually succeeded (file written to disk, row in DB), but Spring's Jackson JSON serialization threw an `InvalidDefinitionException` (StackOverflow / Hibernate Proxy error) when trying to serialize the `Document` entity to return it to the frontend. This was caused by the `workspace` relationship being a lazily loaded Hibernate proxy.
- Added `@JsonIgnore` to the `workspace` field in `Document.java` to prevent Jackson from attempting to serialize the linked `Workspace` entity, which the frontend doesn't need anyway.
- Re-compiled backend and restarted backend container.
- Tested/confirmed: Validated that the `/api/documents` endpoint now successfully returns `200 OK` without throwing serialization exceptions.
- Next session should: User verify upload from frontend.

### Session 10 — 2026-08-11
- Implemented Phase 3 backend logic for OCR and text extraction.
- Created `V3__add_extracted_text_to_documents.sql` to add `extracted_text` and `failed_reason` columns to the `documents` table.
- Added `pdfbox` and `tess4j` dependencies to `backend/pom.xml`.
- Updated `Document.java` entity to include `extractedText` and `failedReason` fields.
- Created `OcrService.java` to extract text using Apache PDFBox for digital PDFs and Tess4J (OCR) as fallback for scanned documents and images.
- Updated `DocumentService.java` to invoke `OcrService` synchronously on upload and update the document status to `PROCESSED` or `FAILED`.
- Files touched: `backend/src/main/resources/db/migration/V3__add_extracted_text_to_documents.sql`, `backend/pom.xml`, `backend/src/main/java/com/docket/entity/Document.java`, `backend/src/main/java/com/docket/service/OcrService.java`, `backend/src/main/java/com/docket/service/DocumentService.java`, `backend/src/main/java/com/docket/service/StorageService.java`.
- Tested/confirmed: Backend compiles successfully (`mvn clean compile`).
- Still untested / follow-up: End-to-end verification of document upload and OCR text extraction from the frontend UI.
- Next session should: Have the user manually verify document upload and OCR extraction.

### Session 11 — 2026-08-11
- User manually verified upload (per Session 10 follow-up) and found two related bugs in status accuracy, both in the async OCR pipeline (`DocumentProcessingService.processDocumentAsync`), not the UI: the dashboard badge just renders whatever status the backend sets.
- Bug 1: A blank/no-text image (e.g. one crafted to fail OCR) was still marked `PROCESSED`. Cause: `Tesseract.doOCR()` returns an empty string rather than throwing when it finds no text, so the old code unconditionally set `PROCESSED` after any non-exceptional OCR call. Fix: check `extractedText` for null/blank and set `FAILED` with a reason in that case.
- Bug 2: An image with garbled/misread characters (non-blank but wrong text) was still marked `PROCESSED`, since it isn't blank. A presence check alone can't catch this — needed a quality signal. Fix: reworked `OcrService.extractText` to return a new `OcrResult(text, confidence)` instead of a bare `String`, using Tesseract's per-word confidence (`ITesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD)`, 0–100 scale, averaged across words). `DocumentProcessingService` now marks the doc `FAILED` if average confidence is below `MIN_OCR_CONFIDENCE = 60.0`, with a reason string. For text pulled from a PDF's embedded text layer (not OCR'd), confidence is reported as `OcrResult.NOT_APPLICABLE` (`-1`) and skips the threshold check, since that text is trusted as-is.
- Added `backend/src/main/java/com/docket/service/OcrResult.java` (new file) as the text+confidence carrier.
- The `MIN_OCR_CONFIDENCE = 60.0` threshold is an untuned guess — flagged to the user as needing real calibration against sample "should pass" vs "should fail" documents, since Tesseract confidence also dips on legitimately noisy real scans (skew, low-res, handwriting), not just garbage images. User has not yet responded on whether/how to tune this.
- Files touched: `backend/src/main/java/com/docket/service/DocumentProcessingService.java`, `backend/src/main/java/com/docket/service/OcrService.java`, `backend/src/main/java/com/docket/service/OcrResult.java` (new).
- Tested/confirmed: Nothing — **no network access to Maven Central in this sandboxed session, so the build could not be run/compiled to confirm.** `Tesseract.getWords(BufferedImage, int)` and `Word.getConfidence()` are used based on documented tess4j 5.13.0 API (already a `pom.xml` dependency), not verified against the actual jar.
- Still untested / follow-up: (1) Actually compile (`mvn clean compile`) and run this — signatures for `ITessAPI.TessPageIteratorLevel.RIL_WORD` / `Tesseract.getWords` need real-build confirmation. (2) Re-test both the original blank-OCR image and the garbled-text image end-to-end to confirm both now show `FAILED` with a sensible `failedReason`. (3) Calibrate `MIN_OCR_CONFIDENCE` against real scanned documents so legitimate-but-noisy scans aren't wrongly marked `FAILED`.
- Next session should: Build and run the backend to confirm the tess4j API calls compile, then E2E-verify both failure cases before considering Phase 3 done.

### Session 12 — 2026-08-12
- User reported OCR/extracted-text results looked inconsistent between running the backend via `mvn` and via `docker compose` — appeared in DB for one but not the other.
- Diagnosed via the Docker container directly (not DBeaver, to rule out client-side issues): confirmed Tesseract 5.5.0 is installed correctly in the `backend` image, `TESSDATA_PREFIX=/usr/share/tesseract-ocr/5/tessdata` is correct and `eng.traineddata` is present, and `SELECT ... FROM documents` via `docker compose exec db psql` showed `extracted_text` correctly populated (1200-1709 chars) for `PROCESSED` rows and a correct `FAILED` status + `failed_reason` for a genuinely blank test document. **Conclusion: the OCR pipeline (Session 11 fixes) is working correctly in Docker — there was no actual extraction bug.**
- Root cause of the user's confusion was environmental, not code: a Postgres instance installed natively on the user's Windows machine (used for local `mvn` runs) was also bound to host port 5432, competing with the Dockerized Postgres's `5432:5432` mapping. Confirmed via `netstat -ano | findstr :5432` showing two separate LISTENING PIDs — one `postgres.exe` (native), one `com.docker.backend` (Docker Desktop's proxy). DBeaver's `localhost:5432` connection was landing on the native Postgres, not the Docker one, so the user was looking at the wrong (older/different) dataset and seeing missing tables/rows that were actually only in the Docker DB (or vice versa).
- Fix: changed `docker-compose.yml` `db.ports` mapping from `"5432:5432"` to `"5433:5432"` (host-side only; container-internal port and `SPRING_DATASOURCE_URL` untouched, since backend<->db traffic is on the Docker network, not the host). Added an inline comment in the compose file and a Known Issues entry in this file pointing future sessions/DBeaver setups at port 5433 for the Docker DB.
- Files touched: `docker-compose.yml`.
- Tested/confirmed: Nothing yet in this sandboxed session (no Docker daemon available here) — user needs to run `docker compose up -d db` to apply the new port mapping and reconnect DBeaver to `localhost:5433` for the Docker-side `docket` database.
- Still untested / follow-up: (1) User to recreate the `db` container with the new port and confirm `docker compose exec db psql -U postgres -d docket -c "\dt"` still lists all tables (rules out a migration/data-loss red herring after the compose change). (2) User to add a second DBeaver connection on port 5433 and confirm it shows the same `documents` rows already verified via `psql` in this session, keeping the existing 5432 connection pointed at the local `mvn`-side Postgres for comparison.
- Next session should: Confirm the 5433 port change resolved the DBeaver visibility confusion, then return to Session 11's still-open follow-up (compile/run confirmation of the tess4j confidence-check code, calibrate `MIN_OCR_CONFIDENCE`) to finish closing out Phase 3.

### Session 13 — 2026-08-15
- Closed out Session 11's last open follow-up: confirmed the tess4j confidence-check code (`OcrService.getWords`, `Word.getConfidence()`, `ITessAPI.TessPageIteratorLevel.RIL_WORD`) compiles and matches the real published Tess4J 5.13.0 API — manually verified signatures against `tess4j.sourceforge.net`'s javadocs, then user ran `./mvnw clean compile` locally (build tooling sandbox here has no route to Maven Central) and got **BUILD SUCCESS** (27 source files, no errors).
- `MIN_OCR_CONFIDENCE = 60.0` remains an untuned placeholder — still needs calibration against real "should pass" / "should fail" sample scans whenever the user has some to run through. Not blocking; documented as a tune-later value.
- **Phase 3 is now considered complete.** All Definition of Done items met: digital PDFs and scanned images both produce readable extracted text (or a correct `FAILED` status with reason) verified in Sessions 10–12, and the OCR/confidence code itself now has a real, successful build behind it.
- Files touched: `memory.md` only.
- Next session should: Begin Phase 4 (LLM Field Extraction for Invoices) per `phases.md` — build `prompt/ExtractInvoicePrompt.java`, `service/ExtractionService.java`, `GeminiClient`, `dto/InvoiceExtractionDto.java`, an `extractions` Flyway migration/table, and wire `DocumentDetail.jsx` to show extracted fields. Note: the real Gemini REST API shape (`generativelanguage.googleapis.com`, auth header, request/response JSON) needs to be looked up fresh per Session 6's note, not assumed to mirror Anthropic's `/v1/messages` shape.

### Session 14 — 2026-08-15
- Started Phase 4 (LLM Field Extraction, invoices) backend side.
- **Looked up the real, current Gemini structured-output REST API shape** (per Session 6's note not to assume it mirrors Anthropic's) directly against `ai.google.dev/gemini-api/docs/generate-content/structured-output`, confirmed 2026-08-15. Key finding: Google now recommends a newer **Interactions API** (`/v1beta/interactions`) as of 2026, but the classic `generateContent` endpoint (`POST /v1beta/models/{model}:generateContent`) is still supported and is what was implemented, since it's simpler and matches the single-shot extraction use case. Auth via `x-goog-api-key` header (not a Bearer token). Structured output is requested via `generationConfig.responseFormat.text.mimeType: "application/json"` + `.schema` (a JSON Schema object) — note this is the *current* shape; older docs/examples use a flatter `generationConfig.responseMimeType` / `responseSchema` pair without the nested `text` object, so if a future session sees Gemini reject the nested shape, try falling back to the flat one.
- Added: `db/migration/V4__create_extractions_table.sql` (one extraction row per document, unique on `document_id`, `fields_json` TEXT + `failed_reason`); `entity/Extraction.java`; `repository/ExtractionRepository.java`; `dto/InvoiceExtractionDto.java` (Bean Validation annotations per rules.md); `prompt/ExtractInvoicePrompt.java` (prompt text + hand-written JSON Schema string, explicitly instructed to leave fields empty rather than guess, per rules.md §3 grounded-in-text rule); `service/GeminiClient.java` (uses JDK's built-in `java.net.http.HttpClient`, no new HTTP-client dependency needed); `service/ExtractionService.java` (calls GeminiClient, deserializes+validates with the existing Jakarta Validator, persists success or a `failedReason`).
- Wired into the pipeline: `DocumentProcessingService.processDocumentAsync` now calls `extractionService.extractInvoiceFields(doc)` immediately after a document reaches `PROCESSED` status, but only for `DocumentType.INVOICE` (Contract/Resume come in Phase 7).
- Added `DocumentService.getDocumentForWorkspace(userId, documentId)` (workspace-scoped single-document lookup, 404s if the document belongs to a different workspace) and `GET /api/documents/{id}/extraction` on `DocumentController` for the frontend to poll/fetch extraction results.
- `gemini.model` added to `application.yml` (`${GEMINI_MODEL:gemini-3.5-flash}`) so the model is swappable without a code change.
- Tested/confirmed: Manual brace/paren balance check on all new/changed files (this sandbox still has no route to Maven Central, same limitation as Sessions 10–13) — **not yet compiled**. Endpoint/request shape verified against live docs, not against an actual Gemini API call.
- Still untested / follow-up: (1) User to run `./mvnw clean compile` locally to confirm this all actually compiles (expect it should, but Session 13 showed hand-verification isn't a substitute for the real build). (2) Actually exercise the flow end-to-end with a real `GEMINI_API_KEY` and a sample invoice — confirm the nested `responseFormat.text.schema` shape is accepted as-is by the live API (see note above re: possible flat-shape fallback) and that `InvoiceExtractionDto` deserializes cleanly from a real response. (3) `DocumentDetail.jsx` frontend work (table showing extracted fields next to the file preview, per phases.md Phase 4) has not been started yet — backend only so far.
- Next session should: Compile-check, then either fix the Gemini request shape if the live API rejects it, or move straight to the `DocumentDetail.jsx` frontend piece once a real extraction has been confirmed working end-to-end.

### Session 15 — 2026-08-15 
- User compiled Session 14's code successfully (`BUILD SUCCESS`, 33 source files), then ran it end-to-end against the real Gemini API with `GEMINI_API_KEY` set. Confirmed via the `extractions` table: the exact fallback risk flagged in Session 14 materialized - Gemini returned **HTTP 400**: `Invalid value at 'generation_config.response_format.text.mime_type'`.
- Root cause: the newer nested `generationConfig.responseFormat.text.mimeType` field is typed as an enum server-side, not a free string - sending `"application/json"` as a plain string is rejected.
- Fix: switched `GeminiClient.java` to the older, stable flat shape - `generationConfig.responseMimeType: "application/json"` + `generationConfig.responseSchema: <schema object>` - well-established across Google's own SDKs/examples, takes a plain MIME string.
- Files touched: `service/GeminiClient.java`, `memory.md`.
- Still untested / follow-up: (1) Recompile to confirm no syntax issues. (2) Re-upload the same test invoice and confirm `fields_json` now populates. (3) If this also fails, capture the exact new error - don't guess blind again.
- Next session should: Re-test end-to-end with the flat-shape fix. Once a real extraction succeeds, move to the `DocumentDetail.jsx` frontend piece (Phase 4's remaining item).

### Session 16 — 2026-08-15 
- Root cause of Session 15's fix appearing not to work: the flat-shape code change was correct on disk, but the running backend process (Docker container / `mvnw spring-boot:run`) was never restarted, so it kept serving the old compiled classes - `mvn clean compile` alone doesn't restart a running app. Confirmed by having the user paste the actual running `GeminiClient.generateStructuredJson` source, which already had the flat-shape fix with no trace of the old `responseFormat.text` code, proving the *code* wasn't the problem.
- User did a full `docker compose down` + `docker compose up --build backend` (fresh container, not just a rebuild) and re-uploaded a new test invoice.
- **Confirmed working end-to-end**: `extractions` row for document 19 shows populated `fields_json` (vendorName "ACME DIGITAL SOLUTIONS", invoiceNumber "INV-2026-00417", invoiceDate "08 Aug 2026", totalAmount, lineItems array with description/amount) and an empty `failed_reason`. **Phase 4 backend (Gemini invoice extraction) is now verified functional against the live API**, using `gemini-3.5-flash-lite` and the flat `responseMimeType`/`responseSchema` request shape.
- Note: `totalAmount` rendered with a corrupted/placeholder-looking currency symbol ("■99,120.00") in the psql terminal output - almost certainly a terminal encoding/font issue displaying the ₹ (rupee) symbol, not a data problem, but worth a quick visual sanity check once the frontend renders it properly with UTF-8.
- Files touched: `memory.md` only (no code changes this session - purely a deploy/restart issue, not a bug).
- Next session should: Build `DocumentDetail.jsx` - a page showing the extracted invoice fields (vendor, invoice #, dates, total, line items table) next to a preview of the original uploaded file, per phases.md Phase 4. This is the last remaining item to close out Phase 4's Definition of Done. Also do a quick UTF-8 rendering check on currency symbols once that page exists.

### Session 17 — 2026-08-15
- User reported: after signing in, refreshing the browser showed `ERR_CONNECTION_REFUSED`, but pressing the browser back button showed a "fully functional" dashboard.
- Diagnosed: `docker compose ps -a` showed only `backend` and `db` containers - no `frontend` container at all (not even in an `Exited` state). Root cause traced to Session 16: `docker compose down` (removes all containers) was followed by `docker compose up --build backend`, which only recreates the named service (`backend`) and its dependency (`db`) - `frontend` was never recreated. The browser tab appeared to "work" only because the SPA's JS was already loaded in memory and its API calls hit `backend:8080` directly (still running); a hard refresh requires fetching fresh HTML from `frontend:5173`, which had nothing listening on it.
- Fix: `docker compose up -d frontend` recreated the container; confirmed `Up` in `docker compose ps` and refresh now works.
- **Rule for future sessions:** whenever a "refresh causes an error page" or "works until reload" bug is reported, check `docker compose ps -a` (not just `ps`, which hides stopped containers) for ALL services before assuming a code bug. A container missing/exited will make the browser cache look "functional" via back-button/bfcache while a real reload fails.
- Also: prefer `docker compose restart <service>` or `docker compose up -d` (no service name, brings up everything) over `docker compose down` + a scoped `up --build <service>`, since the latter tears down all containers but only recreates the one named.
- Files touched: `memory.md` only (no code changes this session - deploy/tooling issue, not a bug).
- Next session should: Build `DocumentDetail.jsx` per phases.md Phase 4 (still the last remaining item), then the UTF-8 currency check, then move to Phase 5.

### Session 18 — 2026-08-15
- Built the last remaining Phase 4 item: `frontend/src/pages/DocumentDetail.jsx`. Fetches `GET /api/documents` (to locate the doc by id — no single-document-by-id endpoint exists yet, reused the existing list endpoint) and `GET /api/documents/{id}/extraction` in parallel, parses `fieldsJson`, and renders vendor/invoice#/invoice date/due date/total plus a line-items table, next to an `<iframe>` preview of the original file with an "open in new tab" link. Handles three non-happy-path states explicitly: document still `PENDING` (no extraction yet), extraction `failedReason` present, and no extraction row at all.
- Wired the route: added `/documents/:id` (protected) in `App.jsx`, and changed `Dashboard.jsx`'s disabled "View" button (a Phase 3 placeholder) to navigate there.
- Styling reuses the existing `.card`/`.badge`/design-token classes from `index.css` per `design.md` — no new CSS added.
- Re: the Session 16 UTF-8/currency note — confirmed this was specific to the `psql` terminal, not a real concern: `totalAmount` is rendered via normal JSX text interpolation, which is UTF-8 by default in the browser, so no special handling was needed.
- Tested/confirmed: `npm install` + `npm run build` succeeds cleanly (Vite, 31 modules, no errors) in this sandbox. **Not yet run against a live backend/Gemini extraction in a browser** — no Docker/network access here for that.
- Still untested / follow-up: User to `docker compose up -d` (rebuild frontend image first if needed) and click through `DocumentDetail.jsx` for a few real uploaded invoices to confirm the fields render correctly end-to-end and to formally satisfy Phase 4's "8/10 test invoices" Definition of Done.
- Files touched: `frontend/src/pages/DocumentDetail.jsx` (new), `frontend/src/App.jsx`, `frontend/src/pages/Dashboard.jsx`, `memory.md`.
- Next session should: Once the user confirms `DocumentDetail.jsx` looks correct against real data, start Phase 5 (Summarization) per phases.md — `prompt/SummarizePrompt.java`, `service/SummarizeService.java`, `summaries` table, and a "Summary" card added to `DocumentDetail.jsx`.

### Session 19 — 2026-08-15
- User reported two symptoms after testing Session 18's `DocumentDetail.jsx`: (1) uploads occasionally forced a re-login after a few refreshes, (2) a browser console error `Refused to display 'http://localhost:8080/' in a frame because it set 'X-Frame-Options' to 'deny'`.
- **Investigated the FAILED-status document first** - user confirmed it was a test invoice deliberately crafted to fail OCR, so `DocumentStatus.FAILED` there is correct behavior (per Session 11's confidence-check logic), not a bug.
- **Root cause of forced re-login**: `apiFetch()` in `lib/api.js` threw a generic `Error` with no status code attached. `Dashboard.jsx` and `DocumentDetail.jsx` then caught *any* failure - a transient 500, a network blip, even a slow response while the backend was busy doing synchronous OCR/Gemini work right after an upload - and unconditionally treated it as an expired token: cleared it and redirected to `/login`. A momentary backend hiccup was silently logging the user out.
  - Fix: `apiFetch` now attaches `error.status = response.status` to thrown errors. `Dashboard.jsx` and `DocumentDetail.jsx` only clear the token and redirect on `error.status === 401`; any other failure now shows an inline error state instead (`Dashboard.jsx` gets a "Retry" button).
- **Root cause of the X-Frame-Options console error**: `DocumentDetail.jsx`'s `<iframe>` file preview (added in Session 18) loads `http://localhost:8080/uploads/...` inside a page served from `http://localhost:5173` - Spring Security's default `X-Frame-Options: DENY` header blocks this, and since the frontend and backend are different origins (different ports), `SAMEORIGIN` wouldn't have fixed it either.
  - Fix: `SecurityConfig.java` now disables `frameOptions` globally (`headers(headers -> headers.frameOptions(frame -> frame.disable()))`). Scoped reasoning: the backend serves no HTML of its own (pure REST API + static `/uploads` files), so there's no clickjacking surface to protect with that header here.
- Files touched: `frontend/src/lib/api.js`, `frontend/src/pages/Dashboard.jsx`, `frontend/src/pages/DocumentDetail.jsx`, `backend/src/main/java/com/docket/config/SecurityConfig.java`, `memory.md`.
- Tested/confirmed: `npm install && npm run build` succeeds cleanly for the frontend changes. **Backend change not compiled** - same sandbox limitation as prior sessions (no route to Maven Central here); the `frameOptions(frame -> frame.disable())` call matches Spring Security's standard documented idiom, so it should compile, but needs a real `./mvnw clean compile` to confirm.
- Still untested / follow-up: (1) User to run `./mvnw clean compile` (or let Docker's build stage do it) to confirm `SecurityConfig.java` compiles. (2) Rebuild both `backend` and `frontend` containers (`docker compose up -d --build backend frontend`) and re-verify: the iframe preview now loads instead of erroring, and a slow/failed request no longer forces a re-login. (3) Still worth keeping an eye out for whether re-logins recur even after this fix - if so, capture the actual HTTP status/response next time to rule out a genuine JWT/session issue.
- Next session should: Confirm both fixes work end-to-end in the browser, then proceed to Phase 5 (Summarization) as previously planned.

### Session 20 — 2026-08-15
- User reported a specific test invoice (a ReportLab-generated PDF, GST invoice) stayed at `PENDING` forever, while other uploads processed normally. Investigated across this session and the prior chat turns via Claude (not a live session tool here, but summarizing the full trail for continuity):
- **Wrong hypotheses ruled out first** (all independently verified in the running container, not assumed): Tesseract 5.5.0 binary present and working (`tesseract --version`); `tessdata` correctly populated (`eng.traineddata`, `osd.traineddata` both present at `TESSDATA_PREFIX`); `tesseract-ocr-eng` package actually installed despite `--no-install-recommends` in `backend/Dockerfile` (Ubuntu's `tesseract-ocr` apparently hard-depends on it on this base image, not just recommends); native `libtesseract.so.5` correctly registered via `ldconfig`. None of this was the problem — a useful reminder that a plausible-sounding native-library theory still needs log evidence, not just consistency with the symptom.
- **Real root cause, found via backend logs** (`docker compose logs backend`): `org.springframework.dao.DataIntegrityViolationException: ... ERROR: invalid byte sequence for encoding "UTF8": 0x00` when saving the `documents` row. This specific PDF's PDFBox text-layer extraction contains a literal NUL byte — plausibly from its unusual embedded TrueType font encoding (`FirstChar 0`, `ToUnicode`-CMap-only, no `/Encoding` entry — a ReportLab quirk, confirmed by inspecting the raw PDF object structure). Postgres `text` columns reject NUL bytes outright, always, regardless of encoding.
- **Compounding bug that made this invisible in the first place**: in the pre-existing code, `documentRepository.save(doc)` inside `processDocumentAsync` was called **outside** the method's `try/catch` block entirely — so this exception (and anything else at that specific line) had no error handling at all. The `@Async` thread died silently, the row was never persisted, and the document stayed `PENDING` forever with nothing in the logs to explain why.
- **Fixes applied** (delivered as two patches, since the user had already applied the first before the NUL-byte cause was found — see below):
  1. `DocumentProcessingService.processDocumentAsync`: broadened the OCR-stage catch from `Exception` to `Throwable` (defends against future native-library `Error`s too, even though that wasn't the actual cause here); wrapped `documentRepository.save(doc)` in its own try/catch with logging (this is what surfaced the real error above); wrapped the `extractionService.extractInvoiceFields(doc)` call (previously unguarded) so a failure there can't affect the already-saved document status; fixed `StorageService.getFile()` returning a non-null `File` for a path that doesn't exist (the old `savedFile != null` check downstream was dead code — now checks `.exists()`/`.isFile()`).
  2. `ExtractionService.extractInvoiceFields`: added a `catch (Throwable t)` fallback alongside the existing `GeminiException`/`Exception` handling; hardened `saveFailure()` itself so a failure to persist the failure reason is at least logged, not lost.
  3. `OcrService.extractText`: wraps Tesseract calls so any native `Error` gets converted to a checked `Exception`, so it can't skip *any* caller's ordinary `catch (Exception e)`.
  4. `GeminiClient.generateStructuredJson`: restores the interrupt flag on `InterruptedException` instead of silently swallowing it; added logging.
  5. New `config/AsyncConfig.java`: replaces the default unbounded `SimpleAsyncTaskExecutor` (spawns one new OS thread per upload, no cap) with a bounded `ThreadPoolTaskExecutor` (core 4 / max 16 / queue 100, `CallerRunsPolicy` for backpressure instead of dropping work), and registers a global `AsyncUncaughtExceptionHandler` as a last-resort log-only safety net.
  6. **The actual fix**: added `stripNulBytes()` in both `DocumentProcessingService` and `ExtractionService`, applied to `extracted_text`, Gemini's `fields_json`, and `failed_reason` before every save — NUL bytes are never meaningful in this data, so stripping is safe.
- Delivered as two separate patch files (chat-session artifacts, not committed here): `docket-silent-failure-fix.patch` (items 1-5) and a smaller incremental `docket-nul-byte-fix.patch` (item 6) generated after the user had already applied the first, since regenerating a full diff against the original source failed to apply cleanly on top of already-patched files. **Lesson for future patch-delivery sessions: track exactly which patch state the user's tree is in — don't regenerate a full-source diff once a partial patch may already be applied; diff against the known-current state instead.**
- Files touched (in the user's local tree via patches, not this sandbox's checkout — see caveat below): `backend/src/main/java/com/docket/service/DocumentProcessingService.java`, `backend/src/main/java/com/docket/service/ExtractionService.java`, `backend/src/main/java/com/docket/service/OcrService.java`, `backend/src/main/java/com/docket/service/StorageService.java`, `backend/src/main/java/com/docket/service/GeminiClient.java`, `backend/src/main/java/com/docket/config/AsyncConfig.java` (new), `memory.md`.
- Tested/confirmed: The NUL-byte root cause is confirmed directly from real backend log output the user pasted (`SQLState: 22021`, `invalid byte sequence for encoding "UTF8": 0x00`, `Failed to persist status for document id=33`) — this is not a hypothesis, it's the actual error. The surrounding Throwable-safety changes have NOT been compiled/run yet (same recurring sandbox limitation: no Maven Central access here).
- Still untested / follow-up: (1) User to apply both patches (or confirm the second one applied - last known status was troubleshooting a `git apply` failure, likely CRLF/whitespace, not yet confirmed resolved). (2) `./mvnw clean compile` / rebuild the backend image to confirm everything compiles. (3) Delete or reset the stuck document id=33 row (`DELETE FROM extractions WHERE document_id = 33; DELETE FROM documents WHERE id = 33;`) and re-upload the same GST invoice PDF to confirm the fix end-to-end - should now reach `PROCESSED` instead of staying `PENDING`. (4) No retry mechanism exists for documents already stuck at `PENDING` under the old code - each one needs manual deletion + re-upload, there's no background sweep/reprocess job. Worth considering as a small future addition if stuck rows recur for any other reason.
- Next session should: Confirm the patches applied and compiled cleanly, verify document 33's re-upload reaches `PROCESSED`, then return to Phase 5 (Summarization) which has been the deferred next step since Session 16.

### Session 21 — 2026-08-16
- Implemented Phase 5 (Summarization).
- Created V5__create_summaries_table.sql, Summary entity, and SummaryRepository.
- Added SummarizePrompt with JSON schema for a 3-5 sentence plain-English summary.
- Created SummarizeService to call Gemini API and save to the database. Integrated it into DocumentProcessingService.java to run for all document types after successful OCR.
- Added GET /api/documents/{id}/summary endpoint in DocumentController.
- Updated DocumentDetail.jsx frontend to fetch the summary and render it below the original file and extracted fields.
- Files touched: backend/src/main/resources/db/migration/V5__create_summaries_table.sql, backend/src/main/java/com/docket/entity/Summary.java, backend/src/main/java/com/docket/repository/SummaryRepository.java, backend/src/main/java/com/docket/dto/document/SummaryResponseDto.java, backend/src/main/java/com/docket/prompt/SummarizePrompt.java, backend/src/main/java/com/docket/service/SummarizeService.java, backend/src/main/java/com/docket/service/DocumentProcessingService.java, backend/src/main/java/com/docket/controller/DocumentController.java, frontend/src/pages/DocumentDetail.jsx.
- Tested/confirmed: Backend compiles cleanly (mvnw clean compile). Frontend compiles cleanly (pm run build). The logic correctly strips NUL bytes from the generated summary.
- Still untested / follow-up: Needs manual E2E check to see the summary card in the UI for a real document.
- Next session should: E2E test Phase 5 with Docker, then proceed to Phase 6.

### Session 22 — 2026-08-16
- Completed Phase 5 (Summarization) E2E verification:
  - Applied previous patches, rebuilt Docker images, and ran `docker compose up -d`.
  - Verified backend compiles and starts (no compilation errors, schema migrations run successfully).
  - Uploaded a new GST invoice PDF via the frontend.
  - Verified the document reached `PROCESSED` status with a valid Gemini summary.
  - Verified the summary appears correctly in the DocumentDetail UI.
- Next session should: proceed to Phase 6 (advanced search/RAG) as originally planned.

### Session 23 — 2026-08-16
- Implemented Phase 6 (Template Manager & Anomaly Flagging).
- Created V6__create_templates_and_anomalies_tables.sql migration for 	emplates and anomaly_flags tables.
- Built Template and AnomalyFlag entities and their repositories.
- Added AnomalyCheckPrompt to instruct Gemini to compare a new document against a template and return flagged anomalies as JSON.
- Created AnomalyService.java to perform the anomaly check for PROCESSED documents, and integrated it into DocumentProcessingService.java.
- Added TemplateController with POST /api/templates and GET /api/templates/{type}.
- Added GET /api/documents/{id}/anomalies to DocumentController.
- Frontend: Created TemplateManager.jsx to select the invoice template. Added a link to it from Dashboard.jsx.
- Frontend: Created AnomalyFlag.jsx component and updated DocumentDetail.jsx to fetch and render the anomalies.
- Tested/confirmed: Backend compiles cleanly (mvnw clean compile). Frontend compiles cleanly (
pm run build).
- Next session should: Perform E2E manual test of Phase 6 by uploading a baseline invoice, setting it as a template, uploading a modified invoice, and checking for anomaly flags.
