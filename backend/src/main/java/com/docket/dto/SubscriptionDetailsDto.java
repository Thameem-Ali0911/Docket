package com.docket.dto;

import java.time.OffsetDateTime;

/**
 * Data Transfer Object representing the current workspace subscription, plan tier,
 * usage metrics, and Stripe test-mode identifiers.
 */
public record SubscriptionDetailsDto(
        String planTier,
        String planDisplayName,
        String subscriptionStatus,
        String stripeCustomerId,
        String stripeSubscriptionId,
        OffsetDateTime billingPeriodStart,
        OffsetDateTime billingPeriodEnd,
        long documentsUsedThisPeriod,
        int monthlyDocumentLimit,
        int dailyLlmUsage,
        int dailyLlmBudget,
        int priceCents
) {}
