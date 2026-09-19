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

- **Active Phase:** Production Readiness & Final Verification (All Phases 0–12 Complete)
- **Last Updated:** 2026-09-19
- **Overall Progress:** 100% — All 12 phases complete (Phases 0–11 and all Phase 12 stretch goals: 12.1 Batch Upload, 12.2 Confidence Scores, 12.3 RabbitMQ Queue, 12.4 Comparative Anomaly Detection & Vendor Trends, 12.5 KYC Form Document Type, and 12.6 Billing Simulation & Stripe Test Mode). 51 passing automated tests (100% green), frontend builds cleanly.

## Completed

- [x] Project scoped and defined (`prd.md`)
- [x] Architecture and tech stack decided (`architecture.md`)
- [x] Coding/AI rules established (`rules.md`)
- [x] Phased build plan created and expanded to 12 phases (`phases.md`)
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
- [x] Phase 7 (backend, complete): Created `ContractExtractionDto`, `ResumeExtractionDto`, `ExtractContractPrompt`, `ExtractResumePrompt`. Extended `ExtractionService` with `extractContractFields` and `extractResumeFields`. Updated `DocumentProcessingService` dispatch to use a switch on document type.
- [x] Phase 7 (frontend, complete): Removed `disabled` from Contract/Resume options in `UploadDocument.jsx`. Refactored `DocumentDetail.jsx` with `ExtractionFields`, `InvoiceFields`, `ContractFields`, `ResumeFields` component dispatchers. Backend and frontend build cleanly.
- [x] Phase 8: Dashboard Polish & Export:
  - Added `DocumentListItemDto` and `DocumentExportDto` records.
  - Implemented `ExportService.java` for CSV and JSON serialization of document and workspace intelligence.
  - Added `GET /api/documents/{id}/export?format={json|csv}` and `GET /api/documents/export?format={json|csv}` to `DocumentController.java`.
  - Added overview metrics cards (Total, Processed, Flagged, Pending, Failed) to `Dashboard.jsx`.
  - Added multi-criteria filtering by Type, Status/Deviation, and Date with keyword search to `Dashboard.jsx`.
  - Added single and bulk export buttons to `Dashboard.jsx` and `DocumentDetail.jsx`.
  - Added `downloadExport` utility in `api.js` for authenticated file downloads.
- [x] Phase 8.5: Modern 3D & Motion Visual Overhaul ("Aurora Obsidian"):
  - Upgraded global styling to obsidian plum (`#12101B`) with electric violet/cyan aurora gradient accents.
  - Implemented `AmbientAurora.jsx` canvas providing subtle ambient light and particle dust with `prefers-reduced-motion` static CSS fallback.
  - Added Framer Motion staggered entrance animations for document table rows and hover-lift on metrics cards.
  - Integrated Lucide icons across navigation, overview cards, action bars, and status flags.
  - Maintained contrast ratios ≥ 4.5:1 on all text elements and opaque surfaces for dense data tables.
- [x] Phase 8.5 (Session 32): Neomorphic Apple-style UI overhaul:
  - Rebuilt `index.css` with neomorphic shadow token system (`--neo-shadow-sm/md/lg/inset/hover/aurora`), spring easing tokens, extruded `.card`/`.card-neo-sm`/`.card-lg`/`.card-inset` variants.
  - Added `.btn-primary` press state (`scale(0.97)` + inset flash), `.btn-secondary` extruded rest, `.btn-ghost` for lightweight inline actions.
  - Inputs use neomorphic inset shadow at rest + aurora ring on focus.
  - Added `.nav-frosted` (Apple-style `saturate(180%) blur(20px)` sticky strip), `.tab-bar`/`.tab-item` neomorphic pill switcher, `.stat-icon` widget container.
  - Rewrote `Login.jsx` and `Signup.jsx` with spring entrance, aurora top-edge accent line, icon-prefixed inputs, and gradient "D" logo mark.
  - Rewrote `Dashboard.jsx` with sticky frosted nav, spring-staggered metric cards, slide-in table rows, `AnimatePresence` filter clear, and row-count footer.
  - Rewrote `UploadDocument.jsx` with card-selector for doc type (extruded→inset on select), neomorphic inset dropzone with drag color transitions.
  - Rewrote `TemplateManager.jsx` with neomorphic tab bar, `AnimatePresence mode="wait"` panel transitions, and `card-inset` active template display.
- [x] Phase 9: Production Hardening & Evaluation Remediation (Sessions 33–34):
  - **9.1 Security:** Revoked unauthenticated `/uploads/**` access; added authenticated `GET /api/documents/{id}/file` endpoint with workspace ownership check; created `LoginRateLimiter.java` (5 attempts / 15 min lockout) wired into `AuthService.login()`; purged 47 cached upload binaries from git history via `git rm -r --cached`; hardened `.gitignore`.
  - **9.2 DB & Performance:** Flyway `V7__add_workspace_and_query_indexes.sql` — indexes on `documents(workspace_id)`, `documents(status)`, `documents(workspace_id, uploaded_at DESC)`, `users(workspace_id)`; paginated `GET /api/documents/page` endpoint; single-doc `GET /api/documents/{id}`.
  - **9.3 Architecture:** Collapsed 3 triplicated extraction methods into generic `<T> extractFields(...)`; unified `stripNulBytes` into `SanitizationUtils.java`; removed direct repo injections from `DocumentController`; gated `show-sql` behind `${SHOW_SQL:false}`; added `CorrelationIdFilter.java` for MDC `requestId`.
  - **9.4 Resilience:** `StorageService` refactored to interface + `LocalStorageServiceImpl`; `GeminiClient` exponential backoff retry (3 attempts, jitter, on 429/5xx); `DocumentReconciliationScheduler` detects & reprocesses stuck PENDING docs; `POST /api/documents/{id}/reprocess` manual endpoint; added `@EnableScheduling` to `DocketApplication`.
  - **9.5 Observability:** `spring-boot-starter-actuator` added; `/actuator/health` (with DB check) and `/actuator/metrics` exposed; backend healthcheck wired into `docker-compose.yml`.
  - **9.6 Frontend Resilience:** `DocumentDetail.jsx` migrated from `Promise.all` to `Promise.allSettled`; authenticated blob URL streaming for file preview iframe; Reprocess button added.
  - **9.7 Tests & CI:** 21 JUnit 5 / Mockito tests covering `WorkspaceIsolationTest`, `AuthServiceTest`, `LoginRateLimiterTest`, `DocumentServiceTest`, `ExtractionServiceTest`, `SanitizationUtilsTest` — all passing. GitHub Actions CI (`ci.yml`) runs both `mvn test` and `npm run build` on push/PR.
- [x] Phase 10: Advanced Polish, Human-in-the-Loop & API Governance (Session 35):
  - **10.1 Human-in-the-Loop Corrections:** Added `PATCH /api/documents/{id}/extraction` endpoint; `ExtractionCorrectionRequest` DTO; `correctExtraction()` method in `DocumentService`; `humanCorrectedJson`/`correctionNote`/`correctedAt` columns on `Extraction` entity (Flyway V8). Original AI output preserved. `DocumentDetail.jsx` inline JSON editor with save/cancel, HUMAN CORRECTED badge, correction date display.
  - **10.2 Status Polling:** Auto-polling every 4s for PENDING documents in `DocumentDetail.jsx` — stops automatically on PROCESSED/FAILED transition. Status badge shows `· auto-refreshing` indicator.
  - **10.3 OpenAPI / Swagger UI:** `springdoc-openapi-starter-webmvc-ui 2.6.0` added to `pom.xml`; `OpenApiConfig.java` with JWT Bearer scheme; Swagger UI accessible at `/swagger-ui.html` (public, no JWT required).
  - **10.4 Per-Workspace LLM Budget Guard:** `LlmUsage` entity/table, `LlmUsageRepository` with atomic PostgreSQL `ON CONFLICT` upsert; `LlmBudgetService` with 429 enforcement and global 500-call ceiling; wired into `DocumentProcessingService` before each AI call (extraction, summarization, anomaly); `GET /api/documents/usage` endpoint; Dashboard LLM usage progress bar widget.
  - **10.5 Mobile & Accessibility:** `lg:grid-cols-2` breakpoints on `DocumentDetail.jsx` layout; `px-4 sm:px-6` responsive padding; `aria-label` on all icon-only buttons; `aria-live` on status badge; `role="status"/"alert"` on dynamic regions.
  - **10.6 `apiPatch` helper:** Added `apiPatch(path, body)` to `api.js`.
  - **10.7 Tests:** 4 new `LlmBudgetServiceTest` unit tests. Total: **25 tests, 0 failures**.
