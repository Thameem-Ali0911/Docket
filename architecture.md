# ARCHITECTURE.md — System Architecture

**Project:** Docket — the AI second pair of eyes for contracts, invoices, and resumes
**Status:** v1.0

---

## 1. High-Level Architecture

```
                          ┌───────────────────────────┐
                          │        Frontend (Web)      │
                          │  React + Vite + Tailwind    │
                          └─────────────┬───────────────┘
                                        │ REST (JSON) / JWT auth
                                        ▼
                          ┌───────────────────────────┐
                          │       Backend API           │
                          │  Node.js + Express (or        │
                          │  FastAPI, see 3.2)          │
                          └───────┬───────────┬─────────┘
                                  │           │
                   ┌──────────────┘           └───────────────┐
                   ▼                                          ▼
        ┌─────────────────────┐                  ┌─────────────────────────┐
        │   Postgres (DB)      │                  │   File Storage (S3-      │
        │  users, workspaces,  │                  │   compatible / local     │
        │  documents, fields,  │                  │   disk for dev)         │
        │  templates, flags    │                  └─────────────────────────┘
        └─────────────────────┘
                   ▲
                   │
        ┌──────────┴──────────────────────────────────────────────┐
        │              Document Processing Pipeline (async job)      │
        │                                                            │
        │  1. OCR (if scanned image/PDF) → raw text                  │
        │  2. LLM Extraction call (Gemini) → structured JSON          │
        │  3. LLM Summarization call (Gemini) → summary text          │
        │  4. Template Diff/Anomaly check → flags                    │
        │  5. Persist results to DB                                  │
        └────────────────────────────────────────────────────────────┘
```

## 2. App Flow

1. **Sign up / Login** → user creates a workspace (or joins one)
2. **Upload document** → user selects doc type (Contract/Invoice/Resume/KYC Form) and uploads a file
3. File is stored in object storage; a `documents` row is created with status `pending`
4. Backend enqueues a processing job (see 3.4 for queue choice)
5. **Processing worker**:
   a. Detects if OCR is needed (scanned image / non-text PDF) → runs OCR → raw text
   b. Sends raw text to Gemini with a document-type-specific extraction prompt → structured JSON
   c. Sends raw text to Gemini for a short summary
   d. If a "standard template" exists for that workspace + doc type, runs a comparison prompt against it → produces anomaly flags
   e. Saves everything; sets document status to `processed` or `failed`
6. **Frontend polls / re-fetches** document status → renders extracted fields, summary, and flags
7. User can mark a document as the new "standard template" for its type at any time
8. User can export data (CSV/JSON) per document or per workspace

## 3. Tech Stack

### 3.1 Frontend
- **React 18** + **Vite** (fast dev/build)
- **Tailwind CSS** for styling (see design.md for theme tokens)
- **React Router** for navigation
- **TanStack Query (React Query)** for data fetching/caching
- **Recharts** for the anomaly/dashboard summary charts
- **Framer Motion (`motion`)** for micro-interactions, page/scroll transitions, and staggered reveals (added Phase 8.5 — see design.md §8)
- **React Three Fiber + drei** (thin React wrapper around Three.js) for the small set of ambient 3D elements defined in design.md §8 — scoped to hero/empty-state/decorative surfaces only, never used for core data tables or forms
- **@react-three/postprocessing** (optional, only if performance budget allows) for subtle bloom/glow on the aurora-gradient signature element

### 3.2 Backend
- **Java 17+ (LTS) with Spring Boot 3.x** — mature, battle-tested, strong typing, excellent tooling, and a natural fit if the student is more comfortable in the Java ecosystem
- **Spring Web (MVC)** for REST controllers
- **Spring Data JPA (Hibernate)** as the ORM
- **Flyway** for database migrations
- **Bean Validation (Jakarta Validation / `@Valid`)** for request/response validation
- **Spring Security** for JWT-based auth
- **Maven** (or Gradle — pick one and stay consistent) for dependency management and build

### 3.3 Database
- **PostgreSQL** — relational, fits structured extraction data + relational workspace/user model well
- Driver: standard **PostgreSQL JDBC driver**, managed automatically via Spring Data JPA
- Hosted free-tier options: Supabase, Neon, or Railway Postgres

