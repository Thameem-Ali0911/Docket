package com.docket.dto.extraction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for PATCH /api/documents/{id}/extraction.
 * Carries human-corrected JSON to replace or augment Gemini-extracted fields.
 */
public record ExtractionCorrectionRequest(

        @NotBlank(message = "correctedFieldsJson must not be blank")
        @Size(max = 65535, message = "correctedFieldsJson exceeds maximum length")
        String correctedFieldsJson,

        String correctionNote

) {}
