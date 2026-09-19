# PHASES.md — Project Build Phases

**Project:** Docket

**Purpose:** Break the project into small, independently completable phases so an AI coding assistant (or the student) never has to hold the whole project in mind at once. Complete phases **in order**. Do not start a phase until the previous one's "Definition of Done" is met.

---

## Phase 0 — Project Setup
**Goal:** Empty-but-running skeleton for both frontend and backend.

- Initialize `frontend/` (Vite + React + JavaScript/JSX + Tailwind) and `backend/` (Spring Boot 3.x project via Spring Initializr — Web, Data JPA, Security, Validation, Flyway dependencies)
- Set up PostgreSQL (local or free-tier cloud), configure `application.yml` datasource, and confirm the first Flyway migration runs on startup
- Set up `.env.example` in both frontend and backend
- Basic `README.md` with how to run both apps locally
- Confirm frontend can hit a `/api/health` backend route and render "OK"

**Definition of Done:** `npm run dev` on both frontend and backend works; health check round-trips successfully.

---

## Phase 1 — Auth & Workspace
**Goal:** Users can sign up, log in, and land on an empty dashboard scoped to their workspace.

- DB schema: `users`, `workspaces`
- Signup/login endpoints with JWT issuance, bcrypt password hashing
- Frontend: Login page, Signup page, protected route wrapper
- On signup, auto-create a workspace for the user
- Empty `Dashboard.jsx` page showing "No documents yet"
- Backend: `AuthController.java` route + `security/JwtService.java` / `security/JwtAuthFilter.java` + `config/SecurityConfig.java` for the filter chain

**Definition of Done:** A new user can sign up, log in, get redirected to a dashboard, and refresh the page without losing session (JWT persisted in storage/cookie).

---

## Phase 2 — Document Upload (Invoice type only, first vertical slice)
**Goal:** A logged-in user can upload an invoice PDF/image and see it appear in their document list with status `pending` → `processed` (extraction/summary can be stubbed/fake for now).

- DB schema: `documents`
- Upload endpoint: validate file type/size, store file (local disk for dev), create `documents` row
- Frontend: `UploadDocument.jsx` page with file picker + document type selector (only Invoice enabled this phase)
- Dashboard lists uploaded documents with status badge

**Definition of Done:** Upload → row appears in dashboard list with correct filename and status, scoped to the correct workspace.

---

## Phase 3 — OCR + Text Extraction Pipeline
**Goal:** Real text is pulled out of the uploaded invoice (from PDF text layer, or OCR if scanned).

- Implement `service/OcrService.java`: try Apache PDFBox text extraction first; if text is empty/too short, fall back to PDFBox-rasterized pages + Tess4J OCR
- Store raw extracted text on the document record (or a related table)
- Handle failure paths per rules.md (mark `failed`, store reason)

**Definition of Done:** Uploading a clean digital invoice PDF and a scanned invoice image both produce readable extracted text, visible in a debug view or DB inspection.

---

