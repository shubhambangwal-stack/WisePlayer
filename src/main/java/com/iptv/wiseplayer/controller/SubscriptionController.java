package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;
import com.iptv.wiseplayer.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for subscription management.
 */
@RestController
@RequestMapping("/api/subscription")
@Tag(name = "Subscription Management", description = "Endpoints for managing user subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Activate a new subscription.
     * Usually called internally or by admin, but exposed for now as per
     * requirements.
     */
    @Operation(summary = "Activate Subscription", description = "Activates a new subscription plan for a device.")
    @PostMapping("/activate")
    public ResponseEntity<SubscriptionResponse> activateSubscription(
            @RequestBody SubscriptionActivationRequest request) {
        SubscriptionResponse response = subscriptionService.activateSubscription(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get subscription status for a device.
     */
    @Operation(summary = "Get Subscription Status", description = "Retrieves the current subscription status for a device.")
    @GetMapping("/status")
    public ResponseEntity<SubscriptionResponse> getSubscriptionStatus(@RequestParam String deviceId) {
        // Note: Accepting deviceId as query parameter for easier GET access
        SubscriptionResponse response = subscriptionService.getSubscriptionStatus(deviceId);
        return ResponseEntity.ok(response);
    }
}
