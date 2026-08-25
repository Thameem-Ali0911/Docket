# DEMO_SCRIPT.md — Docket 5-Minute Evaluation Walkthrough

**Audience:** Judges, Evaluators, and Demonstrators  
**Goal:** Showcase all core capabilities of Docket (AI field extraction, summarization, template deviation detection, human-in-the-loop correction, export, and API governance) in 5 minutes.

---

## 🔑 Demo Access Credentials

| Field | Value |
|---|---|
| **URL (Local)** | `http://localhost:5173` |
| **Demo User** | `demo@docket.ai` |
| **Demo Password** | `Demo1234!` |
| **Workspace** | `Acme Global Demo` (Pre-seeded with sample Invoice, Contract, and Resume) |
| **API Docs (Swagger)** | `http://localhost:8080/swagger-ui.html` |
| **System Health** | `http://localhost:8080/actuator/health` |

---

## ⏱️ Step-by-Step 5-Minute Click Path

```
 ┌────────────────┐     ┌────────────────┐     ┌────────────────┐
 │ 1. Login &     │ ──▶ │ 2. Document    │ ──▶ │ 3. Human-in-   │
 │    Dashboard   │     │    Intelligence│     │    the-Loop    │
 └────────────────┘     └────────────────┘     └────────────────┘
                                                       │
 ┌────────────────┐     ┌────────────────┐             ▼
 │ 5. Export &    │ ◀── │ 4. Multi-Type  │ ◀───────────┘
 │    Governance  │     │    & Templates │
 └────────────────┘     └────────────────┘
```

---

### Step 1: Login & Workspace Dashboard (0:00 – 1:00)

1. Open `http://localhost:5173` in your browser.
2. Sign in with `demo@docket.ai` and `Demo1234!`.
3. **What to highlight:**
   - **Modern Aesthetic:** Neomorphic Apple-style obsidian UI (`#12101B`) with Aurora particle glow and smooth staggered entrance transitions.
   - **Overview Metrics:** Top cards dynamically calculate Total, Processed, Flagged, Processing, and Failed document counts.
   - **AI Budget Guard:** Live LLM usage progress bar showing daily API consumption against the workspace quota (protects against denial-of-wallet).
   - **Interactive Filtering:** Demonstrate searching by filename/vendor or filtering by document type (Invoice, Contract, Resume) and deviation status.

---

### Step 2: Document Intelligence & Anomaly Detection (1:00 – 2:00)

1. On the dashboard table, click **View** on document `#1` (`demo_invoice_apex_cloud.pdf`).
2. **What to highlight:**
   - **Side-by-Side Dual Pane:** Left pane renders the secure, authenticated PDF preview; right pane renders AI-extracted intelligence.
   - **Structured Field Extraction:** Accurate extraction of vendor (`Apex Cloud Systems Inc.`), invoice number (`INV-2026-8891`), dates, totals, and nested itemized line items table.
   - **Plain-English Summary:** Automatic AI summary highlighting critical transaction parameters without reading the whole document.
   - **Template Deviation Anomaly Detection:** Amber warning banner:
     > *"Payment terms specify Net-15 (15 days to pay), deviating from the workspace standard policy of Net-30."*

---

### Step 3: Human-in-the-Loop Correction & Audit Trail (2:00 – 3:00)

1. In the **Extracted Intelligence** header, click the **Edit** button.
2. An inline JSON editor expands with the currently active fields.
3. Make a modification (e.g., change `"dueDate": "2026-08-25"` or add a `correctionNote: "Verified with finance"`).
4. Click **Save Correction**.
5. **What to highlight:**
   - **Immediate Persistence:** Saved via authenticated `PATCH /api/documents/{id}/extraction`.
   - **Green Badge:** "HUMAN CORRECTED" badge and timestamp immediately appear.
   - **Zero-Loss Audit Trail:** Explain that original Gemini AI extraction is permanently retained in `fields_json`, while human corrections live in `human_corrected_json`, maintaining complete compliance auditing.

---

### Step 4: Multi-Document Types & Template Management (3:00 – 4:00)

1. Click **Back to Dashboard**.
2. Click **View** on document `#2` (`demo_master_services_agreement.pdf`):
   - Demonstrates **Contract Extraction**: Parties involved, governing law, term duration, and anomaly flagging (14-day notice vs standard 30-day requirement).
3. Click **View** on document `#3` (`demo_resume_alex_chen.pdf`):
   - Demonstrates **Resume Extraction**: Candidate education, experience timeline table, and parsed technical skill badges.
4. Click **Templates** in the dashboard top action bar:
   - View the active standard baseline documents for Invoices, Contracts, and Resumes.
   - Show how any uploaded document can be designated as the workspace baseline.

---

### Step 5: Data Export & API Governance (4:00 – 5:00)

1. **Document Export:**
   - On the Document Detail page or Dashboard, click **CSV** or **JSON** to download complete intelligence packets.
   - On the Dashboard, click **Export All** to bulk download the workspace dataset.
2. **Interactive OpenAPI / Swagger Documentation:**
   - Open `http://localhost:8080/swagger-ui.html`.
   - Show all documented endpoints with full request/response schemas and JWT Bearer authorization support.
3. **Production Observability:**
   - Open `http://localhost:8080/actuator/health` to demonstrate live Spring Boot Actuator health checks verifying database connectivity and disk status.

---

## 💡 Key Differentiators to Mention

1. **Multi-Tenant Workspace Isolation:** Every document query, file stream, and anomaly comparison is cryptographically scoped to the authenticated user's workspace.
2. **Denial-of-Wallet Protection:** Built-in atomic LLM budget enforcement prevents runaway cloud API spend.
3. **Resilient Architecture:** Jittered backoff retries, async background processing, MDC correlation IDs, and automated reconciliation of interrupted jobs.
4. **Senior Engineering Standards:** 25 passing automated tests (`mvn test`), clean TypeScript/Vite frontend builds, automated Flyway schema migrations, and Docker Compose one-command orchestration.
