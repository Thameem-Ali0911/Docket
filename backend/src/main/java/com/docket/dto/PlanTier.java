package com.docket.dto;

/**
 * Subscription plan tiers supported by Docket in Stripe test mode.
 */
public enum PlanTier {
    FREE("Free Tier", 10, 50, 0),
    PRO("Professional", 100, 250, 4900),
    ENTERPRISE("Enterprise", 1000, 500, 19900);

    private final String displayName;
    private final int monthlyDocLimit;
    private final int dailyLlmBudget;
    private final int priceCents;

    PlanTier(String displayName, int monthlyDocLimit, int dailyLlmBudget, int priceCents) {
        this.displayName = displayName;
        this.monthlyDocLimit = monthlyDocLimit;
        this.dailyLlmBudget = dailyLlmBudget;
        this.priceCents = priceCents;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMonthlyDocLimit() {
        return monthlyDocLimit;
    }

    public int getDailyLlmBudget() {
        return dailyLlmBudget;
    }

    public int getPriceCents() {
        return priceCents;
    }

    public static PlanTier fromString(String val) {
        if (val == null) return FREE;
        try {
            return PlanTier.valueOf(val.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE;
        }
    }
}
