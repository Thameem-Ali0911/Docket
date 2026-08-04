# AGENTS.md — AI Agent Operating Protocol for Docket

**Audience:** This file is written for AI coding agents and assistants — Claude (chat, Claude Code), Antigravity, GitHub Copilot in VS Code, Cursor, or any other agent given access to this repository. If you are an AI agent about to work on this codebase, **read this file in full before touching any code.**

**Purpose:** Docket is built across many sessions, often by different tools or different chat threads that share no memory with each other. This file is the fixed protocol every agent follows so that context, security posture, and project rules survive across sessions — even when chat history doesn't.

---

## 0. The One Rule Above All Others

**Never treat chat history as memory. Treat the repo's docs as memory.**

A new session has no idea what happened in the last one unless it reads `memory.md`. If you are an agent starting work right now, assume you know nothing about this project's current state until you've completed the Session Start Protocol below.

---

## 1. Session Start Protocol (mandatory, in this order)

Before writing a single line of code, an agent MUST:

1. **Read `memory.md`** — specifically "Current Status," "Completed," "In Progress," and "Next Steps." This tells you exactly where the last session left off.
2. **Read `phases.md`** — locate the phase named in `memory.md`'s "Current Status" and re-read its scope and Definition of Done. Do not assume you remember it from a prior conversation.
3. **Read `rules.md`** — re-confirm approved libraries, security rules, and AI/LLM boundaries. This is non-negotiable even if you "already know" the stack.
4. **Read `prd.md`** — re-sync on what's actually in scope. If a request from the user sounds like a new feature, check it against `prd.md` §6/§7 (Stretch Features / Non-Goals) before building it.
5. **Read `architecture.md`** — confirm the folder structure and tech stack before creating any new file. Do not invent a different folder layout or introduce a library not listed in `rules.md` §2.
6. **Read `design.md`** — if the task touches any UI, reuse the existing color tokens, type scale, and component conventions. Do not introduce new colors/fonts ad hoc.
7. **Only then** — begin the task the user actually asked for, scoped to the currently active phase.

If any of these files is missing, contradictory, or clearly out of date relative to the actual code in the repo, **stop and flag this to the user** rather than guessing or silently reconciling it yourself.

## 2. Phase Discipline

- Work **only** within the phase marked "Active" in `memory.md` / `phases.md`, unless the user explicitly instructs otherwise.
- If the user's request belongs to a later phase (e.g., asking for export functionality while still in Phase 2), tell them which phase it belongs to and ask whether they want to jump ahead, rather than silently building it.
- Do not mark a phase complete in `memory.md` until its Definition of Done (stated in `phases.md`) is actually met and verified (build passes, feature manually confirmed, tests green if present).

## 3. Security Checklist (apply to every change, every session)

Before writing or modifying backend code, confirm the change complies with `rules.md` §5. As a standing checklist for agents specifically:

- [ ] No secrets (API keys, DB passwords, JWT secrets) are ever written into a committed file. Only `.example`/placeholder files are committed. If you generate a config file with a real-looking key, immediately flag it and confirm with the user before it's saved anywhere trackable.
- [ ] Every new document/data-fetching query is scoped by `workspace_id`. If you write a repository method or controller endpoint that fetches a `Document`, `Template`, or `AnomalyFlag` by ID alone without a workspace check, that is a security bug — add the check before considering the work done.
- [ ] Passwords are only ever handled via `BCryptPasswordEncoder` (or the approved equivalent) — never logged, never stored in plaintext, never returned in any API response.
- [ ] File uploads are validated (type + size) at the controller/service boundary before reaching OCR or the LLM call — reject anything outside `pdf`, `png`, `jpg`, `jpeg` or over 10MB with a clear 4xx error.
- [ ] No full document text, extracted personal data, or API keys are written to logs in a way that would persist in plaintext beyond a debug session.
- [ ] Any new dependency is checked against `rules.md` §2's approved list. If it's not there, do not silently add it — ask the user or update `rules.md` first with their sign-off.
- [ ] Any LLM prompt you write or edit follows `rules.md` §3: single responsibility per call, strict-JSON-only output instruction, an explicit "return null / don't guess" instruction for missing fields, and output is deserialized + validated (Bean Validation / DTO) before being persisted.
- [ ] JWT/auth logic changes are run past Spring Security's filter chain conventions already established in `SecurityConfig.java` — don't bypass the filter chain with a quick unauthenticated route "just for testing" and leave it in.

If a task would require violating any of the above to "get it working faster," stop and tell the user, rather than shipping an insecure shortcut.

## 4. Coding Boundaries (recap — full detail in `rules.md`)

- Stick to the approved stack: Java 17+/Spring Boot backend, React/Vite/Tailwind frontend, PostgreSQL, Tess4J/PDFBox for OCR, Claude via the Anthropic API. Do not introduce a second backend language, a second database, or a queue/broker outside of Phase 10 stretch work.
- Controllers stay thin; business logic lives in services; repositories only do data access.
- Every new REST endpoint gets a short Javadoc comment (purpose, input, output).
- Follow the existing package layout under `backend/src/main/java/com/docket/...` and `frontend/src/...` — don't restructure folders without updating `architecture.md` first.
- Do not silently "improve" or refactor unrelated code while doing a scoped task. If you notice a real problem outside the current task, note it in `memory.md`'s "Known Issues / Gotchas" instead of fixing it unprompted.

