package com.docket.service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docket.dto.document.AnomalyCheckResponseDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.Template;
import com.docket.prompt.AnomalyCheckPrompt;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.TemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class AnomalyService {

    private static final Logger log = LoggerFactory.getLogger(AnomalyService.class);

    private final TemplateRepository templateRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public AnomalyService(TemplateRepository templateRepository, AnomalyFlagRepository anomalyFlagRepository,
                          GeminiClient geminiClient, ObjectMapper objectMapper, Validator validator) {
        this.templateRepository = templateRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @Transactional
    public void checkAnomalies(Document document) {
        if (document.getExtractedText() == null || document.getExtractedText().isBlank()) {
            return;
        }

        Optional<Template> templateOpt = templateRepository.findByWorkspaceIdAndDocumentType(
                document.getWorkspace().getId(), document.getType());

        if (templateOpt.isEmpty()) {
            return; // No template exists, so nothing to compare against.
        }

        Template template = templateOpt.get();
        if (template.getDocument().getId().equals(document.getId())) {
            return; // Don't flag the template against itself.
        }

        Document templateDoc = template.getDocument();
        if (templateDoc.getExtractedText() == null || templateDoc.getExtractedText().isBlank()) {
            return;
        }

        try {
            String prompt = String.format(AnomalyCheckPrompt.PROMPT, templateDoc.getExtractedText(), document.getExtractedText());
            String responseJson = geminiClient.generateStructuredJson(prompt, AnomalyCheckPrompt.JSON_SCHEMA);

            AnomalyCheckResponseDto dto = objectMapper.readValue(responseJson, AnomalyCheckResponseDto.class);

            Set<ConstraintViolation<AnomalyCheckResponseDto>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errors = violations.stream()
                        .map(v -> v.getPropertyPath() + " " + v.getMessage())
                        .collect(Collectors.joining(", "));
                log.error("Invalid anomaly structure from LLM for document id={}: {}", document.getId(), errors);
                return; // Not saving a failure row for anomalies currently to avoid noise.
            }

            // Clear any existing flags
            anomalyFlagRepository.deleteAll(anomalyFlagRepository.findByDocumentId(document.getId()));

            if (dto.flags() != null) {
                for (var flagDto : dto.flags()) {
                    AnomalyFlag flag = new AnomalyFlag(document,
                            stripNulBytes(flagDto.fieldName()),
                            stripNulBytes(flagDto.description()),
                            stripNulBytes(flagDto.severity()));
                    anomalyFlagRepository.save(flag);
                }
            }

        } catch (GeminiClient.GeminiException e) {
            log.error("Gemini API failed during anomaly check for document id={}", document.getId(), e);
        } catch (Exception e) {
            log.error("Failed to parse or validate LLM anomaly check for document id={}", document.getId(), e);
        } catch (Throwable t) {
            log.error("Unexpected error during anomaly check for document id={}", document.getId(), t);
        }
    }

    private String stripNulBytes(String text) {
        if (text == null) return null;
        return text.indexOf('\u0000') == -1 ? text : text.replace("\u0000", "");
    }
}
