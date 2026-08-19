package com.docket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.docket.entity.DocumentType;
import com.docket.entity.Template;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    Optional<Template> findByWorkspaceIdAndDocumentType(Integer workspaceId, DocumentType documentType);
    List<Template> findByWorkspaceId(Integer workspaceId);
}

