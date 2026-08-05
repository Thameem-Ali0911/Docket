# RULES.md — Project Rules & AI Boundaries

**Project:** Docket

**Purpose:** This file is the contract every AI coding session (Claude Code, Cursor, ChatGPT, etc.) must follow when working on this project. Read this file before writing any code.

---

## 1. Golden Rules

1. **Never work outside the current phase.** Check `phases.md` for the active phase before writing code. Do not jump ahead and build features from a later phase "while you're at it."
2. **Always update `memory.md`** at the end of a working session — what was done, what file was touched last, what's next. If `memory.md` is not updated, the next session has no reliable context.
3. **Never invent scope.** If a feature isn't in `prd.md`, don't build it. If it seems useful, note it as a suggestion in `memory.md` under "Ideas / Not yet approved" instead of building it.
4. **One document type's prompt logic per file.** Do not build one giant prompt that tries to handle invoices, contracts, and resumes at once — see prompts/ folder in architecture.md.
5. **Ask before introducing a new library, service, or paid dependency** not already listed in `architecture.md`. Update `architecture.md` if a new one is approved.

## 2. Approved Libraries / Stack (do not deviate without updating architecture.md)

- Frontend: React, Vite, Tailwind CSS, React Router, TanStack Query, Recharts
- Backend: Java 17+, Spring Boot 3.x, Spring Web, Spring Data JPA (Hibernate), Spring Security, Flyway, Jackson, Bean Validation
- Build tool: Maven (or Gradle — pick one, do not mix)
- OCR: Tess4J (+ native Tesseract engine and `tessdata`)
- PDF parsing: Apache PDFBox
- DB: PostgreSQL (via Spring Data JPA / JDBC driver)
- Auth: Spring Security + jjwt (JWT), `BCryptPasswordEncoder`
- LLM: Anthropic API called via Spring's `RestClient`/`WebClient` (no official Java SDK — use a thin typed wrapper class, see architecture.md §3.6)
- Testing: JUnit 5, Spring Boot Test, Mockito
- Local dev/orchestration: Docker + Docker Compose (see `docker-compose.yml`, `backend/Dockerfile`, `frontend/Dockerfile`) — this is tooling, not application scope, so it doesn't change the runtime stack above

**Do not** add: a second frontend framework, a second backend language/framework, a second database, a message queue (Kafka/RabbitMQ), or any paid third-party API (cloud OCR, cloud NLP) unless explicitly requested and reflected in architecture.md first.

## 3. AI / LLM Prompt Boundaries

- Every LLM call must have a **single, explicit responsibility**: extract fields, OR summarize, OR compare-to-template. Never combine two responsibilities in one prompt.
- Extraction prompts must **instruct the model to return strict JSON only**, matching a predefined schema per document type. Always deserialize the JSON response into a dedicated **Java DTO/record** (e.g., `InvoiceExtractionDto`, `ContractExtractionDto`, `ResumeExtractionDto`) via Jackson and validate it with **Bean Validation (`@Valid`)** before saving to the DB. If deserialization or validation fails, mark the document `failed` and store the raw error — do not silently guess/repair the data.
- Extraction must be **grounded in the document text** — the prompt must explicitly instruct: "If a field cannot be found in the text, return null. Do not guess or fabricate values." This is critical for a document-intelligence tool; fabricated data is worse than missing data.
- Anomaly/comparison prompts must reference **both** the new document's extracted text and the stored template's text — never rely on the model's general knowledge of "typical contracts."
- Never send raw uploaded documents to any third-party service other than the approved OCR library and the Anthropic API. No other data sharing.
- Do not log full document contents or extracted personal data (names, salaries, contract values) to console/logs in a way that would persist in plaintext logs beyond debugging. Redact where practical.

## 4. Error Handling Standards

- Use a global **`@ControllerAdvice` / `GlobalExceptionHandler`** so unhandled errors return a consistent JSON shape: `{ "error": { "message": string, "code": string } }` — never leak stack traces to the client. Use a custom `ApiException` (with an HTTP status + code) for expected, user-facing errors, and a catch-all `@ExceptionHandler(Exception.class)` for unexpected 500s.
- Every LLM/OCR call must be wrapped in try/catch. On failure:
  - Set the document's status to `failed`
  - Store a short, human-readable failure reason (e.g., "OCR could not read this file", "Extraction returned invalid JSON")
  - Never crash the whole server process on a single document's failure
- Validate all file uploads: max size (10MB), allowed types only (`pdf`, `png`, `jpg`, `jpeg`). Reject anything else with a clear 400 error before it reaches OCR/LLM steps.
- Frontend must handle and display: loading, empty, error, and success states for every async view — no blank screens on failure.

## 5. Security & Privacy Rules

- Never commit `.env` files or API keys. `.env.example` only, with placeholder values.
- Passwords must always be hashed (bcrypt) — never stored or logged in plaintext.
- Every document/data query must be scoped by `workspace_id` — a user must never be able to fetch another workspace's documents by guessing an ID. Add this check at the service layer, not just relying on frontend routing.
- Sample/test documents used during development must be synthetic or public templates only — never real, identifiable client or personal data.

## 6. Code Style & Structure

- Frontend: Plain JavaScript with JSX (no TypeScript); use ESLint/oxlint with recommended rules and PropTypes (or JSDoc typedefs) where type-checking value is needed.
- Backend: standard **Java conventions** — follow Google Java Style or the default IntelliJ formatter; keep code compiler-warning clean.
- No business logic inside controllers — controllers call services directly (thin `controller/*.java` classes); keep services focused and testable, repositories handle only data access (no business logic in repositories either).
- Consistent naming: Java classes `PascalCase`, methods/variables `camelCase`, constants `UPPER_SNAKE_CASE`; React components `PascalCase`, frontend variables/functions `camelCase`.
- Every new REST endpoint gets a short Javadoc comment: purpose, expected input, expected output. Add **springdoc-openapi** (optional) if an auto-generated Swagger UI is wanted for the demo/viva.
- Keep methods under ~40 lines where reasonably possible; extract helpers rather than nesting deeply.
- Prefer constructor injection (`@RequiredArgsConstructor` via Lombok, or explicit constructors) over field injection (`@Autowired` on fields) for testability.
- Use Java `record` types for immutable DTOs (request/response bodies, LLM-output schemas) where practical.

## 7. What To Do

- Do build one document type end-to-end (e.g., Invoice) completely before moving to the next — vertical slices, not horizontal layers.
- Do write realistic sample/test documents for each type before building the extraction prompt, so quality can be checked against ground truth.
- Do keep prompts in their own files (`prompts/`) so they can be iterated on without touching service logic.
- Do re-read `prd.md` and `architecture.md` at the start of a new phase to re-sync context.

## 8. What To Avoid

- Avoid building a generic "upload any document type" pipeline — MVP is locked to 3 types (see prd.md §6 Non-Goals).
- Avoid premature optimization: no queues, no caching layers, no microservices until the core single-server flow works end-to-end.
- Avoid silent failures — every failure path must be visible to the user and recorded in the DB.
- Avoid mixing Python and Node in the same codebase — pick one backend language and stay with it (see architecture.md §3.2).
- Avoid scope creep from "while I'm here" additions — log ideas in memory.md instead of implementing them mid-phase.
