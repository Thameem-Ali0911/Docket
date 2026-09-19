package com.docket.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Represents a tenant workspace. Each signup creates one workspace,
 * and all documents/templates within that workspace are scoped by its ID.
 */
@Entity
@Table(name = "workspaces")
public class Workspace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** Daily LLM invocation budget per workspace. Defaults to 100 calls/day. */
    @Column(name = "daily_llm_budget", nullable = false)
    private int dailyLlmBudget = 100;

    @Column(name = "plan_tier", nullable = false, length = 30)
    private String planTier = "FREE";

    @Column(name = "subscription_status", nullable = false, length = 30)
    private String subscriptionStatus = "ACTIVE";

    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 100)
    private String stripeSubscriptionId;

    @Column(name = "billing_period_start")
    private OffsetDateTime billingPeriodStart = OffsetDateTime.now();

    @Column(name = "billing_period_end")
    private OffsetDateTime billingPeriodEnd = OffsetDateTime.now().plusDays(30);

    protected Workspace() {
        // JPA requires a no-arg constructor
    }

    public Workspace(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public int getDailyLlmBudget() {
        return dailyLlmBudget;
    }

    public void setDailyLlmBudget(int dailyLlmBudget) {
        this.dailyLlmBudget = dailyLlmBudget;
    }

    public String getPlanTier() {
        return planTier;
    }

    public void setPlanTier(String planTier) {
        this.planTier = planTier;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public OffsetDateTime getBillingPeriodStart() {
        return billingPeriodStart;
    }

    public void setBillingPeriodStart(OffsetDateTime billingPeriodStart) {
        this.billingPeriodStart = billingPeriodStart;
    }

    public OffsetDateTime getBillingPeriodEnd() {
        return billingPeriodEnd;
    }

    public void setBillingPeriodEnd(OffsetDateTime billingPeriodEnd) {
        this.billingPeriodEnd = billingPeriodEnd;
    }
}
