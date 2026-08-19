package com.docket.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docket.entity.Document;
import com.docket.entity.DocumentType;
import com.docket.entity.Template;
import com.docket.entity.User;
import com.docket.exception.ApiException;
import com.docket.repository.TemplateRepository;
import com.docket.repository.UserRepository;
import com.docket.service.DocumentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final DocumentService documentService;

    public TemplateController(TemplateRepository templateRepository, UserRepository userRepository, DocumentService documentService) {
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.documentService = documentService;
    }

    public record SetTemplateRequest(
        @NotNull(message = "Document ID is required") Integer documentId
    ) {}

    /**
     * Sets the given document as the template for its document type within the user's workspace.
     *
     * @param request        the request body containing the document ID to set as template
     * @param authentication the Spring Security authentication principal
     * @return the created or updated Template entity
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
     * Gets all active templates in the user's workspace.
     *
     * @param authentication the Spring Security authentication principal
     * @return list of active templates for the workspace
     */
    @GetMapping
    public ResponseEntity<List<Template>> getAllTemplates(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        Integer workspaceId = user.getWorkspace().getId();

        List<Template> templates = templateRepository.findByWorkspaceId(workspaceId);
        return ResponseEntity.ok(templates);
    }

    /**
     * Gets the active template document for a specific document type in the user's workspace.
     *
     * @param type           the document type (INVOICE, CONTRACT, RESUME)
     * @param authentication the Spring Security authentication principal
     * @return the active template Document or null if not configured
     */
    @GetMapping("/{type}")
    public ResponseEntity<Document> getTemplate(@PathVariable("type") DocumentType type, Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        Integer workspaceId = user.getWorkspace().getId();

        Optional<Template> templateOpt = templateRepository.findByWorkspaceIdAndDocumentType(workspaceId, type);
        return ResponseEntity.ok(templateOpt.map(Template::getDocument).orElse(null));
    }

    /**
     * Deletes the active template for a specific document type in the user's workspace.
     *
     * @param type           the document type (INVOICE, CONTRACT, RESUME)
     * @param authentication the Spring Security authentication principal
     * @return 204 No Content response
     */
    @DeleteMapping("/{type}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable("type") DocumentType type, Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        Integer workspaceId = user.getWorkspace().getId();

        Optional<Template> templateOpt = templateRepository.findByWorkspaceIdAndDocumentType(workspaceId, type);
        templateOpt.ifPresent(templateRepository::delete);
        return ResponseEntity.noContent().build();
    }
}