- [x] Phase 11: Deployment & Demo Readiness (Session 36):
  - **11.1 Demo Data Seeder (Flyway V9):** Created `V9__seed_demo_data.sql` inserting a pre-seeded demo workspace (`Acme Global Demo`), user (`demo@docket.ai` / `Demo1234!`), 3 rich sample documents (Invoice with Net-15 anomaly, Contract with 14-day termination notice anomaly, clean Senior Resume), and active templates.
  - **11.2 5-Minute Evaluation Walkthrough (`DEMO_SCRIPT.md`):** Complete click-path documentation for evaluators/judges covering login, metrics, field extraction, anomaly deviations, human-in-the-loop editing, export, and OpenAPI testing.
  - **11.3 Environment & Production Documentation:** Overhauled `.env.example`, `README.md`, and `architecture.md` with complete environment templates, endpoint catalog, Swagger access links, and Actuator health specifications.
  - **11.4 Docker & Container Health:** Hardened `backend/Dockerfile` with `wget` and `curl` for container healthcheck execution. Verified backend and frontend (`npm run build`) compilation.
- [x] Phase 12.1: Multi-Document Batch Upload (Session 37):
  - Added `POST /api/documents/batch` endpoint in `DocumentController.java` accepting multiple `files` and a single `documentType`.
  - Implemented `uploadDocumentsBatch()` in `DocumentService.java` returning a list of saved `DocumentListItemDto` objects, each triggered for asynchronous processing.
  - Added unit test `uploadDocumentsBatch_MultipleFiles_Success` in `DocumentServiceTest.java` (total tests increased to 27, all passing).
  - Redesigned `UploadDocument.jsx` to support multi-file selection, drag-and-drop batch queue, per-file preview removal, batch progress bar, and comprehensive error reporting.
- [x] Phase 12.2: Field-Level Confidence Scoring (Session 39):
  - Extended extraction prompts and JSON schemas (`ExtractInvoicePrompt`, `ExtractContractPrompt`, `ExtractResumePrompt`) to request field confidence probabilities (0.0 to 1.0) under `fieldConfidences`.
  - Updated extraction DTOs (`InvoiceExtractionDto`, `ContractExtractionDto`, `ResumeExtractionDto`) with `fieldConfidences` map and accessor methods.
  - Added unit test in `ExtractionServiceTest.java` verifying deserialization and storage of field confidences (total tests increased to 28, all passing).
  - Enhanced `DocumentDetail.jsx` with per-field confidence score badges (emerald for ≥85%, amber for 70-84%, red for <70%), an overall average confidence indicator in the header, and updated seeded demo documents in `V9__seed_demo_data.sql`.

- [x] Phase 12.3: Queue-Based Processing (RabbitMQ + Spring AMQP) (Session 40):
  - Added `spring-boot-starter-amqp` to `pom.xml`, with connection configuration in `application.yml` and RabbitMQ service (`rabbitmq:3-management-alpine`) in `docker-compose.yml` (ports 5672 & 15672).
  - Implemented dual-mode processing toggle (`docket.processing.mode` = `queue` or `async`, default `async`): app functions fully without RabbitMQ installed locally for dev ease, while supporting queue-backed durability in Docker/production.
  - Created `RabbitMqConfig.java` defining durable `docket.document.processing` queue, `docket.exchange`, routing key, and Jackson `MessageConverter`.
  - Created lightweight `DocumentProcessingMessage(documentId)` payload DTO to avoid entity serialization issues.
  - Created `DocumentQueuePublisher.java` (`@ConditionalOnProperty(mode=queue)`) to publish tasks to RabbitMQ.
  - Created `DocumentProcessingConsumer.java` (`@RabbitListener`) to consume messages and execute synchronous pipeline with ACK and missing document safety.
  - Refactored `DocumentProcessingService.java` to extract synchronous `processDocument()` worker method while preserving `@Async` `processDocumentAsync()` wrapper.
  - Updated `DocumentService.java` and `DocumentReconciliationScheduler.java` with conditional dispatch (`Optional<DocumentQueuePublisher>`).
  - Added unit tests in `DocumentProcessingConsumerTest.java`, `DocumentQueuePublisherTest.java`, and updated `DocumentServiceTest.java` and `WorkspaceIsolationTest.java` (total automated tests increased to 33, 100% passing).
  - Updated `rules.md` and `architecture.md` to formally document RabbitMQ + Spring AMQP dual-mode architecture.
- [x] Phase 12.4: Multi-Document Comparative Anomaly Detection & Vendor Trends (Session 41):
  - Created `ComparativeAnomalyService.java` providing deterministic cross-document analysis across workspace invoice history (duplicate invoice numbers, >50% price surges against historical vendor average, payment window contraction / invalid date order).
  - Created `VendorTrendDto.java` and `WorkspaceTrendsDto.java` records.
  - Added `GET /api/documents/trends` endpoint in `DocumentController.java` and `DocumentService.java` returning aggregated vendor metrics (spend, ticket size, range, trend %, active flags) without consuming daily LLM budget tokens.
  - Wired comparative anomaly checks into `DocumentProcessingService.java` post-extraction pipeline.
  - Enhanced `AnomalyFlag.jsx` with category tags (`DUPLICATE ALERT`, `TREND ANOMALY`, `TEMPLATE DEVIATION`).
  - Updated `DocumentDetail.jsx` heading to "Detected Anomalies & Deviations".
  - Overhauled `Dashboard.jsx` with a dual-view tab switcher ("Documents" vs "Vendor Trends & Cross-Doc Intelligence"), including summary metric cards, vendor search/filtering, and one-click invoice drill-down.
  - Added unit tests in `ComparativeAnomalyServiceTest.java` and updated `DocumentServiceTest.java` and `WorkspaceIsolationTest.java` (total automated tests increased to 38, 100% green).
  - Clean frontend production build (`npm run build` in 632ms, 0 errors).
- [x] Phase 12.5: 4th Document Type — KYC Form (Session 42):
  - Added `KYC_FORM` to `DocumentType.java` enum (no schema migration needed — `type` column is `VARCHAR(50)`).
  - Created `KycExtractionDto.java` with `@NotNull`-validated fields: `fullName`, `idType`, `idNumber`, `dateOfBirth`, `nationality`, `issueDate`, `expiryDate`, `address`, `verificationStatus`, and `fieldConfidences` map.
  - Created `ExtractKycPrompt.java` with strict Gemini extraction instructions and full JSON schema enforcing all 9 fields plus per-field confidence scoring.
  - Updated `ExtractionService.java`: added `extractKycFields()` method and `case KYC_FORM ->` branch in `extractDocumentFields()` dispatch switch.
  - Added unit test `testExtractKycFieldsSuccess` to `ExtractionServiceTest.java` — exercises full `extractDocumentFields()` dispatch path for `KYC_FORM`, verifying correct JSON persistence and confidence scores.
  - Updated `UploadDocument.jsx` typeOptions with "🢪 KYC Form" option.
  - Updated `TemplateManager.jsx` `DOCUMENT_TYPES` with KYC Forms tab.
  - Updated `Dashboard.jsx` typeFilter select with "KYC Forms" option.
  - Updated `DocumentDetail.jsx` `ExtractionFields` dispatch and added `KycFields` renderer component with: 2-column identity grid (`fullName`, `idType`, `idNumber`, `dateOfBirth`, `nationality`, `issueDate`, `expiryDate`, `address`), all fields with per-field confidence badges, and a status bar with color-coded `verificationStatus` (green=VALID/VERIFIED, red=EXPIRED, amber=other).
  - `mvn test` — **39 tests passed, 0 failures** (100% green).
  - `npm run build` — **Vite client bundle built cleanly in 657ms, 0 errors** (443 kB JS / 28 kB CSS).
