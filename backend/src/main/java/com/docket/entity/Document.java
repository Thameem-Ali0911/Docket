package com.docket.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType type;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentStatus status;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private OffsetDateTime uploadedAt;

    protected Document() {
        // JPA requires a no-arg constructor
    }

    public Document(Workspace workspace, DocumentType type, String fileUrl, DocumentStatus status) {
        this.workspace = workspace;
        this.type = type;
        this.fileUrl = fileUrl;
        this.status = status;
    }

    public Integer getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public OffsetDateTime getUploadedAt() {
        return uploadedAt;
    }
}
