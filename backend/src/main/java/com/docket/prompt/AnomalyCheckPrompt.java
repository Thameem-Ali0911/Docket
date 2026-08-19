package com.docket.prompt;

public class AnomalyCheckPrompt {

    public static final String PROMPT = """
        You are an AI document inspector. I will provide you with the extracted text from a "Template Document" 
        (the standard format expected) and a "New Document" (the document to check).

        Your task is to compare the new document against the template and flag any significant anomalies or deviations.
        Consider the document type (Invoice, Contract, or Resume) and check for:
        1. Invoices: Different payment terms, bank details, unusual fees/line items, tax calculation anomalies, or deviation from standard billing formats.
        2. Contracts: Deviations in termination clauses, non-standard notice periods, missing standard liability/indemnity clauses, differing governing laws, or unusual payment schedules.
        3. Resumes: Missing core sections (e.g., Education, Experience, Contact info), absence of standard role requirements/qualifications seen in the template, or unusual formatting deviations.
        4. Any other unexpected structural, financial, legal, or content deviation that a reviewer should examine.

        Return a list of flags. If there are no anomalies, return an empty list.
        For each flag, provide:
        - 'fieldName': The specific field, clause, or section name (e.g., 'Termination Clause', 'Payment Terms', 'Total Amount', 'Governing Law', 'Education Section', 'Skills').
        - 'description': A concise, professional explanation comparing what was expected based on the template vs what was found in the new document.
        - 'severity': 'WARNING' for minor deviations or 'CRITICAL' for major legal/financial/structural risks.

        Template Document Text:
        %s

        ---

        New Document Text:
        %s
        """;


    public static final String JSON_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "flags": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "fieldName": { "type": "string" },
                  "description": { "type": "string" },
                  "severity": { "type": "string", "enum": ["WARNING", "CRITICAL"] }
                },
                "required": ["fieldName", "description", "severity"]
              }
            }
          },
          "required": ["flags"]
        }
        """;
}
