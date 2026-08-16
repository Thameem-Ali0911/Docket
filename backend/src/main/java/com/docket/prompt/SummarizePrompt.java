package com.docket.prompt;

public class SummarizePrompt {

    public static final String PROMPT = """
        Read the following document text and provide a concise, plain-English summary.
        The summary must be 3 to 5 sentences long. Focus on the core facts, purpose, and 
        any key dates or values.
        
        Document text:
        %s
        """;

    public static final String JSON_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "summary": {
              "type": "string",
              "description": "A 3-5 sentence plain-English summary of the document."
            }
          },
          "required": ["summary"]
        }
        """;
}
