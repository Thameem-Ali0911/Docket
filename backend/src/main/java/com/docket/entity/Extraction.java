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
@Table(name = "extractions")
public class Extraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Document document;

    // Raw JSON of the type-specific extraction DTO (e.g. InvoiceExtractionDto),
    // stored as text rather than normalized columns since the shape differs per
    // DocumentType (Phase 7 will add Contract/Resume variants).
    @Column(name = "fields_json", nullable = false, columnDefinition = "TEXT")
    private String fieldsJson;

    @Column(name = "failed_reason")
    private String failedReason;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Human-corrected JSON override — present when a user has edited AI-extracted fields.
     *  The original {@code fieldsJson} is preserved for audit purposes. */
    @Column(name = "human_corrected_json", columnDefinition = "TEXT")
    private String humanCorrectedJson;

    /** Optional free-text note explaining the correction reason. */
    @Column(name = "correction_note", length = 1000)
    private String correctionNote;

    /** Timestamp of the last human correction. */
    @Column(name = "corrected_at")
    private OffsetDateTime correctedAt;

    protected Extraction() {
    }

    public Extraction(Document document, String fieldsJson) {
        this.document = document;
        this.fieldsJson = fieldsJson;
    }

    public Long getId() { return id; }
    public Document getDocument() { return document; }
    public String getFieldsJson() { return fieldsJson; }
    public void setFieldsJson(String fieldsJson) { this.fieldsJson = fieldsJson; }
    public String getFailedReason() { return failedReason; }
    public void setFailedReason(String failedReason) { this.failedReason = failedReason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public String getHumanCorrectedJson() { return humanCorrectedJson; }
    public void setHumanCorrectedJson(String humanCorrectedJson) { this.humanCorrectedJson = humanCorrectedJson; }
    public String getCorrectionNote() { return correctionNote; }
    public void setCorrectionNote(String correctionNote) { this.correctionNote = correctionNote; }
    public OffsetDateTime getCorrectedAt() { return correctedAt; }
    public void setCorrectedAt(OffsetDateTime correctedAt) { this.correctedAt = correctedAt; }

    /** Returns the effective fields to display — human-corrected JSON if present, else AI-extracted JSON. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getEffectiveFieldsJson() {
        return humanCorrectedJson != null ? humanCorrectedJson : fieldsJson;
    }
}
