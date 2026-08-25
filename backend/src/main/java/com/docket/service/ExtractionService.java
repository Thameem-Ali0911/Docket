package com.docket.service;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.docket.dto.ContractExtractionDto;
import com.docket.dto.InvoiceExtractionDto;
import com.docket.dto.ResumeExtractionDto;
import com.docket.entity.Document;
import com.docket.entity.Extraction;
import com.docket.prompt.ExtractContractPrompt;
import com.docket.prompt.ExtractInvoicePrompt;
import com.docket.prompt.ExtractResumePrompt;
import com.docket.repository.ExtractionRepository;
import com.docket.util.SanitizationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * Service responsible for LLM-based structured field extraction across all document types.
 * Utilizes generic validation and unified NUL-byte sanitization.
 */
@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

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
     * Generic extraction method parameterized by DTO type and prompt schema.
     * Eliminates copy-paste triplication across document types.
     */
    public <T> void extractFields(Document document, String prompt, String jsonSchema, Class<T> dtoClass) {
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            saveFailure(document, "No extracted text available to run field extraction on.");
            return;
        }

        try {
            String rawJson = geminiClient.generateStructuredJson(prompt, jsonSchema);
            String sanitizedJson = SanitizationUtils.stripNulBytes(rawJson);

            T dto = objectMapper.readValue(sanitizedJson, dtoClass);

            Set<ConstraintViolation<T>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String reasons = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("validation failed");
                saveFailure(document, "Gemini response failed validation: " + reasons);
                return;
            }

            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, sanitizedJson));
            extraction.setFieldsJson(sanitizedJson);
            extraction.setFailedReason(null);
            extractionRepository.save(extraction);

        } catch (GeminiClient.GeminiException e) {
            saveFailure(document, "Gemini extraction failed: " + e.getMessage());
        } catch (Exception e) {
            saveFailure(document, "Could not parse Gemini's response as valid JSON: " + e.getMessage());
        } catch (Throwable t) {
            log.error("Unexpected failure during field extraction for document id={}", document.getId(), t);
            saveFailure(document, "Unexpected extraction failure: " + t.getClass().getSimpleName());
        }
    }

    public void extractInvoiceFields(Document document) {
        String prompt = ExtractInvoicePrompt.buildPrompt(document.getExtractedText());
        extractFields(document, prompt, ExtractInvoicePrompt.RESPONSE_SCHEMA_JSON, InvoiceExtractionDto.class);
    }

    public void extractContractFields(Document document) {
        String prompt = ExtractContractPrompt.PROMPT_TEXT + "\n\nDocument text:\n" + document.getExtractedText();
        extractFields(document, prompt, ExtractContractPrompt.JSON_SCHEMA, ContractExtractionDto.class);
    }

    public void extractResumeFields(Document document) {
        String prompt = ExtractResumePrompt.PROMPT_TEXT + "\n\nDocument text:\n" + document.getExtractedText();
        extractFields(document, prompt, ExtractResumePrompt.JSON_SCHEMA, ResumeExtractionDto.class);
    }

    /**
     * Dispatches structured field extraction based on document type.
     */
    public void extractDocumentFields(Document document) {
        switch (document.getType()) {
            case INVOICE  -> extractInvoiceFields(document);
            case CONTRACT -> extractContractFields(document);
            case RESUME   -> extractResumeFields(document);
            default       -> log.warn("No extractor defined for document type={}", document.getType());
        }
    }

    private void saveFailure(Document document, String reason) {
        try {
            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, "{}"));
            extraction.setFailedReason(SanitizationUtils.stripNulBytes(reason));
            extractionRepository.save(extraction);
        } catch (Throwable t) {
            log.error("Could not persist extraction failure for document id={} (reason was: {})",
                document.getId(), reason, t);
        }
    }
}
