package com.docket.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "anomaly_flags")
public class AnomalyFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Document document;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "severity", nullable = false)
    private String severity;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected AnomalyFlag() {
    }

    public AnomalyFlag(Document document, String fieldName, String description, String severity) {
        this.document = document;
        this.fieldName = fieldName;
        this.description = description;
        this.severity = severity;
    }

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public String getFieldName() { return fieldName; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
