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
        │  2. LLM Extraction call (Claude) → structured JSON          │
        │  3. LLM Summarization call (Claude) → summary text          │
        │  4. Template Diff/Anomaly check → flags                    │
        │  5. Persist results to DB                                  │
        └────────────────────────────────────────────────────────────┘
```

## 2. App Flow

1. **Sign up / Login** → user creates a workspace (or joins one)
2. **Upload document** → user selects doc type (Contract/Invoice/Resume) and uploads a file
3. File is stored in object storage; a `documents` row is created with status `pending`
4. Backend enqueues a processing job (see 3.4 for queue choice)
5. **Processing worker**:
   a. Detects if OCR is needed (scanned image / non-text PDF) → runs OCR → raw text
   b. Sends raw text to Claude with a document-type-specific extraction prompt → structured JSON
   c. Sends raw text to Claude for a short summary
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

### 3.4 Async Job Processing
- MVP simplicity: process synchronously inside the request thread, or offload with Spring's built-in `@Async` + a `ThreadPoolTaskExecutor` (no extra infra needed) since docs are short and demo-scale
- If time permits (stretch): introduce a real queue — **Spring Kafka** or a simpler **RabbitMQ + Spring AMQP** setup — so uploads return immediately and processing happens in a background consumer

### 3.5 OCR & PDF Parsing
- **Tess4J** (Java wrapper around the Tesseract OCR engine) for scanned image/PDF pages — free, open-source, sufficient for typed documents
- **Apache PDFBox** to extract text directly from digital (non-scanned) PDFs before falling back to OCR, and to rasterize PDF pages to images when OCR fallback is needed

### 3.6 AI / LLM Layer
- **Anthropic API (Claude)** called via plain HTTP using **Spring's `RestClient`/`WebClient`** (no official Anthropic Java SDK — call the REST API directly with a small typed client wrapper class), for:
  - Structured field extraction (prompted to return strict JSON per document type)
  - Summarization
  - Template-comparison / anomaly reasoning
- All three are separate, single-purpose prompts — never one giant prompt trying to do everything at once (see rules.md)
- LLM JSON responses are deserialized into dedicated **Java record/DTO classes** (one per document type) using **Jackson**, and validated with Bean Validation before being persisted — invalid output is rejected, never silently patched

### 3.7 File Storage
- Dev: local disk (`/uploads`), served via a simple Spring `Resource`-based download endpoint
- Production-style demo: any S3-compatible bucket via the **AWS SDK for Java v2** (AWS S3 free tier, Cloudflare R2, or Supabase Storage)

### 3.8 Auth
- JWT-based session auth via **Spring Security + jjwt** (or `nimbus-jose-jwt`), passwords hashed with **Spring Security's `BCryptPasswordEncoder`**
- Spring Security filter chain used for auth guards on protected routes
- No third-party OAuth needed for MVP (keep it simple)

### 3.9 Deployment
- **Frontend:** Vercel or Netlify (free tier)
- **Backend:** Render or Railway (free tier) — both support Java/Spring Boot via a Docker build, or a plain `java -jar` start command
- **DB:** Supabase/Neon free tier
- **Env config:** `application.yml` / `application-{profile}.yml` with environment variable placeholders (`${ANTHROPIC_API_KEY}`), never committing real secrets (see rules.md)

## 4. Folder & File Structure

```
docket/
├── prd.md
├── architecture.md
├── rules.md
├── phases.md
├── design.md
├── memory.md
├── README.md
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
│   │   │   ├── layout/           # Navbar, Sidebar, PageShell
│   │   │   ├── documents/        # DocumentCard, DocumentTable, StatusBadge
│   │   │   ├── extraction/       # FieldTable, SummaryCard, AnomalyFlag
│   │   │   └── ui/                # shared buttons, inputs, modal (design.md tokens)
│   │   ├── hooks/                # useAuth, useDocuments, useUpload
│   │   ├── lib/                  # api client, query client setup
│   │   ├── types/                # shared JS shape helpers/JSDoc typedefs (mirrors backend schemas)
│   │   └── styles/                # tailwind.css, theme tokens
│   ├── index.html
│   ├── tailwind.config.js
│   └── package.json
│
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/docket/
│   │   │   │   ├── DocketApplication.java        # Spring Boot entrypoint
│   │   │   │   ├── config/
│   │   │   │   │   ├── SecurityConfig.java        # Spring Security + JWT filter chain
│   │   │   │   │   └── WebClientConfig.java       # RestClient/WebClient bean for Anthropic API
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AuthController.java        # /api/auth/*
│   │   │   │   │   ├── DocumentController.java    # /api/documents/*
│   │   │   │   │   ├── TemplateController.java    # /api/templates/*
│   │   │   │   │   └── WorkspaceController.java   # /api/workspace/*
│   │   │   │   ├── entity/                         # JPA entities
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Workspace.java
│   │   │   │   │   ├── Document.java
│   │   │   │   │   ├── Extraction.java
│   │   │   │   │   ├── Template.java
│   │   │   │   │   └── AnomalyFlag.java
│   │   │   │   ├── repository/                     # Spring Data JPA repositories
│   │   │   │   │   ├── UserRepository.java
│   │   │   │   │   ├── DocumentRepository.java
│   │   │   │   │   └── ...
│   │   │   │   ├── dto/                             # request/response + LLM-output DTOs
│   │   │   │   │   ├── auth/
│   │   │   │   │   ├── document/
│   │   │   │   │   ├── InvoiceExtractionDto.java    # strict schema for invoice extraction JSON
│   │   │   │   │   ├── ContractExtractionDto.java
│   │   │   │   │   └── ResumeExtractionDto.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── OcrService.java              # Tess4J + PDFBox logic
│   │   │   │   │   ├── ExtractionService.java        # Claude call: extract fields
│   │   │   │   │   ├── SummarizeService.java         # Claude call: summarize
│   │   │   │   │   ├── AnomalyService.java           # Claude call: compare vs template
│   │   │   │   │   ├── StorageService.java           # file upload/retrieve (local disk / S3)
│   │   │   │   │   └── AnthropicClient.java          # thin wrapper around Claude REST API
│   │   │   │   ├── prompt/
│   │   │   │   │   ├── ExtractInvoicePrompt.java
│   │   │   │   │   ├── ExtractContractPrompt.java
│   │   │   │   │   ├── ExtractResumePrompt.java
│   │   │   │   │   ├── SummarizePrompt.java
│   │   │   │   │   └── AnomalyCheckPrompt.java
│   │   │   │   ├── security/
│   │   │   │   │   ├── JwtService.java               # token generation/validation
│   │   │   │   │   └── JwtAuthFilter.java            # per-request auth filter
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java   # @ControllerAdvice
│   │   │   │       └── ApiException.java
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       └── db/migration/                     # Flyway SQL migration files
│   │   │           └── V1__init_schema.sql
│   │   └── test/
│   │       └── java/com/docket/                       # JUnit + Spring Boot Test suite
│   ├── pom.xml                                          # or build.gradle if using Gradle
│   └── .env.example                                     # reference only — real config lives in application-*.yml
│
└── docs/
    └── sample-documents/          # anonymized/synthetic test files