### 3.4 Async & Queue Job Processing (Dual-Mode)
- **Dual-Mode Processing Architecture:**
  - Controlled by the `docket.processing.mode` configuration property (env var `PROCESSING_MODE`, defaults to `async`).
  - **`async` mode (default):** Uses Spring's built-in `@Async` with a dedicated `ThreadPoolTaskExecutor` (`AsyncConfig.java` — 4 core / 16 max / 100 queue capacity) and `@Scheduled` reconciliation scheduler (`DocumentReconciliationScheduler`). Requires zero external message brokers — ideal for lightweight local development.
  - **`queue` mode (RabbitMQ):** Uses Spring AMQP (`spring-boot-starter-amqp`) with a durable `DirectExchange` (`docket.exchange`), queue (`docket.document.processing`), and Jackson JSON message conversion (`RabbitMqConfig.java`). Uploads publish a lightweight `DocumentProcessingMessage(documentId)` via `DocumentQueuePublisher`. Background workers consume messages via `@RabbitListener` in `DocumentProcessingConsumer`, invoking the synchronous `processDocument()` pipeline with automatic ACK and error isolation.
- Both modes share the identical OCR → extraction → summarization → anomaly detection pipeline and reconciliation fallback.

### 3.5 OCR & PDF Parsing
- **Tess4J** (Java wrapper around the Tesseract OCR engine) for scanned image/PDF pages — free, open-source, sufficient for typed documents
- **Apache PDFBox** to extract text directly from digital (non-scanned) PDFs before falling back to OCR, and to rasterize PDF pages to images when OCR fallback is needed

### 3.6 AI / LLM Layer
- **Google Gemini API** called via plain HTTP using **Spring's `RestClient`/`WebClient`** (no official Google Gen AI Java SDK dependency added — call the REST API directly with a small typed client wrapper class), for:
  - Structured field extraction (prompted to return strict JSON per document type)
  - Summarization
  - Template-comparison / anomaly reasoning
- All three are separate, single-purpose prompts — never one giant prompt trying to do everything at once (see rules.md)
- LLM JSON responses are deserialized into dedicated **Java record/DTO classes** (one per document type) using **Jackson**, and validated with Bean Validation before being persisted — invalid output is rejected, never silently patched

### 3.6.1 Anomaly Detection Architecture (Hybrid)
- **Template Deviations (LLM-based):** `AnomalyService` compares newly extracted document text against the designated workspace `Template` using single-responsibility Gemini prompts (`AnomalyCheckPrompt`).
- **Multi-Document Comparative & Trend Anomalies (Deterministic / Cross-Doc):** `ComparativeAnomalyService` executes cross-document analysis across workspace document history:
  - **Duplicate Invoice Detection:** Flags identical invoice numbers across documents in the same workspace.
  - **Price Surge & Trend Spikes:** Calculates vendor historical averages; flags invoices with >50% surge above baseline.
  - **Payment Term Anomalies:** Validates payment windows (`dueDate` < `invoiceDate`) and identifies credit term contraction.
  - **Workspace Vendor Intelligence:** `GET /api/documents/trends` aggregates vendor spend, invoice velocity, trend deltas, and risk alerts without consuming LLM budget tokens.

### 3.7 File Storage
- Decoupled behind a `StorageService` interface with pluggable implementations:
  - **Local Disk (`LocalStorageServiceImpl`):** For local development (`/uploads`), served exclusively via authenticated, workspace-checked endpoints (`GET /api/documents/{id}/file`).
  - **S3-Compatible Object Storage (`S3StorageService`):** For staging/production environments (AWS S3, Cloudflare R2, or Supabase Storage) via AWS SDK for Java v2.

### 3.8 Auth & Security
- JWT-based session auth via **Spring Security + jjwt**, passwords hashed with **Spring Security's `BCryptPasswordEncoder`**
- Login attempt limiting & lockout protection on `AuthController.login`
- Workspace isolation enforced on every query and file download at the service layer

### 3.9 Observability & Health
- **Spring Boot Actuator** (`/actuator/health`, `/actuator/metrics`) with database connectivity verification, wired into Docker healthchecks
- MDC-based request correlation ID tracking (`X-Request-ID`) across async processing threads

### 3.10 Deployment
- **Frontend:** Vercel or Netlify (free tier)
- **Backend:** Render or Railway (free tier) — both support Java/Spring Boot via Docker or container runtime
- **DB:** Supabase/Neon free tier
- **Env config:** `application.yml` / `application-{profile}.yml` with environment variable placeholders (`${GEMINI_API_KEY}`), never committing real secrets (see rules.md)

