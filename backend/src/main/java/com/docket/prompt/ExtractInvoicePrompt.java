package com.docket.prompt;

/**
 * Prompt + JSON schema for invoice field extraction via Gemini structured output.
 * Schema field names must stay in sync with dto/InvoiceExtractionDto.java.
 */
public final class ExtractInvoicePrompt {

    private ExtractInvoicePrompt() {
    }

    public static String buildPrompt(String extractedText) {
        return """
            You are extracting structured fields from an invoice's raw OCR/text-layer text below.
            Only use information that is explicitly present in the text - do not guess, infer, or
            fabricate any value. If a field is genuinely not present in the text, use an empty string
            for that field (or an empty array for lineItems).

            Return amounts and dates exactly as they appear in the source text (do not reformat or
            normalize them).

            INVOICE TEXT:
            ---
            %s
            ---
            """.formatted(extractedText);
    }

    /** JSON Schema passed as generationConfig.responseFormat.text.schema in the Gemini request. */
    public static final String RESPONSE_SCHEMA_JSON = """
        {
          "type": "object",
          "properties": {
            "vendorName": { "type": "string", "description": "The name of the vendor/seller issuing the invoice." },
            "invoiceNumber": { "type": "string", "description": "The invoice number or ID." },
            "invoiceDate": { "type": "string", "description": "The date the invoice was issued, as written in the source text." },
            "dueDate": { "type": "string", "description": "The payment due date, as written in the source text. Empty string if not present." },
            "totalAmount": { "type": "string", "description": "The total amount due, as written in the source text (including currency symbol if present)." },
            "lineItems": {
              "type": "array",
              "description": "Individual line items on the invoice.",
              "items": {
                "type": "object",
                "properties": {
                  "description": { "type": "string" },
                  "quantity": { "type": "string" },
                  "unitPrice": { "type": "string" },
                  "amount": { "type": "string" }
                },
                "required": ["description"]
              }
            }
          },
          "required": ["vendorName", "invoiceNumber", "invoiceDate", "totalAmount", "lineItems"]
        }
        """;
}
