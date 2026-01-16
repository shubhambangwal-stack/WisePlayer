package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.DeviceValidationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.DeviceValidationResponse;
import com.iptv.wiseplayer.exception.DeviceNotFoundException;
import com.iptv.wiseplayer.exception.InvalidFingerprintException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Implementation of DeviceService.
 * Handles device registration, validation, and fingerprint hashing.
 */
@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    @Transactional
    public DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request) {
        // Validate input
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }

        // Hash the fingerprint/deviceId (SHA-256)
        String fingerprintHash = hashFingerprint(request.getDeviceId());

        // Check if device already exists (idempotent registration)
        Optional<Device> existingDevice = deviceRepository.findByFingerprintHash(fingerprintHash);

        if (existingDevice.isPresent()) {
            Device device = existingDevice.get();
            // Update device metadata if changed (optional, but good for tracking updates)
            if (request.getPlatform() != null && !request.getPlatform().equals(device.getPlatform())) {
                device.setPlatform(request.getPlatform());
                device.setDeviceModel(request.getDeviceModel());
                device.setOsVersion(request.getOsVersion());
                deviceRepository.save(device);
            }

            return new DeviceRegistrationResponse(
                    device.getDeviceId(),
                    device.getDeviceStatus(),
                    device.getRegisteredAt());
        }

        // Create new device
        Device newDevice = new Device(fingerprintHash, DeviceStatus.INACTIVE);
        newDevice.setDeviceModel(request.getDeviceModel());
        newDevice.setOsVersion(request.getOsVersion());
        newDevice.setPlatform(request.getPlatform());

        // Save to database
        Device savedDevice = deviceRepository.save(newDevice);

        return new DeviceRegistrationResponse(
                savedDevice.getDeviceId(),
                savedDevice.getDeviceStatus(),
                savedDevice.getRegisteredAt());
    }

    @Override
    @Transactional
    public DeviceValidationResponse validateDevice(DeviceValidationRequest request) {
        // Validate input
        if (request.getFingerprint() == null || request.getFingerprint().trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint cannot be null or empty");
        }

        // Hash the provided fingerprint for lookup
        String providedFingerprintHash = hashFingerprint(request.getFingerprint());

        // Find device by fingerprint hash
        Device device = deviceRepository.findByFingerprintHash(providedFingerprintHash)
                .orElseThrow(() -> new DeviceNotFoundException(
                        "Device not found with fingerprint user provided. Please register device first."));

        // Update last seen timestamp
        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

        // Determine access permission based on device status
        boolean allowed = device.getDeviceStatus() == DeviceStatus.ACTIVE;
        String message = determineValidationMessage(device.getDeviceStatus());

        return new DeviceValidationResponse(
                device.getDeviceId(),
                device.getDeviceStatus(),
                allowed,
                message,
                device.getLastSeenAt());
    }

    /**
     * Hash device fingerprint using SHA-256.
     * CRITICAL: Never store raw MAC address or fingerprint.
     *
     * @param fingerprint Raw fingerprint from client
     * @return SHA-256 hash as hex string
     */
    private String hashFingerprint(String fingerprint) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(fingerprint.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));

            // Convert bytes to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Determine validation message based on device status.
     *
     * @param status Device status
     * @return Human-readable message
     */
    private String determineValidationMessage(DeviceStatus status) {
        return switch (status) {
            case ACTIVE -> "Device is active and authorized";
            case INACTIVE -> "Device is registered but not activated. Please activate your subscription.";
            case BLOCKED -> "Device has been blocked. Please contact support.";
            case EXPIRED -> "Device subscription has expired. Please renew your subscription.";
        };

    }

    @Override
    @Transactional
    public void updateDeviceSubscription(java.util.UUID deviceId, DeviceStatus status, LocalDateTime expiresAt) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with ID: " + deviceId));

        device.setDeviceStatus(status);
        device.setExpiresAt(expiresAt);
        deviceRepository.save(device);
    }

    @Override
    public java.util.UUID getDeviceIdByFingerprint(String fingerprint) {
        String fingerprintHash = hashFingerprint(fingerprint);
        Device device = deviceRepository.findByFingerprintHash(fingerprintHash)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with fingerprint user provided."));
        return device.getDeviceId();
    }
}
