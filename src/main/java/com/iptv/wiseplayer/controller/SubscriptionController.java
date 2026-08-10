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
    private final com.iptv.wiseplayer.security.DeviceContext deviceContext;
    private final com.iptv.wiseplayer.service.DeviceService deviceService;

    public SubscriptionController(SubscriptionService subscriptionService, 
                                com.iptv.wiseplayer.security.DeviceContext deviceContext,
                                com.iptv.wiseplayer.service.DeviceService deviceService) {
        this.subscriptionService = subscriptionService;
        this.deviceContext = deviceContext;
        this.deviceService = deviceService;
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
        // Validate IDOR for devices
        java.util.UUID resolvedId = deviceService.resolveDeviceId(request.getDeviceId());
        if (!deviceContext.getCurrentDeviceId().equals(resolvedId)) {
             throw new com.iptv.wiseplayer.exception.AccessDeniedException("You can only activate subscriptions for your own device.");
        }
        SubscriptionResponse response = subscriptionService.activateSubscription(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get subscription status for a device.
     */
    @Operation(summary = "Get Subscription Status", description = "Retrieves the current subscription status for a device.")
    @GetMapping("/status")
    public ResponseEntity<SubscriptionResponse> getSubscriptionStatus(@RequestParam String deviceId) {
        // Validate IDOR
        java.util.UUID resolvedId = deviceService.resolveDeviceId(deviceId);
        if (!deviceContext.getCurrentDeviceId().equals(resolvedId)) {
            throw new com.iptv.wiseplayer.exception.AccessDeniedException("You can only view subscription status for your own device.");
        }
        // Can safely use the resolved string version or original
        SubscriptionResponse response = subscriptionService.getSubscriptionStatus(resolvedId.toString());
        return ResponseEntity.ok(response);
    }
}
