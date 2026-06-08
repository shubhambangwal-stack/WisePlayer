package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.ActivationRequest;
import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.ResellerActivationRequestDto;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.request.CreditPurchaseRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.dto.response.CreditTransactionResponse;
import com.iptv.wiseplayer.dto.response.ResellerDashboardResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.ResellerService;
import com.iptv.wiseplayer.service.CreditService;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.domain.entity.Admin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/sub-reseller")
@Tag(name = "Sub-Reseller API", description = "Endpoints for Sub-Reseller Management")
public class SubResellerController {

    private final ResellerService resellerService;
    private final CreditService creditService;
    private final PaymentService paymentService;
    private final AdminRepository adminRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;

    public SubResellerController(ResellerService resellerService,
                                 CreditService creditService,
                                 PaymentService paymentService,
                                 AdminRepository adminRepository,
                                 com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository) {
        this.resellerService = resellerService;
        this.creditService = creditService;
        this.paymentService = paymentService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    @GetMapping("/credits/balance")
    @Operation(summary = "Get Credit Balance", description = "Get the current credit balance for the logged-in sub-reseller")
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(creditService.getBalance(getCurrentSubResellerId()));
    }

    @GetMapping("/credits/transactions")
    @Operation(summary = "Get Transaction History", description = "Get the credit transaction history for the logged-in sub-reseller")
    public ResponseEntity<Page<CreditTransactionResponse>> getTransactionHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount,
            Pageable pageable) {
        return ResponseEntity.ok(creditService.getTransactionHistory(
                getCurrentSubResellerId(), search, type, fromDate, toDate, minAmount, maxAmount, pageable));
    }

    @PostMapping("/credits/purchase")
    @Operation(summary = "Purchase Credits", description = "Initiate a PayPal checkout session for purchasing credits")
    public ResponseEntity<CheckoutResponse> purchaseCredits(@Valid @RequestBody CreditPurchaseRequest request) {
        return ResponseEntity
                .ok(paymentService.createCreditCheckoutSession(getCurrentSubResellerId(), request.getCreditAmount()));
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Sub-Reseller Dashboard", description = "Get overview metrics for the sub-reseller")
    public ResponseEntity<ResellerDashboardResponse> getDashboard() {
        return ResponseEntity.ok(resellerService.getDashboardOverview(getCurrentSubResellerId()));
    }

    @PostMapping("/user")
    @Operation(summary = "Create End User", description = "Register an end user device under this sub-reseller")
    public ResponseEntity<java.util.Map<String, Object>> createEndUser(
            @Valid @RequestBody DeviceRegistrationRequest request) {
        return ResponseEntity.ok(resellerService.createEndUser(getCurrentSubResellerId(), request));
    }

    @GetMapping("/users")
    @Operation(summary = "Get Users", description = "Get devices managed by this sub-reseller with filters for status, plan, registered and expiry date range")
    public ResponseEntity<org.springframework.data.domain.Page<Device>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) com.iptv.wiseplayer.domain.enums.DeviceStatus status,
            @RequestParam(required = false) com.iptv.wiseplayer.domain.enums.SubscriptionType subscription,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate registeredFrom,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate registeredTo,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate expiresFrom,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate expiresTo,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getResellerUsers(
                getCurrentSubResellerId(), search, status, subscription,
                registeredFrom, registeredTo, expiresFrom, expiresTo, pageable));
    }

    @PutMapping("/users/{deviceId}/disable")
    @Operation(summary = "Toggle User Status", description = "Toggle a specific device/user between active and inactive")
    public ResponseEntity<Void> disableUser(@PathVariable UUID deviceId) {
        resellerService.disableUser(getCurrentSubResellerId(), deviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/activation-request")
    @Operation(summary = "Submit Activation Request", description = "Submit a request to activate a user/device subscription")
    public ResponseEntity<ActivationRequest> submitRequest(@Valid @RequestBody ResellerActivationRequestDto request) {
        return ResponseEntity.ok(resellerService.submitActivationRequest(getCurrentSubResellerId(), request));
    }

    @GetMapping("/activation-request")
    @Operation(summary = "Get Activation Requests", description = "Get a list of all activation requests submitted by this sub-reseller")
    public ResponseEntity<org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.ActivationRequestResponse>> getRequests(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate fromDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate toDate,
            @RequestParam(required = false) java.math.BigDecimal minCredits,
            @RequestParam(required = false) java.math.BigDecimal maxCredits,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(resellerService.getResellerRequests(
                getCurrentSubResellerId(), search, status, planName, fromDate, toDate, minCredits, maxCredits, pageable));
    }

    private UUID getCurrentSubResellerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(username)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(username)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Reseller not found for: " + username));
    }
}