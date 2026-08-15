package com.docket.service;

import java.io.File;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docket.exception.ApiException;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.User;
import com.docket.repository.DocumentRepository;
import com.docket.repository.UserRepository;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final DocumentProcessingService documentProcessingService;

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, StorageService storageService, DocumentProcessingService documentProcessingService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.documentProcessingService = documentProcessingService;
    }

    public Document uploadDocument(Integer userId, DocumentType type, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String fileUrl = storageService.store(file);

        Document doc = new Document(user.getWorkspace(), type, fileUrl, DocumentStatus.PENDING);
        doc = documentRepository.save(doc);

        // Process OCR asynchronously
        documentProcessingService.processDocumentAsync(doc);

        return doc;
    }

    public List<Document> getDocumentsForWorkspace(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        return documentRepository.findByWorkspaceIdOrderByUploadedAtDesc(user.getWorkspace().getId());
    }

    /**
     * Fetches a single document, scoped to the requesting user's workspace so users
     * can't read documents belonging to another workspace by guessing IDs.
     */
    public Document getDocumentForWorkspace(Integer userId, Integer documentId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found"));

        if (!doc.getWorkspace().getId().equals(user.getWorkspace().getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "Document not found");
        }
        return doc;
    }
}

