package com.docket.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docket.dto.PlanTier;
import com.docket.dto.SimulateWebhookRequestDto;
import com.docket.dto.SimulatedInvoiceDto;
import com.docket.dto.SubscriptionDetailsDto;
import com.docket.entity.SimulatedInvoice;
import com.docket.entity.Workspace;
import com.docket.exception.ApiException;
import com.docket.repository.DocumentRepository;
import com.docket.repository.SimulatedInvoiceRepository;
import com.docket.repository.WorkspaceRepository;

/**
 * Service managing billing simulation, subscription tiers, Stripe test-mode identifiers,
 * monthly document quota checks, and simulated Stripe webhook events.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final WorkspaceRepository workspaceRepository;
    private final DocumentRepository documentRepository;
    private final SimulatedInvoiceRepository simulatedInvoiceRepository;
    private final LlmBudgetService llmBudgetService;

    public BillingService(WorkspaceRepository workspaceRepository,
                          DocumentRepository documentRepository,
                          SimulatedInvoiceRepository simulatedInvoiceRepository,
                          LlmBudgetService llmBudgetService) {
        this.workspaceRepository = workspaceRepository;
        this.documentRepository = documentRepository;
        this.simulatedInvoiceRepository = simulatedInvoiceRepository;
        this.llmBudgetService = llmBudgetService;
    }

    /**
     * Retrieves the current workspace subscription details, usage metrics, and limits.
     *
     * @param workspaceId the workspace ID
     * @return SubscriptionDetailsDto
     */
    @Transactional(readOnly = true)
    public SubscriptionDetailsDto getSubscriptionDetails(Integer workspaceId) {
        Workspace workspace = findWorkspace(workspaceId);
        PlanTier tier = PlanTier.fromString(workspace.getPlanTier());

        OffsetDateTime periodStart = workspace.getBillingPeriodStart() != null
                ? workspace.getBillingPeriodStart()
                : OffsetDateTime.now().minusDays(30);

        long documentsUsed = documentRepository.countByWorkspaceIdAndUploadedAtGreaterThanEqual(workspaceId, periodStart);
        int dailyUsage = llmBudgetService.getTodayUsage(workspaceId);

        return new SubscriptionDetailsDto(
                tier.name(),
                tier.getDisplayName(),
                workspace.getSubscriptionStatus(),
                workspace.getStripeCustomerId(),
                workspace.getStripeSubscriptionId(),
                workspace.getBillingPeriodStart(),
                workspace.getBillingPeriodEnd(),
                documentsUsed,
                tier.getMonthlyDocLimit(),
                dailyUsage,
                workspace.getDailyLlmBudget(),
                tier.getPriceCents()
        );
    }

    /**
     * Retrieves simulated invoices for the workspace ordered by creation date descending.
     *
     * @param workspaceId the workspace ID
     * @return list of SimulatedInvoiceDto
     */
    @Transactional(readOnly = true)
    public List<SimulatedInvoiceDto> getInvoices(Integer workspaceId) {
        findWorkspace(workspaceId); // Workspace existence check
        return simulatedInvoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .map(inv -> new SimulatedInvoiceDto(
                        inv.getId(),
                        inv.getInvoiceNumber(),
                        inv.getAmountCents(),
                        inv.getCurrency(),
                        inv.getStatus(),
                        inv.getDescription(),
                        inv.getPeriodStart(),
                        inv.getPeriodEnd(),
                        inv.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Changes or upgrades the workspace subscription plan.
     * Generates simulated Stripe test mode IDs if not already present, adjusts the daily
     * LLM invocation budget, and generates a simulated invoice record for paid tiers.
     *
     * @param workspaceId the workspace ID
     * @param newTier     the target PlanTier
     * @return updated SubscriptionDetailsDto
     */
    @Transactional
    public SubscriptionDetailsDto upgradePlan(Integer workspaceId, PlanTier newTier) {
        Workspace workspace = findWorkspace(workspaceId);

        if (workspace.getStripeCustomerId() == null || workspace.getStripeCustomerId().isBlank()) {
            workspace.setStripeCustomerId("cus_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        }
        workspace.setStripeSubscriptionId("sub_sim_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14));
        workspace.setPlanTier(newTier.name());
        workspace.setSubscriptionStatus("ACTIVE");
        workspace.setDailyLlmBudget(newTier.getDailyLlmBudget());

        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusDays(30);
        workspace.setBillingPeriodStart(start);
        workspace.setBillingPeriodEnd(end);

        workspace = workspaceRepository.save(workspace);

        if (newTier.getPriceCents() > 0) {
            String invNum = "INV-SIM-" + (System.currentTimeMillis() % 10000000L);
            SimulatedInvoice invoice = new SimulatedInvoice(
                    workspace,
                    invNum,
                    newTier.getPriceCents(),
                    "USD",
                    "PAID",
                    "Docket " + newTier.getDisplayName() + " Plan (Monthly)",
                    start,
                    end
            );
            simulatedInvoiceRepository.save(invoice);
            log.info("Generated simulated invoice {} for workspace {} (tier={})", invNum, workspaceId, newTier);
        }

        return getSubscriptionDetails(workspaceId);
    }

    /**
     * Handles simulated Stripe webhook events for developer testing and demonstration.
     * Supported events:
     * - invoice.payment_succeeded
     * - invoice.payment_failed
     * - customer.subscription.updated
     * - customer.subscription.deleted
     *
     * @param workspaceId the workspace ID
     * @param request     the webhook simulation payload
     * @return updated SubscriptionDetailsDto
     */
    @Transactional
    public SubscriptionDetailsDto handleSimulatedWebhook(Integer workspaceId, SimulateWebhookRequestDto request) {
        Workspace workspace = findWorkspace(workspaceId);
        String event = request.eventType() != null ? request.eventType().trim() : "";
        PlanTier currentTier = PlanTier.fromString(workspace.getPlanTier());

        switch (event) {
            case "invoice.payment_succeeded" -> {
                workspace.setSubscriptionStatus("ACTIVE");
                OffsetDateTime newEnd = (workspace.getBillingPeriodEnd() != null ? workspace.getBillingPeriodEnd() : OffsetDateTime.now()).plusDays(30);
                workspace.setBillingPeriodEnd(newEnd);
                if (currentTier.getPriceCents() > 0) {
                    String invNum = "INV-SIM-" + (System.currentTimeMillis() % 10000000L);
                    SimulatedInvoice invoice = new SimulatedInvoice(
                            workspace,
                            invNum,
                            currentTier.getPriceCents(),
                            "USD",
                            "PAID",
                            "Simulated Webhook Payment: " + currentTier.getDisplayName(),
                            workspace.getBillingPeriodStart(),
                            newEnd
                    );
                    simulatedInvoiceRepository.save(invoice);
                }
                log.info("Simulated invoice.payment_succeeded handled for workspace {}", workspaceId);
            }
            case "invoice.payment_failed" -> {
                workspace.setSubscriptionStatus("PAST_DUE");
                String invNum = "INV-SIM-" + (System.currentTimeMillis() % 10000000L);
                SimulatedInvoice invoice = new SimulatedInvoice(
                        workspace,
                        invNum,
                        currentTier.getPriceCents(),
                        "USD",
                        "FAILED",
                        "Simulated Payment Failure: " + currentTier.getDisplayName(),
                        workspace.getBillingPeriodStart(),
                        workspace.getBillingPeriodEnd()
                );
                simulatedInvoiceRepository.save(invoice);
                log.warn("Simulated invoice.payment_failed handled for workspace {} — status set to PAST_DUE", workspaceId);
            }
            case "customer.subscription.deleted" -> {
                workspace.setSubscriptionStatus("CANCELED");
                workspace.setPlanTier(PlanTier.FREE.name());
                workspace.setDailyLlmBudget(PlanTier.FREE.getDailyLlmBudget());
                log.info("Simulated customer.subscription.deleted handled for workspace {} — downgraded to FREE", workspaceId);
            }
            case "customer.subscription.updated" -> {
                PlanTier targetTier = request.planTier() != null && !request.planTier().isBlank()
                        ? PlanTier.fromString(request.planTier())
                        : currentTier;
                workspace.setPlanTier(targetTier.name());
                workspace.setDailyLlmBudget(targetTier.getDailyLlmBudget());
                workspace.setSubscriptionStatus("ACTIVE");
                log.info("Simulated customer.subscription.updated handled for workspace {} — updated to {}", workspaceId, targetTier);
            }
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "UNSUPPORTED_EVENT_TYPE",
                    "Unsupported simulated webhook event: " + event + ". Supported events: invoice.payment_succeeded, invoice.payment_failed, customer.subscription.updated, customer.subscription.deleted"
            );
        }

        workspaceRepository.save(workspace);
        return getSubscriptionDetails(workspaceId);
    }

    /**
     * Checks if the workspace has remaining document capacity in the current billing cycle.
     * Throws an ApiException if the monthly quota is exhausted or if the subscription is PAST_DUE.
     *
     * @param workspaceId         the workspace ID
     * @param additionalDocsCount number of new documents attempted to upload
     */
    @Transactional(readOnly = true)
    public void checkDocumentQuota(Integer workspaceId, int additionalDocsCount) {
        Workspace workspace = findWorkspace(workspaceId);

        if ("PAST_DUE".equalsIgnoreCase(workspace.getSubscriptionStatus())) {
            throw new ApiException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "SUBSCRIPTION_PAST_DUE",
                    "Your workspace subscription is past due. Please resolve billing in the Billing portal before processing additional documents."
            );
        }

        PlanTier tier = PlanTier.fromString(workspace.getPlanTier());
        OffsetDateTime periodStart = workspace.getBillingPeriodStart() != null
                ? workspace.getBillingPeriodStart()
                : OffsetDateTime.now().minusDays(30);

        long currentCount = documentRepository.countByWorkspaceIdAndUploadedAtGreaterThanEqual(workspaceId, periodStart);

        if (currentCount + additionalDocsCount > tier.getMonthlyDocLimit()) {
            log.warn("Document quota exceeded for workspace {} — current: {}, requested: {}, limit: {} (tier: {})",
                    workspaceId, currentCount, additionalDocsCount, tier.getMonthlyDocLimit(), tier);
            throw new ApiException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "PLAN_QUOTA_EXCEEDED",
                    "Monthly document limit reached for your " + tier.getDisplayName() + " plan (" +
                    currentCount + "/" + tier.getMonthlyDocLimit() + " documents used). Upgrade your subscription in Billing to process more documents."
            );
        }
    }

    private Workspace findWorkspace(Integer workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WORKSPACE_NOT_FOUND", "Workspace not found"));
    }
}
