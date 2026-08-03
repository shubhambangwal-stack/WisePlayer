package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Subscription;
import com.iptv.wiseplayer.domain.entity.SubscriptionPlanConfig;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.PlanConfigRepository;
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
 * Plan details are resolved dynamically from subscription_plan_configs — no
 * hard-coded enum.
 */
@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SubscriptionServiceImpl.class);

    private final SubscriptionRepository subscriptionRepository;
    private final DeviceService deviceService;
    private final com.iptv.wiseplayer.repository.DeviceRepository deviceRepository;
    private final PlanConfigRepository planConfigRepository;

    public SubscriptionServiceImpl(SubscriptionRepository subscriptionRepository,
            DeviceService deviceService,
            com.iptv.wiseplayer.repository.DeviceRepository deviceRepository,
            PlanConfigRepository planConfigRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.deviceService = deviceService;
        this.deviceRepository = deviceRepository;
        this.planConfigRepository = planConfigRepository;
    }

    @Override
    @Transactional
    public SubscriptionResponse activateSubscription(SubscriptionActivationRequest request) {
        if (request.getDeviceId() == null || request.getPlanName() == null) {
            throw new IllegalArgumentException("Device ID (Fingerprint) and planName are required");
        }

        // Resolve plan config from DB — no enum needed
        SubscriptionPlanConfig planConfig = planConfigRepository.findByName(request.getPlanName())
                .orElseThrow(() -> new ResourceNotFoundException("Plan not found: " + request.getPlanName()));

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

        // 2. Create or Update subscription — duration comes from planConfig
        LocalDateTime endDate = startDate.plusDays(planConfig.getDurationDays());

        if (subscriptionToUpdate != null) {
            subscriptionToUpdate.setPlanName(planConfig.getName());
            subscriptionToUpdate.setStartDate(startDate);
            subscriptionToUpdate.setEndDate(endDate);
            subscriptionToUpdate.setStatus(SubscriptionStatus.ACTIVE);
        } else {
            subscriptionToUpdate = new Subscription(
                    resolvedDeviceId,
                    planConfig.getName(),
                    startDate,
                    endDate,
                    SubscriptionStatus.ACTIVE,
                    "PAYPAL");
        }

        Subscription savedSubscription = subscriptionRepository.save(subscriptionToUpdate);

        // 3. Update Device status via DeviceService
        SubscriptionType type = resolveSubscriptionType(planConfig);

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

        // Fallback for devices without a subscription record (not yet activated or legacy devices)
        com.iptv.wiseplayer.domain.entity.Device device = deviceRepository.findByDeviceId(resolvedDeviceId)
                .orElseThrow(() -> new com.iptv.wiseplayer.exception.DeviceNotFoundException("Device not found"));

        SubscriptionResponse resp = new SubscriptionResponse();
        resp.setSubscriptionId(resolvedDeviceId); // Use device ID as placeholder
        resp.setDeviceId(resolvedDeviceId);
        resp.setPlanName(device.getSubscriptionType() != null ? device.getSubscriptionType().name() : "TRIAL");
        resp.setType(device.getSubscriptionType() != null ? device.getSubscriptionType() : SubscriptionType.TRIAL);

        // Determine dates and status based on device state
        if (device.getExpiresAt() != null) {
            // Device has a real expiry date (was activated at some point)
            LocalDateTime start = device.getActivatedAt() != null ? device.getActivatedAt()
                    : (device.getRegisteredAt() != null ? device.getRegisteredAt() : device.getExpiresAt().minusDays(7));
            resp.setStartDate(start);
            resp.setEndDate(device.getExpiresAt());

            if (LocalDateTime.now().isAfter(device.getExpiresAt())) {
                resp.setStatus(SubscriptionStatus.EXPIRED);
            } else {
                resp.setStatus(device.getSubscriptionType() == SubscriptionType.TRIAL
                        ? SubscriptionStatus.TRIAL : SubscriptionStatus.ACTIVE);
            }
        } else {
            // Device has NOT been activated yet — no expires_at set
            // Show trial window based on registration date
            LocalDateTime registeredAt = device.getRegisteredAt() != null
                    ? device.getRegisteredAt()
                    : (device.getCreatedAt() != null ? device.getCreatedAt() : LocalDateTime.now());
            resp.setStartDate(registeredAt);
            resp.setEndDate(registeredAt.plusDays(7));
            resp.setStatus(SubscriptionStatus.TRIAL);
        }

        return resp;
    }

    @Override
    @Transactional
    public void expireOverdueSubscriptions(LocalDateTime now) {
        List<Subscription> expiredSubs = subscriptionRepository.findExpiredSubscriptions(now);
        log.info("Found {} overdue subscriptions to expire", expiredSubs.size());

        for (Subscription sub : expiredSubs) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);

            // Determine subscription type from plan name for device update
            SubscriptionType type = resolveSubscriptionTypeByName(sub.getPlanName());

            deviceService.updateDeviceSubscription(
                    sub.getDeviceId(),
                    com.iptv.wiseplayer.domain.enums.DeviceStatus.INACTIVE,
                    type,
                    sub.getEndDate());
        }
    }

    private SubscriptionResponse mapToResponse(Subscription sub) {
        SubscriptionType type = resolveSubscriptionTypeByName(sub.getPlanName());

        return new SubscriptionResponse(
                sub.getId(),
                sub.getDeviceId(),
                sub.getPlanName(),
                sub.getStatus(),
                type,
                sub.getStartDate(),
                sub.getEndDate());
    }

    @Override
    @Transactional
    public void initializeTrial(UUID deviceId, LocalDateTime expiresAt) {
        log.info("Initializing/Updating free trial for device: {} expiring at {}", deviceId, expiresAt);

        Optional<Subscription> existingSub = subscriptionRepository.findByDeviceId(deviceId);

        Subscription trialSub;
        if (existingSub.isPresent()) {
            trialSub = existingSub.get();
            log.info("Device {} has an existing subscription record (ID: {}). Updating to TRIAL...", deviceId,
                    trialSub.getId());
            trialSub.setPlanName("TRIAL");
            trialSub.setStartDate(LocalDateTime.now());
            trialSub.setEndDate(expiresAt);
            trialSub.setStatus(SubscriptionStatus.TRIAL);
            trialSub.setActivationSource("SYSTEM");
        } else {
            trialSub = new Subscription(
                    deviceId,
                    "TRIAL",
                    LocalDateTime.now(),
                    expiresAt,
                    SubscriptionStatus.TRIAL,
                    "SYSTEM");
        }

        subscriptionRepository.save(trialSub);

        deviceService.updateDeviceSubscription(
                deviceId,
                com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE,
                SubscriptionType.TRIAL,
                expiresAt);
    }

    @Override
    @Transactional
    public void revokeSubscription(String deviceId) {
        log.warn("Revoking subscription for device: {}", deviceId);

        UUID resolvedDeviceId;
        try {
            resolvedDeviceId = deviceService.resolveDeviceId(deviceId);
        } catch (Exception e) {
            log.error("Device not found for revocation: {}", deviceId);
            return;
        }

        Optional<Subscription> subOpt = subscriptionRepository.findByDeviceId(resolvedDeviceId);
        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            sub.setStatus(SubscriptionStatus.EXPIRED);
            sub.setEndDate(LocalDateTime.now().minusSeconds(1));
            subscriptionRepository.save(sub);
            log.info("Subscription record marked as EXPIRED for device: {}", resolvedDeviceId);
        }

        deviceService.updateDeviceSubscription(
                resolvedDeviceId,
                com.iptv.wiseplayer.domain.enums.DeviceStatus.INACTIVE,
                SubscriptionType.TRIAL,
                LocalDateTime.now().minusSeconds(1));

        log.info("Device subscription status revoked/downgraded for device: {}", resolvedDeviceId);
    }

    /**
     * Resolves the SubscriptionType for a given plan config.
     * LIFETIME plans have durationDays >= 36500 (100 years).
     */
    private SubscriptionType resolveSubscriptionType(SubscriptionPlanConfig planConfig) {
        if ("TRIAL".equalsIgnoreCase(planConfig.getName())) {
            return SubscriptionType.TRIAL;
        }
        if (planConfig.getDurationDays() >= 36500) {
            return SubscriptionType.PAID_LIFETIME;
        }
        return SubscriptionType.PAID_ANNUAL;
    }

    /**
     * Resolves the SubscriptionType from a plan name string (no DB lookup
     * required).
     */
    private SubscriptionType resolveSubscriptionTypeByName(String planName) {
        if ("TRIAL".equalsIgnoreCase(planName)) {
            return SubscriptionType.TRIAL;
        }
        if ("LIFETIME".equalsIgnoreCase(planName)) {
            return SubscriptionType.PAID_LIFETIME;
        }
        return SubscriptionType.PAID_ANNUAL;
    }
}
