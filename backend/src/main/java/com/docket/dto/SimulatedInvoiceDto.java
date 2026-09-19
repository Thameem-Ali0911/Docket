package com.docket.dto;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object representing a simulated invoice for display in the billing history.
 */
public record SimulatedInvoiceDto(
        Long id,
        String invoiceNumber,
        int amountCents,
        String currency,
        String status,
        String description,
        OffsetDateTime periodStart,
        OffsetDateTime periodEnd,
        OffsetDateTime createdAt
) {}
