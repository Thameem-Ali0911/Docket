package com.docket.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.docket.dto.PlanTier;
import com.docket.dto.SimulateWebhookRequestDto;
import com.docket.dto.SimulatedInvoiceDto;
import com.docket.dto.SubscriptionDetailsDto;
import com.docket.dto.UpgradePlanRequestDto;
import com.docket.entity.User;
import com.docket.exception.ApiException;
import com.docket.repository.UserRepository;
import com.docket.service.BillingService;

import jakarta.validation.Valid;

/**
 * REST controller for billing simulation, subscription plan changes,
 * invoice history, and Stripe test-mode webhook simulation.
 */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService billingService;
    private final UserRepository userRepository;

    public BillingController(BillingService billingService, UserRepository userRepository) {
        this.billingService = billingService;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves the current workspace subscription, plan tier, usage metrics, and limits.
     *
     * @param authentication the authenticated user's details
     * @return SubscriptionDetailsDto
     */
    @GetMapping("/subscription")
    public ResponseEntity<SubscriptionDetailsDto> getSubscription(Authentication authentication) {
        Integer workspaceId = getWorkspaceId(authentication);
        return ResponseEntity.ok(billingService.getSubscriptionDetails(workspaceId));
    }

    /**
     * Retrieves simulated invoices and receipts for the workspace.
     *
     * @param authentication the authenticated user's details
     * @return list of SimulatedInvoiceDto
     */
    @GetMapping("/invoices")
    public ResponseEntity<List<SimulatedInvoiceDto>> getInvoices(Authentication authentication) {
        Integer workspaceId = getWorkspaceId(authentication);
        return ResponseEntity.ok(billingService.getInvoices(workspaceId));
    }

    /**
     * Upgrades or changes the workspace subscription tier in simulated Stripe test mode.
     *
     * @param request        payload containing target planTier
     * @param authentication the authenticated user's details
     * @return updated SubscriptionDetailsDto
     */
    @PostMapping("/upgrade")
    public ResponseEntity<SubscriptionDetailsDto> upgradePlan(
            @Valid @RequestBody UpgradePlanRequestDto request,
            Authentication authentication) {
        Integer workspaceId = getWorkspaceId(authentication);
        PlanTier tier = PlanTier.fromString(request.planTier());
        SubscriptionDetailsDto updated = billingService.upgradePlan(workspaceId, tier);
        return ResponseEntity.ok(updated);
    }

    /**
     * Simulates Stripe webhook events (e.g., payment succeeded, payment failed, subscription updated/deleted).
     *
     * @param request        payload containing eventType and optional planTier
     * @param authentication the authenticated user's details
     * @return updated SubscriptionDetailsDto reflecting event outcome
     */
    @PostMapping("/webhook/simulate")
    public ResponseEntity<SubscriptionDetailsDto> simulateWebhook(
            @Valid @RequestBody SimulateWebhookRequestDto request,
            Authentication authentication) {
        Integer workspaceId = getWorkspaceId(authentication);
        SubscriptionDetailsDto updated = billingService.handleSimulatedWebhook(workspaceId, request);
        return ResponseEntity.ok(updated);
    }

    private Integer getWorkspaceId(Authentication authentication) {
        Integer userId = (Integer) authentication.getPrincipal();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        return user.getWorkspace().getId();
    }
}
