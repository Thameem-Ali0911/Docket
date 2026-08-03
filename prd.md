# PRD.md — Product Requirement Document

**Project:** Docket — AI-Powered Document Intelligence Platform (B2B SaaS)
**Type:** Final Year Project (Engineering / SIH-style problem statement)
**Status:** Draft v1.0

**One-liner:** Docket reads your contracts, invoices, and resumes in seconds — pulling out the key facts, summarizing the rest in plain English, and flagging the moment something deviates from your standard. Skip the manual read-through; Docket's already done it.

---

## 1. Problem Statement

Businesses in banking, legal, insurance, and HR handle large volumes of unstructured documents — contracts, invoices, resumes, KYC forms, loan agreements. Employees manually:

- Read documents to find key fields (dates, amounts, clauses, names)
- Cross-check clauses against a company's "standard" template to spot risky deviations
- Summarize long contracts for people who don't have time to read them
- Manually re-key extracted data into spreadsheets or internal systems

This is slow, error-prone, and doesn't scale. Existing tools (Docsumo, Rossum, Hyperscience) solve this but are expensive, closed, and enterprise-only — inaccessible for SMEs, small law firms, and NBFCs (Non-Banking Financial Companies).

## 2. Vision

A focused, self-serve SaaS where a business uploads a document (PDF/image/scan) and within seconds gets:

1. **Structured data extraction** — key fields pulled out as JSON/table
2. **Plain-English summarization** — a 3-5 sentence summary of the document
3. **Anomaly/deviation flagging** — highlights where a document differs from the company's saved "standard template" (e.g., a non-standard termination clause, an invoice total that doesn't match line items)

The product is intentionally scoped to **three document types** for the MVP: **Contracts, Invoices, Resumes** — not a generic "any document" tool. This keeps extraction quality high and the project achievable in one academic year.

## 3. Target Users

| Persona | Description | Primary Need |
|---|---|---|
| **Legal Ops / Paralegal** (small law firm, 5-50 people) | Reviews vendor/employment contracts daily | Fast clause extraction + deviation flagging vs. firm's standard contract |
| **Finance/AP Clerk** (SME or NBFC) | Processes vendor invoices for payment | Extract invoice number, vendor, line items, totals; flag mismatches |
| **HR/Recruiter** (SME, staffing agency) | Screens incoming resumes | Extract skills, experience, education into structured profile; rank/summarize |
| **Compliance Officer** (secondary persona) | Audits a batch of processed documents | Dashboard view of flagged anomalies across all documents |

**Explicitly out of scope for MVP:** enterprise IT admins, API-only customers, multi-language documents (English-only for v1).

## 4. Goals & Success Metrics

Since this is an academic project, "success" is measured by demo-ability and technical depth, not real revenue:

- Correctly extract ≥85% of key fields on a test set of 20 sample documents per document type
- Anomaly detection correctly flags injected deviations in ≥80% of test cases
- End-to-end processing (upload → results) completes in under 30 seconds for a 5-page document
- A working multi-tenant dashboard where a demo user can: sign up, upload a doc, see extraction results, upload a "standard template," and see a flagged comparison
- Clean, defensible architecture and code the student can explain clause-by-clause in a viva/demo

## 5. Core Features (MVP)

### 5.1 Authentication & Workspace
- Email/password signup & login
- Each business = one "workspace" (multi-tenant by workspace_id)
- Simple role: Admin (only role needed for MVP)

### 5.2 Document Upload
- Upload PDF, PNG, JPG (single file, up to 10MB)
- Select document type at upload: Contract / Invoice / Resume
- Upload history list per workspace

### 5.3 Extraction Engine
- OCR fallback for scanned/image documents
- LLM-based structured extraction per document type, e.g.:
  - **Invoice:** vendor name, invoice number, date, line items, subtotal, tax, total
  - **Contract:** parties, effective date, term length, termination clause, payment terms, governing law
  - **Resume:** name, contact info, skills, work history, education, total years experience
- Output shown as an editable structured table + downloadable JSON/CSV

### 5.4 Summarization
- Auto-generated 3-5 sentence plain-English summary of any uploaded document

### 5.5 Template / Standard Comparison (Anomaly Flagging)
- User uploads (or picks) one document per type as the "standard template"
- New documents of that type are diffed against the template's expected clauses/fields
- Flags shown as: ✅ Matches standard / ⚠️ Deviation found (with a short explanation of what differs)
- Example: "Termination notice period is 15 days; your standard template specifies 30 days"

### 5.6 Dashboard
- List/grid of all processed documents with status (Processed / Flagged / Failed)
- Click into a document to see: original file preview, extracted fields, summary, anomaly flags
- Basic filters: by document type, by flagged status, by date

### 5.7 Export
- Export extracted data as CSV or JSON per document, or bulk-export a workspace's data

## 6. Stretch Features (post-MVP, only if time permits)

- Batch upload (multiple files at once)
- Confidence scores per extracted field
- User-editable extraction (correcting a field retrains/improves prompt few-shot examples)
- Basic usage-based billing simulation (Stripe test mode)
- Email notification when a flagged anomaly is found
- Support for a 4th document type (e.g., KYC forms)

## 7. Non-Goals

- Not building a generic "any document type" extractor
- Not handling non-English documents
- Not building real payment/billing infrastructure (simulate only if attempted)
- Not building mobile apps — responsive web only
- Not implementing e-signature or document editing/redlining

## 8. Assumptions & Constraints

- Single developer (student) building over ~1 academic year with AI pair-programming assistance
- LLM calls (extraction, summarization, anomaly reasoning) will use the Anthropic API (Claude)
- OCR will use an open-source or free-tier library (see architecture.md for exact choice)
- Deployment target: free/low-cost tiers (Vercel/Render/Railway + a managed Postgres free tier) since this is an academic, not commercial, deployment
- Sample/test documents will be synthetic or anonymized public templates — no real client data

## 9. Key Risks

| Risk | Mitigation |
|---|---|
| LLM extraction accuracy varies by document quality (scans, handwriting) | Scope OCR to typed/clean documents for MVP; note handwriting as future work |
| Scope creep (trying to support all document types) | Hard-lock MVP to 3 document types; stretch features are explicitly optional |
| Running out of time before demo | Follow phases.md strictly; each phase has a "must-ship" definition of done |
| LLM hallucinating extracted fields | Always show source document alongside extraction; add a "low confidence" flag when field isn't found verbatim in text |
