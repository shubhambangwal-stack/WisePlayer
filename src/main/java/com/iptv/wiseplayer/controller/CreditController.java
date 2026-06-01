package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.CreditPurchaseRequest;
import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.service.CreditService;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/reseller/credits")
@Tag(name = "Reseller Credit API", description = "Endpoints for Reseller Credit Management")
public class CreditController {

    private final CreditService creditService;
    private final PaymentService paymentService;
    private final AdminRepository adminRepository;

    public CreditController(CreditService creditService,
                            PaymentService paymentService,
                            AdminRepository adminRepository) {
        this.creditService = creditService;
        this.paymentService = paymentService;
        this.adminRepository = adminRepository;
    }

    @GetMapping("/balance")
    @Operation(summary = "Get Credit Balance", description = "Get the current credit balance for the logged-in reseller")
    public ResponseEntity<BigDecimal> getBalance() {
        return ResponseEntity.ok(creditService.getBalance(getCurrentResellerId()));
    }

    @GetMapping("/pricing")
    @Operation(summary = "Get Credit Pricing", description = "Get the unit price for a specific quantity of credits")
    public ResponseEntity<BigDecimal> getPricing(@RequestParam int quantity) {
        return ResponseEntity.ok(creditService.calculateUnitPrice(quantity));
    }

    @GetMapping("/transactions")
    @Operation(summary = "Get Transaction History", description = "Get the credit transaction history for the logged-in reseller")
    public ResponseEntity<org.springframework.data.domain.Page<com.iptv.wiseplayer.dto.response.CreditTransactionResponse>> getTransactionHistory(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(creditService.getTransactionHistory(getCurrentResellerId(), search, type, pageable));
    }

    @PostMapping("/purchase")
    @Operation(summary = "Purchase Credits", description = "Initiate a PayPal checkout session for purchasing credits")
    public ResponseEntity<CheckoutResponse> purchaseCredits(@Valid @RequestBody CreditPurchaseRequest request) {
        return ResponseEntity
                .ok(paymentService.createCreditCheckoutSession(getCurrentResellerId(), request.getCreditAmount()));
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer Credits", description = "Transfer credits from current reseller to a sub-reseller")
    public ResponseEntity<Void> transferCredits(
            @Valid @RequestBody com.iptv.wiseplayer.dto.request.CreditTransferRequest request) {
        creditService.transferCredits(getCurrentResellerId(), request.getSubResellerId(), request.getAmount());
        return ResponseEntity.ok().build();
    }

    private UUID getCurrentResellerId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(identifier)
                .map(Admin::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found for: " + identifier));
    }
}