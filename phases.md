# PHASES.md — Project Build Phases

**Project:** Docket

**Purpose:** Break the project into small, independently completable phases so an AI coding assistant (or the student) never has to hold the whole project in mind at once. Complete phases **in order**. Do not start a phase until the previous one's "Definition of Done" is met.

---

## Phase 0 — Project Setup
**Goal:** Empty-but-running skeleton for both frontend and backend.

- Initialize `frontend/` (Vite + React + TS + Tailwind) and `backend/` (Spring Boot 3.x project via Spring Initializr — Web, Data JPA, Security, Validation, Flyway dependencies)
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
- Empty `Dashboard.tsx` page showing "No documents yet"
- Backend: `AuthController.java` route + `security/JwtService.java` / `security/JwtAuthFilter.java` + `config/SecurityConfig.java` for the filter chain

**Definition of Done:** A new user can sign up, log in, get redirected to a dashboard, and refresh the page without losing session (JWT persisted in storage/cookie).

---

## Phase 2 — Document Upload (Invoice type only, first vertical slice)
**Goal:** A logged-in user can upload an invoice PDF/image and see it appear in their document list with status `pending` → `processed` (extraction/summary can be stubbed/fake for now).

- DB schema: `documents`
- Upload endpoint: validate file type/size, store file (local disk for dev), create `documents` row
- Frontend: `UploadDocument.tsx` page with file picker + document type selector (only Invoice enabled this phase)
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
**Goal:** Structured invoice fields (vendor, invoice #, date, line items, total) are extracted via Claude and shown in the UI.

- Build `prompt/ExtractInvoicePrompt.java` — strict JSON-only output, grounded-in-text instruction (see rules.md §3)
- Build `service/ExtractionService.java` to call Claude via `AnthropicClient`, deserialize with Jackson into `InvoiceExtractionDto`, validate with `@Valid`, persist to `extractions` table
- Frontend: `DocumentDetail.tsx` shows extracted fields in a clean table next to a preview of the original file

**Definition of Done:** Uploading a sample invoice produces correct, review-able structured fields for at least 8/10 test invoices.

---

## Phase 5 — Summarization (all types, since it's type-agnostic)
**Goal:** Every processed document gets a short plain-English summary.

- Build `prompt/SummarizePrompt.java` and `service/SummarizeService.java`
- Store in `summaries` table
- Display in `DocumentDetail.tsx` as a "Summary" card

**Definition of Done:** Every processed document shows a coherent 3-5 sentence summary.

---

## Phase 6 — Template Manager & Anomaly Flagging (Invoice)
**Goal:** User can designate a document as the "standard template" for invoices, and new invoices get compared against it.

- DB schema: `templates`, `anomaly_flags`
- `TemplateManager.tsx` page: pick/upload a document to mark as the standard for a type
- Build `prompt/AnomalyCheckPrompt.java` + `service/AnomalyService.java`: compares new document's extracted text/fields against the template's, returns a list of flagged differences with short explanations
- Frontend: `AnomalyFlag.tsx` component shows ⚠️/✅ per relevant field in `DocumentDetail.tsx`

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

## Phase 9 — Deployment & Demo Readiness
**Goal:** Publicly accessible, demo-ready deployment.

- Deploy frontend (Vercel/Netlify) and backend (Render/Railway), connect to hosted Postgres
- Seed the deployed DB with a demo workspace + sample documents for judges/evaluators
- Write a short `DEMO_SCRIPT.md`: exact click-path to show off all features in under 5 minutes
- Final pass on `memory.md` summarizing full project state

**Definition of Done:** A stranger can open the deployed URL, log in with demo credentials, and see the full flow work without local setup.

---

## Phase 10 (Stretch — only if ahead of schedule)

- Batch upload
- Confidence scores on extracted fields
- Background job queue (Spring Kafka or RabbitMQ + Spring AMQP)
- 4th document type (KYC form)
- Billing simulation (Stripe test mode)

**Note:** Do not start Phase 10 items until Phases 0–9 are fully complete and demo-stable.