## 4. Folder & File Structure

```
docket/
├── prd.md
├── architecture.md
├── rules.md
├── phases.md
├── design.md
├── memory.md
├── DEMO_SCRIPT.md            # 5-minute evaluator & judge walkthrough click-path
├── README.md
├── docker-compose.yml
├── .env.example
│
├── frontend/
│   ├── src/
│   │   ├── main.jsx
│   │   ├── App.jsx
│   │   ├── pages/
│   │   │   ├── Login.jsx
│   │   │   ├── Signup.jsx
│   │   │   ├── Dashboard.jsx
│   │   │   ├── DocumentDetail.jsx
│   │   │   ├── UploadDocument.jsx
│   │   │   └── TemplateManager.jsx
│   │   ├── components/
│   │   │   ├── layout/           # Navbar, Frosted Nav
│   │   │   ├── ui/               # AmbientAurora, Neo Cards, Buttons
│   │   │   └── AnomalyFlag.jsx   # Severity badges and descriptions
│   │   ├── lib/                  # api client, auth tokens, downloadExport, fetchBlobUrl, apiPatch
│   │   └── styles/               # index.css (Neomorphic design tokens, Aurora variables)
│   ├── index.html
│   └── package.json
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/docket/
│   │   │   │   ├── DocketApplication.java        # Spring Boot entrypoint (@EnableScheduling, @EnableAsync)
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java        # Spring Security + JWT filter chain + Swagger permit
│   │   │   │   │   └── OpenApiConfig.java         # springdoc-openapi Swagger UI + Bearer JWT scheme
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java        # /api/auth/*
│   │   │   │   │   ├── DocumentController.java    # /api/documents/* (incl. file stream, patch extraction, usage)
│   │   │   │   │   └── TemplateController.java    # /api/templates/*
│   │   │   │   ├── entity/                         # JPA entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Workspace.java
│   │   │   │   │   ├── Document.java
│   │   │   │   │   ├── Extraction.java            # AI fields + human_corrected_json audit
│   │   │   │   │   ├── Template.java
│   │   │   │   │   ├── AnomalyFlag.java
│   │   │   │   │   └── LlmUsage.java              # Daily LLM invocation tracking
│   │   │   │   ├── repository/                     # Spring Data JPA repositories
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── DocumentRepository.java
│   │   │   │   │   ├── ExtractionRepository.java
│   │   │   │   │   ├── SummaryRepository.java
│   │   │   │   │   ├── AnomalyFlagRepository.java
│   │   │   │   │   ├── TemplateRepository.java
│   │   │   │   │   └── LlmUsageRepository.java    # Atomic PostgreSQL ON CONFLICT upserts
│   │   │   │   ├── dto/                             # request/response + LLM-output DTOs
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── document/                  # DocumentListItemDto, DocumentExportDto
│   │   │   │   │   ├── extraction/                # ExtractionCorrectionRequest
					├── InvoiceExtractionDto.java
					├── ContractExtractionDto.java
					├── ResumeExtractionDto.java
					└── KycExtractionDto.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── OcrService.java
│   │   │   │   │   ├── ExtractionService.java        # Generic type-dispatched Gemini engine
│   │   │   │   │   ├── SummarizeService.java
│   │   │   │   │   ├── AnomalyService.java
│   │   │   │   │   ├── ExportService.java            # CSV & JSON serializer
│   │   │   │   │   ├── StorageService.java           # Interface + LocalStorageServiceImpl
│   │   │   │   │   ├── LlmBudgetService.java         # Denial-of-wallet daily budget enforcer
│   │   │   │   │   ├── DocumentReconciliationScheduler.java # Auto-reprocess stuck PENDING docs
│   │   │   │   │   └── GeminiClient.java             # Exponential backoff retry loop
│   │   │   │   ├── prompt/
					├── ExtractInvoicePrompt.java
					├── ExtractContractPrompt.java
					├── ExtractResumePrompt.java
					├── ExtractKycPrompt.java
					├── SummarizePrompt.java
					└── AnomalyCheckPrompt.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtService.java
│   │   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   │   ├── LoginRateLimiter.java         # 5 attempts / 15 min brute-force guard
│   │   │   │   │   └── CorrelationIdFilter.java      # MDC X-Request-ID tracking
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java   # @ControllerAdvice structured errors
│   │   │   │       └── ApiException.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/                     # Flyway SQL migration files (V1 -> V9)
│   │   └── test/
│   │       └── java/com/docket/                       # 25 JUnit 5 / Mockito tests
│   ├── pom.xml
│   └── Dockerfile
```