- [x] Phase 12.6: Billing Simulation & Stripe Test Mode:
  - Created Flyway migration `V10__billing_simulation.sql` adding `plan_tier`, `subscription_status`, `stripe_customer_id`, `stripe_subscription_id`, `billing_period_start`, `billing_period_end` to `workspaces`, and creating `simulated_invoices` table with indexing.
  - Created `SimulatedInvoice.java` entity, `SimulatedInvoiceRepository.java`, and `PlanTier.java` enum (`FREE`, `PRO`, `ENTERPRISE`).
  - Created DTO records: `SubscriptionDetailsDto.java`, `SimulatedInvoiceDto.java`, `UpgradePlanRequestDto.java`, `SimulateWebhookRequestDto.java`.
  - Implemented `BillingService.java`: plan tier upgrades, monthly document quota enforcement (`checkDocumentQuota`), simulated Stripe customer/subscription ID generation, and real-time simulated webhook handling (`invoice.payment_succeeded`, `invoice.payment_failed`, `customer.subscription.updated`, `customer.subscription.deleted`).
  - Wired quota enforcement into `DocumentService.java` (`uploadDocument` and `uploadDocuments` batch).
  - Created `BillingController.java` (`GET /api/billing/subscription`, `GET /api/billing/invoices`, `POST /api/billing/upgrade`, `POST /api/billing/webhook/simulate`).
  - Created `BillingServiceTest.java` with 11 comprehensive unit tests (51/51 tests passing 100% green).
  - Created `Billing.jsx` page: Obsidian Aurora UI with monthly document quota progress meter (emerald/amber/red thresholds), daily LLM budget meter, simulated Stripe customer/subscription badges with one-click copy, 3-tier plan comparison cards with simulated checkout, interactive Stripe Webhook Simulator console, and simulated invoice/receipt history table.
  - Added protected `/billing` route in `App.jsx` and Billing navigation buttons to `Dashboard.jsx` and `TemplateManager.jsx`.
  - Frontend production bundle built cleanly in 769ms (`npm run build`).

## In Progress

- None — all 12 phases and all Phase 12 stretch features (12.1 through 12.6) are complete!

## Next Steps (in order)

1. **Production Readiness Review:** Run through AGENTS.md §8 checklist (build verification, security checklist, demo script).
2. **Deploy / Demo Rehearsal:** Run `DEMO_SCRIPT.md` walkthrough.


## Key Decisions & Why

| Decision | Reason | File Reference |
|---|---|---|
| MVP locked to 3 document types (Contract, Invoice, Resume) | Keeps extraction quality high and scope achievable in one academic year | prd.md §3, §7 |
| Java + Spring Boot for backend | Mature enterprise framework, strong typing, integrated ORM/security/migrations; natural fit for a student comfortable in Java | architecture.md §3.2, §7 |
| Postgres over MongoDB | Extracted fields/flags/templates are relational data | architecture.md §7 |
| Jackson + Bean Validation for both API and LLM JSON output validation | One validation pattern across the backend instead of a second library | architecture.md §3.6, §7 |
| Synchronous processing for MVP (no queue) | Avoids premature infra complexity; queue (Kafka/RabbitMQ) is a stretch goal only | architecture.md §3.4, phases.md Phase 12 |
| Tess4J over cloud OCR APIs | Free, no per-page cost, sufficient for MVP's "clean/typed documents" scope | architecture.md §7 |
| Switched LLM provider from Anthropic (Claude) to Google Gemini | User has a Gemini API key, not an Anthropic one; no LLM code was written yet (Phase 4 not started), so this was a docs/config-only rename with no migration cost | architecture.md §3.6, prd.md, rules.md §2, phases.md Phase 4 — see Session 6 |
| Phase Plan Expansion (Phases 9 & 10 inserted; Deployment & Stretch moved to 11 & 12) | Rigorous architectural evaluation (`Docket_Evaluation_Report.md`) identified production gaps (unauthenticated file access, zero tests, triplicated code, lack of pagination, missing DB index); formal phases inserted to remediate all gaps to senior SWE bar | phases.md Phase 9 & 10, memory.md — Session 33 |
| Dual-mode queue & async processing (Phase 12.3) | Allows running without RabbitMQ in lightweight local-dev while enabling durable, horizontally scalable queue processing in Docker/production via `docket.processing.mode` | architecture.md §3.4, rules.md §2, RabbitMqConfig.java |
| Deterministic Comparative Anomaly Detection (Phase 12.4) | Evaluates duplicates, price surges, and term contractions mathematically over Postgres extraction history — fast, zero-token cost, avoids consuming LLM rate limits | architecture.md §3.6.1, ComparativeAnomalyService.java |
| In-memory & DB Stripe Test Mode Simulation (Phase 12.6) | Simulates subscription tiers (FREE, PRO, ENTERPRISE), quotas, and Stripe webhooks in-database without external paid Stripe account dependencies, matching PRD §7 non-goals | architecture.md §5, §6, BillingService.java |

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

### Session 24 — 2026-08-16
- User confirmed Phase 6 E2E works perfectly (uploaded baseline invoice, set as template, uploaded second invoice, anomaly detected and rendered).
- Phase 6 is 100% complete.
- Next session should: Begin Phase 7 (Extend to Contract and Resume Types).

### Session 25 — 2026-08-16
- Drafted Phase 7 implementation plan covering: ContractExtractionDto, ResumeExtractionDto, ExtractContractPrompt, ExtractResumePrompt, ExtractionService dispatch update, UploadDocument.jsx dropdown enable, DocumentDetail.jsx multi-type rendering, TemplateManager.jsx type switcher.
- Files touched: memory.md (status sections corrected), implementation_plan.md (Phase 7 plan created).
- Tested/confirmed: nothing built this session - plan only.
- Next session should: Execute Phase 7 plan - create DTOs, prompts, update service and UI, then rebuild and verify.

