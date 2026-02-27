package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Subscription;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.AdminSubscriptionResponse;
import com.iptv.wiseplayer.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;

    public AdminSubscriptionService(SubscriptionRepository subscriptionRepository,
            SubscriptionService subscriptionService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
    }

    public Page<AdminSubscriptionResponse> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable).map(this::convertToResponse);
    }

    @Transactional
    public void manualActivate(SubscriptionActivationRequest request) {
        subscriptionService.activateSubscription(request);
    }

    @Transactional
    public void revokeSubscription(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
    }

    private AdminSubscriptionResponse convertToResponse(Subscription subscription) {
        AdminSubscriptionResponse response = new AdminSubscriptionResponse();
        response.setSubscriptionId(subscription.getId());
        response.setDeviceId(subscription.getDeviceId());
        response.setPlan(subscription.getPlan());
        response.setStatus(subscription.getStatus());
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        return response;
    }
}
