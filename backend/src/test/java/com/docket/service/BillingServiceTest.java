package com.docket.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private SimulatedInvoiceRepository simulatedInvoiceRepository;
    @Mock private LlmBudgetService llmBudgetService;

    private BillingService billingService;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(
                workspaceRepository,
                documentRepository,
                simulatedInvoiceRepository,
                llmBudgetService
        );
        workspace = new Workspace("Acme Legal");
        workspace.setPlanTier("FREE");
        workspace.setSubscriptionStatus("ACTIVE");
        workspace.setDailyLlmBudget(50);
        workspace.setBillingPeriodStart(OffsetDateTime.now().minusDays(10));
        workspace.setBillingPeriodEnd(OffsetDateTime.now().plusDays(20));
    }

    @Test
    @DisplayName("getSubscriptionDetails aggregates tier, status, documents used, and LLM budget")
    void testGetSubscriptionDetails() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(documentRepository.countByWorkspaceIdAndUploadedAtGreaterThanEqual(eq(1), any(OffsetDateTime.class)))
                .thenReturn(7L);
        when(llmBudgetService.getTodayUsage(1)).thenReturn(14);

        SubscriptionDetailsDto details = billingService.getSubscriptionDetails(1);

        assertNotNull(details);
        assertEquals("FREE", details.planTier());
        assertEquals("Free Tier", details.planDisplayName());
        assertEquals("ACTIVE", details.subscriptionStatus());
        assertEquals(7L, details.documentsUsedThisPeriod());
        assertEquals(10, details.monthlyDocumentLimit());
        assertEquals(14, details.dailyLlmUsage());
        assertEquals(50, details.dailyLlmBudget());
        assertEquals(0, details.priceCents());
    }

    @Test
    @DisplayName("upgradePlan to PRO generates Stripe IDs, sets budget to 250, and creates paid invoice")
    void testUpgradePlanToPro() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.countByWorkspaceIdAndUploadedAtGreaterThanEqual(eq(1), any(OffsetDateTime.class)))
                .thenReturn(0L);

        SubscriptionDetailsDto details = billingService.upgradePlan(1, PlanTier.PRO);

        assertEquals("PRO", details.planTier());
        assertEquals("Professional", details.planDisplayName());
        assertEquals("ACTIVE", details.subscriptionStatus());
        assertNotNull(workspace.getStripeCustomerId());
        assertTrue(workspace.getStripeCustomerId().startsWith("cus_sim_"));
        assertNotNull(workspace.getStripeSubscriptionId());
        assertTrue(workspace.getStripeSubscriptionId().startsWith("sub_sim_"));
        assertEquals(250, workspace.getDailyLlmBudget());

        ArgumentCaptor<SimulatedInvoice> invoiceCaptor = ArgumentCaptor.forClass(SimulatedInvoice.class);
        verify(simulatedInvoiceRepository).save(invoiceCaptor.capture());
        SimulatedInvoice invoice = invoiceCaptor.getValue();
        assertEquals(4900, invoice.getAmountCents());
        assertEquals("PAID", invoice.getStatus());
        assertEquals("USD", invoice.getCurrency());
        assertTrue(invoice.getInvoiceNumber().startsWith("INV-SIM-"));
    }

    @Test
    @DisplayName("upgradePlan to FREE does not generate paid invoice")
    void testUpgradePlanToFree() {
        workspace.setPlanTier("PRO");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        SubscriptionDetailsDto details = billingService.upgradePlan(1, PlanTier.FREE);

        assertEquals("FREE", details.planTier());
        assertEquals(50, workspace.getDailyLlmBudget());
        verify(simulatedInvoiceRepository, never()).save(any(SimulatedInvoice.class));
    }

    @Test
    @DisplayName("checkDocumentQuota succeeds when under monthly limit")
    void testCheckDocumentQuotaSuccess() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(documentRepository.countByWorkspaceIdAndUploadedAtGreaterThanEqual(eq(1), any(OffsetDateTime.class)))
                .thenReturn(5L);

        assertDoesNotThrow(() -> billingService.checkDocumentQuota(1, 1));
    }

    @Test
    @DisplayName("checkDocumentQuota throws 402 PLAN_QUOTA_EXCEEDED when monthly limit is reached")
    void testCheckDocumentQuotaExceeded() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(documentRepository.countByWorkspaceIdAndUploadedAtGreaterThanEqual(eq(1), any(OffsetDateTime.class)))
                .thenReturn(10L); // Limit is 10 for FREE

        ApiException ex = assertThrows(ApiException.class, () -> billingService.checkDocumentQuota(1, 1));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        assertEquals("PLAN_QUOTA_EXCEEDED", ex.getCode());
    }

    @Test
    @DisplayName("checkDocumentQuota throws 402 SUBSCRIPTION_PAST_DUE when subscription is past due")
    void testCheckDocumentQuotaPastDue() {
        workspace.setSubscriptionStatus("PAST_DUE");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

        ApiException ex = assertThrows(ApiException.class, () -> billingService.checkDocumentQuota(1, 1));

        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        assertEquals("SUBSCRIPTION_PAST_DUE", ex.getCode());
    }

    @Test
    @DisplayName("handleSimulatedWebhook invoice.payment_failed sets status to PAST_DUE and generates FAILED invoice")
    void testSimulateWebhookPaymentFailed() {
        workspace.setPlanTier("PRO");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        SimulateWebhookRequestDto req = new SimulateWebhookRequestDto("invoice.payment_failed", null);
        SubscriptionDetailsDto res = billingService.handleSimulatedWebhook(1, req);

        assertEquals("PAST_DUE", res.subscriptionStatus());
        assertEquals("PAST_DUE", workspace.getSubscriptionStatus());

        ArgumentCaptor<SimulatedInvoice> captor = ArgumentCaptor.forClass(SimulatedInvoice.class);
        verify(simulatedInvoiceRepository).save(captor.capture());
        assertEquals("FAILED", captor.getValue().getStatus());
        assertEquals(4900, captor.getValue().getAmountCents());
    }

    @Test
    @DisplayName("handleSimulatedWebhook invoice.payment_succeeded reactivates subscription to ACTIVE")
    void testSimulateWebhookPaymentSucceeded() {
        workspace.setPlanTier("PRO");
        workspace.setSubscriptionStatus("PAST_DUE");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        SimulateWebhookRequestDto req = new SimulateWebhookRequestDto("invoice.payment_succeeded", null);
        SubscriptionDetailsDto res = billingService.handleSimulatedWebhook(1, req);

        assertEquals("ACTIVE", res.subscriptionStatus());
        assertEquals("ACTIVE", workspace.getSubscriptionStatus());
        verify(simulatedInvoiceRepository).save(any(SimulatedInvoice.class));
    }

    @Test
    @DisplayName("handleSimulatedWebhook customer.subscription.deleted cancels and downgrades to FREE")
    void testSimulateWebhookSubscriptionDeleted() {
        workspace.setPlanTier("PRO");
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        SimulateWebhookRequestDto req = new SimulateWebhookRequestDto("customer.subscription.deleted", null);
        SubscriptionDetailsDto res = billingService.handleSimulatedWebhook(1, req);

        assertEquals("CANCELED", res.subscriptionStatus());
        assertEquals("FREE", res.planTier());
        assertEquals(50, workspace.getDailyLlmBudget());
    }

    @Test
    @DisplayName("handleSimulatedWebhook customer.subscription.updated switches plan tier")
    void testSimulateWebhookSubscriptionUpdated() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(i -> i.getArgument(0));

        SimulateWebhookRequestDto req = new SimulateWebhookRequestDto("customer.subscription.updated", "ENTERPRISE");
        SubscriptionDetailsDto res = billingService.handleSimulatedWebhook(1, req);

        assertEquals("ENTERPRISE", res.planTier());
        assertEquals(500, workspace.getDailyLlmBudget());
        assertEquals("ACTIVE", res.subscriptionStatus());
    }

    @Test
    @DisplayName("handleSimulatedWebhook throws 400 on unsupported event")
    void testSimulateWebhookUnsupportedEvent() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

        SimulateWebhookRequestDto req = new SimulateWebhookRequestDto("unknown.event", null);
        ApiException ex = assertThrows(ApiException.class, () -> billingService.handleSimulatedWebhook(1, req));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("UNSUPPORTED_EVENT_TYPE", ex.getCode());
    }

    @Test
    @DisplayName("getInvoices returns simulated invoice list")
    void testGetInvoices() {
        when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
        SimulatedInvoice inv = new SimulatedInvoice(
                workspace, "INV-SIM-12345", 4900, "USD", "PAID",
                "Pro Plan", OffsetDateTime.now(), OffsetDateTime.now().plusDays(30)
        );
        when(simulatedInvoiceRepository.findByWorkspaceIdOrderByCreatedAtDesc(1)).thenReturn(List.of(inv));

        List<SimulatedInvoiceDto> invoices = billingService.getInvoices(1);

        assertEquals(1, invoices.size());
        assertEquals("INV-SIM-12345", invoices.get(0).invoiceNumber());
        assertEquals(4900, invoices.get(0).amountCents());
        assertEquals("PAID", invoices.get(0).status());
    }
}
