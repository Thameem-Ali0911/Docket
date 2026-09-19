package com.docket.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for dispatching simulated Stripe webhook events in test mode.
 */
public record SimulateWebhookRequestDto(
        @NotBlank(message = "eventType is required")
        String eventType,
        String planTier
) {}
