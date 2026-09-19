package com.docket.service;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.repository.DocumentRepository;

@Service
public class DocumentProcessingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingService.class);

    // Below this average per-word Tesseract confidence (0-100 scale), we treat the
    // extraction as unreliable/garbled rather than trusting it just because it's non-empty.
    private static final double MIN_OCR_CONFIDENCE = 60.0;

    private final OcrService ocrService;
    private final StorageService storageService;
    private final DocumentRepository documentRepository;
    private final ExtractionService extractionService;
    private final SummarizeService summarizeService;
    private final AnomalyService anomalyService;
    private final ComparativeAnomalyService comparativeAnomalyService;
    private final LlmBudgetService llmBudgetService;

    public DocumentProcessingService(OcrService ocrService, StorageService storageService,
                                      DocumentRepository documentRepository, ExtractionService extractionService,
                                      SummarizeService summarizeService, AnomalyService anomalyService,
                                      ComparativeAnomalyService comparativeAnomalyService,
                                      LlmBudgetService llmBudgetService) {
        this.ocrService = ocrService;
        this.storageService = storageService;
        this.documentRepository = documentRepository;
        this.extractionService = extractionService;
        this.summarizeService = summarizeService;
        this.anomalyService = anomalyService;
        this.comparativeAnomalyService = comparativeAnomalyService;
        this.llmBudgetService = llmBudgetService;
    }

    /**
     * Thin @Async wrapper — used in "async" mode (default) where uploads trigger
     * processing on Spring's bounded ThreadPoolTaskExecutor.
     *
     * <p>In "queue" mode, {@link DocumentProcessingConsumer} calls
     * {@link #processDocument(Document)} directly instead of this method,
     * since the RabbitMQ listener thread is already the worker thread.</p>
     */
    @Async
    public void processDocumentAsync(Document doc) {
        processDocument(doc);
    }

    /**
     * Core synchronous processing pipeline: OCR → field extraction → summarization → anomaly check.
     *
     * <p>Called by:
     * <ul>
     *   <li>{@link #processDocumentAsync(Document)} in async mode</li>
     *   <li>{@link DocumentProcessingConsumer#onMessage} in queue mode</li>
     * </ul>
     *
     * <p>Catches {@code Throwable} (not just {@code Exception}) because native OCR bindings
     * (Tess4J/JNI) can throw {@code Error}s on misconfigured environments. The document
     * always ends up in a terminal, visible state.</p>
     */
    public void processDocument(Document doc) {
        try {
            File savedFile = storageService.getFile(doc.getFileUrl());
            if (savedFile != null && savedFile.exists() && savedFile.isFile()) {
                OcrResult result = ocrService.extractText(savedFile);
                String extractedText = result.getText();

                if (extractedText == null || extractedText.isBlank()) {
                    doc.setStatus(DocumentStatus.FAILED);
                    doc.setFailedReason("OCR completed but no text could be extracted from the file.");
                } else if (result.isConfidenceApplicable() && result.getConfidence() < MIN_OCR_CONFIDENCE) {
                    // Text came back, but Tesseract wasn't confident in what it read -
                    // likely garbled/incorrect characters rather than a genuine extraction.
                    doc.setStatus(DocumentStatus.FAILED);
                    doc.setFailedReason(String.format(
                        "OCR confidence too low (%.0f%%) - extracted text is likely inaccurate.",
                        result.getConfidence()));
                } else {
                    // PostgreSQL's text columns cannot store a NUL byte (0x00) under any
                    // encoding - some PDFs (particularly ones with unusual embedded/custom
                    // font encodings) yield a text-layer extraction that contains one.
                    doc.setExtractedText(com.docket.util.SanitizationUtils.stripNulBytes(extractedText));
                    doc.setStatus(DocumentStatus.PROCESSED);
                }
            } else {
                doc.setStatus(DocumentStatus.FAILED);
                doc.setFailedReason("File could not be found on disk for OCR.");
            }
        } catch (Throwable t) {
            log.error("OCR/processing failed for document id={} fileUrl={}", doc.getId(), doc.getFileUrl(), t);
            doc.setStatus(DocumentStatus.FAILED);
            doc.setFailedReason("OCR failed: " + summarize(t));
        }

        try {
            documentRepository.save(doc);
        } catch (Throwable t) {
            // If even the save fails (DB blip, constraint violation, etc.) there is nothing
            // more we can do for this row from here, but we must not let it escape silently -
            // log loudly so it's visible instead of a document just vanishing into PENDING.
            log.error("Failed to persist status for document id={} - it may remain stuck as PENDING.",
                doc.getId(), t);
            return;
        }

        // Run structured field extraction for successfully-OCR'd documents.
        if (doc.getStatus() == DocumentStatus.PROCESSED) {
            try {
                llmBudgetService.checkAndIncrementBudget(doc.getWorkspace().getId());
                extractionService.extractDocumentFields(doc);
            } catch (com.docket.exception.ApiException budgetEx) {
                log.warn("LLM budget exhausted for workspace {} — skipping extraction for doc {}",
                    doc.getWorkspace().getId(), doc.getId());
            } catch (Throwable t) {
                log.error("Field extraction failed for document id={}", doc.getId(), t);
            }
        }

        // Run summarization for all successfully-OCR'd documents.
        if (doc.getStatus() == DocumentStatus.PROCESSED) {
            try {
                llmBudgetService.checkAndIncrementBudget(doc.getWorkspace().getId());
                summarizeService.summarizeDocument(doc);
            } catch (com.docket.exception.ApiException budgetEx) {
                log.warn("LLM budget exhausted for workspace {} — skipping summarization for doc {}",
                    doc.getWorkspace().getId(), doc.getId());
            } catch (Throwable t) {
                log.error("Summarization failed for document id={}", doc.getId(), t);
            }
            
            // Run anomaly checks against the workspace template
            try {
                llmBudgetService.checkAndIncrementBudget(doc.getWorkspace().getId());
                anomalyService.checkAnomalies(doc);
            } catch (com.docket.exception.ApiException budgetEx) {
                log.warn("LLM budget exhausted for workspace {} — skipping anomaly check for doc {}",
                    doc.getWorkspace().getId(), doc.getId());
            } catch (Throwable t) {
                log.error("Anomaly checking failed for document id={}", doc.getId(), t);
            }

            // Run comparative & trend anomaly checks across workspace document history
            try {
                comparativeAnomalyService.detectComparativeAnomalies(doc);
            } catch (Throwable t) {
                log.error("Comparative anomaly checking failed for document id={}", doc.getId(), t);
            }
        }
    }

    private String summarize(Throwable t) {
        String message = t.getMessage();
        return t.getClass().getSimpleName() + (message != null ? ": " + message : "");
    }
}

