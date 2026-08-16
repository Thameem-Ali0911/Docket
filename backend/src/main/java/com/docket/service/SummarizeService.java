package com.docket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.docket.dto.document.SummaryResponseDto;
import com.docket.entity.Document;
import com.docket.entity.Summary;
import com.docket.prompt.SummarizePrompt;
import com.docket.repository.SummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SummarizeService {

    private static final Logger log = LoggerFactory.getLogger(SummarizeService.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final SummaryRepository summaryRepository;

    public SummarizeService(GeminiClient geminiClient, ObjectMapper objectMapper,
                            Validator validator, SummaryRepository summaryRepository) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.summaryRepository = summaryRepository;
    }

    public void summarizeDocument(Document document) {
        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            saveFailure(document, "No extracted text available to summarize.");
            return;
        }

        try {
            String prompt = String.format(SummarizePrompt.PROMPT, document.getExtractedText());
            String responseJson = geminiClient.generateStructuredJson(prompt, SummarizePrompt.JSON_SCHEMA);

            SummaryResponseDto dto = objectMapper.readValue(responseJson, SummaryResponseDto.class);

            Set<ConstraintViolation<SummaryResponseDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                saveFailure(document, "Invalid summary structure from LLM: " + errors);
                return;
            }

            Summary summary = summaryRepository.findByDocumentId(document.getId())
                    .orElseGet(() -> new Summary(document, null));

            summary.setSummaryText(stripNulBytes(dto.summary()));
            summary.setFailedReason(null);
            summaryRepository.save(summary);

        } catch (GeminiClient.GeminiException e) {
            saveFailure(document, "Gemini API failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to parse or validate LLM summary for document id={}", document.getId(), e);
            saveFailure(document, "Failed to parse summary: " + e.getMessage());
        } catch (Throwable t) {
            log.error("Unexpected error during summarization for document id={}", document.getId(), t);
            saveFailure(document, "Unexpected error: " + t.getMessage());
        }
    }

    private void saveFailure(Document document, String reason) {
        try {
            Summary summary = summaryRepository.findByDocumentId(document.getId())
                    .orElseGet(() -> new Summary(document, null));
            summary.setFailedReason(stripNulBytes(reason));
            summaryRepository.save(summary);
        } catch (Throwable t) {
            log.error("Failed to persist summary failure reason for document id={}", document.getId(), t);
        }
    }

    private String stripNulBytes(String text) {
        if (text == null) return null;
        return text.indexOf('\u0000') == -1 ? text : text.replace("\u0000", "");
    }
}
