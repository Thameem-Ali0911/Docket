# Docket — Brutally Honest Production Evaluation Report

> **Reviewer role:** Principal Software Engineer + Software Architect + Tech Lead + Security Engineer + DevOps + Product Engineer
> **Standard applied:** Senior SWE interview bar / production deployment review
> **Codebase reviewed:** `main` branch, Thameem-Ali0911/Docket

---

## PRELIMINARY OBSERVATIONS (before scoring)

1. **This is a final-year engineering project**, and it says so itself in the README ("Final year engineering project — B2B SaaS-style document intelligence platform... scoped tightly to be genuinely buildable in one academic year"). That context matters and the scores below reflect it.
2. **This codebase is more disciplined than the average student project**, and more disciplined than most junior-engineer production code. Flyway migrations instead of `ddl-auto=update`, workspace-scoped authorization enforced at the service layer on *every* endpoint, `catch (Throwable)` used correctly (not lazily) around native OCR bindings, a non-root Docker user, path-traversal protection on file storage, and a real docker-compose stack. These are not accidents.
3. **There are real gaps that would matter in production**: zero tests despite the README explicitly claiming a JUnit 5 + Mockito test suite exists, an unauthenticated static file endpoint that serves every uploaded document (invoices, contracts, resumes) to anyone with the URL, no pagination anywhere, no rate limiting on login, real user-uploaded PDFs and images committed to git despite a `.gitignore` rule that claims to exclude them, and three near-identical 30-line extraction methods that should be one generic method.

Both realities are true simultaneously. This report names both without softening either.

---

## SECTION 1 — DETAILED AREA SCORES

---

### 1. Business Problem Solving — 7/10

**What is good:**
The problem is real and the scope is honest about being real: extract structured fields from invoices/contracts/resumes, summarize them, and flag deviations from a workspace's saved "template" document. Unlike a lot of student AI wrappers, this isn't "call an LLM and print the output" — there's a genuine product loop (upload → OCR → structured extraction → summary → anomaly diff against a template), and the template/anomaly-diff feature in particular is a legitimately useful, non-obvious idea for this problem space (compare an invoice against your organization's known-good invoice, not against a hardcoded rule).

**What is bad:**
There's no way to *fix* a bad extraction. If Gemini extracts the wrong invoice total, there is no edit/correct UI — the wrong number sits there. There's no multi-document comparison across time (e.g. "flag invoices from this vendor that jumped 20% month over month"), which is the natural next feature for the anomaly engine already built. Processing is asynchronous with no push notification — the user has to keep refreshing the document list to see if OCR/extraction finished (there is no polling or WebSocket either, confirmed by reading `Dashboard.jsx` and `DocumentDetail.jsx` — data is fetched once on mount).

**Why it is bad:**
An "AI-powered document intelligence" product whose core value is *trustworthy structured data* has no correction mechanism when the AI gets it wrong — and LLM extraction from OCR'd text is exactly the kind of pipeline that gets it wrong on messy real-world scans. Silently trusting Gemini's output with no human-in-the-loop correction step undermines the product's own value proposition.

**Production risk:** None acute — this is a product-completeness gap, not a crash risk.

**Senior engineer improvement:** Add an editable fields view on `DocumentDetail.jsx` that PATCHes corrected values back to the `Extraction` row, and treat corrected fields as ground truth that overrides future anomaly comparisons for that field.

---

### 2. Product Design — 6/10

**What is good:**
The UI is visually considered — there's a deliberate design language ("Aurora Obsidian edition" per the code comments), Tailwind v4, Framer Motion for transitions, and a genuinely custom `AmbientAurora.jsx` background component rather than an unstyled Bootstrap default. Toasts, loading states, and empty states appear across the pages reviewed.

**What is bad:**
No mobile responsiveness testing signal anywhere in the codebase — no `useMediaQuery`, no responsive Tailwind breakpoints beyond ad hoc `md:`/`lg:` classes scattered in a few places. No accessibility affordances: no `aria-label` on icon-only buttons, no visible focus states beyond browser defaults. `DocumentDetail.jsx` fetches the *entire* document list just to find one document by ID (`documents.find(d => String(d.id) === String(id))`) instead of calling a single-document endpoint — which doesn't exist. That's a product/API design gap, not just a performance one: the frontend has no way to deep-link to a single document without loading everyone else's too.

**Why it is bad:**
Fetching the whole workspace's document list to render one detail page means the detail page's load time scales with total documents in the workspace, and it does this on every visit — a design smell that gets worse as the product succeeds.

