package com.docket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.docket.entity.Document;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    
    // Scopes fetching of documents by workspace
    List<Document> findByWorkspaceIdOrderByUploadedAtDesc(Integer workspaceId);
}