## 5. Data Model (Core Tables)

- **users** (id, email, password_hash, workspace_id, created_at)
- **workspaces** (id, name, plan_tier [FREE/PRO/ENTERPRISE], subscription_status, stripe_customer_id, stripe_subscription_id, billing_period_start, billing_period_end, daily_llm_budget, created_at)
- **documents** (id, workspace_id, type [CONTRACT/INVOICE/RESUME/KYC_FORM], file_url, status, uploaded_at)
- **extractions** (id, document_id, field_key, field_value, confidence)
- **summaries** (id, document_id, summary_text)
- **templates** (id, workspace_id, type, document_id — points to which document is the "standard")
- **anomaly_flags** (id, document_id, field_key, description, severity)
- **simulated_invoices** (id, workspace_id, invoice_number, amount_cents, currency, status, description, period_start, period_end, created_at)

## 6. API Design (representative endpoints)

```
POST   /api/auth/signup
POST   /api/auth/login
GET    /api/documents                    # Supports ?page=0&size=20&sort=uploadedAt,desc
POST   /api/documents/upload
GET    /api/documents/{id}               # Single document metadata + list-item shape
GET    /api/documents/{id}/file          # Authenticated, workspace-scoped file stream
GET    /api/documents/{id}/extraction    # Extracted structured fields
PATCH  /api/documents/{id}/extraction    # Human-in-the-loop field corrections (Phase 10)
GET    /api/documents/{id}/summary       # Plain-English summary
GET    /api/documents/{id}/anomalies     # Template comparison flags
POST   /api/documents/{id}/reprocess     # Re-trigger failed OCR/extraction job
POST   /api/templates                    # Set a document as the standard template
GET    /api/templates/{type}
DELETE /api/templates/{type}
GET    /api/documents/{id}/export?format={json|csv}
GET    /api/documents/export?format={json|csv}
GET    /api/billing/subscription         # Subscription tier, limits, and live usage meters (Phase 12.6)
GET    /api/billing/invoices             # Simulated invoice history (Phase 12.6)
POST   /api/billing/upgrade              # Upgrade/downgrade subscription plan (Phase 12.6)
POST   /api/billing/webhook/simulate     # Interactive Stripe test-mode webhook simulator (Phase 12.6)
GET    /actuator/health                  # Database + application health check
GET    /swagger-ui.html                  # Interactive OpenAPI docs (Phase 10)
```

## 8. Prerequisites & Local Setup

Everything needed on a developer's machine before Phase 0 can start.