## 5. What To Do When Requirements Are Ambiguous

- Check `prd.md` first — many "is this in scope" questions are already answered there (features, non-goals, assumptions).
- If still ambiguous, make the most reasonable assumption, state it explicitly in your response, and proceed — don't stall on a task that can be reasonably scoped. Log the assumption in `memory.md` under "Key Decisions & Why" so future sessions know why a particular choice was made.
- If a decision would be expensive to reverse (schema design, auth approach, a new external service), surface it to the user before proceeding rather than deciding unilaterally.

## 6. Mandatory: Updating the Docs as You Work

This is the second-most important rule in this file after §0. **An agent's work is not complete when the code compiles — it is complete when the docs reflect what changed.**

### 6.1 Update `memory.md` at the end of every session (non-negotiable)

Before ending a working session (or when the user indicates they're wrapping up, switching tools, or starting a new chat), update `memory.md`:

- **Current Status** — update the active phase and overall progress percentage if it changed
- **Completed** — check off anything genuinely finished (built, run, verified) this session
- **In Progress** — note exactly what's mid-flight and which file was last touched, so the next session can pick up mid-task without re-deriving context
- **Next Steps** — rewrite this list to reflect the actual next actions, not the stale plan from before this session
- **Key Decisions & Why** — append any new architectural/technical decision made this session, with a one-line reason and a pointer to the relevant file/section
- **Known Issues / Gotchas** — add anything discovered that isn't yet fixed (a flaky test, a library quirk, a TODO deliberately deferred)
- **Ideas / Not Yet Approved** — log any scope-creep idea that came up but wasn't approved, instead of building it
- **Commands Reference** — update if setup/run commands changed
- **Session Log** — **append** a new dated entry (never overwrite prior entries) summarizing: what was built/changed, which files were touched, what was tested/confirmed vs. still untested, and what the next session should do first

A session log entry should look like this:

```markdown
### Session N — YYYY-MM-DD
- [What was built/changed, in plain language]
- Files touched: path/to/File1.java, path/to/File2.jsx
- Tested/confirmed: [what you actually ran/verified]
- Still untested / follow-up: [anything left in a known-incomplete state]
- Next session should: [concrete next action]
```

### 6.2 Update other docs when the corresponding thing changes

Don't just update `memory.md` — update the doc that actually owns the information that changed:

| If you changed... | Update... |
|---|---|
| A feature's scope, a new user story, a non-goal reconsidered | `prd.md` |
| The tech stack, a library swap, folder structure, data model, an API endpoint shape | `architecture.md` |
| An approved library list, an error-handling convention, a new security rule | `rules.md` |
| Which phase is active, a phase's Definition of Done, a new phase inserted | `phases.md` |
| A color, font, spacing rule, or component convention | `design.md` |
| Anything about current progress, decisions, or session history | `memory.md` (always, in addition to the above) |

**Rule of thumb:** if a future session (or a human) would be misled by re-reading the docs after your change, the docs are now stale — fix them in the same session as the code change, not "later."

### 6.3 Never silently invalidate a doc

If a past decision recorded in `memory.md`'s "Key Decisions & Why" turns out to be wrong and you're changing it, don't just delete the old row — update it and add a note explaining why the earlier decision was reversed. Deleting history without explanation is exactly the kind of context loss this whole system exists to prevent.

## 7. End-of-Session Checklist (run through this before finishing)

- [ ] Code compiles/builds and the specific feature worked on was manually verified (or tests pass, if present)
- [ ] Security checklist (§3) reviewed for anything touched this session
- [ ] `memory.md` updated: Current Status, Completed, In Progress, Next Steps, Session Log entry appended
- [ ] Any other doc affected by this session's changes (§6.2 table) updated
- [ ] No secrets committed; `.env`/`application-dev.yml` real values still untracked
- [ ] If a phase was completed, its Definition of Done in `phases.md` was actually satisfied, not just assumed

## 8. Quick Reference — File Roles

| File | What it's for | When to read it | When to update it |
|---|---|---|---|
| `prd.md` | Scope, users, features | Every session start; when scope is unclear | When scope/features change |
| `architecture.md` | Stack, structure, data model, prerequisites | Every session start; before creating new files | When stack/structure/API changes |
| `rules.md` | Coding rules, security, AI boundaries | Every session start; before adding a dependency | When a new rule/library is approved |
| `phases.md` | Build plan, Definition of Done per phase | Every session start; before starting new work | When a phase is completed or re-scoped |
| `design.md` | Visual system | Whenever UI is touched | When a new visual convention is introduced |
| `memory.md` | Progress log, decisions, session history | **First**, every session | **Last**, every session (mandatory) |
| `README.md` | Human-facing project overview, setup instructions | When onboarding or setting up locally | When setup steps, stack, or features change materially |
| `AGENTS.md` (this file) | Agent operating protocol | Once per agent, before first action in a session | When the team's process itself changes |

---

**Summary for agents in one paragraph:** Read `memory.md` → `phases.md` → `rules.md` → `prd.md` → `architecture.md` → `design.md`, in that order, before doing anything. Work only within the active phase. Apply the security checklist to every change touching auth, data access, file uploads, or LLM prompts. Never commit secrets. When you're done, update `memory.md` (always) and any other doc whose subject matter actually changed — this is not optional cleanup, it *is* the deliverable that lets the next session, tool, or person continue without re-deriving everything you just figured out.
