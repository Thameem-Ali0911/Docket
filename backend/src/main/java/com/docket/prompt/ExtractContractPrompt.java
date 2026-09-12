package com.docket.prompt;

public class ExtractContractPrompt {

    public static final String PROMPT_TEXT = """
            You are a strict data extraction assistant.
            Extract the following fields from the provided contract document text.
            If a field is not present in the document, return an empty string "" (or empty array [] for lists).
            Do NOT hallucinate or guess values.
            
            Fields to extract:
            - contractTitle: The title of the contract or agreement.
            - parties: An array of strings representing the names of the parties involved.
            - effectiveDate: The date the contract becomes effective.
            - governingLaw: The governing law or jurisdiction for the contract.
            - termOrDuration: The length, duration, or term of the contract.
            - totalValue: Any monetary value, contract amount, or total value mentioned.
            
            Also estimate an extraction confidence score between 0.0 and 1.0 for each extracted field
            (contractTitle, parties, effectiveDate, governingLaw, termOrDuration, totalValue) in 'fieldConfidences'.
            Higher scores (e.g. 0.90 - 1.0) indicate exact, unambiguous text matches; lower scores indicate
            partial clarity or ambiguity.
            """;

    public static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "contractTitle": {
                  "type": "string"
                },
                "parties": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "effectiveDate": {
                  "type": "string"
                },
                "governingLaw": {
                  "type": "string"
                },
                "termOrDuration": {
                  "type": "string"
                },
                "totalValue": {
                  "type": "string"
                },
                "fieldConfidences": {
                  "type": "object",
                  "description": "Confidence scores between 0.0 and 1.0 for each field.",
                  "properties": {
                    "contractTitle": { "type": "number" },
                    "parties": { "type": "number" },
                    "effectiveDate": { "type": "number" },
                    "governingLaw": { "type": "number" },
                    "termOrDuration": { "type": "number" },
                    "totalValue": { "type": "number" }
                  }
                }
              },
              "required": [
                "contractTitle",
                "parties",
                "effectiveDate",
                "governingLaw",
                "termOrDuration",
                "totalValue"
              ]
            }
            """;
}
