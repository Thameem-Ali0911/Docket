package com.docket.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docket.entity.Document;
import com.docket.entity.DocumentType;
import com.docket.service.DocumentService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
}
