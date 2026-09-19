package com.docket.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docket.dto.InvoiceExtractionDto;
import com.docket.dto.document.VendorTrendDto;
import com.docket.dto.document.WorkspaceTrendsDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.Extraction;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.ExtractionRepository;
import com.docket.util.SanitizationUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service providing cross-document comparative anomaly detection and workspace-level
 * vendor intelligence trends.
 *
 * <p>Identifies:
 * <ul>
 *   <li>Duplicate invoice numbers across documents in the workspace.</li>
 *   <li>Price surges (>50% jump compared to historical vendor average).</li>
 *   <li>Unusual billing drops / underbilling.</li>
 *   <li>Payment term anomalies (e.g. due date precedes invoice date).</li>
 * </ul>
 * </p>
 */
@Service
public class ComparativeAnomalyService {

    private static final Logger log = LoggerFactory.getLogger(ComparativeAnomalyService.class);
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]{1,2})?)");
    private static final Set<String> COMPARATIVE_FIELD_PREFIXES = Set.of(
            "invoiceNumber (Duplicate)",
            "totalAmount (Price Surge)",
            "totalAmount (Unusual Drop)",
            "dueDate (Invalid Window)",
            "dueDate (Term Contraction)"
    );

    private final ExtractionRepository extractionRepository;
    private final AnomalyFlagRepository anomalyFlagRepository;
    private final ObjectMapper objectMapper;

    public ComparativeAnomalyService(ExtractionRepository extractionRepository,
                                     AnomalyFlagRepository anomalyFlagRepository,
                                     ObjectMapper objectMapper) {
        this.extractionRepository = extractionRepository;
        this.anomalyFlagRepository = anomalyFlagRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes comparative cross-document analysis for a newly processed document.
     *
     * @param document the processed document
     */
    @Transactional
    public void detectComparativeAnomalies(Document document) {
        if (document == null || document.getId() == null || document.getWorkspace() == null) {
            return;
        }

        if (document.getType() != DocumentType.INVOICE) {
            // For MVP Phase 12.4, comparative analysis is focused on Invoices / Vendors.
            return;
        }

        Optional<Extraction> currentExtractionOpt = extractionRepository.findByDocumentId(document.getId());
        if (currentExtractionOpt.isEmpty()) {
            return;
        }

        Extraction currentExtraction = currentExtractionOpt.get();
        String json = currentExtraction.getEffectiveFieldsJson();
        if (json == null || json.isBlank()) {
            return;
        }

        InvoiceExtractionDto currentDto;
        try {
            currentDto = objectMapper.readValue(json, InvoiceExtractionDto.class);
        } catch (Exception e) {
            log.warn("Could not deserialize extraction for document id={} to InvoiceExtractionDto: {}",
                    document.getId(), e.getMessage());
            return;
        }

        // Clean up previous comparative anomaly flags on this document before re-evaluating
        List<AnomalyFlag> existingFlags = anomalyFlagRepository.findByDocumentId(document.getId());
        List<AnomalyFlag> toRemove = existingFlags.stream()
                .filter(f -> isComparativeFlag(f.getFieldName()))
                .toList();
        if (!toRemove.isEmpty()) {
            anomalyFlagRepository.deleteAll(toRemove);
        }

        // Fetch all extractions in the workspace to compare against
        List<Extraction> workspaceExtractions = extractionRepository.findByDocumentWorkspaceId(document.getWorkspace().getId());

        List<AnomalyFlag> newFlags = new ArrayList<>();

        // 1. Check for Duplicate Invoice Number
        checkDuplicateInvoiceNumber(document, currentDto, workspaceExtractions, newFlags);

        // 2. Check for Price Surges / Amount Outliers
        checkPriceSurgeAndTrends(document, currentDto, workspaceExtractions, newFlags);

        // 3. Check for Payment Term Anomalies
        checkPaymentTermAnomalies(document, currentDto, workspaceExtractions, newFlags);

        // Persist newly detected flags
        if (!newFlags.isEmpty()) {
            anomalyFlagRepository.saveAll(newFlags);
            log.info("Detected {} comparative anomaly flags for document id={}", newFlags.size(), document.getId());
        }
    }

    /**
     * Aggregates workspace invoices into vendor trend summaries and risk indicators.
     *
     * @param workspaceId the workspace ID
     * @return WorkspaceTrendsDto containing vendor trend metrics and workspace anomaly counts
     */
    @Transactional(readOnly = true)
    public WorkspaceTrendsDto calculateWorkspaceTrends(Integer workspaceId) {
        List<Extraction> extractions = extractionRepository.findByDocumentWorkspaceId(workspaceId);

        // Map normalized vendor name -> list of invoice data points
        Map<String, List<InvoiceDataPoint>> vendorInvoices = new HashMap<>();
        Map<String, String> vendorDisplayNames = new HashMap<>();

        for (Extraction ext : extractions) {
            Document doc = ext.getDocument();
            if (doc == null || doc.getStatus() != DocumentStatus.PROCESSED || doc.getType() != DocumentType.INVOICE) {
                continue;
            }

            String json = ext.getEffectiveFieldsJson();
            if (json == null || json.isBlank()) continue;

            try {
                InvoiceExtractionDto dto = objectMapper.readValue(json, InvoiceExtractionDto.class);
                String rawVendor = dto.getVendorName();
                if (rawVendor == null || rawVendor.isBlank()) continue;

                String normVendor = rawVendor.trim().toLowerCase();
                vendorDisplayNames.putIfAbsent(normVendor, rawVendor.trim());

                Optional<Double> amountOpt = parseAmount(dto.getTotalAmount());
                double amount = amountOpt.orElse(0.0);
                LocalDate date = parseDate(dto.getInvoiceDate()).orElse(null);

                vendorInvoices.computeIfAbsent(normVendor, k -> new ArrayList<>())
                        .add(new InvoiceDataPoint(doc.getId(), amount, date, dto.getInvoiceNumber(), dto.getInvoiceDate()));
            } catch (Exception e) {
                // Ignore malformed individual entries
            }
        }

        // Fetch anomaly flags for this workspace's documents
        List<Integer> allDocIds = vendorInvoices.values().stream()
                .flatMap(List::stream)
                .map(InvoiceDataPoint::documentId)
                .toList();
        List<AnomalyFlag> allFlags = allDocIds.isEmpty() ? List.of() : anomalyFlagRepository.findByDocumentIdIn(allDocIds);

        Map<Integer, List<AnomalyFlag>> flagsByDoc = new HashMap<>();
        for (AnomalyFlag flag : allFlags) {
            if (flag.getDocument() != null) {
                flagsByDoc.computeIfAbsent(flag.getDocument().getId(), k -> new ArrayList<>()).add(flag);
            }
        }

        int totalComparativeAnomalies = 0;
        int duplicateInvoicesCount = 0;
        int priceSurgesCount = 0;

        for (AnomalyFlag f : allFlags) {
            if (isComparativeFlag(f.getFieldName())) {
                totalComparativeAnomalies++;
                if (f.getFieldName().contains("Duplicate")) duplicateInvoicesCount++;
                if (f.getFieldName().contains("Price Surge")) priceSurgesCount++;
            }
        }

        List<VendorTrendDto> vendorTrends = new ArrayList<>();

        for (Map.Entry<String, List<InvoiceDataPoint>> entry : vendorInvoices.entrySet()) {
            String normVendor = entry.getKey();
            List<InvoiceDataPoint> points = entry.getValue();
            String displayName = vendorDisplayNames.getOrDefault(normVendor, normVendor);

            // Sort chronologically (earliest first, latest last)
            points.sort(Comparator.comparing((InvoiceDataPoint p) -> p.date() != null ? p.date() : LocalDate.MIN)
                    .thenComparing(InvoiceDataPoint::documentId));

            long count = points.size();
            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            List<Double> history = new ArrayList<>();
            List<Integer> docIds = new ArrayList<>();
            int vendorAnomalyCount = 0;

            for (InvoiceDataPoint p : points) {
                sum += p.amount();
                if (p.amount() < min) min = p.amount();
                if (p.amount() > max) max = p.amount();
                history.add(p.amount());
                docIds.add(p.documentId());

                List<AnomalyFlag> docFlags = flagsByDoc.get(p.documentId());
                if (docFlags != null) {
                    vendorAnomalyCount += docFlags.size();
                }
            }

            double avg = count > 0 ? sum / count : 0.0;
            if (min == Double.MAX_VALUE) min = 0.0;
            if (max == Double.MIN_VALUE) max = 0.0;

            InvoiceDataPoint latestPoint = points.get(points.size() - 1);
            Double latestAmount = latestPoint.amount();
            String latestInvoiceDate = latestPoint.rawDate();

            Double trendPct = null;
            if (count >= 2) {
                // Prior average (excluding latest point)
                double priorSum = sum - latestAmount;
                double priorAvg = priorSum / (count - 1);
                if (priorAvg > 0.0) {
                    trendPct = Math.round(((latestAmount - priorAvg) / priorAvg) * 1000.0) / 10.0;
                }
            }

            vendorTrends.add(new VendorTrendDto(
                    displayName,
                    count,
                    Math.round(sum * 100.0) / 100.0,
                    Math.round(avg * 100.0) / 100.0,
                    Math.round(min * 100.0) / 100.0,
                    Math.round(max * 100.0) / 100.0,
                    latestAmount,
                    latestInvoiceDate,
                    trendPct,
                    vendorAnomalyCount,
                    history,
                    docIds
            ));
        }

        // Sort vendors by total spend descending
        vendorTrends.sort(Comparator.comparingDouble(VendorTrendDto::totalSpend).reversed());

        return new WorkspaceTrendsDto(
                vendorTrends.size(),
                allDocIds.size(),
                totalComparativeAnomalies,
                duplicateInvoicesCount,
                priceSurgesCount,
                vendorTrends
        );
    }

    private void checkDuplicateInvoiceNumber(Document currentDoc,
                                            InvoiceExtractionDto currentDto,
                                            List<Extraction> workspaceExtractions,
                                            List<AnomalyFlag> outFlags) {
        String currentInvNum = currentDto.getInvoiceNumber();
        if (currentInvNum == null || currentInvNum.trim().isEmpty()) {
            return;
        }

        String normInvNum = currentInvNum.trim();

        for (Extraction other : workspaceExtractions) {
            Document otherDoc = other.getDocument();
            if (otherDoc == null || otherDoc.getId().equals(currentDoc.getId())) {
                continue;
            }
            if (otherDoc.getStatus() != DocumentStatus.PROCESSED || otherDoc.getType() != DocumentType.INVOICE) {
                continue;
            }

            String otherJson = other.getEffectiveFieldsJson();
            if (otherJson == null || otherJson.isBlank()) continue;

            try {
                InvoiceExtractionDto otherDto = objectMapper.readValue(otherJson, InvoiceExtractionDto.class);
                if (otherDto.getInvoiceNumber() != null &&
                        normInvNum.equalsIgnoreCase(otherDto.getInvoiceNumber().trim())) {
                    String otherVendor = otherDto.getVendorName() != null ? otherDto.getVendorName().trim() : "Unknown";
                    String desc = String.format(
                            "Duplicate invoice detected: Invoice #%s is identical to existing document #%d (%s).",
                            normInvNum, otherDoc.getId(), otherVendor
                    );
                    outFlags.add(new AnomalyFlag(
                            currentDoc,
                            SanitizationUtils.stripNulBytes("invoiceNumber (Duplicate)"),
                            SanitizationUtils.stripNulBytes(desc),
                            "HIGH"
                    ));
                    break; // One duplicate flag is sufficient
                }
            } catch (Exception ignored) {}
        }
    }

    private void checkPriceSurgeAndTrends(Document currentDoc,
                                         InvoiceExtractionDto currentDto,
                                         List<Extraction> workspaceExtractions,
                                         List<AnomalyFlag> outFlags) {
        String currentVendor = currentDto.getVendorName();
        if (currentVendor == null || currentVendor.trim().isEmpty()) return;

        Optional<Double> currentAmountOpt = parseAmount(currentDto.getTotalAmount());
        if (currentAmountOpt.isEmpty() || currentAmountOpt.get() <= 0.0) return;

        double currentAmount = currentAmountOpt.get();
        String normVendor = currentVendor.trim().toLowerCase();

        List<Double> historicalAmounts = new ArrayList<>();

        for (Extraction other : workspaceExtractions) {
            Document otherDoc = other.getDocument();
            if (otherDoc == null || otherDoc.getId().equals(currentDoc.getId())) continue;
            if (otherDoc.getStatus() != DocumentStatus.PROCESSED || otherDoc.getType() != DocumentType.INVOICE) continue;

            String otherJson = other.getEffectiveFieldsJson();
            if (otherJson == null || otherJson.isBlank()) continue;

            try {
                InvoiceExtractionDto otherDto = objectMapper.readValue(otherJson, InvoiceExtractionDto.class);
                if (otherDto.getVendorName() != null &&
                        normVendor.equalsIgnoreCase(otherDto.getVendorName().trim())) {
                    parseAmount(otherDto.getTotalAmount()).ifPresent(amt -> {
                        if (amt > 0) historicalAmounts.add(amt);
                    });
                }
            } catch (Exception ignored) {}
        }

        // Require at least 2 historical invoices to establish a reliable baseline
        if (historicalAmounts.size() < 2) {
            return;
        }

        double sum = 0.0;
        for (Double amt : historicalAmounts) {
            sum += amt;
        }
        double avg = sum / historicalAmounts.size();

        // Check for Price Surge: > 50% higher than historical average
        if (currentAmount >= avg * 1.50) {
            double percentSurge = ((currentAmount - avg) / avg) * 100.0;
            String severity = percentSurge >= 100.0 ? "HIGH" : "MEDIUM";
            String desc = String.format(
                    "Price surge detected: Total amount (%.2f) is %.1f%% higher than the historical average (%.2f) across %d prior invoice(s) for vendor '%s'.",
                    currentAmount, percentSurge, avg, historicalAmounts.size(), currentVendor.trim()
            );
            outFlags.add(new AnomalyFlag(
                    currentDoc,
                    SanitizationUtils.stripNulBytes("totalAmount (Price Surge)"),
                    SanitizationUtils.stripNulBytes(desc),
                    severity
            ));
        } else if (currentAmount <= avg * 0.40 && avg >= 100.0) {
            // Unusual billing drop (> 60% lower than average for non-trivial invoices)
            double percentDrop = ((avg - currentAmount) / avg) * 100.0;
            String desc = String.format(
                    "Unusual billing drop: Total amount (%.2f) is %.1f%% lower than the historical average (%.2f) for vendor '%s'.",
                    currentAmount, percentDrop, avg, currentVendor.trim()
            );
            outFlags.add(new AnomalyFlag(
                    currentDoc,
                    SanitizationUtils.stripNulBytes("totalAmount (Unusual Drop)"),
                    SanitizationUtils.stripNulBytes(desc),
                    "LOW"
            ));
        }
    }

    private void checkPaymentTermAnomalies(Document currentDoc,
                                          InvoiceExtractionDto currentDto,
                                          List<Extraction> workspaceExtractions,
                                          List<AnomalyFlag> outFlags) {
        Optional<LocalDate> invDateOpt = parseDate(currentDto.getInvoiceDate());
        Optional<LocalDate> dueDateOpt = parseDate(currentDto.getDueDate());

        if (invDateOpt.isPresent() && dueDateOpt.isPresent()) {
            LocalDate invDate = invDateOpt.get();
            LocalDate dueDate = dueDateOpt.get();

            if (dueDate.isBefore(invDate)) {
                String desc = String.format(
                        "Invalid payment window: Due date (%s) precedes invoice date (%s).",
                        currentDto.getDueDate(), currentDto.getInvoiceDate()
                );
                outFlags.add(new AnomalyFlag(
                        currentDoc,
                        SanitizationUtils.stripNulBytes("dueDate (Invalid Window)"),
                        SanitizationUtils.stripNulBytes(desc),
                        "HIGH"
                ));
            } else {
                // Check for payment term contraction compared to vendor history
                long currentWindow = ChronoUnit.DAYS.between(invDate, dueDate);
                if (currentWindow == 0) {
                    // Due on receipt when prior invoices had credit terms
                    checkVendorTermContraction(currentDoc, currentDto.getVendorName(), currentWindow, workspaceExtractions, outFlags);
                }
            }
        }
    }

    private void checkVendorTermContraction(Document currentDoc,
                                           String vendorName,
                                           long currentDays,
                                           List<Extraction> workspaceExtractions,
                                           List<AnomalyFlag> outFlags) {
        if (vendorName == null || vendorName.isBlank()) return;
        String normVendor = vendorName.trim().toLowerCase();

        List<Long> priorWindows = new ArrayList<>();
        for (Extraction other : workspaceExtractions) {
            Document otherDoc = other.getDocument();
            if (otherDoc == null || otherDoc.getId().equals(currentDoc.getId())) continue;
            if (otherDoc.getStatus() != DocumentStatus.PROCESSED || otherDoc.getType() != DocumentType.INVOICE) continue;

            String otherJson = other.getEffectiveFieldsJson();
            if (otherJson == null || otherJson.isBlank()) continue;

            try {
                InvoiceExtractionDto otherDto = objectMapper.readValue(otherJson, InvoiceExtractionDto.class);
                if (otherDto.getVendorName() != null && normVendor.equalsIgnoreCase(otherDto.getVendorName().trim())) {
                    Optional<LocalDate> id = parseDate(otherDto.getInvoiceDate());
                    Optional<LocalDate> dd = parseDate(otherDto.getDueDate());
                    if (id.isPresent() && dd.isPresent() && !dd.get().isBefore(id.get())) {
                        priorWindows.add(ChronoUnit.DAYS.between(id.get(), dd.get()));
                    }
                }
            } catch (Exception ignored) {}
        }

        if (priorWindows.size() >= 2) {
            double avgWindow = priorWindows.stream().mapToLong(Long::longValue).average().orElse(0.0);
            if (avgWindow >= 25.0 && currentDays <= 3) {
                String desc = String.format(
                        "Payment term contraction: Due date allows only %d day(s), whereas prior invoices averaged %.0f days for vendor '%s'.",
                        currentDays, avgWindow, vendorName.trim()
                );
                outFlags.add(new AnomalyFlag(
                        currentDoc,
                        SanitizationUtils.stripNulBytes("dueDate (Term Contraction)"),
                        SanitizationUtils.stripNulBytes(desc),
                        "MEDIUM"
                ));
            }
        }
    }

    private boolean isComparativeFlag(String fieldName) {
        if (fieldName == null) return false;
        for (String prefix : COMPARATIVE_FIELD_PREFIXES) {
            if (fieldName.startsWith(prefix)) return true;
        }
        return false;
    }

    public static Optional<Double> parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        try {
            // Remove common currency symbols and comma separators
            String cleaned = raw.replace(",", "")
                    .replace("$", "")
                    .replace("€", "")
                    .replace("£", "")
                    .replace("₹", "")
                    .trim();

            Matcher matcher = AMOUNT_PATTERN.matcher(cleaned);
            if (matcher.find()) {
                return Optional.of(Double.parseDouble(matcher.group(1)));
            }
        } catch (Exception ignored) {}
        return Optional.empty();
    }

    public static Optional<LocalDate> parseDate(String raw) {
        if (raw == null || raw.isBlank()) return Optional.empty();
        String trimmed = raw.trim();

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE, // yyyy-MM-dd
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d MMM yyyy"),
                DateTimeFormatter.ofPattern("dd MMM yyyy"),
                DateTimeFormatter.ofPattern("MMMM d, yyyy"),
                DateTimeFormatter.ofPattern("MMMM dd, yyyy")
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                return Optional.of(LocalDate.parse(trimmed, dtf));
            } catch (Exception ignored) {}
        }
        return Optional.empty();
    }

    private record InvoiceDataPoint(
            Integer documentId,
            double amount,
            LocalDate date,
            String invoiceNumber,
            String rawDate
    ) {}
}
