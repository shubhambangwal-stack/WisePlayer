package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.ResellerPricingTier;
import com.iptv.wiseplayer.dto.request.ResellerPricingTierRequest;
import com.iptv.wiseplayer.dto.response.ResellerPricingTierResponse;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.ResellerPricingTierRepository;
import com.iptv.wiseplayer.service.ResellerPricingTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResellerPricingTierServiceImpl implements ResellerPricingTierService {

    private final ResellerPricingTierRepository repository;

    @Override
    public List<ResellerPricingTierResponse> getAllTiers() {
        return repository.findAllByOrderByMinQuantityAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ResellerPricingTierResponse getTierById(UUID id) {
        return repository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing tier not found with id: " + id));
    }

    @Override
    @Transactional
    public ResellerPricingTierResponse createTier(ResellerPricingTierRequest request) {
        validateRequest(request);
        ResellerPricingTier tier = new ResellerPricingTier();
        updateEntityFromRequest(tier, request);
        try {
            return mapToResponse(repository.save(tier));
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Pricing tier with this name might already exist.");
        }
    }

    @Override
    @Transactional
    public ResellerPricingTierResponse updateTier(UUID id, ResellerPricingTierRequest request) {
        validateRequest(request);
        ResellerPricingTier tier = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pricing tier not found with id: " + id));
        updateEntityFromRequest(tier, request);
        try {
            return mapToResponse(repository.save(tier));
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Pricing tier with this name might already exist.");
        }
    }

    @Override
    @Transactional
    public void deleteTier(UUID id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Pricing tier not found with id: " + id);
        }
        repository.deleteById(id);
    }

    private void validateRequest(ResellerPricingTierRequest request) {
        if (request.getMaxQuantity() != null && request.getMinQuantity() > request.getMaxQuantity()) {
            throw new BadRequestException("Minimum quantity cannot be greater than maximum quantity.");
        }
    }

    private void updateEntityFromRequest(ResellerPricingTier tier, ResellerPricingTierRequest request) {
        tier.setName(request.getName());
        tier.setMinQuantity(request.getMinQuantity());
        tier.setMaxQuantity(request.getMaxQuantity());
        tier.setUnitPrice(request.getUnitPrice());
    }

    private ResellerPricingTierResponse mapToResponse(ResellerPricingTier tier) {
        ResellerPricingTierResponse response = new ResellerPricingTierResponse();
        response.setId(tier.getId());
        response.setName(tier.getName());
        response.setMinQuantity(tier.getMinQuantity());
        response.setMaxQuantity(tier.getMaxQuantity());
        response.setUnitPrice(tier.getUnitPrice());
        response.setCreatedAt(tier.getCreatedAt());
        response.setUpdatedAt(tier.getUpdatedAt());
        return response;
    }
}
