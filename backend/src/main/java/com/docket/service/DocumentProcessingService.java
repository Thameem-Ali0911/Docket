package com.docket.service;

import java.io.File;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.repository.DocumentRepository;

@Service
public class DocumentProcessingService {

    private final OcrService ocrService;
    private final StorageService storageService;
    private final DocumentRepository documentRepository;

    public DocumentProcessingService(OcrService ocrService, StorageService storageService, DocumentRepository documentRepository) {
        this.ocrService = ocrService;
        this.storageService = storageService;
        this.documentRepository = documentRepository;
    }

    @Async
    public void processDocumentAsync(Document doc) {
        try {
            File savedFile = storageService.getFile(doc.getFileUrl());
            if (savedFile != null) {
                String extractedText = ocrService.extractText(savedFile);
                doc.setExtractedText(extractedText);
                doc.setStatus(DocumentStatus.PROCESSED);
            } else {
                doc.setStatus(DocumentStatus.FAILED);
                doc.setFailedReason("File could not be found for OCR.");
            }
        } catch (Exception e) {
            doc.setStatus(DocumentStatus.FAILED);
            doc.setFailedReason("OCR failed: " + e.getMessage());
        }
        documentRepository.save(doc);
    }
}
