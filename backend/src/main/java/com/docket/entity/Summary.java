package com.docket.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "summaries")
public class Summary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Document document;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Summary() {
    }

    public Summary(Document document, String summaryText) {
        this.document = document;
        this.summaryText = summaryText;
    }

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public String getSummaryText() { return summaryText; }
    public void setSummaryText(String summaryText) { this.summaryText = summaryText; }
    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
