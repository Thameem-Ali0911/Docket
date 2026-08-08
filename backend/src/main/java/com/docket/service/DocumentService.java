package com.docket.service;

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

    public DocumentService(DocumentRepository documentRepository, UserRepository userRepository, StorageService storageService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    public Document uploadDocument(Integer userId, DocumentType type, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String fileUrl = storageService.store(file);

        Document doc = new Document(user.getWorkspace(), type, fileUrl, DocumentStatus.PENDING);
        return documentRepository.save(doc);
    }

    public List<Document> getDocumentsForWorkspace(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        return documentRepository.findByWorkspaceIdOrderByUploadedAtDesc(user.getWorkspace().getId());
    }
}
