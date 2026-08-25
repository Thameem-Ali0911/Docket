package com.docket.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
import com.docket.service.DocumentService;
import com.docket.service.ExportService;

/**
 * REST controller for document ingestion, inspection, secure file streaming,
 * reprocessing, and export generation.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final ExportService exportService;

    public DocumentController(DocumentService documentService, ExportService exportService) {
        this.documentService = documentService;
        this.exportService = exportService;
    }

    /**
     * Retrieves all documents for the authenticated user's workspace, enriched with anomaly counts.
     * Supports optional pagination (?page=0&size=20) or returns all items by default.
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
     * Retrieves a paginated list of documents for the workspace.
     *
     * @param pageable       pagination and sorting parameters
     * @param authentication the authenticated user's details
     * @return Page of DocumentListItemDto
     */
    @GetMapping("/page")
    public ResponseEntity<Page<DocumentListItemDto>> getDocumentsPaged(
            @PageableDefault(size = 20, sort = "uploadedAt", direction = Sort.Direction.DESC) Pageable pageable,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        Page<DocumentListItemDto> page = documentService.getEnrichedDocumentsForWorkspace(userId, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Retrieves metadata and anomaly summary for a single document by ID.
     *
     * @param id             the document ID
     * @param authentication the authenticated user's details
     * @return DocumentListItemDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentListItemDto> getDocumentById(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        DocumentListItemDto dto = documentService.getDocumentDtoForWorkspace(userId, id);
        return ResponseEntity.ok(dto);
    }

    /**
     * Securely streams the stored file content for viewing/previewing.
     * Access is strictly guarded by workspace ownership verification.
     *
     * @param id             the document ID
     * @param authentication the authenticated user's details
     * @return ResponseEntity streaming the file Resource
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getDocumentFile(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        Document doc = documentService.getDocumentForWorkspace(userId, id);
        Resource resource = documentService.getDocumentFileForWorkspace(userId, id);

        String contentType = "application/octet-stream";
        String fileUrlLower = doc.getFileUrl().toLowerCase();
        if (fileUrlLower.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (fileUrlLower.endsWith(".jpg") || fileUrlLower.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (fileUrlLower.endsWith(".png")) {
            contentType = "image/png";
        }

        String filename = doc.getFileUrl().substring(doc.getFileUrl().lastIndexOf('/') + 1);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * Manually triggers reprocessing of a failed or stuck document.
     *
     * @param id             the document ID
     * @param authentication the authenticated user's details
     * @return the updated Document entity
     */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<Document> reprocessDocument(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        Document doc = documentService.reprocessDocument(userId, id);
        return ResponseEntity.ok(doc);
    }

    /**
     * Uploads a document to the current user's workspace.
     *
     * @param type           the DocumentType enum (e.g. INVOICE)
     * @param file           the MultipartFile to upload
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
        Extraction extraction = documentService.getExtractionForWorkspace(userId, id);
        return ResponseEntity.ok(extraction);
    }

    /**
     * Fetches the Gemini-generated summary for a document, if any.
     */
    @GetMapping("/{id}/summary")
    public ResponseEntity<Summary> getSummary(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        Summary summary = documentService.getSummaryForWorkspace(userId, id);
        return ResponseEntity.ok(summary);
    }

    /**
     * Fetches any anomaly flags associated with the document.
     */
    @GetMapping("/{id}/anomalies")
    public ResponseEntity<List<AnomalyFlag>> getAnomalies(
            @PathVariable("id") Integer id,
            Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        List<AnomalyFlag> anomalies = documentService.getAnomaliesForWorkspace(userId, id);
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
