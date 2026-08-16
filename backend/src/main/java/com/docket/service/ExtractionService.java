package com.docket.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.docket.dto.InvoiceExtractionDto;
import com.docket.dto.ContractExtractionDto;
import com.docket.dto.ResumeExtractionDto;
import com.docket.entity.Document;
import com.docket.entity.Extraction;
import com.docket.prompt.ExtractInvoicePrompt;
import com.docket.prompt.ExtractContractPrompt;
import com.docket.prompt.ExtractResumePrompt;
import com.docket.repository.ExtractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

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
     * Extracts structured invoice fields for a document that already has OCR'd
     * extractedText, and persists an Extraction row (success or failure reason).
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

            // Same NUL-byte issue as extractedText (see DocumentProcessingService) can show up
            // here too, since Gemini may echo back snippets of the source text verbatim into
            // the JSON fields it returns - sanitize before persisting for the same reason.
            String sanitizedJson = stripNulBytes(rawJson);
            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, sanitizedJson));
            extraction.setFieldsJson(sanitizedJson);
            extraction.setFailedReason(null);
            extractionRepository.save(extraction);
        } catch (GeminiClient.GeminiException e) {
            saveFailure(document, "Gemini extraction failed: " + e.getMessage());
        } catch (Exception e) {
            saveFailure(document, "Could not parse Gemini's response as valid invoice JSON: " + e.getMessage());
        } catch (Throwable t) {
            // Defense in depth: JSON parsing, validation, or serialization libraries could in
            // principle throw an Error (e.g. StackOverflowError on a pathological response).
            // Never let extraction die silently - always leave a visible failure record.
            log.error("Unexpected failure during invoice field extraction for document id={}",
                document.getId(), t);
            saveFailure(document, "Unexpected extraction failure: " + t.getClass().getSimpleName());
        }
    }

    public void extractContractFields(Document document) {
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            saveFailure(document, "No extracted text available to run field extraction on.");
            return;
        }

        try {
            String prompt = ExtractContractPrompt.PROMPT_TEXT + "\n\nDocument text:\n" + extractedText;
            String rawJson = geminiClient.generateStructuredJson(prompt, ExtractContractPrompt.JSON_SCHEMA);

            ContractExtractionDto dto = objectMapper.readValue(rawJson, ContractExtractionDto.class);

            Set<ConstraintViolation<ContractExtractionDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String reasons = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("validation failed");
                saveFailure(document, "Gemini response failed validation: " + reasons);
                return;
            }

            String sanitizedJson = stripNulBytes(rawJson);
            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, sanitizedJson));
            extraction.setFieldsJson(sanitizedJson);
            extraction.setFailedReason(null);
            extractionRepository.save(extraction);
        } catch (GeminiClient.GeminiException e) {
            saveFailure(document, "Gemini extraction failed: " + e.getMessage());
        } catch (Exception e) {
            saveFailure(document, "Could not parse Gemini's response as valid contract JSON: " + e.getMessage());
        } catch (Throwable t) {
            log.error("Unexpected failure during contract field extraction for document id={}",
                document.getId(), t);
            saveFailure(document, "Unexpected extraction failure: " + t.getClass().getSimpleName());
        }
    }

    public void extractResumeFields(Document document) {
        String extractedText = document.getExtractedText();
        if (extractedText == null || extractedText.isBlank()) {
            saveFailure(document, "No extracted text available to run field extraction on.");
            return;
        }

        try {
            String prompt = ExtractResumePrompt.PROMPT_TEXT + "\n\nDocument text:\n" + extractedText;
            String rawJson = geminiClient.generateStructuredJson(prompt, ExtractResumePrompt.JSON_SCHEMA);

            ResumeExtractionDto dto = objectMapper.readValue(rawJson, ResumeExtractionDto.class);

            Set<ConstraintViolation<ResumeExtractionDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String reasons = violations.stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("validation failed");
                saveFailure(document, "Gemini response failed validation: " + reasons);
                return;
            }

            String sanitizedJson = stripNulBytes(rawJson);
            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, sanitizedJson));
            extraction.setFieldsJson(sanitizedJson);
            extraction.setFailedReason(null);
            extractionRepository.save(extraction);
        } catch (GeminiClient.GeminiException e) {
            saveFailure(document, "Gemini extraction failed: " + e.getMessage());
        } catch (Exception e) {
            saveFailure(document, "Could not parse Gemini's response as valid resume JSON: " + e.getMessage());
        } catch (Throwable t) {
            log.error("Unexpected failure during resume field extraction for document id={}",
                document.getId(), t);
            saveFailure(document, "Unexpected extraction failure: " + t.getClass().getSimpleName());
        }
    }

    private void saveFailure(Document document, String reason) {
        try {
            Extraction extraction = extractionRepository.findByDocumentId(document.getId())
                .orElse(new Extraction(document, "{}"));
            extraction.setFailedReason(stripNulBytes(reason));
            extractionRepository.save(extraction);
        } catch (Throwable t) {
            // Last line of defense - if we can't even persist the failure reason (DB blip,
            // constraint issue), at least log it loudly instead of losing it silently.
            log.error("Could not persist extraction failure for document id={} (reason was: {})",
                document.getId(), reason, t);
        }
    }

    /**
     * PostgreSQL's text/UTF8 columns reject NUL (0x00) bytes outright. See the identical
     * helper in DocumentProcessingService for the full explanation - this is the same fix
     * applied to the extraction side of the pipeline.
     */
    private String stripNulBytes(String text) {
        if (text == null) {
            return null;
        }
        return text.indexOf('\u0000') == -1 ? text : text.replace("\u0000", "");
    }
}
