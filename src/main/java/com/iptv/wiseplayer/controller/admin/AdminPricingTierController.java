package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.request.ResellerPricingTierRequest;
import com.iptv.wiseplayer.dto.response.ResellerPricingTierResponse;
import com.iptv.wiseplayer.service.ResellerPricingTierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/pricing-tiers")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Pricing Tiers API", description = "Endpoints for managing reseller pricing tiers by Super Admin")
public class AdminPricingTierController {

    private final ResellerPricingTierService pricingTierService;

    @GetMapping
    @Operation(summary = "Get all pricing tiers", description = "Retrieve all reseller pricing tiers")
    public ResponseEntity<List<ResellerPricingTierResponse>> getAllTiers() {
        return ResponseEntity.ok(pricingTierService.getAllTiers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pricing tier by ID", description = "Retrieve a specific pricing tier by its ID")
    public ResponseEntity<ResellerPricingTierResponse> getTierById(@PathVariable UUID id) {
        return ResponseEntity.ok(pricingTierService.getTierById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new pricing tier", description = "Create a new reseller pricing tier")
    public ResponseEntity<ResellerPricingTierResponse> createTier(@Valid @RequestBody ResellerPricingTierRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pricingTierService.createTier(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a pricing tier", description = "Update an existing reseller pricing tier")
    public ResponseEntity<ResellerPricingTierResponse> updateTier(
            @PathVariable UUID id, @Valid @RequestBody ResellerPricingTierRequest request) {
        return ResponseEntity.ok(pricingTierService.updateTier(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pricing tier", description = "Delete a specific reseller pricing tier")
    public ResponseEntity<Void> deleteTier(@PathVariable UUID id) {
        pricingTierService.deleteTier(id);
        return ResponseEntity.noContent().build();
    }
}