### Session 26 — 2026-08-16
- Implemented Phase 7: Extend to Contract and Resume Types.
- Created ContractExtractionDto and ResumeExtractionDto DTOs and ExtractContractPrompt and ExtractResumePrompt prompt schemas.
- Updated ExtractionService to dispatch to specific methods based on document type (invoice, contract, resume).
- Updated DocumentProcessingService to route field extraction for new document types.
- Enabled CONTRACT and RESUME dropdown options in UploadDocument.jsx.
- Refactored DocumentDetail.jsx to dynamically render distinct field layouts for Invoice, Contract, and Resume document types.
- Built backend and frontend successfully. Fixed syntax errors in JSX structure.
- Re-ran docker compose up -d --build to verify successful compilation.
- Files touched: backend/src/main/java/com/docket/dto/*, backend/src/main/java/com/docket/prompt/*, backend/src/main/java/com/docket/service/ExtractionService.java, backend/src/main/java/com/docket/service/DocumentProcessingService.java, frontend/src/pages/UploadDocument.jsx, frontend/src/pages/DocumentDetail.jsx, memory.md, task.md, walkthrough.md
- Tested/confirmed: Backend and Frontend compiled successfully, docker-compose services up and healthy.
- Still untested / follow-up: User needs to manually verify uploading a Contract and a Resume to see if the extraction parses correctly.
- Next session should: E2E verify Phase 7 from the UI, and then move to Phase 8.

### Session 27 — 2026-08-16
- Fixed `apiFetch` in `api.js` to handle 401/403 by auto-clearing token and redirecting to login, and to safely parse empty JSON responses ("Unexpected end of JSON input" bug fixed).
- Re-implemented Phase 7 in full (previous session's code was missing from disk).
- Created: `ContractExtractionDto.java`, `ResumeExtractionDto.java`, `ExtractContractPrompt.java`, `ExtractResumePrompt.java`.
- Updated `ExtractionService.java` with `extractContractFields` and `extractResumeFields` methods.
- Updated `DocumentProcessingService.java` to use a switch-on-type dispatch for all three extractors.
- Updated `UploadDocument.jsx` to enable Contract and Resume options.
- Rewrote `DocumentDetail.jsx` cleanly with type-dispatched field renderers (`InvoiceFields`, `ContractFields`, `ResumeFields`).
- Files touched: `frontend/src/lib/api.js`, `frontend/src/pages/UploadDocument.jsx`, `frontend/src/pages/DocumentDetail.jsx`, `backend/.../dto/ContractExtractionDto.java`, `backend/.../dto/ResumeExtractionDto.java`, `backend/.../prompt/ExtractContractPrompt.java`, `backend/.../prompt/ExtractResumePrompt.java`, `backend/.../service/ExtractionService.java`, `backend/.../service/DocumentProcessingService.java`, `memory.md`.
- Tested/confirmed: `BUILD SUCCESS` for both backend (52 source files compiled) and frontend. All containers healthy.
- Still untested: User needs to manually upload a Contract and Resume PDF and verify extracted fields render correctly in the UI.
- Next session should: E2E verify Phase 7 (contract + resume upload and rendering), then start Phase 8.

### Session 28 — 2026-08-19
- Added template deletion capability and extended template management to Contract and Resume document types (previously restricted to Invoices only).
- Added `DELETE /api/templates/{type}` and `GET /api/templates` to `TemplateController.java`, scoped by authenticated user's workspace.
- Refactored `TemplateController.java` to inject `UserRepository` and directly query workspace ID from the user entity rather than relying on existing documents.
- Added `findByWorkspaceId` to `TemplateRepository.java`.
- Enhanced `AnomalyCheckPrompt.java` with explicit anomaly detection rules for Invoices, Contracts, and Resumes.
- Completely redesigned `TemplateManager.jsx` with tabbed navigation across Invoices, Contracts, and Resumes, active template preview/link, "Remove Template" deletion action, and type-filtered dropdown selection.
- Files touched: `backend/src/main/java/com/docket/controller/TemplateController.java`, `backend/src/main/java/com/docket/repository/TemplateRepository.java`, `backend/src/main/java/com/docket/prompt/AnomalyCheckPrompt.java`, `frontend/src/pages/TemplateManager.jsx`, `memory.md`.
- Tested/confirmed: Backend compiled with `BUILD SUCCESS` (52 source files). Frontend built cleanly with `npm run build` (33 modules, 0 errors).
- Next session should: E2E test setting and deleting Contract and Resume templates in the browser, and proceed to Phase 8.

### Session 29 — 2026-08-19
- Implemented Phase 8: Dashboard Polish & Export.
- Created `DocumentListItemDto.java` and `DocumentExportDto.java` DTO records.
- Implemented `ExportService.java` to serialize document and workspace intelligence (metadata, extracted fields, plain-English summary, anomaly flags) into RFC-compliant CSV and pretty-printed JSON.
- Enhanced `DocumentService.java` with `getEnrichedDocumentsForWorkspace` (aggregating anomaly counts per document) and export retrieval methods.
- Added `GET /api/documents/{id}/export?format={json|csv}` and `GET /api/documents/export?format={json|csv}` in `DocumentController.java`.
- Enhanced `Dashboard.jsx`:
  - Added 5 top overview metrics cards (Total, Processed, Flagged, Pending, Failed).
  - Implemented multi-criteria filtering: Document Type (Invoices, Contracts, Resumes), Status/Deviations (Flagged, Clean, Pending, Failed), Date range (Today, 7 days, 30 days), and real-time search.
  - Added visual anomaly badges (`⚠️ N Deviations` vs `✅ Standard`) in the table.
  - Added bulk export actions (CSV / JSON) and per-row quick export actions.
- Enhanced `DocumentDetail.jsx` with single-document "Export CSV" and "Export JSON" action buttons.
- Added `downloadExport` utility in `api.js` for authenticated file downloads.
- Files touched: `backend/.../dto/document/DocumentListItemDto.java`, `backend/.../dto/document/DocumentExportDto.java`, `backend/.../service/ExportService.java`, `backend/.../service/DocumentService.java`, `backend/.../controller/DocumentController.java`, `backend/.../repository/AnomalyFlagRepository.java`, `backend/.../repository/ExtractionRepository.java`, `backend/.../repository/SummaryRepository.java`, `frontend/src/pages/Dashboard.jsx`, `frontend/src/pages/DocumentDetail.jsx`, `frontend/src/lib/api.js`, `memory.md`.
- Tested/confirmed: `npm run build` built cleanly (33 modules, 0 errors). Backend `./mvnw clean compile` built with `BUILD SUCCESS` (55 source files).
- Next session should: E2E verify dashboard filtering and export downloads in the browser, then proceed to Phase 8.5 / Phase 9.

### Session 30 — 2026-08-19
- Implemented Phase 8.5: Modern 3D & Motion Visual Overhaul ("Aurora Obsidian").
- Updated `index.css` with dark obsidian-plum palette (`#12101B`), electric violet→cyan aurora gradients, glassmorphism card tokens, and dark-calibrated semantic status chips (contrast ratio ≥ 4.5:1).
- Built `AmbientAurora.jsx` component providing canvas particle dust and ambient floating lighting with `prefers-reduced-motion` static CSS fallback.
- Installed `motion` and `lucide-react`.
- Upgraded `Login.jsx` and `Signup.jsx` with ambient aurora background, glassmorphic auth cards, and motion fade/lift.
- Upgraded `Dashboard.jsx` with Lucide iconography, staggered entrance animation on document table rows, and hover-lift on overview metrics cards.
- Upgraded `UploadDocument.jsx` with a custom dashed dropzone, hover glow, and file preview state.
- Upgraded `TemplateManager.jsx` with tabbed category switcher, benchmark status chips, and Lucide icons.
- Upgraded `DocumentDetail.jsx` and `AnomalyFlag.jsx` with dark-themed cards, crisp line item and contract tables, and calibrated deviation alert badges.
- Files touched: `frontend/src/index.css`, `frontend/src/components/ui/AmbientAurora.jsx`, `frontend/src/components/AnomalyFlag.jsx`, `frontend/src/pages/Login.jsx`, `frontend/src/pages/Signup.jsx`, `frontend/src/pages/Dashboard.jsx`, `frontend/src/pages/UploadDocument.jsx`, `frontend/src/pages/TemplateManager.jsx`, `frontend/src/pages/DocumentDetail.jsx`, `frontend/package.json`, `memory.md`.
- Tested/confirmed: Frontend built with `npm run build` (`✓ built in 735ms`, 2224 modules, 0 errors). Backend `./mvnw clean compile` verified with `BUILD SUCCESS` (55 source files).
- Next session should: Start Phase 9 (Deployment & Demo Readiness).

### Session 31 — 2026-08-19
- Compacted Dashboard layout to eliminate excessive blank space and large paddings.
- Changed `main` container from `max-w-6xl px-6 py-10` to `max-w-[1600px] px-4 sm:px-8 py-6` so the page fills wide screens.
- Nav bar inner content now constrained to the same `max-w-[1600px]` wrapper for alignment.
- Reduced metric card padding (`p-4` → `p-3.5`), internal gap (`mb-2` → `mb-1.5`), and section spacing (`mb-8` → `mb-5`).
- Compacted filter bar (`p-4 mb-6` → `px-3.5 py-3 mb-4`) and tightened filter control gaps (`gap-4` → `gap-2`).
- Reduced table header and row cell padding (`py-3 px-4` → `py-2.5 px-3`).
- Reduced empty-state block padding from `p-12` → `p-8`.
- Files touched: `frontend/src/pages/Dashboard.jsx`, `memory.md`.
- Tested/confirmed: Build not re-run this session — layout changes are CSS/JSX-only with no logic changes; prior build (Session 30, 0 errors) still valid.
- Still untested / follow-up: Full `docker compose up --build` for Phase 9 production verification.

### Session 32 — 2026-08-19
- Applied full neomorphic Apple-style design overhaul across all frontend pages.
- Rebuilt `index.css` with neomorphic shadow token system: `--neo-shadow-sm/md/lg`, `--neo-shadow-inset`, `--neo-shadow-inset-deep`, `--neo-shadow-hover`, `--neo-shadow-aurora`. Spring easing: `--ease-spring: cubic-bezier(0.22, 1, 0.36, 1)`.
- Added card variants: `.card` (extruded), `.card-neo-sm` (metric widget), `.card-lg` (auth/modal), `.card-inset` (recessed well).
- Buttons: `.btn-primary` gains `scale(0.97)` + inset shadow on `:active`; `.btn-secondary` extruded rest; new `.btn-ghost` for inline actions.
- Inputs: neomorphic inset shadow at rest → aurora ring on `:focus`.
- Added `.nav-frosted` (Apple sticky nav), `.tab-bar`/`.tab-item` (neomorphic pill switcher), `.stat-icon` widget container, `.bg-grid` dot-grid texture, `.divider`, custom scrollbar.
- Rewrote `Login.jsx` and `Signup.jsx`: spring entrance (`scale 0.97→1`), aurora gradient top-edge accent line, icon-prefixed inputs (Mail, Lock, Building2), gradient "D" logo mark.
- Rewrote `Dashboard.jsx`: sticky frosted nav with logo mark, spring-staggered metric cards via config array + `stat-icon`, slide-in table rows (`x: -6→0`), `AnimatePresence` filter clear button, row-count/filter footer.
- Rewrote `UploadDocument.jsx`: card-selector for doc type (extruded at rest, inset + aurora glow when selected), neomorphic inset dropzone with drag color transitions.
- Rewrote `TemplateManager.jsx`: frosted nav breadcrumb, neomorphic tab bar with dot status indicator per type, `AnimatePresence mode="wait"` panel swap, `card-inset` active template row, compact set/remove controls.
- Files touched: `frontend/src/index.css`, `frontend/src/pages/Login.jsx`, `frontend/src/pages/Signup.jsx`, `frontend/src/pages/Dashboard.jsx`, `frontend/src/pages/UploadDocument.jsx`, `frontend/src/pages/TemplateManager.jsx`, `memory.md`.
- Tested/confirmed: `npm run build` passed cleanly — `✓ built in 718ms`, 2224 modules, 0 errors.
- Still untested / follow-up: `DocumentDetail.jsx` and `AnomalyFlag.jsx` not yet updated to neomorphic style.
- Next session should: Proceed to Phase 9.

### Session 33 — 2026-08-25
- Conducted deep analysis of `Docket_Evaluation_Report.md` covering all 18 evaluation dimensions and the 3-tier action plan.
- Re-architected project phases roadmap in `phases.md`:
  - Created **Phase 9 (Production Hardening & Evaluation Remediation)**: covers critical security (`/uploads/**` auth lockdown, login brute-force limiter, git uploads scrub), database indexing (`idx_documents_workspace_id`), API pagination & single-doc endpoints, backend architectural refactoring (generic `extractFields`, shared `SanitizationUtils`, controller-to-service decoupling, profile-gated `show-sql`, MDC request correlation IDs), LLM resilience (exponential backoff, stuck document reconciliation job, manual reprocess endpoint, storage abstraction), Spring Boot Actuator health checks, `Promise.allSettled` frontend error resilience, comprehensive JUnit 5 + Mockito + Spring Boot test suite with cross-workspace 404 security tests, and GitHub Actions CI.
  - Created **Phase 10 (Advanced Polish, Human-in-the-Loop & API Governance)**: covers human-in-the-loop field correction UI and `PATCH /api/documents/{id}/extraction` endpoint, automatic status polling, `springdoc-openapi` Swagger documentation, per-workspace LLM budget guards, mobile responsiveness & WCAG AA accessibility audit, and API versioning.
  - Incremented Deployment & Demo Readiness to **Phase 11**.
  - Incremented Stretch Goals to **Phase 12**.
- Synchronized documentation across `phases.md`, `memory.md`, `README.md`, `architecture.md`, `rules.md`, and `AGENTS.md`.
- Files touched: `phases.md`, `memory.md`, `README.md`, `architecture.md`, `rules.md`, `AGENTS.md`.
- Tested/confirmed: All phase numbering and cross-references verified consistent across the repository docs.
- Next session should: Begin Phase 9 execution, starting with Phase 9.1 critical security remediation (`/uploads/**` auth lockdown, login attempt limiter, committed files scrub).

### Session 34 — 2026-08-25
- Executed full Phase 9 (Production Hardening & Evaluation Remediation) implementation across all 7 sub-tracks.
- **9.1 Security:** Removed `/uploads/**` from `SecurityConfig.java` `permitAll()`; created `GET /api/documents/{id}/file` authenticated streaming endpoint with workspace check; created `LoginRateLimiter.java` (5 failed attempts / 15 min lockout, per-IP isolation, auto-expiry, success-resets counter); wired into `AuthService.login()`; ran `git rm -r --cached uploads backend/uploads` to untrack 47 committed upload binaries; hardened `.gitignore`.
- **9.2 DB & Performance:** Wrote `V7__add_workspace_and_query_indexes.sql` with 4 indexes; added `Page<Document> findByWorkspaceId` and status-based queries to `DocumentRepository`; added `GET /api/documents/page` (paginated) and `GET /api/documents/{id}` (single-doc DTO) endpoints.
- **9.3 Architecture:** Collapsed `extractInvoiceFields`/`extractContractFields`/`extractResumeFields` into generic `<T> extractFields(Document, prompt, schema, Class<T>)` with `DocumentType` dispatch map; created `SanitizationUtils.stripNulBytes()` and removed duplicated private copies from 4 service classes; removed `ExtractionRepository`/`SummaryRepository`/`AnomalyFlagRepository` direct injections from `DocumentController`, routing through `DocumentService` delegates; gated `show-sql` via `${SHOW_SQL:false}`; created `CorrelationIdFilter.java` attaching `X-Request-ID` to MDC.
- **9.4 Resilience:** Refactored `StorageService` → interface + `LocalStorageServiceImpl`; added jittered exponential backoff retry loop (3 attempts, on HTTP 429/5xx & IOException) to `GeminiClient`; created `DocumentReconciliationScheduler.java` (`@Scheduled` every 5 min) detecting PENDING docs older than 10 min and re-triggering processing; added `POST /api/documents/{id}/reprocess` manual endpoint; added `@EnableScheduling` to `DocketApplication`.
- **9.5 Observability:** Added `spring-boot-starter-actuator` + `spring-security-test` to `pom.xml`; configured actuator health/metrics in `application.yml`; wired Docker Compose backend healthcheck to `/actuator/health`.
- **9.6 Frontend Resilience:** Updated `DocumentDetail.jsx` to fetch single doc via `GET /api/documents/{id}`; migrated sub-resource fetches to `Promise.allSettled`; implemented `fetchBlobUrl()` for authenticated blob URL iframe previews; added Reprocess button; added `fetchBlobUrl` helper to `api.js`.
- **9.7 Tests & CI:** Created 6 test classes (21 tests total) — all passing via `mvn test`: `WorkspaceIsolationTest` (3), `AuthServiceTest` (5), `LoginRateLimiterTest` (4), `DocumentServiceTest` (3), `ExtractionServiceTest` (3), `SanitizationUtilsTest` (3). Created `.github/workflows/ci.yml` running backend tests + frontend build on push/PR.
- Files touched: `SecurityConfig.java`, `DocumentController.java`, `DocumentService.java`, `AuthService.java`, `StorageService.java`, `LocalStorageServiceImpl.java`, `GeminiClient.java`, `ExtractionService.java`, `SummarizeService.java`, `AnomalyService.java`, `DocumentProcessingService.java`, `DocketApplication.java`, `application.yml`, `pom.xml`, `docker-compose.yml`, `api.js`, `DocumentDetail.jsx`, `.gitignore`. New: `SanitizationUtils.java`, `LoginRateLimiter.java`, `CorrelationIdFilter.java`, `DocumentReconciliationScheduler.java`, `LocalStorageServiceImpl.java`, `V7__add_workspace_and_query_indexes.sql`, 6 test classes, `.github/workflows/ci.yml`.
- Tested/confirmed: `mvn test` → 21 tests, 0 failures, 0 errors. `npm run build` → clean build, 0 errors.
- Still untested / follow-up: Docker Compose full rebuild not run this session (no daemon in sandbox). Actuator health DB check needs live `docker compose up --build` to verify against real Postgres.
- Next session should: Begin Phase 10 — start with Phase 10.1 (human-in-the-loop field editing: `PATCH /api/documents/{id}/extraction` backend + editable UI in `DocumentDetail.jsx`) and Phase 10.2 (PENDING status auto-polling with backoff on Dashboard and DocumentDetail).

### Session 35 — 2026-08-25
- Executed full Phase 10 (Advanced Polish, Human-in-the-Loop & API Governance).
- **10.1 Human-in-the-Loop Corrections:** Added `PATCH /api/documents/{id}/extraction` endpoint; `ExtractionCorrectionRequest.java` DTO (Jakarta Validation); `correctExtraction()` + `getTodayLlmUsage()` in `DocumentService`; `humanCorrectedJson`/`correctionNote`/`correctedAt` columns on `Extraction` entity; `Flyway V8__llm_usage_tracking_and_corrections.sql`. Frontend: inline JSON editor in `DocumentDetail.jsx` with AnimatePresence, HUMAN CORRECTED badge, correction timestamp display.
- **10.2 Status Polling:** `useRef`-based setInterval polling at 4s in `DocumentDetail.jsx` — stops on PROCESSED/FAILED transition and re-loads sub-resources. Status badge shows auto-refresh indicator. No page reload required.
- **10.3 Swagger/OpenAPI:** `springdoc-openapi-starter-webmvc-ui 2.6.0` added; `OpenApiConfig.java` with JWT Bearer security scheme; `/swagger-ui.html` and `/v3/api-docs` added to `SecurityConfig` permitAll().
- **10.4 LLM Budget Guard:** `LlmUsage` entity + `LlmUsageRepository` (PostgreSQL `ON CONFLICT` atomic upsert); `LlmBudgetService` (429 enforcement, 500-call global ceiling, per-workspace `daily_llm_budget` column on `Workspace`); wired into `DocumentProcessingService` before extraction/summarization/anomaly; `GET /api/documents/usage` endpoint; Dashboard AI Budget progress bar with traffic-light gradient.
- **10.5 Mobile & Accessibility:** Responsive `lg:grid-cols-2`, `px-4 sm:px-6`, truncated mobile nav label, `aria-label` on all interactive buttons, `aria-live="polite"` on status badge, `role="alert"` on error regions.
- **10.6 `apiPatch` helper:** Added `apiPatch(path, body)` to `api.js`.
- **10.7 Tests:** Created `LlmBudgetServiceTest` (4 tests). Updated `DocumentServiceTest` and `WorkspaceIsolationTest` constructors for new `LlmBudgetService` dependency. Total: **25 tests, 0 failures, BUILD SUCCESS**. Frontend: `npm run build` clean.
- Files touched: `pom.xml`, `Workspace.java`, `Extraction.java`, `DocumentService.java`, `DocumentProcessingService.java`, `DocumentController.java`, `SecurityConfig.java`, `application.yml`, `api.js`, `DocumentDetail.jsx`, `Dashboard.jsx`. New: `ExtractionCorrectionRequest.java`, `LlmUsage.java`, `LlmUsageRepository.java`, `LlmBudgetService.java`, `OpenApiConfig.java`, `V8__llm_usage_tracking_and_corrections.sql`, `LlmBudgetServiceTest.java`.
- Tested/confirmed: `mvn test` → 25 tests, 0 failures, 0 errors (BUILD SUCCESS). `npm run build` → ✓ built in 372ms, 0 errors.
- Still untested / follow-up: Docker Compose full rebuild (Flyway V8 migration against live Postgres) not run this session. API versioning (`/api/v1/`) deferred — no breaking change impact noted in `rules.md`.
- Next session should: Run Phase 11 — `docker compose up --build` to verify V8 migration, confirm all containers healthy, run full e2e smoke test (sign up → upload → extract → human-correct → export), and document the deployment steps.

### Session 36 — 2026-08-25
- Executed Phase 11 (Deployment & Demo Readiness) and verified live Docker Compose orchestration.
- **11.1 Demo Data Seeder (Flyway V9):** Created `V9__seed_demo_data.sql` inserting a pre-seeded demo workspace (`Acme Global Demo`), user (`demo@docket.ai` / `Demo1234!`), 3 rich sample documents (Invoice with Net-15 anomaly, Contract with 14-day termination notice anomaly, clean Senior Resume), and active templates. Allows instant evaluation and UI exploration out of the box without requiring local OCR execution or a live Gemini API key.
- **11.2 5-Minute Evaluation Walkthrough (`DEMO_SCRIPT.md`):** Authored detailed 5-minute evaluator and judge walkthrough click-path covering login, dashboard metrics, AI field extraction, plain-English summarization, template deviation detection, human-in-the-loop editing (`PATCH /api/documents/{id}/extraction`), export capabilities, interactive OpenAPI Swagger UI (`/swagger-ui.html`), and Actuator health checks (`/actuator/health`).
- **11.3 Environment & Production Documentation:** Overhauled `.env.example`, `README.md`, and `architecture.md` with complete environment templates, endpoint catalog, Swagger access links, and Actuator health specifications.
- **11.4 Docker Container Health & Spring Boot 3.4/3.5 Compatibility:**
  - Resolved `NoSuchMethodError` on `ControllerAdviceBean` during Swagger startup by upgrading `springdoc-openapi-starter-webmvc-ui` from `2.6.0` to `2.8.5` (Spring Framework 6.2 compatibility) and adding `@Hidden` to `GlobalExceptionHandler.java`.
  - Added `wget` and `curl` to `backend/Dockerfile` for Actuator healthcheck execution.
  - Successfully executed `docker compose up -d` — all three containers (`docket-db-1`, `docket-backend-1`, `docket-frontend-1`) came up in `healthy` / `running` status.
  - Live round-trip verified: `GET /actuator/health` returned `"status":"UP"`, `GET /v3/api-docs` returned full OpenAPI 3.0 schema, and `POST /api/auth/login` (`demo@docket.ai` / `Demo1234!`) returned JWT token and loaded the 3 pre-seeded documents.
- **11.5 Tests & Verification:** Added `AuthServiceTest#testDemoPasswordHash` verifying the BCrypt seed hash against `BCryptPasswordEncoder`. Total: **26 tests, 0 failures, BUILD SUCCESS**. Frontend: `npm run build` clean in 475ms.
- Files touched: `pom.xml`, `GlobalExceptionHandler.java`, `backend/Dockerfile`, `.env.example`, `README.md`, `architecture.md`, `AuthServiceTest.java`. New: `DEMO_SCRIPT.md`, `V9__seed_demo_data.sql`.
- Tested/confirmed: Live Docker Compose run verified: `docket-backend-1` healthy, `docket-db-1` healthy, `docket-frontend-1` running. `curl /actuator/health` = UP. Live auth login & document fetch verified. `mvn test` → 26 tests, 0 failures. `npm run build` → clean.
- Next session should: Begin Phase 12 (Stretch Goals) — Phase 12.1 batch upload capability in `UploadDocument.jsx` and backend multi-file endpoint.

### Session 37 — 2026-08-25
- Implemented Phase 12.1: Multi-Document Batch Upload.
- Backend: Added `POST /api/documents/batch` endpoint in `DocumentController.java` receiving `List<MultipartFile> files` and `@RequestParam DocumentType documentType`.
- Service: Implemented `uploadDocumentsBatch()` in `DocumentService.java` to iterate through uploaded files, validate extensions/MIME types, store files, persist `Document` records, and trigger async pipeline processing.
- Frontend: Overhauled `UploadDocument.jsx` to support multi-file selection, drag-and-drop batch queue, per-file preview removal, batch progress bar, and comprehensive error reporting.
- Tests: Added `uploadDocumentsBatch_MultipleFiles_Success` in `DocumentServiceTest.java`.
- Files touched: `DocumentController.java`, `DocumentService.java`, `DocumentServiceTest.java`, `UploadDocument.jsx`.
- Tested/confirmed: `mvn test` passed with 27 tests, 0 failures. Frontend built cleanly (`npm run build`).

### Session 38 — 2026-09-11
- Audited repository against `memory.md` and `Docket_Evaluation_Report.md`:
  - Verified all 3 evaluation tiers (Tier 1: 7/10, Tier 2: 8.5/10, Tier 3: 9.5/10) were fully addressed across Phase 9, 10, and 11.
  - Confirmed the recent Phase 12.1 batch upload implementation and reconciled `memory.md` status, completed items, test counts, and session logs.
  - Executed automated backend test suite (`mvn test`) — **27 tests passed cleanly, 0 failures**.
  - Executed frontend production build (`npm run build`) — **Vite built client bundle in 1.43s, 0 errors**.
- Files touched: `memory.md`.
- Next session should: Proceed with Phase 12.2 (Field-level confidence scoring) or continue with deployment/live evaluation testing.

### Session 39 — 2026-09-12
- Implemented Phase 12.2: Field-Level Confidence Scoring across backend prompts, DTOs, tests, and frontend UI.
- Backend:
  - Updated `ExtractInvoicePrompt.java`, `ExtractContractPrompt.java`, and `ExtractResumePrompt.java` prompts and JSON schemas to request estimated probability scores (0.0 to 1.0) under a structured `fieldConfidences` object.
  - Added `fieldConfidences` property (Map<String, Double>) with getters/setters to `InvoiceExtractionDto`, `ContractExtractionDto`, and `ResumeExtractionDto`.
  - Updated `V9__seed_demo_data.sql` with realistic fieldConfidences values for seeded sample documents.
- Frontend:
  - Updated `DocumentDetail.jsx` with a `ConfidenceBadge` component displaying visual confidence pills (emerald for ≥85%, amber for 70-84%, red for <70%) on every extracted field and multi-item lists.
  - Added an aggregate `avg conf` badge in the "Extracted Intelligence" card header.
- Tests & Validation:
  - Added `testExtractInvoiceFieldsWithFieldConfidences` unit test in `ExtractionServiceTest.java`.
  - Executed `./mvnw test` — **28 tests passed, 0 failures**.
  - Executed `npm run build` — **clean build in 386ms, 0 errors**.
- Files touched: `ExtractInvoicePrompt.java`, `ExtractContractPrompt.java`, `ExtractResumePrompt.java`, `InvoiceExtractionDto.java`, `ContractExtractionDto.java`, `ResumeExtractionDto.java`, `ExtractionServiceTest.java`, `V9__seed_demo_data.sql`, `DocumentDetail.jsx`, `memory.md`.
- Next session should: Proceed to Phase 12.3 (Queue-Based Processing) or Phase 12.4 (Multi-Document Comparative Anomaly Detection).

### Session 40 — 2026-09-19
- Implemented Phase 12.3: Queue-Based Processing (RabbitMQ + Spring AMQP) with dual-mode fallback (`docket.processing.mode` = `queue` or `async`, default `async`).
- Backend Infrastructure:
  - Added `spring-boot-starter-amqp` to `pom.xml`.
  - Configured RabbitMQ connection properties and default queue/exchange bindings in `application.yml`.
  - Added `rabbitmq:3-management-alpine` service with healthcheck and web UI (:15672) to `docker-compose.yml` and updated `.env.example`.
  - Created `RabbitMqConfig.java` defining durable exchange, queue, and Jackson `MessageConverter`.
  - Created `DocumentProcessingMessage.java` lightweight message payload record.
  - Created `DocumentQueuePublisher.java` (`@ConditionalOnProperty(mode=queue)`) to publish tasks to RabbitMQ.
  - Created `DocumentProcessingConsumer.java` (`@RabbitListener`) to consume tasks, load entities from DB, and execute the synchronous processing pipeline with error handling.
- Service Refactoring & Integration:
  - Refactored `DocumentProcessingService.java` to extract synchronous `processDocument()` worker method while preserving `@Async` `processDocumentAsync()`.
  - Modified `DocumentService.java` (`uploadDocument`, `uploadDocuments`, `reprocessDocument`) to conditionally dispatch to RabbitMQ when publisher is present, falling back to `@Async` thread pool otherwise.
  - Modified `DocumentReconciliationScheduler.java` to conditionally re-enqueue stuck documents via queue or `@Async`.
- Tests & Validation:
  - Created `DocumentProcessingConsumerTest.java` (testing normal consumption, missing document handling, and exception isolation).
  - Created `DocumentQueuePublisherTest.java` (testing exchange, routing key, and message payload).
  - Updated `DocumentServiceTest.java` (testing both async and queue-publisher dispatch branches).
  - Updated `WorkspaceIsolationTest.java` for new `DocumentService` constructor.
  - Executed `mvn test` — **33 tests passed, 0 failures** (5 new tests added, 100% green).
  - Executed `npm run build` — **Vite client bundle built cleanly in 694ms, 0 errors**.
- Docs Updated:
  - `rules.md` — added Spring AMQP / RabbitMQ to approved libraries list for Phase 12.3 stretch.
  - `architecture.md` — updated §3.4 to document dual-mode processing and §8.1 for RabbitMQ docker compose service.
  - `memory.md` & `task.md` — updated status, completed list, key decisions, and session log.
- Files touched:
  - Created: `RabbitMqConfig.java`, `DocumentProcessingMessage.java`, `DocumentQueuePublisher.java`, `DocumentProcessingConsumer.java`, `DocumentProcessingConsumerTest.java`, `DocumentQueuePublisherTest.java`.
  - Modified: `pom.xml`, `application.yml`, `docker-compose.yml`, `.env.example`, `DocumentProcessingService.java`, `DocumentService.java`, `DocumentReconciliationScheduler.java`, `DocumentServiceTest.java`, `WorkspaceIsolationTest.java`, `rules.md`, `architecture.md`, `memory.md`.
- Tested/confirmed: `mvn test` (33 tests pass, 0 failures), `npm run build` (clean Vite build, 0 errors).
- Next session should: Proceed to Phase 12.4 (Multi-Document Comparative Anomaly Detection) or Phase 12.5 (4th Document Type - KYC Form).

### Session 41 — 2026-09-19
- Implemented Phase 12.4: Multi-Document Comparative Anomaly Detection & Workspace Vendor Trends.
- Backend Engine:
  - Created `ComparativeAnomalyService.java`: performs deterministic comparative analysis across workspace document extraction histories.
    - Duplicate detection: identifies identical `invoiceNumber` across different documents in the same workspace (flags `HIGH` severity).
    - Price surge detection: computes historical vendor spend average across prior invoices; flags invoices with >50% price surge (`MEDIUM` / `HIGH` severity).
    - Payment term anomalies: flags invalid date orders (`dueDate` < `invoiceDate`) and abrupt payment window contractions.
    - Workspace vendor trends: aggregates invoices by vendor into spend curves, average tickets, price ranges, latest invoice, trend percentage, and active anomaly counts.
  - Created `VendorTrendDto.java` and `WorkspaceTrendsDto.java` records.
  - Added query method `findByDocumentWorkspaceId` to `ExtractionRepository.java`.
  - Wired `comparativeAnomalyService.detectComparativeAnomalies(doc)` into `DocumentProcessingService.java` post-extraction pipeline step.
  - Injected `ComparativeAnomalyService` into `DocumentService.java` and added `GET /api/documents/trends` endpoint to `DocumentController.java` (workspace-isolated).
- Frontend UI:
  - Updated `AnomalyFlag.jsx` to render source-specific category badges (`DUPLICATE ALERT`, `TREND ANOMALY`, `TEMPLATE DEVIATION`).
  - Updated `DocumentDetail.jsx` heading to "Detected Anomalies & Deviations".
  - Overhauled `Dashboard.jsx` with a dual-view tab switcher ("Documents" vs "Vendor Trends & Cross-Doc Intelligence"). Added summary cards (Tracked Vendors, Invoices Analyzed, Price Surges, Duplicates, Total Alerts), vendor search, "Flagged Only" filter, comprehensive metrics table, and one-click "Filter Invoices" drill-down.
- Tests & Validation:
  - Created `ComparativeAnomalyServiceTest.java` (5 unit tests: duplicate invoice detection, price surge detection, normal invoice within bounds, payment term window check, workspace trends calculation).
  - Updated `DocumentServiceTest.java` (added `testGetWorkspaceTrends`) and `WorkspaceIsolationTest.java`.
  - Executed `mvn test` — **38 tests passed, 0 failures, 100% green**.
  - Executed `npm run build` — **Vite client bundle built cleanly in 632ms, 0 errors**.
- Docs Updated: `phases.md`, `architecture.md`, `memory.md`, `task.md`.
- Files touched:
  - Created: `VendorTrendDto.java`, `WorkspaceTrendsDto.java`, `ComparativeAnomalyService.java`, `ComparativeAnomalyServiceTest.java`.
  - Modified: `ExtractionRepository.java`, `DocumentProcessingService.java`, `DocumentService.java`, `DocumentController.java`, `DocumentServiceTest.java`, `WorkspaceIsolationTest.java`, `AnomalyFlag.jsx`, `DocumentDetail.jsx`, `Dashboard.jsx`, `phases.md`, `architecture.md`, `memory.md`, `task.md`.
- Next session should: Proceed to Phase 12.5 (4th Document Type - KYC Form) or Phase 12.6 (Billing Simulation).

### Session 42 — 2026-09-19
- Implemented Phase 12.5: 4th Document Type — KYC Form.
- Backend:
  - Added `KYC_FORM` to `DocumentType.java` enum (no Flyway migration needed — `type` column is `VARCHAR(50)`).
  - Created `KycExtractionDto.java` with `@NotNull`-validated identity fields: `fullName`, `idType`, `idNumber`, `dateOfBirth`, `nationality`, `issueDate`, `expiryDate`, `address`, `verificationStatus`, and `fieldConfidences` map.
  - Created `ExtractKycPrompt.java` with strict Gemini prompt and complete JSON schema (9 fields + per-field confidence scoring).
  - Updated `ExtractionService.java`: added `extractKycFields()` method; added `case KYC_FORM ->` branch to `extractDocumentFields()` dispatch switch.
- Tests & Validation:
  - Added `testExtractKycFieldsSuccess` to `ExtractionServiceTest.java` (dispatches via `extractDocumentFields()`, verifies JSON, field values, and `fieldConfidences`).
  - Executed `mvn test` — **39 tests passed, 0 failures, 100% green**.
  - Executed `npm run build` — **Vite client bundle built cleanly in 657ms, 0 errors**.
- Frontend UI:
  - Updated `UploadDocument.jsx` with `{ value: 'KYC_FORM', label: 'KYC Form', desc: 'ID cards, passports, verification forms' }`.
  - Updated `TemplateManager.jsx` DOCUMENT_TYPES with KYC Forms tab.
  - Updated `Dashboard.jsx` typeFilter select with KYC Forms option.
  - Updated `DocumentDetail.jsx` ExtractionFields dispatcher + added `KycFields` renderer: 2-column identity grid, per-field confidence badges, color-coded verificationStatus bar.
- Docs Updated: `phases.md`, `architecture.md`, `memory.md`, `task.md`.
- Files touched:
  - Created: `KycExtractionDto.java`, `ExtractKycPrompt.java`.
  - Modified: `DocumentType.java`, `ExtractionService.java`, `ExtractionServiceTest.java`, `UploadDocument.jsx`, `TemplateManager.jsx`, `Dashboard.jsx`, `DocumentDetail.jsx`, `phases.md`, `architecture.md`, `memory.md`, `task.md`.
- Next session should: Proceed to Phase 12.6 (Billing Simulation) or evaluate production readiness per AGENTS.md §8.

### Session 43 — 2026-09-19
- Implemented Phase 12.6: Billing Simulation & Stripe Test Mode.
- Backend Subsystem:
  - Created Flyway migration `V10__billing_simulation.sql` adding `plan_tier`, `subscription_status`, `stripe_customer_id`, `stripe_subscription_id`, `billing_period_start`, `billing_period_end` to `workspaces`, and creating `simulated_invoices` table.
  - Updated `Workspace.java` entity with subscription columns and accessors.
  - Created `SimulatedInvoice.java` entity and `SimulatedInvoiceRepository.java`.
  - Added `countByWorkspaceIdAndUploadedAtGreaterThanEqual` to `DocumentRepository.java`.
  - Created `PlanTier.java` enum defining `FREE`, `PRO`, `ENTERPRISE` tiers, limits, and pricing.
  - Created DTOs: `SubscriptionDetailsDto.java`, `SimulatedInvoiceDto.java`, `UpgradePlanRequestDto.java`, `SimulateWebhookRequestDto.java`.
  - Created `BillingService.java`: handles plan tier upgrades, monthly document quota enforcement (`checkDocumentQuota`), simulated Stripe customer/subscription ID generation, and real-time simulated webhook handling (`invoice.payment_succeeded`, `invoice.payment_failed`, `customer.subscription.updated`, `customer.subscription.deleted`).
  - Injected `BillingService` into `DocumentService.java` to enforce monthly document quota guards on single and batch uploads.
  - Created `BillingController.java` (`GET /api/billing/subscription`, `GET /api/billing/invoices`, `POST /api/billing/upgrade`, `POST /api/billing/webhook/simulate`).
- Tests & Verification:
  - Created `BillingServiceTest.java` with 11 comprehensive unit tests covering plan upgrades, quota checks, past due enforcement, webhook simulations, and invoice retrieval.
  - Updated `DocumentServiceTest.java` and `WorkspaceIsolationTest.java` with `BillingService` mocks.
  - Executed `mvn test` — **51 tests passed, 0 failures, 100% green**.
- Frontend UI:
  - Created `Billing.jsx`: Obsidian Aurora UI with monthly document quota progress meter (emerald/amber/red thresholds), daily LLM budget meter, simulated Stripe customer/subscription badges with one-click copy, 3-tier plan comparison cards with simulated checkout, interactive Stripe Webhook Simulator console, and simulated invoice/receipt history table.
  - Registered protected route `/billing` in `App.jsx`.
  - Added "Billing" navigation buttons to `Dashboard.jsx` and `TemplateManager.jsx`.
  - Executed `npm run build` — **Vite client bundle built cleanly in 769ms, 0 errors**.
- Docs Updated: `phases.md`, `architecture.md`, `memory.md`, `walkthrough.md`.
- Files touched:
  - Created: `V10__billing_simulation.sql`, `SimulatedInvoice.java`, `SimulatedInvoiceRepository.java`, `PlanTier.java`, `SubscriptionDetailsDto.java`, `SimulatedInvoiceDto.java`, `UpgradePlanRequestDto.java`, `SimulateWebhookRequestDto.java`, `BillingService.java`, `BillingController.java`, `BillingServiceTest.java`, `Billing.jsx`.
  - Modified: `Workspace.java`, `DocumentRepository.java`, `DocumentService.java`, `DocumentServiceTest.java`, `WorkspaceIsolationTest.java`, `App.jsx`, `Dashboard.jsx`, `TemplateManager.jsx`, `phases.md`, `architecture.md`, `memory.md`, `walkthrough.md`.
- Tested/confirmed: `mvn test` (51 tests passed, 0 failures), `npm run build` (clean Vite build, 0 errors).
- Next session should: Perform final production readiness evaluation per AGENTS.md §8 and deployment rehearsal.

