package com.docket.service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.docket.dto.document.DocumentExportDto;
import com.docket.dto.document.DocumentExportDto.AnomalyFlagExportDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.Extraction;
import com.docket.entity.Summary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ExportService {

    private static final Logger log = LoggerFactory.getLogger(ExportService.class);
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObjectMapper objectMapper;

    public ExportService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Builds a DocumentExportDto from the document and its related data.
     */
    public DocumentExportDto buildExportDto(Document document, Extraction extraction, Summary summary, List<AnomalyFlag> anomalies) {
        Map<String, Object> fieldsMap = Collections.emptyMap();
        if (extraction != null && extraction.getFieldsJson() != null && !extraction.getFieldsJson().isBlank()) {
            try {
                fieldsMap = objectMapper.readValue(extraction.getFieldsJson(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse fields_json for export of document id={}: {}", document.getId(), e.getMessage());
            }
        }

        List<AnomalyFlagExportDto> anomalyDtos = Collections.emptyList();
        if (anomalies != null && !anomalies.isEmpty()) {
            anomalyDtos = anomalies.stream()
                    .map(a -> new AnomalyFlagExportDto(a.getFieldName(), a.getDescription(), a.getSeverity()))
                    .collect(Collectors.toList());
        }

        return new DocumentExportDto(
                document.getId(),
                document.getType(),
                document.getFileUrl(),
                document.getStatus(),
                document.getUploadedAt(),
                document.getFailedReason(),
                summary != null ? summary.getSummaryText() : null,
                fieldsMap,
                anomalyDtos
        );
    }

    /**
     * Converts a single DocumentExportDto to a pretty-printed JSON string.
     */
    public String exportAsJson(DocumentExportDto exportDto) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportDto);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize document export to JSON", e);
        }
    }

    /**
     * Converts a list of DocumentExportDtos to a pretty-printed JSON array string.
     */
    public String exportAsJson(List<DocumentExportDto> exportDtos) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(exportDtos);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize workspace documents export to JSON", e);
        }
    }

    /**
     * Converts a single DocumentExportDto to CSV format.
     */
    public String exportAsCsv(DocumentExportDto exportDto) {
        return exportAsCsv(List.of(exportDto));
    }

    /**
     * Converts a list of DocumentExportDtos to CSV format.
     */
    public String exportAsCsv(List<DocumentExportDto> exportDtos) {
        StringBuilder csv = new StringBuilder();
        // CSV Header
        csv.append("Document ID,Type,Filename,Status,Uploaded At,Failed Reason,Summary,Anomaly Count,Anomalies Summary,Extracted Fields\n");

        for (DocumentExportDto doc : exportDtos) {
            String filename = doc.fileUrl() != null ? doc.fileUrl().substring(doc.fileUrl().lastIndexOf('/') + 1) : "";
            String uploadedAt = doc.uploadedAt() != null ? doc.uploadedAt().format(ISO_FORMATTER) : "";
            int anomalyCount = doc.anomalies() != null ? doc.anomalies().size() : 0;
            
            String anomaliesSummary = "";
            if (doc.anomalies() != null && !doc.anomalies().isEmpty()) {
                anomaliesSummary = doc.anomalies().stream()
                        .map(a -> String.format("[%s] %s: %s", a.severity(), a.fieldName(), a.description()))
                        .collect(Collectors.joining("; "));
            }

            String extractedFieldsJson = "";
            if (doc.extractedFields() != null && !doc.extractedFields().isEmpty()) {
                try {
                    extractedFieldsJson = objectMapper.writeValueAsString(doc.extractedFields());
                } catch (Exception e) {
                    extractedFieldsJson = doc.extractedFields().toString();
                }
            }

            csv.append(escapeCsv(String.valueOf(doc.id()))).append(",");
            csv.append(escapeCsv(doc.type() != null ? doc.type().name() : "")).append(",");
            csv.append(escapeCsv(filename)).append(",");
            csv.append(escapeCsv(doc.status() != null ? doc.status().name() : "")).append(",");
            csv.append(escapeCsv(uploadedAt)).append(",");
            csv.append(escapeCsv(doc.failedReason() != null ? doc.failedReason() : "")).append(",");
            csv.append(escapeCsv(doc.summary() != null ? doc.summary() : "")).append(",");
            csv.append(anomalyCount).append(",");
            csv.append(escapeCsv(anomaliesSummary)).append(",");
            csv.append(escapeCsv(extractedFieldsJson)).append("\n");
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        // Escape quotes by doubling them
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
