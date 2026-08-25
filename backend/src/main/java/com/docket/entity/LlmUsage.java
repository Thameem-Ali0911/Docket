package com.docket.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Tracks per-workspace daily LLM invocation counts for denial-of-wallet protection.
 * One row per (workspace, date) pair; call_count is incremented atomically.
 */
@Entity
@Table(name = "llm_usage")
public class LlmUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "call_count", nullable = false)
    private int callCount;

    protected LlmUsage() {}

    public LlmUsage(Workspace workspace, LocalDate usageDate) {
        this.workspace = workspace;
        this.usageDate = usageDate;
        this.callCount = 0;
    }

    public Long getId() { return id; }
    public Workspace getWorkspace() { return workspace; }
    public LocalDate getUsageDate() { return usageDate; }
    public int getCallCount() { return callCount; }
    public void setCallCount(int callCount) { this.callCount = callCount; }
    public void increment() { this.callCount++; }
}
