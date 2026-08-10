package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.response.ResellerPricingTierResponse;
import com.iptv.wiseplayer.service.ResellerPricingTierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/pricing")
@RequiredArgsConstructor
@Tag(name = "Public Pricing API", description = "Endpoints for fetching public pricing data")
public class PublicPricingController {

    private final ResellerPricingTierService pricingTierService;

    @GetMapping("/reseller-tiers")
    @Operation(summary = "Get Reseller Pricing Tiers", description = "Retrieve all available reseller pricing tiers")
    public ResponseEntity<List<ResellerPricingTierResponse>> getResellerPricingTiers() {
        return ResponseEntity.ok(pricingTierService.getAllTiers());
    }
}
