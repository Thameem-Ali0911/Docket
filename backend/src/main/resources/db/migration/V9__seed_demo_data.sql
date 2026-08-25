-- Phase 11: Seed initial demo workspace, user, templates, and sample documents
-- This enables evaluators, demo viewers, and judges to log in immediately
-- with pre-loaded intelligence without needing local OCR or an active Gemini key.

DO $$
DECLARE
    demo_ws_id INTEGER;
    demo_user_id INTEGER;
    doc1_id INTEGER;
    doc2_id INTEGER;
    doc3_id INTEGER;
BEGIN
    -- Check if demo workspace already exists to guarantee idempotency
    IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'demo@docket.ai') THEN

        -- 1. Create Demo Workspace
        INSERT INTO workspaces (name, daily_llm_budget)
        VALUES ('Acme Global Demo', 100)
        RETURNING id INTO demo_ws_id;

        -- 2. Create Demo User (Password: Demo1234!)
        INSERT INTO users (workspace_id, email, password_hash)
        VALUES (
            demo_ws_id,
            'demo@docket.ai',
            '$2a$10$sBRs4Oyo57t6siwkred.3uvnWVoMvlxdmjllIph/vSl88fdqNdkFK'
        )
        RETURNING id INTO demo_user_id;

        -- 3. Insert Sample Document 1: Invoice
        INSERT INTO documents (workspace_id, type, file_url, status, extracted_text, uploaded_at)
        VALUES (
            demo_ws_id,
            'INVOICE',
            '/uploads/demo_invoice_apex_cloud.pdf',
            'PROCESSED',
            'INVOICE #INV-2026-8891\nVendor: Apex Cloud Systems Inc.\nInvoice Date: 2026-08-10\nDue Date: 2026-08-25\nBill To: Acme Global Demo\nLine Items:\n1. Dedicated GPU Cluster (H100 x 8) - $12,500.00\n2. High-Speed VPC Interconnect - $2,350.00\nTotal Amount: $14,850.00\nPayment Terms: Net-15',
            now() - interval '2 days'
        )
        RETURNING id INTO doc1_id;

        -- Extractions for Doc 1
        INSERT INTO extractions (document_id, fields_json, created_at)
        VALUES (
            doc1_id,
            '{"vendorName":"Apex Cloud Systems Inc.","invoiceNumber":"INV-2026-8891","invoiceDate":"2026-08-10","dueDate":"2026-08-25","totalAmount":"$14,850.00","lineItems":[{"description":"Dedicated GPU Cluster (H100 x 8)","quantity":"1","unitPrice":"$12,500.00","amount":"$12,500.00"},{"description":"High-Speed VPC Interconnect","quantity":"1","unitPrice":"$2,350.00","amount":"$2,350.00"}]}',
            now() - interval '2 days'
        );

        -- Summary for Doc 1
        INSERT INTO summaries (document_id, summary_text, created_at)
        VALUES (
            doc1_id,
            'Invoice INV-2026-8891 from Apex Cloud Systems Inc. billing $14,850.00 for dedicated GPU infrastructure and high-speed network interconnect. Payment is due on August 25, 2026 with accelerated Net-15 terms.',
            now() - interval '2 days'
        );

        -- Anomaly for Doc 1 (Net-15 vs standard Net-30)
        INSERT INTO anomaly_flags (document_id, field_name, description, severity, created_at)
        VALUES (
            doc1_id,
            'dueDate',
            'Payment terms specify Net-15 (15 days to pay), deviating from the workspace standard policy of Net-30.',
            'HIGH',
            now() - interval '2 days'
        );

        -- 4. Insert Sample Document 2: Contract (MSA)
        INSERT INTO documents (workspace_id, type, file_url, status, extracted_text, uploaded_at)
        VALUES (
            demo_ws_id,
            'CONTRACT',
            '/uploads/demo_master_services_agreement.pdf',
            'PROCESSED',
            'MASTER SERVICES AGREEMENT\nThis Agreement is entered into on 2026-09-01 by and between Acme Global Demo ("Client") and Nexus Logistics Inc. ("Provider").\nTerm: 12 Months\nGoverning Law: State of California\nTermination: Either party may terminate with 14 days written notice.\nContract Value: $240,000.00 payable quarterly.',
            now() - interval '1 day'
        )
        RETURNING id INTO doc2_id;

        -- Extractions for Doc 2
        INSERT INTO extractions (document_id, fields_json, created_at)
        VALUES (
            doc2_id,
            '{"contractTitle":"Master Services Agreement (MSA)","effectiveDate":"2026-09-01","termOrDuration":"12 Months","governingLaw":"State of California","totalValue":"$240,000.00","parties":["Acme Global Demo","Nexus Logistics Inc."]}',
            now() - interval '1 day'
        );

        -- Summary for Doc 2
        INSERT INTO summaries (document_id, summary_text, created_at)
        VALUES (
            doc2_id,
            'A 12-month Master Services Agreement between Acme Global Demo and Nexus Logistics Inc. for enterprise logistics operations totaling $240,000.00, governed by the laws of California.',
            now() - interval '1 day'
        );

        -- Anomaly for Doc 2 (14 days termination vs 30 days standard)
        INSERT INTO anomaly_flags (document_id, field_name, description, severity, created_at)
        VALUES (
            doc2_id,
            'termOrDuration',
            'Termination notice clause allows exit on 14 days notice, which is below the organizational minimum standard of 30 days.',
            'MEDIUM',
            now() - interval '1 day'
        );

        -- 5. Insert Sample Document 3: Resume
        INSERT INTO documents (workspace_id, type, file_url, status, extracted_text, uploaded_at)
        VALUES (
            demo_ws_id,
            'RESUME',
            '/uploads/demo_resume_alex_chen.pdf',
            'PROCESSED',
            'ALEX CHEN\nEmail: alex.chen@example.com | Phone: (555) 234-8901\nEducation: B.S. in Computer Science, Stanford University (2020)\nSkills: Java, Spring Boot, React, TypeScript, PostgreSQL, Docker, Kubernetes, AWS, REST APIs\nExperience:\n- Senior Software Engineer at CloudScale Labs (2023 - Present)\n- Full Stack Engineer at DataForge Corp (2020 - 2023)',
            now() - interval '4 hours'
        )
        RETURNING id INTO doc3_id;

        -- Extractions for Doc 3
        INSERT INTO extractions (document_id, fields_json, created_at)
        VALUES (
            doc3_id,
            '{"candidateName":"Alex Chen","email":"alex.chen@example.com","phone":"(555) 234-8901","education":"B.S. in Computer Science, Stanford University","skills":["Java","Spring Boot","React","TypeScript","PostgreSQL","Docker","Kubernetes","AWS","REST APIs"],"experience":[{"company":"CloudScale Labs","role":"Senior Software Engineer","duration":"2023 - Present"},{"company":"DataForge Corp","role":"Full Stack Engineer","duration":"2020 - 2023"}]}',
            now() - interval '4 hours'
        );

        -- Summary for Doc 3
        INSERT INTO summaries (document_id, summary_text, created_at)
        VALUES (
            doc3_id,
            'Senior Full-Stack Engineer with 6+ years of production experience in Spring Boot, React, and cloud-native Kubernetes architectures. Stanford CS graduate with solid domain leadership experience.',
            now() - interval '4 hours'
        );

        -- 6. Set Template references for the workspace
        INSERT INTO templates (workspace_id, document_type, document_id, created_at)
        VALUES
            (demo_ws_id, 'INVOICE', doc1_id, now() - interval '2 days'),
            (demo_ws_id, 'CONTRACT', doc2_id, now() - interval '1 day'),
            (demo_ws_id, 'RESUME', doc3_id, now() - interval '4 hours');

    END IF;
END $$;
