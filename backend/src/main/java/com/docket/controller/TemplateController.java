package com.docket.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docket.entity.Document;
import com.docket.entity.DocumentType;
import com.docket.entity.Template;
import com.docket.repository.TemplateRepository;
import com.docket.service.DocumentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateRepository templateRepository;
    private final DocumentService documentService;

    public TemplateController(TemplateRepository templateRepository, DocumentService documentService) {
        this.templateRepository = templateRepository;
        this.documentService = documentService;
    }

    public record SetTemplateRequest(
        @NotNull(message = "Document ID is required") Integer documentId
    ) {}

    /**
     * Sets the given document as the template for its document type within the user's workspace.
     */
    @PostMapping
    public ResponseEntity<Template> setTemplate(@Valid @RequestBody SetTemplateRequest request, Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        Document doc = documentService.getDocumentForWorkspace(userId, request.documentId());

        Optional<Template> existingOpt = templateRepository.findByWorkspaceIdAndDocumentType(doc.getWorkspace().getId(), doc.getType());
        Template template;
        if (existingOpt.isPresent()) {
            template = existingOpt.get();
            template.setDocument(doc);
        } else {
            template = new Template(doc.getWorkspace(), doc.getType(), doc);
        }

        templateRepository.save(template);
        return ResponseEntity.ok(template);
    }

    /**
     * Gets the active template document for a specific document type in the user's workspace.
     */
    @GetMapping("/{type}")
    public ResponseEntity<Document> getTemplate(@PathVariable("type") DocumentType type, Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        // Since workspace is 1:1 with user for MVP, we just fetch a document from the user to resolve workspace ID.
        // Or we can just get workspaceId from any document they own, or from user entity.
        // For simplicity, we just fetch all documents and get workspace ID from the first one.
        List<Document> userDocs = documentService.getDocumentsForWorkspace(userId);
        if (userDocs.isEmpty()) {
            return ResponseEntity.ok(null);
        }
        Integer workspaceId = userDocs.get(0).getWorkspace().getId();

        Optional<Template> templateOpt = templateRepository.findByWorkspaceIdAndDocumentType(workspaceId, type);
        return ResponseEntity.ok(templateOpt.map(Template::getDocument).orElse(null));
    }
}
