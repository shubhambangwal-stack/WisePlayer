package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Subscription;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;
import com.iptv.wiseplayer.repository.SubscriptionRepository;
import com.iptv.wiseplayer.service.DeviceService;
import com.iptv.wiseplayer.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of SubscriptionService.
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final DeviceService deviceService;
    private final com.iptv.wiseplayer.repository.DeviceRepository deviceRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository, DeviceService deviceService,
            com.iptv.wiseplayer.repository.DeviceRepository deviceRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.deviceService = deviceService;
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public SubscriptionResponse activateSubscription(SubscriptionActivationRequest request) {
        if (request.getDeviceId() == null || request.getPlan() == null) {
            throw new IllegalArgumentException("Device ID (Fingerprint) and Plan are required");
        }

        // 0. Resolve UUID from Fingerprint
        UUID resolvedDeviceId = deviceService.resolveDeviceId(request.getDeviceId());

        // 1. Check for existing active subscription
        Optional<Subscription> existingSub = subscriptionRepository.findByDeviceIdAndStatus(
                resolvedDeviceId, SubscriptionStatus.ACTIVE);

        if (existingSub.isPresent()) {
            throw new IllegalStateException("Device already has an active subscription");
        }

        // 2. Create new subscription
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(request.getPlan().getDays());

        Subscription subscription = new Subscription(
                resolvedDeviceId,
                request.getPlan(),
                startDate,
                endDate,
                SubscriptionStatus.ACTIVE);

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        // 3. Update Device status via DeviceService
        deviceService.updateDeviceSubscription(
                resolvedDeviceId,
                DeviceStatus.ACTIVE,
                endDate);

        return mapToResponse(savedSubscription);
    }

    @Override
    public SubscriptionResponse getSubscriptionStatus(String deviceIdFingerprint) {
        // Resolve UUID
        UUID resolvedDeviceId;
        try {
            resolvedDeviceId = deviceService.resolveDeviceId(deviceIdFingerprint);
        } catch (Exception e) {
            // If device not found, throw exception
            throw e;
        }

        Optional<Subscription> subOpt = subscriptionRepository.findByDeviceIdAndStatus(resolvedDeviceId,
                SubscriptionStatus.ACTIVE);

        if (subOpt.isPresent()) {
            return mapToResponse(subOpt.get());
        }

        // Check if device is in TRIAL
        com.iptv.wiseplayer.domain.entity.Device device = deviceRepository.findByDeviceId(resolvedDeviceId)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.DeviceNotFoundException("Device not found"));

        if (device.getSubscriptionType() == com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL) {
            SubscriptionResponse resp = new SubscriptionResponse();
            resp.setDeviceId(resolvedDeviceId);
            resp.setStatus(SubscriptionStatus.TRIAL);
            resp.setEndDate(device.getExpiresAt());
            return resp;
        }

        SubscriptionResponse resp = new SubscriptionResponse();
        resp.setDeviceId(resolvedDeviceId);
        resp.setStatus(SubscriptionStatus.EXPIRED);
        return resp;
    }

    @Override
    @Transactional
    public void expireOverdueSubscriptions(LocalDateTime now) {
        // 1. Expire normal subscriptions
        List<Subscription> expiredSubs = subscriptionRepository.findExpiredActiveSubscriptions(now);

        for (Subscription sub : expiredSubs) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            // Update Device status
            deviceService.updateDeviceSubscription(
                    sub.getDeviceId(),
                    com.iptv.wiseplayer.domain.enums.DeviceStatus.INACTIVE,
                    sub.getEndDate());
        }

        // 2. Expire active devices that passed their expiry date
        List<com.iptv.wiseplayer.domain.entity.Device> expiredActiveDevices = deviceRepository
                .findByDeviceStatusAndExpiresAtBefore(
                        com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE, now);

        for (com.iptv.wiseplayer.domain.entity.Device device : expiredActiveDevices) {
            device.setDeviceStatus(com.iptv.wiseplayer.domain.enums.DeviceStatus.INACTIVE);
            deviceRepository.save(device);
        }
    }

    private SubscriptionResponse mapToResponse(Subscription sub) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getDeviceId(),
                sub.getPlan(),
                sub.getStatus(),
                sub.getStartDate(),
                sub.getEndDate());
    }
}
