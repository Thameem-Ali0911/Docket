package com.docket.dto.document;

import java.util.List;

/**
 * Aggregated trends and comparative metrics for a single vendor across multiple documents.
 */
public record VendorTrendDto(
        String vendorName,
        long documentCount,
        double totalSpend,
        double averageAmount,
        double minAmount,
        double maxAmount,
        Double latestAmount,
        String latestInvoiceDate,
        Double trendPercentage,
        int anomalyCount,
        List<Double> amountHistory,
        List<Integer> documentIds
) {}
