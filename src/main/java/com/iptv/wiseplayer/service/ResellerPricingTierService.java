package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.ResellerPricingTierRequest;
import com.iptv.wiseplayer.dto.response.ResellerPricingTierResponse;

import java.util.List;
import java.util.UUID;

public interface ResellerPricingTierService {
    List<ResellerPricingTierResponse> getAllTiers();
    ResellerPricingTierResponse getTierById(UUID id);
    ResellerPricingTierResponse createTier(ResellerPricingTierRequest request);
    ResellerPricingTierResponse updateTier(UUID id, ResellerPricingTierRequest request);
    void deleteTier(UUID id);
}
