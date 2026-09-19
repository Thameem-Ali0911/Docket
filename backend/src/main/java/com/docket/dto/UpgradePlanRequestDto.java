package com.docket.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for changing the workspace's subscription tier.
 */
public record UpgradePlanRequestDto(
        @NotBlank(message = "planTier is required")
        String planTier
) {}
