package com.docket.service;

import java.io.File;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.repository.DocumentRepository;

@Service
public class DocumentProcessingService {

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
        try {
            File savedFile = storageService.getFile(doc.getFileUrl());
            if (savedFile != null) {
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
                doc.setFailedReason("File could not be found for OCR.");
            }
        } catch (Exception e) {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setFailedReason("OCR failed: " + e.getMessage());
        }
        documentRepository.save(doc);

        // Phase 4: run structured field extraction for successfully-OCR'd invoices.
        // Contract/Resume extraction is added in Phase 7.
        if (doc.getStatus() == DocumentStatus.PROCESSED && doc.getType() == com.docket.entity.DocumentType.INVOICE) {
            extractionService.extractInvoiceFields(doc);
        }
    }
}
