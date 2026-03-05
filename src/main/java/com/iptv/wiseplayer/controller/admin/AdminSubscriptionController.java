package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.AdminSubscriptionResponse;
import com.iptv.wiseplayer.service.AdminSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/subscriptions")
@Tag(name = "Admin Subscription Management", description = "Endpoints for managing device subscriptions")
public class AdminSubscriptionController {

    private final AdminSubscriptionService adminSubscriptionService;

    public AdminSubscriptionController(AdminSubscriptionService adminSubscriptionService) {
        this.adminSubscriptionService = adminSubscriptionService;
    }

    @Operation(summary = "List All Subscriptions", description = "Retrieves a paginated list of all subscriptions.")
    @GetMapping
    public ResponseEntity<Page<AdminSubscriptionResponse>> getAllSubscriptions(Pageable pageable) {
        return ResponseEntity.ok(adminSubscriptionService.getAllSubscriptions(pageable));
    }

    @Operation(summary = "Manual Activation", description = "Manually activates a subscription for a device.")
    @PostMapping("/manual")
    public ResponseEntity<?> manualActivate(@RequestBody SubscriptionActivationRequest request) {
        adminSubscriptionService.manualActivate(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscription manually activated"));
    }

    @Operation(summary = "Revoke Subscription", description = "Revokes an active subscription.")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> revokeSubscription(@PathVariable String id) {
        adminSubscriptionService.revokeSubscription(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "Subscription revoked"));
    }
}
