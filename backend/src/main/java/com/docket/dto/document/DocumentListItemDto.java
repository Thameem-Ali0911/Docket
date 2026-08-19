package com.docket.dto.document;

import java.time.OffsetDateTime;

import com.docket.entity.DocumentType;
import com.docket.entity.DocumentStatus;

/**
 * Enriched representation of a document for dashboard listings,
 * including anomaly flag counts for immediate visual status.
 */
public record DocumentListItemDto(
    Integer id,
    DocumentType type,
    String fileUrl,
    DocumentStatus status,
    OffsetDateTime uploadedAt,
    String failedReason,
    long anomalyCount
) {}
