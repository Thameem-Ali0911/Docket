package com.docket.prompt;

public class ExtractResumePrompt {

    public static final String PROMPT_TEXT = """
            You are a strict data extraction assistant.
            Extract the following fields from the provided resume document text.
            If a field is not present in the document, return an empty string "" (or empty array [] for lists).
            Do NOT hallucinate or guess values.
            
            Fields to extract:
            - candidateName: The full name of the candidate.
            - email: The candidate's email address.
            - phone: The candidate's phone number.
            - skills: An array of strings representing the candidate's skills.
            - experience: An array of objects representing work experience, each with 'company', 'role', and 'duration'.
            - education: A summary string of the candidate's education.
            
            Also estimate an extraction confidence score between 0.0 and 1.0 for each extracted field
            (candidateName, email, phone, skills, experience, education) in 'fieldConfidences'.
            Higher scores (e.g. 0.90 - 1.0) indicate exact, unambiguous text matches; lower scores indicate
            partial clarity or ambiguity.
            """;

    public static final String JSON_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "candidateName": {
                  "type": "string"
                },
                "email": {
                  "type": "string"
                },
                "phone": {
                  "type": "string"
                },
                "skills": {
                  "type": "array",
                  "items": {
                    "type": "string"
                  }
                },
                "experience": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "company": {
                        "type": "string"
                      },
                      "role": {
                        "type": "string"
                      },
                      "duration": {
                        "type": "string"
                      }
                    },
                    "required": [
                      "company",
                      "role",
                      "duration"
                    ]
                  }
                },
                "education": {
                  "type": "string"
                },
                "fieldConfidences": {
                  "type": "object",
                  "description": "Confidence scores between 0.0 and 1.0 for each field.",
                  "properties": {
                    "candidateName": { "type": "number" },
                    "email": { "type": "number" },
                    "phone": { "type": "number" },
                    "skills": { "type": "number" },
                    "experience": { "type": "number" },
                    "education": { "type": "number" }
                  }
                }
              },
              "required": [
                "candidateName",
                "email",
                "phone",
                "skills",
                "experience",
                "education"
              ]
            }
            """;
}