### Core Runtimes
- **Java 17+ (JDK, LTS)** — backend (Spring Boot). Use Temurin/Adoptium or Oracle JDK.
- **Maven 3.9+** (or Gradle 8+, if that's the chosen build tool — pick one and stay consistent)
- **Node.js 18+ and npm** — frontend (React/Vite)
- **Git** — version control

### Database
- **PostgreSQL 14+** — install locally, OR skip local install and use a free-tier hosted DB (Supabase/Neon/Railway) and point `application.yml` at it. Hosted is the recommended path for a solo student project — one less thing to configure/debug locally.

### OCR / PDF System Dependencies
Tess4J bundles JNA bindings to the native Tesseract library, but the native Tesseract engine and trained language data still need to be present on the machine.

- **Tesseract OCR engine + trained data** (`tessdata`)
  - Windows: installer from the UB-Mannheim Tesseract build; note the install path (needed for `TESSDATA_PREFIX`)
  - Mac: `brew install tesseract`
  - Linux: `sudo apt install tesseract-ocr libtesseract-dev`
- Set the **`TESSDATA_PREFIX`** environment variable to the folder containing `tessdata/` so Tess4J can find the trained language files
- **Apache PDFBox** is a pure-Java library (added via Maven/Gradle dependency) — no separate system install needed for PDF text extraction/rasterization

### API Access
- **Google Gemini API key** — sign up at aistudio.google.com/apikey, generate a key, place it in `backend/src/main/resources/application-dev.yml` (or as an environment variable referenced via `${GEMINI_API_KEY}`). Never commit real keys — keep only placeholders in any committed `application.yml` (see rules.md §5).

### Editor / Tooling
- **IntelliJ IDEA** (Community or Ultimate) — recommended for Spring Boot: built-in Spring Initializr, run configs, and JPA/SQL tooling. VS Code with the "Extension Pack for Java" + "Spring Boot Extension Pack" also works.
- Frontend extensions (if using VS Code for the frontend half): ESLint, Prettier, Tailwind CSS IntelliSense
- **Postman or Thunder Client** — for testing endpoints; pair with **springdoc-openapi** (optional dependency) to get an auto-generated Swagger UI similar to FastAPI's `/docs`

### Optional
- **pgAdmin or TablePlus** — GUI for inspecting Postgres data during development

## 8.1 Running Everything via Docker Compose (recommended)

Rather than installing Java/Maven/Node/Postgres/Tesseract natively, the whole stack (Postgres, backend, frontend) can be run with a single command via the root-level `docker-compose.yml`:

```bash
cp .env.example .env      # fill in GEMINI_API_KEY at minimum
docker compose up --build
```

This starts:
- `db` — Postgres 16, with a named volume (`docket_pgdata`) so data survives restarts
- `rabbitmq` — RabbitMQ 3 Management Alpine, message broker on port 5672 and web management dashboard on `http://localhost:15672` (guest/guest)
- `backend` — built from `backend/Dockerfile` (multi-stage: Maven build → Temurin JRE runtime, with the native Tesseract engine installed for OCR), on `http://localhost:8080` (configured with `PROCESSING_MODE=queue`)
- `frontend` — built from `frontend/Dockerfile` (multi-stage: `npm run build` → served by nginx), on `http://localhost:5173`

The backend reads `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`, `JWT_SECRET`, and `GEMINI_API_KEY` from environment variables (see `backend/src/main/resources/application.yml` — all have safe local-dev defaults so `mvn spring-boot:run` against a local Postgres still works unchanged). The frontend's `VITE_API_BASE_URL` is a **build-time** arg (Vite bakes env vars into the JS bundle), passed through from `.env` via `docker-compose.yml`.

Native installs (Java, Maven, Node, Postgres, Tesseract) are still documented above and remain fully supported — Docker is an alternative, not a replacement, for local development.

### Sanity Check
Run these after installing everything to confirm the environment is ready:

```bash
java -version          # should show 17+
mvn -version            # should show Maven 3.9+ and pick up the correct JDK
node --version           # should show 18+
psql --version           # only if Postgres installed locally
tesseract --version
echo $TESSDATA_PREFIX    # (or echo %TESSDATA_PREFIX% on Windows) confirms the path is set
```

If any of these fail, resolve it before starting Phase 0 — OCR (Phase 3) will silently fail later if `TESSDATA_PREFIX` isn't correctly set or the native Tesseract binary isn't installed.

---

## 9. Why These Choices (Rationale)

- **Java + Spring Boot over Python/Node**: Spring Boot is a mature, heavily-documented enterprise framework with strong typing, a huge ecosystem, and is a natural fit for a student more comfortable in Java. Spring Data JPA, Spring Security, and Flyway cover ORM, auth, and migrations as first-class, well-integrated pieces rather than bolted-on libraries.
- **Postgres over MongoDB**: extracted fields, flags, and templates are naturally relational (a document has many fields, a workspace has one template per type) — a relational model avoids messy nested-document queries.
- **Gemini for extraction over a classic NLP/regex pipeline**: contract/resume documents are too varied in language for regex; an LLM with a strict JSON-output prompt generalizes far better and is realistic for a document-intelligence SaaS in 2026.
- **Jackson + Bean Validation for LLM output**: since Spring already uses Jackson for JSON (de)serialization and Bean Validation for request validation, reusing both to validate Gemini's JSON extraction output keeps one consistent pattern across the backend instead of introducing a new library just for this purpose.
- **Synchronous processing for MVP**: a queue adds real infrastructure complexity (Kafka/RabbitMQ, consumers) that isn't worth it until the core extraction quality is proven. Introduce it only as a stretch goal.
- **Tess4J over cloud OCR APIs**: free, no per-page cost, good enough for typed/clean documents which is the MVP's stated scope.
