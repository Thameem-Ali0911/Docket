package com.docket.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docket.entity.Document;
import com.docket.entity.DocumentType;
import com.docket.entity.Extraction;
import com.docket.entity.Summary;
import com.docket.repository.ExtractionRepository;
import com.docket.repository.SummaryRepository;
import com.docket.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final ExtractionRepository extractionRepository;
    private final SummaryRepository summaryRepository;

    public DocumentController(DocumentService documentService, ExtractionRepository extractionRepository, SummaryRepository summaryRepository) {
        this.documentService = documentService;
        this.extractionRepository = extractionRepository;
        this.summaryRepository = summaryRepository;
    }

    /**
     * Retrieves all documents for the authenticated user's workspace.
     * @param authentication the authenticated user's details
     * @return List of Documents
     */
    @GetMapping
    public ResponseEntity<List<Document>> getDocuments(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        List<Document> documents = documentService.getDocumentsForWorkspace(userId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Uploads a document to the current user's workspace.
     * @param type the DocumentType enum (e.g. INVOICE)
     * @param file the MultipartFile to upload
     * @param authentication the authenticated user's details
     * @return the saved Document entity
     */
    @PostMapping("/upload")
    public ResponseEntity<Document> uploadDocument(
            @RequestParam("type") DocumentType type,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        
        Integer userId = (Integer) authentication.getPrincipal();
        Document document = documentService.uploadDocument(userId, type, file);
        return ResponseEntity.ok(document);
    }

    /**
     * Fetches the Gemini-extracted structured fields for a document, if any.
     * Returns 200 with null body if extraction hasn't run/completed yet (e.g. still
     * PENDING, or non-INVOICE type not supported until Phase 7) - frontend treats a
     * null/empty response as "no extraction yet" rather than an error.
     */
    @GetMapping("/{id}/extraction")
    public ResponseEntity<Extraction> getExtraction(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        // Confirms the document belongs to the caller's workspace before exposing extraction data.
        documentService.getDocumentForWorkspace(userId, id);

        Optional<Extraction> extraction = extractionRepository.findByDocumentId(id);
        return ResponseEntity.ok(extraction.orElse(null));
    }

    /**
     * Fetches the Gemini-generated summary for a document, if any.
     * Returns 200 with null body if summarization hasn't run/completed yet.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<Summary> getSummary(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        // Confirms the document belongs to the caller's workspace before exposing summary data.
        documentService.getDocumentForWorkspace(userId, id);

        Optional<Summary> summary = summaryRepository.findByDocumentId(id);
        return ResponseEntity.ok(summary.orElse(null));
    }
}
