package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Subscription;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.AdminSubscriptionResponse;
import com.iptv.wiseplayer.exception.DeviceNotFoundException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.SubscriptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class AdminSubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionService subscriptionService;
    private final DeviceRepository deviceRepository;

    public AdminSubscriptionService(SubscriptionRepository subscriptionRepository,
            SubscriptionService subscriptionService,
            DeviceRepository deviceRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionService = subscriptionService;
        this.deviceRepository = deviceRepository;
    }



    public Page<AdminSubscriptionResponse> getAllSubscriptions(
            String deviceId,
            String plan,
            SubscriptionStatus status,
            Pageable pageable) {
        return subscriptionRepository.searchSubscriptions(deviceId, plan, status, pageable).map(this::convertToResponse);
    }

    @Transactional
    public void manualActivate(SubscriptionActivationRequest request) {
        subscriptionService.activateSubscription(request);
    }

    @Transactional
    public void revokeSubscription(String idOrMac) {
        Subscription subscription = findSubscriptionByIdentifier(idOrMac);
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
    }

    private Subscription findSubscriptionByIdentifier(String identifier) {
        try {
            // Try parsing as UUID first (Subscription ID)
            UUID id = UUID.fromString(identifier);
            return subscriptionRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Subscription not found by ID: " + identifier));
        } catch (IllegalArgumentException e) {
            // If not a UUID, treat as MAC address, find the device, then the active
            // subscription
            String hash = hashMacAddress(identifier);
            Device device = deviceRepository.findByFingerprintHash(hash)
                    .orElseThrow(() -> new DeviceNotFoundException("Device not found by MAC address: " + identifier));

            return subscriptionRepository.findByDeviceIdAndStatus(device.getDeviceId(), SubscriptionStatus.ACTIVE)
                    .stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No active subscription found for device: " + identifier));
        }
    }

    private String hashMacAddress(String mac) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(mac.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (byte b : encodedhash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash MAC address", e);
        }
    }

    private AdminSubscriptionResponse convertToResponse(Subscription subscription) {
        AdminSubscriptionResponse response = new AdminSubscriptionResponse();
        response.setSubscriptionId(subscription.getId());
        response.setDeviceId(subscription.getDeviceId());
        response.setPlanName(subscription.getPlanName());
        response.setStatus(subscription.getStatus());
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        if (subscription.getDeviceId() != null) {
            deviceRepository.findByDeviceId(subscription.getDeviceId()).ifPresent(device -> {
                response.setMacAddress(device.getMacAddress());
            });
        }
        return response;
    }


}
