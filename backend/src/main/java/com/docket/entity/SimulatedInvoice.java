package com.docket.entity;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a simulated billing invoice generated in Stripe test mode.
 */
@Entity
@Table(name = "simulated_invoices")
public class SimulatedInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Workspace workspace;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @Column(name = "amount_cents", nullable = false)
    private int amountCents;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(nullable = false, length = 30)
    private String status = "PAID";

    @Column(length = 255)
    private String description;

    @Column(name = "period_start")
    private OffsetDateTime periodStart;

    @Column(name = "period_end")
    private OffsetDateTime periodEnd;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SimulatedInvoice() {
    }

    public SimulatedInvoice(Workspace workspace, String invoiceNumber, int amountCents, String currency,
                            String status, String description, OffsetDateTime periodStart, OffsetDateTime periodEnd) {
        this.workspace = workspace;
        this.invoiceNumber = invoiceNumber;
        this.amountCents = amountCents;
        this.currency = currency;
        this.status = status;
        this.description = description;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
    }

    public Long getId() {
        return id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public int getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public OffsetDateTime getPeriodStart() {
        return periodStart;
    }

    public OffsetDateTime getPeriodEnd() {
        return periodEnd;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
