package com.docket.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import org.springframework.stereotype.Service;

import com.docket.dto.InvoiceExtractionDto;
import com.docket.entity.Document;
import com.docket.entity.Extraction;
import com.docket.prompt.ExtractInvoicePrompt;
import com.docket.repository.ExtractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExtractionService {

    private final GeminiClient geminiClient;
    private final ExtractionRepository extractionRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ExtractionService(GeminiClient geminiClient,
                              ExtractionRepository extractionRepository,
                              ObjectMapper objectMapper) {
        this.geminiClient = geminiClient;
        this.extractionRepository = extractionRepository;
        this.objectMapper = objectMapper;
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            this.validator = factory.getValidator();
        }
    }

    /**
     * Extracts structured invoice fields for a document that already has OCR'd
     * extractedText, and persists an Extraction row (success or failure reason).
     * Only invoked for DocumentType.INVOICE for now (Phase 7 adds Contract/Resume).
     */
    public void extractInvoiceFields(Document document) {
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            saveFailure(document, "No extracted text available to run field extraction on.");
            return;
        }

        try {
            String prompt = ExtractInvoicePrompt.buildPrompt(extractedText);
            String rawJson = geminiClient.generateStructuredJson(prompt, ExtractInvoicePrompt.RESPONSE_SCHEMA_JSON);

            InvoiceExtractionDto dto = objectMapper.readValue(rawJson, InvoiceExtractionDto.class);

            Set<ConstraintViolation<InvoiceExtractionDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String reasons = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("validation failed");
                saveFailure(document, "Gemini response failed validation: " + reasons);
                return;
            }

            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, rawJson));
            extraction.setFieldsJson(rawJson);
            extraction.setFailedReason(null);
            extractionRepository.save(extraction);
        } catch (GeminiClient.GeminiException e) {
            saveFailure(document, "Gemini extraction failed: " + e.getMessage());
        } catch (Exception e) {
            saveFailure(document, "Could not parse Gemini's response as valid invoice JSON: " + e.getMessage());
        }
    }

    private void saveFailure(Document document, String reason) {
        Extraction extraction = extractionRepository.findByDocumentId(document.getId())
            .orElse(new Extraction(document, "{}"));
        extraction.setFailedReason(reason);
        extractionRepository.save(extraction);
    }
}