```

## 5. Data Model (Core Tables)

- **users** (id, email, password_hash, workspace_id, created_at)
- **workspaces** (id, name, created_at)
- **documents** (id, workspace_id, type [contract/invoice/resume], file_url, status, uploaded_at)
- **extractions** (id, document_id, field_key, field_value, confidence)
- **summaries** (id, document_id, summary_text)
- **templates** (id, workspace_id, type, document_id — points to which document is the "standard")
- **anomaly_flags** (id, document_id, field_key, description, severity)

## 6. API Design (representative endpoints)

```
POST   /api/auth/signup
POST   /api/auth/login
GET    /api/documents
POST   /api/documents/upload
GET    /api/documents/:id
POST   /api/templates              # mark a document as the standard template
GET    /api/templates/:type
GET    /api/documents/:id/export
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
- **Anthropic API key** — sign up at console.anthropic.com, generate a key, place it in `backend/src/main/resources/application-dev.yml` (or as an environment variable referenced via `${ANTHROPIC_API_KEY}`). Never commit real keys — keep only placeholders in any committed `application.yml` (see rules.md §5).

### Editor / Tooling
- **IntelliJ IDEA** (Community or Ultimate) — recommended for Spring Boot: built-in Spring Initializr, run configs, and JPA/SQL tooling. VS Code with the "Extension Pack for Java" + "Spring Boot Extension Pack" also works.
- Frontend extensions (if using VS Code for the frontend half): ESLint, Prettier, Tailwind CSS IntelliSense
- **Postman or Thunder Client** — for testing endpoints; pair with **springdoc-openapi** (optional dependency) to get an auto-generated Swagger UI similar to FastAPI's `/docs`

### Optional
- **pgAdmin or TablePlus** — GUI for inspecting Postgres data during development

## 8.1 Running Everything via Docker Compose (recommended)

Rather than installing Java/Maven/Node/Postgres/Tesseract natively, the whole stack (Postgres, backend, frontend) can be run with a single command via the root-level `docker-compose.yml`:

```bash
cp .env.example .env      # fill in ANTHROPIC_API_KEY at minimum
docker compose up --build
```

This starts:
- `db` — Postgres 16, with a named volume (`docket_pgdata`) so data survives restarts
- `backend` — built from `backend/Dockerfile` (multi-stage: Maven build → Temurin JRE runtime, with the native Tesseract engine installed for OCR), on `http://localhost:8080`
- `frontend` — built from `frontend/Dockerfile` (multi-stage: `npm run build` → served by nginx), on `http://localhost:5173`

The backend reads `SPRING_DATASOURCE_URL`/`_USERNAME`/`_PASSWORD`, `JWT_SECRET`, and `ANTHROPIC_API_KEY` from environment variables (see `backend/src/main/resources/application.yml` — all have safe local-dev defaults so `mvn spring-boot:run` against a local Postgres still works unchanged). The frontend's `VITE_API_BASE_URL` is a **build-time** arg (Vite bakes env vars into the JS bundle), passed through from `.env` via `docker-compose.yml`.

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
- **Claude for extraction over a classic NLP/regex pipeline**: contract/resume documents are too varied in language for regex; an LLM with a strict JSON-output prompt generalizes far better and is realistic for a document-intelligence SaaS in 2026.
- **Jackson + Bean Validation for LLM output**: since Spring already uses Jackson for JSON (de)serialization and Bean Validation for request validation, reusing both to validate Claude's JSON extraction output keeps one consistent pattern across the backend instead of introducing a new library just for this purpose.
- **Synchronous processing for MVP**: a queue adds real infrastructure complexity (Kafka/RabbitMQ, consumers) that isn't worth it until the core extraction quality is proven. Introduce it only as a stretch goal.
- **Tess4J over cloud OCR APIs**: free, no per-page cost, good enough for typed/clean documents which is the MVP's stated scope.
