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
}
