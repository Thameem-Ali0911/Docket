package com.docket.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.docket.dto.document.DocumentExportDto;
import com.docket.dto.document.DocumentListItemDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.Extraction;
import com.docket.entity.Summary;
import com.docket.entity.User;
import com.docket.exception.ApiException;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.DocumentRepository;
import com.docket.repository.ExtractionRepository;
import com.docket.repository.SummaryRepository;
import com.docket.repository.UserRepository;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final DocumentProcessingService documentProcessingService;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final ExtractionRepository extractionRepository;
    private final SummaryRepository summaryRepository;
    private final ExportService exportService;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           StorageService storageService,
                           DocumentProcessingService documentProcessingService,
                           AnomalyFlagRepository anomalyFlagRepository,
                           ExtractionRepository extractionRepository,
                           SummaryRepository summaryRepository,
                           ExportService exportService) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.documentProcessingService = documentProcessingService;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.extractionRepository = extractionRepository;
        this.summaryRepository = summaryRepository;
        this.exportService = exportService;
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
     * Retrieves all documents for the workspace enriched with anomaly counts.
     */
    public List<DocumentListItemDto> getEnrichedDocumentsForWorkspace(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        List<Document> docs = documentRepository.findByWorkspaceIdOrderByUploadedAtDesc(user.getWorkspace().getId());
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> docIds = docs.stream().map(Document::getId).collect(Collectors.toList());
        List<AnomalyFlag> allFlags = anomalyFlagRepository.findByDocumentIdIn(docIds);
        Map<Integer, Long> flagCounts = allFlags.stream()
                .collect(Collectors.groupingBy(f -> f.getDocument().getId(), Collectors.counting()));

        return docs.stream()
                .map(d -> new DocumentListItemDto(
                        d.getId(),
                        d.getType(),
                        d.getFileUrl(),
                        d.getStatus(),
                        d.getUploadedAt(),
                        d.getFailedReason(),
                        flagCounts.getOrDefault(d.getId(), 0L)
                ))
                .collect(Collectors.toList());
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

    /**
     * Fetches the complete export data for a single document.
     */
    public DocumentExportDto getDocumentExport(Integer userId, Integer documentId) {
        Document doc = getDocumentForWorkspace(userId, documentId);
        Extraction extraction = extractionRepository.findByDocumentId(documentId).orElse(null);
        Summary summary = summaryRepository.findByDocumentId(documentId).orElse(null);
        List<AnomalyFlag> anomalies = anomalyFlagRepository.findByDocumentId(documentId);

        return exportService.buildExportDto(doc, extraction, summary, anomalies);
    }

    /**
     * Fetches export data for all documents in the user's workspace.
     */
    public List<DocumentExportDto> getWorkspaceExports(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        List<Document> docs = documentRepository.findByWorkspaceIdOrderByUploadedAtDesc(user.getWorkspace().getId());
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> docIds = docs.stream().map(Document::getId).collect(Collectors.toList());
        Map<Integer, Extraction> extractionMap = extractionRepository.findByDocumentIdIn(docIds).stream()
                .collect(Collectors.toMap(e -> e.getDocument().getId(), e -> e, (a, b) -> a));
        Map<Integer, Summary> summaryMap = summaryRepository.findByDocumentIdIn(docIds).stream()
                .collect(Collectors.toMap(s -> s.getDocument().getId(), s -> s, (a, b) -> a));
        Map<Integer, List<AnomalyFlag>> anomalyMap = anomalyFlagRepository.findByDocumentIdIn(docIds).stream()
                .collect(Collectors.groupingBy(a -> a.getDocument().getId()));

        return docs.stream()
                .map(doc -> exportService.buildExportDto(
                        doc,
                        extractionMap.get(doc.getId()),
                        summaryMap.get(doc.getId()),
                        anomalyMap.getOrDefault(doc.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }
}


