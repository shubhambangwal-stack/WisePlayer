package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Subscription;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SubscriptionServiceImpl.class);

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

        // 1. Check for existing subscription record (TRIAL, ACTIVE, or EXPIRED)
        Optional<Subscription> existingSub = subscriptionRepository.findByDeviceId(resolvedDeviceId);

        LocalDateTime startDate = LocalDateTime.now();
        Subscription subscriptionToUpdate = null;

        if (existingSub.isPresent()) {
            subscriptionToUpdate = existingSub.get();
            log.info("Device {} has an existing subscription record (Status: {}). Updating/Extending...",
                    resolvedDeviceId, subscriptionToUpdate.getStatus());

            // If the existing one is ACTIVE and hasn't expired, start the extension from
            // the current end date
            if (subscriptionToUpdate.getStatus() == SubscriptionStatus.ACTIVE
                    && subscriptionToUpdate.getEndDate().isAfter(LocalDateTime.now())) {
                startDate = subscriptionToUpdate.getEndDate();
            }
        }

        // 2. Create or Update subscription
        LocalDateTime endDate = startDate.plusDays(request.getPlan().getDays());

        if (subscriptionToUpdate != null) {
            subscriptionToUpdate.setPlan(request.getPlan());
            subscriptionToUpdate.setStartDate(startDate);
            subscriptionToUpdate.setEndDate(endDate);
            subscriptionToUpdate.setStatus(SubscriptionStatus.ACTIVE);
        } else {
            subscriptionToUpdate = new Subscription(
                    resolvedDeviceId,
                    request.getPlan(),
                    startDate,
                    endDate,
                    SubscriptionStatus.ACTIVE);
        }

        Subscription savedSubscription = subscriptionRepository.save(subscriptionToUpdate);

        // 3. Update Device status via DeviceService
        com.iptv.wiseplayer.domain.enums.SubscriptionType type = request
                .getPlan() == com.iptv.wiseplayer.domain.enums.SubscriptionPlan.LIFETIME
                        ? com.iptv.wiseplayer.domain.enums.SubscriptionType.PAID_LIFETIME
                        : com.iptv.wiseplayer.domain.enums.SubscriptionType.PAID_ANNUAL;

        deviceService.updateDeviceSubscription(
                resolvedDeviceId,
                com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE,
                type,
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

        // Check subscriptions table first
        Optional<Subscription> subOpt = subscriptionRepository.findByDeviceId(resolvedDeviceId);

        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            // If the record exists and is ACTIVE but endDate has passed, it should be
            // EXPIRED
            if (sub.getStatus() == SubscriptionStatus.ACTIVE && sub.getEndDate().isBefore(LocalDateTime.now())) {
                sub.setStatus(SubscriptionStatus.EXPIRED);
                subscriptionRepository.save(sub);
            }
            return mapToResponse(sub);
        }

        // Fallback for devices without a subscription record (old devices)
        com.iptv.wiseplayer.domain.entity.Device device = deviceRepository.findByDeviceId(resolvedDeviceId)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.DeviceNotFoundException("Device not found"));

        SubscriptionResponse resp = new SubscriptionResponse();
        resp.setDeviceId(resolvedDeviceId);
        resp.setStatus(device.getSubscriptionType() == com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL
                ? SubscriptionStatus.TRIAL
                : SubscriptionStatus.EXPIRED);
        resp.setType(device.getSubscriptionType());
        resp.setEndDate(device.getExpiresAt());
        return resp;
    }

    @Override
    @Transactional
    public void expireOverdueSubscriptions(LocalDateTime now) {
        // 1. Expire normal/trial subscriptions
        List<Subscription> expiredSubs = subscriptionRepository.findExpiredSubscriptions(now);
        log.info("Found {} overdue subscriptions to expire", expiredSubs.size());

        for (Subscription sub : expiredSubs) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            // Update Device status
            com.iptv.wiseplayer.domain.enums.SubscriptionType type;
            if (sub.getPlan() == com.iptv.wiseplayer.domain.enums.SubscriptionPlan.LIFETIME) {
                type = com.iptv.wiseplayer.domain.enums.SubscriptionType.PAID_LIFETIME;
            } else if (sub.getPlan() == com.iptv.wiseplayer.domain.enums.SubscriptionPlan.TRIAL) {
                type = com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL;
            } else {
                type = com.iptv.wiseplayer.domain.enums.SubscriptionType.PAID_ANNUAL;
            }

            deviceService.updateDeviceSubscription(
                    sub.getDeviceId(),
                    com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE, // Keep ACTIVE
                    type,
                    sub.getEndDate());
        }

        // 2. Expire active devices that passed their expiry date
        // No longer auto-inactivating devices here to keep them "activated"
        /*
         * List<com.iptv.wiseplayer.domain.entity.Device> expiredActiveDevices =
         * deviceRepository
         * .findByDeviceStatusAndExpiresAtBefore(
         * com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE, now);
         * 
         * for (com.iptv.wiseplayer.domain.entity.Device device : expiredActiveDevices)
         * {
         * device.setDeviceStatus(com.iptv.wiseplayer.domain.enums.DeviceStatus.INACTIVE
         * );
         * deviceRepository.save(device);
         * }
         */
    }

    private SubscriptionResponse mapToResponse(Subscription sub) {
        com.iptv.wiseplayer.domain.enums.SubscriptionType type;
        if (sub.getPlan() == com.iptv.wiseplayer.domain.enums.SubscriptionPlan.LIFETIME) {
            type = com.iptv.wiseplayer.domain.enums.SubscriptionType.PAID_LIFETIME;
        } else if (sub.getPlan() == com.iptv.wiseplayer.domain.enums.SubscriptionPlan.TRIAL) {
            type = com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL;
        } else {
            type = com.iptv.wiseplayer.domain.enums.SubscriptionType.PAID_ANNUAL;
        }

        return new SubscriptionResponse(
                sub.getId(),
                sub.getDeviceId(),
                sub.getPlan(),
                sub.getStatus(),
                type,
                sub.getStartDate(),
                sub.getEndDate());
    }

    @Override
    @Transactional
    public void initializeTrial(UUID deviceId, LocalDateTime expiresAt) {
        log.info("Initializing free trial for device: {} expiring at {}", deviceId, expiresAt);

        Subscription trialSub = new Subscription(
                deviceId,
                com.iptv.wiseplayer.domain.enums.SubscriptionPlan.TRIAL,
                LocalDateTime.now(),
                expiresAt,
                SubscriptionStatus.TRIAL);

        subscriptionRepository.save(trialSub);

        // Update device status to ACTIVE via DeviceService
        deviceService.updateDeviceSubscription(
                deviceId,
                com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE,
                com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL,
                expiresAt);
    }

    @Override
    @Transactional
    public void revokeSubscription(String deviceId) {
        log.warn("Revoking subscription for device: {}", deviceId);

        // Resolve UUID
        UUID resolvedDeviceId;
        try {
            resolvedDeviceId = deviceService.resolveDeviceId(deviceId);
        } catch (Exception e) {
            log.error("Device not found for revocation: {}", deviceId);
            return;
        }

        // 1. Mark subscription as EXPIRED
        Optional<Subscription> subOpt = subscriptionRepository.findByDeviceId(resolvedDeviceId);
        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setEndDate(LocalDateTime.now().minusSeconds(1)); // Ensure it's in the past
            subscriptionRepository.save(sub);
            log.info("Subscription record marked as EXPIRED for device: {}", resolvedDeviceId);
        }

        // 2. Downgrade device status
        com.iptv.wiseplayer.domain.enums.SubscriptionType type = com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL;

        deviceService.updateDeviceSubscription(
                resolvedDeviceId,
                com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE,
                type,
                LocalDateTime.now().minusSeconds(1)); // Expired trial

        log.info("Device subscription status revoked/downgraded for device: {}", resolvedDeviceId);
    }
}