**Senior improvement:** Add `GET /api/documents/{id}` returning `DocumentListItemDto`; have `DocumentDetail.jsx` call it directly instead of filtering the bulk list.

---

### 3. Requirements Engineering — 6/10

**What is good:**
This is the strongest area of the project, and unusually so for a student submission — there are five separate planning documents (`prd.md`, `architecture.md`, `design.md`, `phases.md`, `rules.md`) plus a 76KB `memory.md` development log and a 20KB `AGENTS.md`. The `rules.md` file explicitly states real engineering constraints ("Every document/data query must be scoped by `workspace_id`... Add this check at the service layer, not just relying on frontend routing") — and the codebase actually follows that rule, consistently, everywhere it was checked.

**What is bad:**
The documentation has drifted from the code in ways that matter. The README's tech stack table lists **TanStack Query and Recharts** for the frontend — neither appears in `package.json`; the actual data fetching is manual `useEffect` + `fetch` with no caching layer. The README's Testing section states plainly: **"Backend: JUnit 5 + Spring Boot Test + Mockito"** — there are zero test files anywhere in `backend/src/test` (the directory doesn't exist). `rules.md` §5 states sample/test documents "must be synthetic or public templates only — never real, identifiable client or personal data" — yet the repository has 41 real uploaded files committed under `/uploads` and `/backend/uploads`, including scanned invoices and what appear to be personal document images.

**Why it is bad:**
This is the exact same failure mode flagged in the SCMS review under a different name: a README that describes the *intended* system rather than the *actual* one. A stated test suite that doesn't exist is worse than no claim at all, because it actively misleads anyone (a recruiter, a teammate, future-you in six months) who trusts the README instead of `find . -name "*Test.java"`.

**Production risk:** Low technical risk, high trust risk — this is the kind of discrepancy a technical interviewer or code reviewer finds in under two minutes and immediately loses confidence over.

---

### 4. System Design — 6/10

**What is good:**
Correct shape for the problem: React SPA → Spring Boot REST API → PostgreSQL, stateless JWT auth, async `@Async` thread pool (bounded, with `CallerRunsPolicy` backpressure — a genuinely correct choice, not the SCMS-style `SimpleAsyncTaskExecutor` trap) for the OCR/LLM pipeline so uploads don't block the request thread. Docker Compose wires Postgres, backend, and frontend together with health checks and named volumes. Flyway migrations are the actual migration strategy, not a hope.

**What is bad:**
Single-node, no horizontal scaling story — but unlike SCMS, this project doesn't *pretend* otherwise (no fragile in-memory rate limiter masquerading as a real one, because there's no rate limiter at all — see Security). File storage is local disk (`./uploads` volume-mounted) with no S3/object-storage abstraction, so scaling the backend beyond one instance breaks file access entirely — a document uploaded to instance A is invisible to a request served by instance B. No caching layer for the Gemini calls, meaning re-processing the same document re-spends the LLM API budget. No queue (SQS/RabbitMQ) — `@Async` is in-process, so a backend restart mid-processing silently drops in-flight OCR/extraction jobs with the document stuck at `PENDING` forever (there's no reconciliation job to catch and retry orphaned `PENDING` documents).

**Why it is bad:**
The local-disk storage is the single-node equivalent of SCMS's in-memory rate limiter: it works perfectly in the demo, and is a guaranteed outage the moment anyone scales the backend to two containers behind a load balancer.

**Senior improvement:** Swap `StorageService` for an interface with a local-disk implementation (dev) and an S3-compatible implementation (prod); add a scheduled job that finds `Document`s `PENDING` for over N minutes and re-triggers processing.

---

### 5. Software Architecture — 6.5/10

**What is good:**
Clean layered architecture, strictly respected — every controller method reviewed delegates to a service; no controller touches a repository for business logic beyond the intentionally thin `AuthController.me()` lookup. DTOs shield the JSON API surface for exports and error responses. Constructor injection everywhere (no field `@Autowired`), which is the correct choice for testability even though no tests exist yet to benefit from it. `@Transactional` is applied correctly and narrowly (`AnomalyService.checkAnomalies`) rather than sprayed across every method.

**What is bad:**
`ExtractionService` has three methods — `extractInvoiceFields`, `extractContractFields`, `extractResumeFields` — that are ~40 lines each and differ only in which prompt/schema/DTO class is used. This is copy-paste, not architecture: a bug fixed in the invoice path (e.g. the NUL-byte stripping logic) has to be manually re-applied to the other two, and on inspection it *was* correctly re-applied all three times — meaning someone is doing find-and-replace maintenance on triplicated logic. `AnomalyService`, `SummarizeService`, and `ExtractionService` each independently define their own private `stripNulBytes()` helper — the same seven-line method exists three times in three different files.

**Why it is bad:**
Triplicated logic is a time bomb with a randomized fuse: it works today because whoever wrote it was careful to update all three copies. The moment a second person touches this code, or the original author is tired, one copy gets the fix and the other two don't — and now you have three services with silently divergent behavior for the same conceptual operation.

**Senior improvement:** Extract a generic `extractFields(Document, String promptText, String schema, Class<T> dtoType)` method using a `Map<DocumentType, ExtractorConfig>` or simple `switch`, collapsing three methods into one. Move `stripNulBytes` to a single shared utility class.

---

### 6. Backend Engineering — 7/10

**What is good:**
Spring Boot 3.5 on Java 25 — current, not stale. `ddl-auto: none` with Flyway doing schema management — the single most important production-safety decision this backend makes, and it makes it correctly where SCMS did not. The `@Async` error handling is genuinely sophisticated: `DocumentProcessingService.processDocumentAsync` catches `Throwable` (not `Exception`) specifically because Tess4J is a JNI wrapper and native library failures surface as `Error`, not `Exception` — and the code comment explains exactly why, correctly. `AsyncConfig` uses a bounded thread pool with `CallerRunsPolicy` instead of the unbounded-thread-per-task default. `Validator`-based Bean Validation is run against every LLM response before it's trusted and persisted — the code does not blindly trust Gemini's JSON.

**What is bad:**
Zero tests — not one `@SpringBootTest`, `@WebMvcTest`, or JUnit method exists, despite `spring-boot-starter-test` being a declared Maven dependency and the README claiming a full test suite. `AuthService.signup()` creates a `Workspace` and a `User` in two separate `save()` calls inside one `@Transactional` method — correct for atomicity, but there's no email-format or password-strength validation beyond whatever `@Valid` enforces on `SignupRequest` (worth checking that DTO specifically — see Security). `getDocumentForWorkspace` is called separately inside `getExtraction`, `getSummary`, and `getAnomalies` in `DocumentController` — three near-identical "look up user, look up document, verify workspace match" round trips for what could be one call whose result is reused, though this is a minor N+1-adjacent inefficiency rather than a correctness bug.

**Why it is bad:**
The `@Async` thread pool and `Throwable`-catching discipline show real engineering maturity — but none of it is verifiable without tests. A regression in `DocumentProcessingService`'s status-transition logic (e.g. a document silently never leaving `PENDING`) would not be caught by anything before a real user notices their upload never finished.

**Senior improvement:** At minimum, add `@DataJpaTest` coverage for the repository query methods and a `@SpringBootTest` for the `AuthService` signup/login happy path and the email-uniqueness conflict path — those are the two flows where a silent regression is worst.

---

### 7. Frontend Engineering — 6/10

**What is good:**
React 19 with functional components/hooks throughout. No class components, no obvious `useEffect` infinite-loop bugs in the files reviewed. The `apiFetch` wrapper in `lib/api.js` centralizes 401/403 handling (auto-clear-token-and-redirect) so individual pages don't each reimplement session-expiry logic — a clean, DRY pattern. `downloadExport` correctly parses the `Content-Disposition` header for filenames instead of hardcoding one.

**What is bad:**
No TypeScript, no PropTypes despite `rules.md` explicitly recommending "PropTypes (or JSDoc typedefs) where type-checking value is needed" — none were found in the components reviewed. No shared data-fetching/caching layer: `Dashboard.jsx` and `DocumentDetail.jsx` each independently `Promise.all([...])` their own set of endpoints on mount with their own `loading`/`error` state, the same boilerplate repeated per page (exactly the pattern the README claims TanStack Query would solve, and exactly why the README lists it as a dependency that isn't actually installed). No tests — no Vitest, no React Testing Library, despite the README's own "Known Limitations" honestly flagging this ("add Vitest + React Testing Library as the project matures" — a rare instance of the docs being *more* honest than the dependency list, since even that placeholder plan hasn't been executed).

**Why it is bad:**
`DocumentDetail.jsx` fetching the full document list, extraction, summary, and anomalies with `Promise.all` means if *any single one* of those four requests fails (e.g. the anomalies endpoint 500s because of a Gemini timeout), the entire page shows a generic error and the user can't see the extraction data that *did* load successfully — a single-point-of-failure UX pattern that will produce user complaints disproportionate to the actual severity of the underlying failure.

**Senior improvement:** Use `Promise.allSettled` instead of `Promise.all` on `DocumentDetail.jsx`'s initial load so a failed anomaly check doesn't blank out an otherwise-successful summary/extraction render.

---

### 8. API Design — 6/10

**What is good:**
RESTful and consistent: `/api/documents/{id}/extraction`, `/summary`, `/anomalies`, `/export` are well-named sub-resources. Correct HTTP verbs and a consistent JSON error envelope (`{"error": {"message", "code"}}`) enforced globally through `GlobalExceptionHandler`. `GET /api/documents/{id}/export?format=csv|json` and the workspace-wide bulk variant are a genuinely useful, well-thought-out feature most student projects wouldn't bother building.

**What is bad:**
No pagination on `GET /api/documents` — it returns the entire workspace's document list as a JSON array, confirmed in `DocumentService.getEnrichedDocumentsForWorkspace`. No API versioning (no `/api/v1/`). No OpenAPI/Swagger documentation, despite `rules.md` explicitly flagging `springdoc-openapi` as an option "if an auto-generated Swagger UI is wanted" — it wasn't added. There is no single-document `GET /api/documents/{id}` endpoint returning the list-item shape — the frontend has to fetch the whole collection and filter client-side to render a detail page (see Product Design).

**Why it is bad:**
A workspace with a few thousand processed invoices will return a multi-megabyte JSON array to the dashboard on every load, and the frontend will re-fetch and re-render that entire array every time `DocumentDetail.jsx` is opened for a single document. This is the same "no pagination" failure mode flagged in the SCMS review, and it will hit the same wall at the same order of magnitude.

**Senior improvement:** Add `Pageable` to the documents list query; add a real `GET /api/documents/{id}` endpoint.

---

### 9. Database Design — 6.5/10

**What is good:**
Properly normalized schema (`workspaces`, `users`, `documents`, `extractions`, `summaries`, `templates`, `anomaly_flags`), all managed through six sequential Flyway migrations rather than one big schema dump — meaning the migration history itself is legible and incremental, which is exactly what Flyway is for. Correct `ON DELETE CASCADE` on child tables (`extractions`, `summaries`, `anomaly_flags`, `templates`) referencing `documents`, so deleting a document doesn't leave orphan rows. `UNIQUE` constraints where they matter: `idx_extractions_document_id` and `idx_summaries_document_id` are unique indexes enforcing the intended 1:1 relationship at the database level, not just in application code. `templates` has a `UNIQUE(workspace_id, document_type)` constraint correctly enforcing "one active template per document type per workspace" as a DB-level invariant.

**What is bad:**
No index on `documents.workspace_id` — the single most frequently filtered column in the entire application (every list, export, and template-lookup query filters by it via `findByWorkspaceIdOrderByUploadedAtDesc`), and it has no index anywhere across the six migration files. `anomaly_flags.document_id` has an explicit index; `documents.workspace_id` does not — an inconsistent standard applied within the same schema. No index on `users.workspace_id` either, though that table will stay small for a long time and matters far less.

**Why it is bad:**
Every dashboard load, every export, every template lookup does `WHERE workspace_id = ?` against `documents` with no index — a full table scan that gets linearly worse as total documents across all workspaces grow, regardless of how few documents any single workspace has.

**Senior improvement:**
```sql
CREATE INDEX idx_documents_workspace_id ON documents(workspace_id);
```
One line, in a new Flyway migration, closes the biggest gap in an otherwise well-indexed schema.

---

### 10. Security — 5/10

**What is good:**
BCrypt for password hashing — correct, default strength, no custom "clever" hashing scheme. JWT secret pulled from environment variable with an explicit `dev-only-insecure-secret-change-me` default that is clearly labeled as unsafe rather than silently shipped as a real secret. CORS is origin-restricted (`http://localhost:5173`), not wildcarded. **Workspace-scoped authorization is enforced at the service layer on every single document/template endpoint reviewed** — `getDocumentForWorkspace` re-verifies the document's `workspace_id` matches the requesting user's on every read, which is the exact discipline `rules.md` §5 demands and the exact check that SCMS's peer project got right in some places and wrong in others. File upload validates content-type against an allowlist (PDF/JPEG/PNG only) and has real path-traversal protection in `StorageService.store()` (both a `..` string check and a post-resolution "does the final path still live inside the storage directory" check — belt and suspenders, correctly implemented).

**What is bad:**
`/uploads/**` is `permitAll()` in `SecurityConfig` — **every uploaded file is served to anyone with the URL, with zero authentication and zero workspace check.** The only protection is that filenames are UUID-prefixed, which is security-through-obscurity: any URL that leaks via browser history, a referrer header, a shared screenshot, a proxy log, or a copy-pasted link gives permanent, unauthenticated access to that file — a real invoice, contract, or resume — to anyone who has it. There is no rate limiting anywhere: `AuthController.login` has no attempt cap, no lockout, no delay — an attacker can brute-force any account's password at whatever rate the server can physically handle. The JWT has no refresh mechanism and is stored in `localStorage` (confirmed in `lib/api.js`), which is readable by any JavaScript that runs on the page — the standard XSS-to-token-theft path, mitigated only by there being no obvious stored-XSS vector in the reviewed frontend code (no `dangerouslySetInnerHTML`, no raw HTML rendering found).

**Why it is bad:**
The `/uploads/**` gap is the most serious finding in this entire review. This product's core value proposition is handling *sensitive business documents* — invoices with vendor/pricing data, signed contracts, resumes with personal information — and the mechanism serving those documents back to the browser has no authorization check whatsoever. The code comment in `SecurityConfig` explaining *why* frame-options was disabled (to let the `<iframe>` preview work) is a good comment about a real UX need, but it documents the symptom, not the fact that the underlying endpoint has no auth at all.

**Production risk:** Any leaked, logged, or shared `/uploads/{uuid}_{filename}` URL grants permanent unauthenticated access to that specific file. Because filenames are UUIDv4, this is not exploitable by *enumeration* — but it is fully exploitable by *any URL leak*, and PDF/image URLs leak constantly (browser history sync, screenshot tools, support tickets, proxy access logs, Slack link previews).

**Senior improvement:** Route file serving through an authenticated controller endpoint (`GET /api/documents/{id}/file`) that reuses the existing `getDocumentForWorkspace` check, instead of a public static resource handler. For login brute-force, add a simple attempt counter (Redis or even the in-memory pattern SCMS used — acceptable for a single-node MVP as long as it's not the *only* line of defense) with exponential backoff or a temporary lockout.

---

### 11. AI/LLM Integration Engineering — 7.5/10

**What is good:**
This is the standout area of the project and worth scoring separately because it's the product's core differentiator. `GeminiClient` uses `responseSchema` + `responseMimeType: application/json` to get schema-constrained structured output directly from Gemini, rather than the common student-project mistake of asking for JSON in a text prompt and regex-parsing the response. Every LLM response is deserialized into a typed DTO *and* run through Jakarta Bean Validation before being trusted — a response that doesn't match the expected shape gets rejected and logged as a failure, not silently persisted as garbage. The NUL-byte stripping (`stripNulBytes`) is a specific, real bug fix — Gemini can echo source text containing a `\u0000` byte back into its JSON response, and PostgreSQL's `text` columns reject NUL bytes outright; the code comments explain this is a fix for an actual observed failure, not defensive paranoia. Failures are recorded as first-class data (`Extraction.failedReason`, `Summary.failedReason`) rather than swallowed, so a failed extraction is visible in the UI instead of silently absent.

**What is bad:**
No retry logic on Gemini API failures — a transient network blip or a `429` from Google fails the entire document's processing pipeline permanently (the document sits at `PROCESSED` OCR-wise but with `failedReason` set on its extraction/summary, and nothing ever retries it). No caching — reprocessing the same document (e.g. after a code deploy that improves the prompt) means a fresh, billed API call, with no way to force-reprocess from the UI even if you wanted to. The Gemini API key has no per-workspace usage cap or budget guard — a single workspace uploading thousands of documents in a loop has no throttle before it burns through the entire application's LLM budget.

**Why it is bad:**
LLM API calls are the most expensive and most failure-prone external dependency in this system, and the only resilience mechanism is "log the failure and move on." That's honest and non-silent, which is good — but it means a temporary Gemini outage permanently strands every document uploaded during that window with no automatic recovery.

**Senior improvement:** Add exponential-backoff retry (e.g. 3 attempts) inside `GeminiClient` for `5xx`/timeout responses before giving up; add a manual "reprocess" button on `DocumentDetail.jsx` that re-triggers `processDocumentAsync` for a `FAILED` document.

---

### 12. Error Handling — 6.5/10

**What is good:**
`GlobalExceptionHandler` is correctly centralized and never leaks stack traces — the catch-all `Exception` handler logs full detail server-side and returns a generic message client-side, exactly as the code comment states ("Per rules.md §4: never leak stack traces to the client"). The `Throwable`-catching discipline in the async processing pipeline (discussed under Backend Engineering) means a document upload never silently vanishes — it always reaches a terminal, visible status (`PROCESSED` or `FAILED` with a reason), which is a meaningfully better failure mode than most beginner async code, where an uncaught exception in a background thread just disappears.

**What is bad:**
`Promise.all` in `DocumentDetail.jsx` means one failed sub-request (extraction/summary/anomalies) blanks the entire page instead of degrading gracefully (already flagged under Frontend Engineering, but it's an error-handling issue at its core). `ExportService.exportAsJson` wraps Jackson serialization failures in a bare `RuntimeException` with no custom error code, which will fall through to the generic `INTERNAL_ERROR` handler — acceptable, but inconsistent with the rest of the codebase's discipline around using `ApiException` with specific codes.

**Why it is bad:** Minor — this is a "could be more consistent" finding, not a correctness risk.

---

### 13. Logging — 5.5/10

**What is good:**
SLF4J via Spring Boot Logging is used consistently. `log.error()` calls throughout the async pipeline include the document ID and full throwable — genuinely useful for debugging a specific stuck document rather than a bare "something failed" message. No credentials are logged anywhere in the reviewed code (a specific check was made — no `log.info` statements print passwords, tokens, or API keys).

**What is bad:**
No structured logging (JSON/MDC) — logs are plain text, which is fine for `docker compose logs` during a demo but not queryable in any real log aggregation tool. No request correlation ID, so tracing a single user's request across the async OCR → extraction → summarization → anomaly-check chain requires manually correlating by document ID across four different log lines from four different service classes. `show-sql: true` is enabled in `application.yml` with no environment gating — every SQL statement is logged, including in whatever environment this config file ships to, which is a performance and log-noise concern in production (though not a security one, since no bind parameters containing secrets appear in this schema).

**Senior improvement:** Gate `show-sql` behind a Spring profile so it's on for `dev` and off by default; add an MDC-based request ID filter.

---

### 14. Monitoring — 1.5/10

**What is good:** Nothing meaningfully present. `HealthController.health()` returns a bare string `"OK"` with no dependency checks (doesn't verify the database is reachable, doesn't verify Gemini is configured) — it's a liveness check in spirit only.

**What is bad:** No Spring Boot Actuator. No `/actuator/health`, `/actuator/metrics`. No Prometheus/Micrometer. No APM. No alerting. No dashboard. If Gemini's API silently starts rejecting every request (bad key, quota exhausted, model deprecated), the only symptom is that every document's `failedReason` fills up with the same error — and nothing surfaces that pattern to a human until someone happens to look.

**Production risk:** Same category as SCMS's finding — this is a blocker for any real deployment. You deploy blind and stay blind until a user complains.

**Senior improvement:** Add `spring-boot-starter-actuator`, wire `/actuator/health` into `docker-compose.yml`'s backend healthcheck (currently only the `db` service has one), and add a scheduled check that alerts if the `FAILED`-with-Gemini-error rate over the last hour crosses a threshold.

---

### 15. DevOps / Deployment — 6.5/10

**What is good:**
This category is a clear strength relative to typical student work. A real multi-stage `Dockerfile` (build stage on `eclipse-temurin:25-jdk`, run stage on the smaller `-jre` image), a **non-root container user** (`groupadd`/`useradd docket`, then `USER docket`) — a detail most professional codebases get wrong, let alone student ones. `docker-compose.yml` wires all three services together with a Postgres healthcheck gating backend startup (`depends_on: condition: service_healthy`), and environment variables are consistently externalized with safe non-secret defaults for local dev.

**What is bad:**
No CI/CD pipeline anywhere — no `.github/workflows`, no equivalent. Nothing runs on push; there's no automated build, lint, or (nonexistent) test execution gating merges. No image tagging/versioning strategy visible. The frontend Docker build bakes `VITE_API_BASE_URL` in at build time (`http://localhost:8080` default) — meaning the exact same compiled frontend image can't be deployed against a different backend URL without a rebuild, which will bite the first time this goes to a real staging/prod split.

**Senior improvement:** Add a GitHub Actions workflow that builds both images and runs `mvn test` (once tests exist) on every PR — even a minimal one is infinitely better than none.

---

### 16. Testing — 0.5/10

**What is good:** `spring-boot-starter-test` is a declared Maven dependency, so the scaffolding to add tests exists with zero additional setup. The README's "Known Limitations" section is at least honest that frontend testing is aspirational ("add Vitest + React Testing Library as the project matures").

**What is bad:** Zero test files exist anywhere in the repository — not `backend/src/test`, not any `*.test.jsx`/`*.spec.jsx` in the frontend. This is worse than SCMS's zero-test finding in one specific way: the README **explicitly and falsely claims** "Backend: JUnit 5 + Spring Boot Test + Mockito" as the testing approach, stated as fact rather than as a plan.

**Why it is bad:** No test exists to catch a regression in the workspace-isolation checks — the single most security-critical piece of business logic in the app (`getDocumentForWorkspace`). If a future refactor accidentally weakens that check, nothing would catch it before a real user could read another workspace's documents.

**Senior improvement:** Start exactly where SCMS's action plan started — with the highest-value, lowest-effort tests first: a `@SpringBootTest` that asserts a user in workspace A gets a 404 (not the actual document) when requesting a document ID belonging to workspace B. That single test protects the app's most important security invariant and would take under an hour to write.

---

### 17. Code Quality / Maintainability — 6.5/10

**What is good:**
Consistent naming conventions throughout (matches `rules.md`'s own stated Java/React conventions). Javadoc comments on every controller method state purpose, expected input, and expected output, exactly as `rules.md` §6 requires — a rare case of a written code-style rule that's actually being followed at the point of writing. Methods are generally kept short and single-purpose; no god-methods were found doing five unrelated things.

**What is bad:**
The `stripNulBytes` triplication and the three near-identical `extract*Fields` methods (both already discussed under Software Architecture) are the clearest maintainability debt in the codebase — not because any individual copy is wrong, but because the pattern guarantees future divergence. `DocumentController` injects `ExtractionRepository`, `SummaryRepository`, and `AnomalyFlagRepository` directly rather than going through `DocumentService` for those three sub-resource fetches — a minor architectural inconsistency (the controller *does* delegate for uploads/exports but reaches straight into repositories for extraction/summary/anomalies), contradicting the codebase's own `rules.md` rule that "controllers call services directly."

**Senior improvement:** Move the `extraction`/`summary`/`anomalies` repository calls into `DocumentService` methods for consistency with the rest of the controller.

---

### 18. Documentation Quality — 6.5/10

**What is good:**
Genuinely extensive: `prd.md`, `architecture.md`, `design.md`, `phases.md`, `rules.md`, and a 76KB running `memory.md` development log — far beyond what almost any student project produces, and structured well enough to actually be usable by a new contributor.

**What is bad:**
As detailed under Requirements Engineering, the top-level README has drifted from reality in ways that would actively mislead a reader: a testing strategy stated as fact that doesn't exist, and two frontend dependencies (TanStack Query, Recharts) listed in the tech stack table that aren't installed. Documentation this extensive raises the stakes of drift — there's more surface area for the docs and the code to disagree, and in the two places checked, they did.

**Senior improvement:** A five-minute pass reconciling the README's tech stack table and Testing section against `package.json`/`pom.xml`/the actual `test` directories would close this gap entirely — this is the cheapest fix in the whole report relative to its trust impact.

---

## SECTION 2 — TOP 15 STRENGTHS

1. **Flyway-managed schema with `ddl-auto: none`** — the exact production-safety decision SCMS got wrong
2. **Workspace-scoped authorization enforced at the service layer on every document/template endpoint, consistently**
3. **Bounded, backpressure-aware `@Async` thread pool** with `CallerRunsPolicy` — not the unbounded-thread-per-task default trap
4. **`catch (Throwable)` used correctly**, specifically around native JNI (Tesseract) failure modes, with comments explaining exactly why
5. **Schema-constrained LLM output** (Gemini `responseSchema`) validated with Jakarta Bean Validation before being trusted — not blind JSON parsing
6. **Real path-traversal protection** in file storage (string check + post-resolution path containment check)
7. **Non-root Docker user** in the backend Dockerfile
8. **Multi-stage Docker build** with a slim JRE runtime image
9. **Docker Compose stack with a real Postgres healthcheck gating backend startup**
10. **NUL-byte sanitization** for a real, specific PostgreSQL failure mode encountered with certain PDF text layers
11. **Consistent JSON error envelope** via a genuinely centralized `GlobalExceptionHandler` that never leaks stack traces
12. **File-content-type allowlisting** on upload (PDF/JPEG/PNG only)
13. **Extensive up-front planning documentation** (`prd.md`, `architecture.md`, `rules.md`) that mostly reflects real, followed engineering rules
14. **Failures recorded as first-class, visible data** (`failedReason` fields) rather than silently swallowed
15. **CSV/JSON export feature**, single-document and workspace-wide, correctly implemented with proper `Content-Disposition` handling

---

## SECTION 3 — EXACT ACTION PLAN

### To reach 7/10 (from current ~5.9/10 average):
1. Move `/uploads/**` behind an authenticated, workspace-checked controller endpoint (1 day) — **highest priority, do this first**
2. Add a login attempt limiter/lockout (1 day)
3. Add `idx_documents_workspace_id` index via a new Flyway migration (30 minutes)
4. Fix the README: remove the false JUnit/Mockito testing claim, remove TanStack Query/Recharts from the tech stack table, or actually install them (1 hour)
5. Write the single highest-value test: cross-workspace document access must return 404 (2 hours)
6. Add `GET /api/documents/{id}` so the frontend stops fetching the full list to render one document (2 hours)
7. Untrack the committed `/uploads` files from git history and confirm `.gitignore` actually excludes future ones (`git rm -r --cached`) (2 hours)
8. Switch `DocumentDetail.jsx`'s `Promise.all` to `Promise.allSettled` (30 minutes)

**Estimated total: ~1 week of focused work**

### To reach 8.5/10 (from 7/10):
1. Collapse the three `extract*Fields` methods into one generic method; deduplicate `stripNulBytes` into a shared utility (1 day)
2. Full backend test suite for `AuthService`, `DocumentService`, `ExtractionService` — 70%+ coverage on service layer (1.5 weeks)
3. Add `spring-boot-starter-actuator` + wire into the Compose healthcheck (2 hours)
4. Add retry-with-backoff to `GeminiClient` for transient failures (1 day)
5. Add pagination to `GET /api/documents` (1 day)
6. Move file storage behind an interface with an S3-compatible implementation (3 days)
7. Add a scheduled job to detect and retry documents stuck at `PENDING` (1 day)
8. GitHub Actions CI: build + lint + test on every PR (1 day)
9. Add JWT refresh tokens; consider moving off `localStorage` to a more XSS-resistant storage strategy (2 days)
10. Structured (JSON) logging + request correlation ID (1 day)

**Estimated total: 4-6 weeks of focused work**

### To reach 9.5/10 (from 8.5/10):
1. Full mobile-responsive pass + WCAG AA accessibility audit
2. Editable/correctable extraction fields with human-in-the-loop override of AI output
3. OpenAPI/Swagger documentation
4. Rate limiting via Redis (multi-instance-safe) rather than a single-node limiter
5. Per-workspace Gemini API budget/usage caps
6. Real-time status updates (WebSocket/SSE) instead of manual refresh
7. Full E2E test suite (Playwright) covering the upload → process → view flow
8. Contract testing for the Gemini integration to catch upstream API shape changes
9. Multi-document comparative anomaly detection (trend-based, not just template-diff)
10. API versioning, `/api/v1/`

**Estimated total: 2-3 months of professional-level work**

---

## FINAL VERDICT

This is a genuinely well-engineered final-year project, and in several specific dimensions — schema migration discipline, async error handling, LLM response validation, container hardening — it shows judgment that's ahead of a lot of professional codebases, not just student ones. The choice to validate every Gemini response against a Bean Validation schema before trusting it, and to catch `Throwable` specifically because a JNI library can throw `Error`, are the kind of decisions that come from having actually been burned by the failure mode, not from copying a tutorial.

That said: the unauthenticated `/uploads/**` endpoint is a genuine, serious finding for a product whose entire pitch is handling sensitive business documents, and it should be the very first thing fixed — before pagination, before tests, before anything else on the list above. The zero-test situation, sitting directly underneath a README that claims a full test suite exists, is the kind of discrepancy that costs credibility fast in a technical interview the moment someone runs `find . -name "*Test.java"` and gets nothing back.

**The honest truth:** if you walked into a senior engineer interview with this code open and were asked to defend the workspace-isolation logic, you'd do well — it's correct and it's consistent. If they then opened `SecurityConfig.java`, pointed at `"/uploads/**"`, and asked "who can access this," the conversation would get uncomfortable fast. And if they asked you to pull up the test suite the README describes, that's where it would end.

Fix the file-access hole. Reconcile the README with reality. Write the one test that protects your workspace-isolation invariant. Then this is a strong portfolio piece — arguably a stronger one than most bootcamp-to-junior-dev submissions get to see, because the parts that are done well are done *correctly*, not just done.

---

*Report generated: 2026-08-21*
*Reviewer: Principal Software Engineer / Architect perspective*
*Codebase: Docket, Spring Boot 3.5 (Java 25), React 19, PostgreSQL 16*
