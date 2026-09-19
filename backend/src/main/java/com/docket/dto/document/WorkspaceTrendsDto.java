package com.docket.dto.document;

import java.util.List;

/**
 * Top-level workspace comparative intelligence summary.
 */
public record WorkspaceTrendsDto(
        int totalVendors,
        long totalInvoicesAnalyzed,
        int totalComparativeAnomalies,
        int duplicateInvoicesCount,
        int priceSurgesCount,
        List<VendorTrendDto> vendors
) {}
