package com.docket.dto.document;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.docket.entity.DocumentType;
import com.docket.entity.DocumentStatus;

/**
 * Full export structure representing a document's extracted intelligence,
 * summary, and anomaly flags.
 */
public record DocumentExportDto(
    Integer id,
    DocumentType type,
    String fileUrl,
    DocumentStatus status,
    OffsetDateTime uploadedAt,
    String failedReason,
    String summary,
    Map<String, Object> extractedFields,
    List<AnomalyFlagExportDto> anomalies
) {
    public record AnomalyFlagExportDto(
        String fieldName,
        String description,
        String severity
    ) {}
}
