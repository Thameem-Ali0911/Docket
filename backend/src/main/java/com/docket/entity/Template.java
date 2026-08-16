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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "templates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"workspace_id", "document_type"})
})
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    private DocumentType documentType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Template() {
    }

    public Template(Workspace workspace, DocumentType documentType, Document document) {
        this.workspace = workspace;
        this.documentType = documentType;
        this.document = document;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public DocumentType getDocumentType() { return documentType; }
    public Document getDocument() { return document; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    
    public void setDocument(Document document) { this.document = document; }
}
