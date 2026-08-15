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

    public DocumentProcessingService(OcrService ocrService, StorageService storageService,
                                      DocumentRepository documentRepository, ExtractionService extractionService) {
        this.ocrService = ocrService;
        this.storageService = storageService;
        this.documentRepository = documentRepository;
        this.extractionService = extractionService;
    }

    @Async
    public void processDocumentAsync(Document doc) {
        // IMPORTANT: catches Throwable, not just Exception. Native OCR bindings (Tess4J/JNI)
        // can throw Errors (UnsatisfiedLinkError, NoClassDefFoundError, etc.) on misconfigured
        // environments, and a plain `catch (Exception e)` here lets those escape the async
        // thread silently - the document row never gets saved and stays PENDING forever with
        // no error surfaced anywhere. Catching Throwable guarantees we always reach the save
        // below and the document always ends up in a terminal, visible state.
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
                    doc.setExtractedText(extractedText);
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

        // Phase 4: run structured field extraction for successfully-OCR'd invoices.
        // Contract/Resume extraction is added in Phase 7.
        // Wrapped separately (and defensively) so a failure here can never re-open or affect
        // the document's own status, which has already been safely persisted above.
        if (doc.getStatus() == DocumentStatus.PROCESSED && doc.getType() == com.docket.entity.DocumentType.INVOICE) {
            try {
                extractionService.extractInvoiceFields(doc);
            } catch (Throwable t) {
                log.error("Field extraction failed for document id={}", doc.getId(), t);
            }
        }
    }

    private String summarize(Throwable t) {
        String message = t.getMessage();
        return t.getClass().getSimpleName() + (message != null ? ": " + message : "");
    }
}
