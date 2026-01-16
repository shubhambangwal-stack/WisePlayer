package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;
import com.iptv.wiseplayer.service.SubscriptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for subscription management.
 */
@RestController
@RequestMapping("/api/subscription")
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
    @PostMapping("/activate")
    public ResponseEntity<SubscriptionResponse> activateSubscription(
            @RequestBody SubscriptionActivationRequest request) {
        SubscriptionResponse response = subscriptionService.activateSubscription(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get subscription status for a device.
     */
    @GetMapping("/status")
    public ResponseEntity<SubscriptionResponse> getSubscriptionStatus(@RequestParam String deviceId) {
        // Note: Accepting deviceId as query parameter for easier GET access
        SubscriptionResponse response = subscriptionService.getSubscriptionStatus(deviceId);
        return ResponseEntity.ok(response);
    }
}