## Phase 4 — LLM Field Extraction (Invoice)
**Goal:** Structured invoice fields (vendor, invoice #, date, line items, total) are extracted via Gemini and shown in the UI.

- Build `prompt/ExtractInvoicePrompt.java` — strict JSON-only output, grounded-in-text instruction (see rules.md §3)
- Build `service/ExtractionService.java` to call Gemini via `GeminiClient`, deserialize with Jackson into `InvoiceExtractionDto`, validate with `@Valid`, persist to `extractions` table
- Frontend: `DocumentDetail.jsx` shows extracted fields in a clean table next to a preview of the original file

**Definition of Done:** Uploading a sample invoice produces correct, review-able structured fields for at least 8/10 test invoices.

---

## Phase 5 — Summarization (all types, since it's type-agnostic)
**Goal:** Every processed document gets a short plain-English summary.

- Build `prompt/SummarizePrompt.java` and `service/SummarizeService.java`
- Store in `summaries` table
- Display in `DocumentDetail.jsx` as a "Summary" card

**Definition of Done:** Every processed document shows a coherent 3-5 sentence summary.

---

## Phase 6 — Template Manager & Anomaly Flagging (Invoice)
**Goal:** User can designate a document as the "standard template" for invoices, and new invoices get compared against it.

- DB schema: `templates`, `anomaly_flags`
- `TemplateManager.jsx` page: pick/upload a document to mark as the standard for a type
- Build `prompt/AnomalyCheckPrompt.java` + `service/AnomalyService.java`: compares new document's extracted text/fields against the template's, returns a list of flagged differences with short explanations
- Frontend: `AnomalyFlag.jsx` component shows ⚠️/✅ per relevant field in `DocumentDetail.jsx`

**Definition of Done:** Uploading an invoice with a deliberately altered field (e.g., different payment terms) against a saved template correctly produces a flag with an accurate explanation.

---

## Phase 7 — Extend to Contract and Resume Types
**Goal:** Repeat Phases 2, 4, and 6's logic for Contract and Resume document types (upload, extraction, template comparison already share summarization and most infrastructure).

- Add `prompt/ExtractContractPrompt.java` and `prompt/ExtractResumePrompt.java`, each paired with a matching DTO (`ContractExtractionDto`, `ResumeExtractionDto`) in `dto/`
- Enable document type selector fully in upload flow
- Test each type against its own sample set (10 contracts, 10 resumes)

**Definition of Done:** All 3 document types can be uploaded, extracted, summarized, and compared against a template, each with ≥80% field accuracy on test samples.

---

## Phase 8 — Dashboard Polish & Export
**Goal:** Dashboard becomes genuinely usable: filtering, status overview, export.

- Dashboard filters: by type, by flagged status, by date
- CSV/JSON export per document and bulk per workspace
- Empty/loading/error states everywhere (per rules.md §4)
- Apply full visual design system from `design.md`

**Definition of Done:** A demo user can filter to "flagged contracts," open one, and export its data — all without console errors.

---

## Phase 8.5 — Modern 3D & Motion Visual Overhaul
**Goal:** Replace the flat "calm B2B" v1 look with the "Aurora Obsidian" design system (see `design.md` §8): a distinctive, modern, subtly-3D, animated UI — without breaking accessibility, performance, or any functional flow built in Phases 0–8.

- Re-read `design.md` §8 (new tokens, motion rules, 3D scoping rules) before touching any component
- Install `motion` (Framer Motion) and `@react-three/fiber` + `@react-three/drei`; update `package.json` — these are now approved in `architecture.md` §3.1 and `rules.md` §2, no further sign-off needed
- Swap Tailwind theme tokens (colors, radii, shadows) to the new Aurora Obsidian palette defined in `design.md` §8.1 — one file change (`tailwind.config.js` + CSS variables), not per-component hex edits
- Add the signature ambient element: a low-poly/particle aurora gradient mesh (React Three Fiber, `<Canvas>`) behind the dashboard hero and login/signup screens only — static fallback (CSS gradient, no WebGL) for `prefers-reduced-motion` or low-end devices
- Add Framer Motion micro-interactions: page-transition fades, staggered list reveals for the document dashboard, card hover-lift (translateY + shadow, not scale-jump), and a subtle status-badge pulse for `processing` documents
- Give cards/panels a soft glassmorphism treatment (backdrop-blur + translucent surface + 1px gradient border) per `design.md` §8.2 — keep data tables and forms flat/high-contrast, glass is for containers only, never for text-bearing rows
- Re-skin buttons, badges, empty states, and the anomaly-flag component with the new tokens; keep all copy/UX behavior from earlier phases untouched
- Run the full `ui-ux-pro-max` accessibility + motion checklist (contrast 4.5:1, 150–300ms durations, no animating width/height, keyboard focus visible, reduced-motion respected) before marking this phase done

**Definition of Done:** Every screen built in Phases 1–8 still works exactly as before functionally, now rendered in the Aurora Obsidian system; Lighthouse accessibility score ≥ 90; page interactive within budget on a mid-tier laptop with the 3D canvas enabled; `prefers-reduced-motion` users get a fully static, still-attractive equivalent.

---

## Phase 9 — Production Hardening & Evaluation Remediation
**Goal:** Address and eliminate 100% of the weaknesses, security vulnerabilities, design flaws, code smells, reliability gaps, and testing deficiencies identified in `Docket_Evaluation_Report.md`, elevating the codebase from an MVP state to senior-engineer / production-ready standard (targeting ≥ 9.0/10 across all evaluation areas).

### 9.1 — Critical Security & Privacy Remediation (Immediate Priority)
- **Authenticated File Access:** Remove `permitAll()` for `/uploads/**` from `SecurityConfig.java`. Replace unauthenticated static resource exposure with a secure, workspace-isolated controller endpoint: `GET /api/documents/{id}/file` (and `/preview`). Re-verify that `getDocumentForWorkspace` enforces strict workspace ownership checks before streaming files.
- **Login Rate Limiting & Brute-Force Protection:** Implement an authentication attempt limiter and temporary account lockout mechanism on `AuthController.login` to defend against credential stuffing and brute-force attacks.
- **Git Artifact & Sensitive File Purge:** Scrub and untrack all committed user-uploaded files from `/uploads` and `/backend/uploads` (`git rm -r --cached`), and harden `.gitignore` to prevent any future uploaded PDFs or images from being staged or committed.
- **JWT Hardening & Token Refresh Strategy:** Implement refresh token infrastructure (or HTTP-only secure cookie strategy) and remove reliance on vulnerable raw `localStorage` tokens.

### 9.2 — Database & API Performance Optimization
- **Workspace Indexing Migration:** Create a new Flyway migration (`V7__add_workspace_and_query_indexes.sql`) adding explicit indexes on `documents(workspace_id)`, `documents(status)`, and `users(workspace_id)` to eliminate table scans on workspace-scoped queries.
- **Paginated Document Listing:** Upgrade `GET /api/documents` to support Spring Data `Pageable` (`page`, `size`, `sort`) with default page limits and a metadata envelope (total elements, total pages, current page), preventing memory spikes on large workspaces.
- **Single-Document API Endpoint:** Add `GET /api/documents/{id}` returning `DocumentListItemDto` so clients and detail views can fetch a single document directly without loading the entire workspace collection.

### 9.3 — Architecture & Backend Code Quality Refactoring
- **Generic Extraction Engine:** Collapse the triplicated methods in `ExtractionService.java` (`extractInvoiceFields`, `extractContractFields`, `extractResumeFields`) into a single generic method: `extractFields(Document, String promptText, String schema, Class<T> dtoType)` driven by a `Map<DocumentType, ExtractorConfig>` or clean strategy pattern.
- **Sanitization Utility Unification:** Deduplicate the repeated `stripNulBytes()` helper from `AnomalyService`, `SummarizeService`, `DocumentProcessingService`, and `ExtractionService` into a single shared utility (`SanitizationUtils.java`).
- **Controller/Service Layer Boundary Discipline:** Refactor `DocumentController.java` to eliminate direct repository dependencies (`ExtractionRepository`, `SummaryRepository`, `AnomalyFlagRepository`), routing all sub-resource retrieval through `DocumentService`.
- **Environment-Gated Logging Configuration:** Gate `show-sql` in `application.yml` so that verbose SQL logging is active only in the `dev` profile and disabled in production.
- **Structured Logging & Correlation IDs:** Implement an MDC-based request correlation ID filter (`X-Request-ID`) that propagates across async processing threads to allow end-to-end trace correlation in logs.

### 9.4 — LLM Resilience, Async Recovery & Storage Abstraction
- **Gemini Client Exponential Backoff & Retry:** Implement resilient retry logic with jittered exponential backoff (e.g. 3 attempts) in `GeminiClient.java` for transient network blips, HTTP 429 rate limits, and 5xx API errors before marking documents as failed.
- **Stuck Document Reconciliation Job:** Add a scheduled background reconciliation task (`DocumentReconciliationScheduler`) that detects documents stuck in `PENDING` status for more than N minutes and re-triggers OCR/extraction pipelines.
- **Manual Reprocess Endpoint:** Add `POST /api/documents/{id}/reprocess` on `DocumentController` and a "Reprocess" button on `DocumentDetail.jsx` allowing users to re-trigger failed processing jobs.
- **Storage Service Abstraction:** Decouple file storage behind a `StorageService` interface with `LocalStorageService` (development) and an S3-compatible `S3StorageService` implementation (production) configured via Spring profiles.

### 9.5 — Monitoring, Observability & Health Checks
- **Spring Boot Actuator:** Add `spring-boot-starter-actuator` with configured `/actuator/health` (including PostgreSQL database connectivity check and disk space) and `/actuator/metrics` endpoints.
- **Docker Healthcheck Integration:** Wire `/actuator/health` into `docker-compose.yml` for the `backend` service healthcheck.
- **LLM Diagnostics:** Provide diagnostic telemetry capturing Gemini API failure rates and latency.

### 9.6 — Frontend Error Resilience
- **Graceful Error Handling in Detail View:** Refactor `DocumentDetail.jsx` from `Promise.all` to `Promise.allSettled` (or isolated queries), ensuring that an anomaly check or summarization timeout does not crash or blank the entire document detail view.

### 9.7 — Automated Test Suite & CI/CD Automation
- **Comprehensive Backend Testing (JUnit 5 + Spring Boot Test + Mockito):**
  - High-priority security test: Verify cross-workspace isolation (requesting document ID of Workspace B as User from Workspace A returns 404).
  - Integration tests for `AuthService` (signup happy-path, duplicate email conflict, login verification, lockout on brute force).
  - Service tests for `DocumentService`, `ExtractionService`, `SummarizeService`, `AnomalyService`, and `ExportService`.
  - Repository `@DataJpaTest` tests for custom query methods and Flyway migrations.
- **GitHub Actions CI Pipeline:** Create `.github/workflows/ci.yml` running automated Maven builds & tests (`mvn test`) and frontend validation (`npm run build` / lint / tests) on every push and PR.

### 9.8 — Documentation & Dependency Reconciliation
- **Documentation Parity:** Reconcile `README.md`, `architecture.md`, and `prd.md` with actual codebase reality (dependencies, test setup, storage architecture, API endpoints).

**Definition of Done (Phase 9):**
1. `/uploads/**` is completely protected; files can only be accessed via authenticated, workspace-checked endpoints (`GET /api/documents/{id}/file`).
2. Login rate limiting is active and verified against brute-force attempts.
3. Flyway migration `V7` adds `idx_documents_workspace_id` and query indexes.
4. `GET /api/documents` supports pagination and `GET /api/documents/{id}` returns single document details.
5. `ExtractionService` and `SanitizationUtils` refactored with zero duplicate code; `DocumentController` strictly delegates to services.
6. `GeminiClient` has exponential backoff retry; stuck documents are automatically reconciled or manually reprocessable.
7. Spring Boot Actuator `/actuator/health` is active, checks DB health, and is wired into Docker Compose.
8. `DocumentDetail.jsx` uses `Promise.allSettled` to prevent single-point-of-failure page crashes.
9. Full backend test suite passes (`mvn test`) with ≥70% service coverage, including cross-workspace isolation tests.
10. GitHub Actions CI pipeline passes cleanly on build and test execution.
11. `README.md` and documentation 100% accurately reflect the codebase without false claims.

---

## Phase 10 — Advanced Polish, Human-in-the-Loop & API Governance
**Goal:** Implement enterprise-grade features identified in the 9.5/10 tier of the evaluation report, including human-in-the-loop data correction, real-time status updates, OpenAPI documentation, and API governance.

- **Human-in-the-Loop Field Editing & Corrections:** Build an editable fields interface in `DocumentDetail.jsx` and a corresponding backend endpoint (`PATCH /api/documents/{id}/extraction`) to allow users to review, correct, and save extracted data, treating human corrections as authoritative ground truth that overrides future anomaly comparisons.
- **Automatic Status Polling / Real-Time Updates:** Implement automated polling with backoff (or WebSocket/SSE) on `Dashboard.jsx` and `DocumentDetail.jsx` so documents transition from `PENDING` to `PROCESSED` without requiring manual browser refreshes.
- **OpenAPI / Swagger Documentation:** Add `springdoc-openapi` to provide auto-generated, interactive API documentation at `/swagger-ui.html`.
- **Per-Workspace LLM Budget & Usage Guard:** Introduce per-workspace rate and budget limits on LLM invocations to prevent denial-of-wallet / budget exhaustion attacks.
- **Mobile Responsiveness & Accessibility (WCAG AA):** Conduct a mobile responsive audit across all screen widths (`sm`, `md`, `lg`, `xl`), ensure visible focus rings, and add proper `aria-label` attributes on icon-only buttons.
- **API Versioning:** Introduce clean URL versioning (`/api/v1/`) across all endpoints.
- **Frontend Component Tests:** Add Vitest and React Testing Library tests for critical UI components.

**Definition of Done (Phase 10):**
1. Users can edit extracted fields directly in `DocumentDetail.jsx`, saving corrections via `PATCH /api/documents/{id}/extraction`.
2. Dashboard and detail pages update processing status automatically without manual reload.
3. Interactive Swagger UI is available and functional at `/swagger-ui.html`.
4. Mobile layout and accessibility pass WCAG AA standards.
5. Per-workspace API budget/usage guards prevent runaway LLM costs.

---

## Phase 11 — Deployment & Demo Readiness
**Goal:** Publicly accessible, demo-ready deployment.

- Deploy frontend (Vercel/Netlify) and backend (Render/Railway), connect to hosted Postgres
- Seed the deployed DB with a demo workspace + sample documents for judges/evaluators
- Write a short `DEMO_SCRIPT.md`: exact click-path to show off all features in under 5 minutes
- Final pass on `memory.md` summarizing full project state

**Definition of Done:** A stranger can open the deployed URL, log in with demo credentials, and see the full flow work without local setup.

---

## Phase 12 (Stretch — only if ahead of schedule)

- [x] Phase 12.1: Batch upload (multiple files at once via drag-and-drop batch queue & `POST /api/documents/batch`)
- [x] Phase 12.2: Confidence scores on extracted fields (Gemini schema `fieldConfidences` + visual score pills)
- [x] Phase 12.3: Background job queue (RabbitMQ + Spring AMQP with dual-mode `@Async` fallback)
- [x] Phase 12.4: Multi-document comparative anomaly detection (duplicate invoices, price surge spikes >50%, payment term drift, and workspace vendor trends)
- [ ] Phase 12.5: 4th document type (KYC form)
- [ ] Phase 12.6: Billing simulation (Stripe test mode)

**Note:** Phases 0–11 and 12.1–12.4 are fully implemented, verified, and test-covered (38 passing automated tests).
