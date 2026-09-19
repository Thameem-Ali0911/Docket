package com.docket.prompt;

public class ExtractKycPrompt {

    public static final String PROMPT_TEXT = """
            You are a strict data extraction assistant.
            Extract the following KYC (Know Your Customer) and identity verification fields from the provided document text.
            If a field is not present in the document, return an empty string "".
            Do NOT hallucinate or guess values.

            Fields to extract:
            - fullName: The full legal name of the individual.
            - idType: The type of identity document (e.g. Passport, National ID, Driver's License, Tax ID / PAN / SSN).
            - idNumber: The unique identification or document number.
            - dateOfBirth: The individual's date of birth.
            - nationality: The nationality or issuing country.
            - issueDate: The document issue date.
            - expiryDate: The document expiration date.
            - address: The residential or registered address.
            - verificationStatus: The verification or validity status indicated in the document (e.g. "VALID", "EXPIRED", "UNVERIFIED", "VERIFIED").

            Also estimate an extraction confidence score between 0.0 and 1.0 for each extracted field
            (fullName, idType, idNumber, dateOfBirth, nationality, issueDate, expiryDate, address, verificationStatus) in 'fieldConfidences'.
            Higher scores (e.g. 0.90 - 1.0) indicate exact, unambiguous text matches; lower scores indicate
            partial clarity or ambiguity.
            """;

    public static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "fullName": { "type": "string" },
                "idType": { "type": "string" },
                "idNumber": { "type": "string" },
                "dateOfBirth": { "type": "string" },
                "nationality": { "type": "string" },
                "issueDate": { "type": "string" },
                "expiryDate": { "type": "string" },
                "address": { "type": "string" },
                "verificationStatus": { "type": "string" },
                "fieldConfidences": {
                  "type": "object",
                  "description": "Confidence scores between 0.0 and 1.0 for each field.",
                  "properties": {
                    "fullName": { "type": "number" },
                    "idType": { "type": "number" },
                    "idNumber": { "type": "number" },
                    "dateOfBirth": { "type": "number" },
                    "nationality": { "type": "number" },
                    "issueDate": { "type": "number" },
                    "expiryDate": { "type": "number" },
                    "address": { "type": "number" },
                    "verificationStatus": { "type": "number" }
                  }
                }
              },
              "required": [
                "fullName",
                "idType",
                "idNumber",
                "dateOfBirth",
                "nationality",
                "issueDate",
                "expiryDate",
                "address",
                "verificationStatus"
              ]
            }
            """;
}
