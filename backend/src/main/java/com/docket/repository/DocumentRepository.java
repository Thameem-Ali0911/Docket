package com.docket.repository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    
    // Scopes fetching of documents by workspace
    List<Document> findByWorkspaceIdOrderByUploadedAtDesc(Integer workspaceId);

    // Paginated document retrieval
    Page<Document> findByWorkspaceId(Integer workspaceId, Pageable pageable);

    // Status queries for reconciliation
    List<Document> findByStatus(DocumentStatus status);

    List<Document> findByStatusAndUploadedAtBefore(DocumentStatus status, OffsetDateTime threshold);

    // Usage tracking for billing periods
    long countByWorkspaceIdAndUploadedAtGreaterThanEqual(Integer workspaceId, OffsetDateTime since);
}

