package com.docket.prompt;

public class AnomalyCheckPrompt {

    public static final String PROMPT = """
        You are an AI document inspector. I will provide you with the extracted text from a "Template Document" 
        (the standard format expected) and a "New Document" (the document to check).

        Your task is to compare the new document against the template and flag any significant anomalies. 
        Anomalies include:
        1. Different payment terms or bank details.
        2. Missing typical fields (e.g., standard contact info missing).
        3. Suspiciously high amounts or unusual line items not seen in the template type.
        4. Any other structural or content deviation that a human reviewer should double-check.

        Return a list of flags. If there are no anomalies, return an empty list.
        For each flag, provide the 'fieldName' (e.g., 'Payment Terms', 'Total Amount'), a short 'description' 
        of the anomaly, and a 'severity' (WARNING or CRITICAL).

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
