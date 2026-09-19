package com.docket.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.docket.dto.document.DocumentExportDto;
import com.docket.dto.document.DocumentListItemDto;
import com.docket.dto.extraction.ExtractionCorrectionRequest;
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

/**
 * Service managing document lifecycles, workspace isolation checks, enrichment,
 * secure file retrieval, reprocessing, and export generation.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final DocumentProcessingService documentProcessingService;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final ExtractionRepository extractionRepository;
    private final SummaryRepository summaryRepository;
    private final ExportService exportService;
    private final LlmBudgetService llmBudgetService;
    private final Optional<DocumentQueuePublisher> queuePublisher;

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository,
                           StorageService storageService,
                           DocumentProcessingService documentProcessingService,
                           AnomalyFlagRepository anomalyFlagRepository,
                           ExtractionRepository extractionRepository,
                           SummaryRepository summaryRepository,
                           ExportService exportService,
                           LlmBudgetService llmBudgetService,
                           Optional<DocumentQueuePublisher> queuePublisher) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.documentProcessingService = documentProcessingService;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.extractionRepository = extractionRepository;
        this.summaryRepository = summaryRepository;
        this.exportService = exportService;
        this.llmBudgetService = llmBudgetService;
        this.queuePublisher = queuePublisher;
    }

    public Document uploadDocument(Integer userId, DocumentType type, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        String fileUrl = storageService.store(file);

        Document doc = new Document(user.getWorkspace(), type, fileUrl, DocumentStatus.PENDING);
        doc = documentRepository.save(doc);

        // Dispatch for processing (queue or async depending on config)
        dispatchProcessing(doc);

        return doc;
    }

    /**
     * Batch uploads multiple documents to the current user's workspace.
     * Each file is validated, stored, and queued for asynchronous OCR & extraction processing.
     *
     * @param userId the authenticated user's ID
     * @param type   the DocumentType of the batch
     * @param files  list of MultipartFiles (up to 10 files per batch)
     * @return list of saved Document entities with PENDING status
     */
    @Transactional
    public List<Document> uploadDocuments(Integer userId, DocumentType type, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_BATCH", "No files provided in batch upload");
        }
        if (files.size() > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BATCH_TOO_LARGE", "Maximum 10 files allowed per batch upload");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        List<Document> documents = new java.util.ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String fileUrl = storageService.store(file);
            Document doc = new Document(user.getWorkspace(), type, fileUrl, DocumentStatus.PENDING);
            documents.add(doc);
        }

        if (documents.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_FILES", "All uploaded files were empty");
        }

        List<Document> savedDocs = documentRepository.saveAll(documents);

        for (Document doc : savedDocs) {
            dispatchProcessing(doc);
        }

        return savedDocs;
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
     * Retrieves a page of documents for the workspace enriched with anomaly counts.
     */
    public Page<DocumentListItemDto> getEnrichedDocumentsForWorkspace(Integer userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        Page<Document> docPage = documentRepository.findByWorkspaceId(user.getWorkspace().getId(), pageable);
        if (docPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Integer> docIds = docPage.getContent().stream().map(Document::getId).collect(Collectors.toList());
        List<AnomalyFlag> allFlags = anomalyFlagRepository.findByDocumentIdIn(docIds);
        Map<Integer, Long> flagCounts = allFlags.stream()
                .collect(Collectors.groupingBy(f -> f.getDocument().getId(), Collectors.counting()));

        List<DocumentListItemDto> dtos = docPage.getContent().stream()
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

        return new PageImpl<>(dtos, pageable, docPage.getTotalElements());
    }

    /**
     * Fetches a single document, scoped to the requesting user's workspace so users
     * cannot read documents belonging to another workspace by guessing IDs.
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
     * Fetches a single document enriched DTO, scoped to workspace.
     */
    public DocumentListItemDto getDocumentDtoForWorkspace(Integer userId, Integer documentId) {
        Document doc = getDocumentForWorkspace(userId, documentId);
        long anomalyCount = anomalyFlagRepository.findByDocumentId(documentId).size();

        return new DocumentListItemDto(
                doc.getId(),
                doc.getType(),
                doc.getFileUrl(),
                doc.getStatus(),
                doc.getUploadedAt(),
                doc.getFailedReason(),
                anomalyCount
        );
    }

    /**
     * Resolves the secure Spring Resource for a document after confirming workspace ownership.
     */
    public Resource getDocumentFileForWorkspace(Integer userId, Integer documentId) {
        Document doc = getDocumentForWorkspace(userId, documentId);
        return storageService.getResource(doc.getFileUrl());
    }

    /**
     * Manually triggers reprocessing for a failed or stuck document.
     */
    public Document reprocessDocument(Integer userId, Integer documentId) {
        Document doc = getDocumentForWorkspace(userId, documentId);
        doc.setStatus(DocumentStatus.PENDING);
        doc.setFailedReason(null);
        doc = documentRepository.save(doc);

        dispatchProcessing(doc);
        return doc;
    }

    /**
     * Dispatches a document for processing via RabbitMQ queue (if available) or
     * the @Async thread pool (default fallback).
     */
    private void dispatchProcessing(Document doc) {
        if (queuePublisher.isPresent()) {
            queuePublisher.get().publish(doc);
        } else {
            documentProcessingService.processDocumentAsync(doc);
        }
    }

    public Extraction getExtractionForWorkspace(Integer userId, Integer documentId) {
        getDocumentForWorkspace(userId, documentId);
        return extractionRepository.findByDocumentId(documentId).orElse(null);
    }

    public Summary getSummaryForWorkspace(Integer userId, Integer documentId) {
        getDocumentForWorkspace(userId, documentId);
        return summaryRepository.findByDocumentId(documentId).orElse(null);
    }

    public List<AnomalyFlag> getAnomaliesForWorkspace(Integer userId, Integer documentId) {
        getDocumentForWorkspace(userId, documentId);
        return anomalyFlagRepository.findByDocumentId(documentId);
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

    /**
     * Saves a human correction to an extraction, preserving the original AI output in fieldsJson.
     * The correctedJson is stored in humanCorrectedJson and becomes the authoritative value
     * returned by getEffectiveFieldsJson(). Downstream anomaly comparisons will use this corrected
     * version going forward.
     *
     * @param userId     the authenticated user
     * @param documentId the document whose extraction is being corrected
     * @param request    the correction payload
     * @return the updated Extraction entity
     */
    @Transactional
    public Extraction correctExtraction(Integer userId, Integer documentId,
                                         ExtractionCorrectionRequest request) {
        getDocumentForWorkspace(userId, documentId); // workspace isolation check

        Extraction extraction = extractionRepository.findByDocumentId(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "EXTRACTION_NOT_FOUND",
                        "No extraction found for document " + documentId));

        String sanitized = com.docket.util.SanitizationUtils.stripNulBytes(request.correctedFieldsJson());
        extraction.setHumanCorrectedJson(sanitized);
        extraction.setCorrectionNote(request.correctionNote());
        extraction.setCorrectedAt(OffsetDateTime.now());

        return extractionRepository.save(extraction);
    }

    /**
     * Returns today's LLM call count for the workspace associated with the given user.
     */
    public int getTodayLlmUsage(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return llmBudgetService.getTodayUsage(user.getWorkspace().getId());
    }
}
