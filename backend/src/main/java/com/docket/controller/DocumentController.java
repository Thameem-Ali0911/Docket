package com.docket.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docket.dto.document.DocumentExportDto;
import com.docket.dto.document.DocumentListItemDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.DocumentType;
import com.docket.entity.Extraction;
import com.docket.entity.Summary;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.ExtractionRepository;
import com.docket.repository.SummaryRepository;
import com.docket.service.DocumentService;
import com.docket.service.ExportService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final ExtractionRepository extractionRepository;
    private final SummaryRepository summaryRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final ExportService exportService;

    public DocumentController(DocumentService documentService,
                              ExtractionRepository extractionRepository, 
                              SummaryRepository summaryRepository,
                              AnomalyFlagRepository anomalyFlagRepository,
                              ExportService exportService) {
        this.documentService = documentService;
        this.extractionRepository = extractionRepository;
        this.summaryRepository = summaryRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.exportService = exportService;
    }

    /**
     * Retrieves all documents for the authenticated user's workspace, enriched with anomaly counts.
     *
     * @param authentication the authenticated user's details
     * @return List of DocumentListItemDto
     */
    @GetMapping
    public ResponseEntity<List<DocumentListItemDto>> getDocuments(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        List<DocumentListItemDto> documents = documentService.getEnrichedDocumentsForWorkspace(userId);
        return ResponseEntity.ok(documents);
    }

    /**
     * Uploads a document to the current user's workspace.
     *
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
     */
    @GetMapping("/{id}/extraction")
    public ResponseEntity<Extraction> getExtraction(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        documentService.getDocumentForWorkspace(userId, id);

        Optional<Extraction> extraction = extractionRepository.findByDocumentId(id);
        return ResponseEntity.ok(extraction.orElse(null));
    }

    /**
     * Fetches the Gemini-generated summary for a document, if any.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<Summary> getSummary(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        documentService.getDocumentForWorkspace(userId, id);

        Optional<Summary> summary = summaryRepository.findByDocumentId(id);
        return ResponseEntity.ok(summary.orElse(null));
    }

    /**
     * Fetches any anomaly flags associated with the document.
     */
    @GetMapping("/{id}/anomalies")
    public ResponseEntity<List<AnomalyFlag>> getAnomalies(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        documentService.getDocumentForWorkspace(userId, id);

        List<AnomalyFlag> anomalies = anomalyFlagRepository.findByDocumentId(id);
        return ResponseEntity.ok(anomalies);
    }

    /**
     * Exports a single document and its intelligence (extraction, summary, anomalies) as JSON or CSV.
     *
     * @param id             the document ID
     * @param format         export format ("json" or "csv", default is "json")
     * @param authentication the authenticated user's details
     * @return ResponseEntity with export file attachment
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportDocument(
            @PathVariable("id") Integer id,
            @RequestParam(value = "format", defaultValue = "json") String format,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        DocumentExportDto exportDto = documentService.getDocumentExport(userId, id);

        boolean isCsv = "csv".equalsIgnoreCase(format);
        String filename = String.format("document-%d-export.%s", id, isCsv ? "csv" : "json");
        String content = isCsv ? exportService.exportAsCsv(exportDto) : exportService.exportAsJson(exportDto);
        MediaType mediaType = isCsv ? MediaType.parseMediaType("text/csv; charset=UTF-8") : MediaType.APPLICATION_JSON;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Bulk exports all documents and intelligence for the workspace as JSON or CSV.
     *
     * @param format         export format ("json" or "csv", default is "json")
     * @param authentication the authenticated user's details
     * @return ResponseEntity with bulk export file attachment
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportWorkspaceDocuments(
            @RequestParam(value = "format", defaultValue = "json") String format,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        List<DocumentExportDto> exportDtos = documentService.getWorkspaceExports(userId);

        boolean isCsv = "csv".equalsIgnoreCase(format);
        String filename = String.format("docket-workspace-export.%s", isCsv ? "csv" : "json");
        String content = isCsv ? exportService.exportAsCsv(exportDtos) : exportService.exportAsJson(exportDtos);
        MediaType mediaType = isCsv ? MediaType.parseMediaType("text/csv; charset=UTF-8") : MediaType.APPLICATION_JSON;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(content.getBytes(StandardCharsets.UTF_8));
    }
}

