package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.DeviceValidationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.DeviceValidationResponse;
import com.iptv.wiseplayer.exception.DeviceNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.security.DeviceTokenUtil;
import com.iptv.wiseplayer.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of DeviceService.
 * Handles device registration, validation, and fingerprint hashing.
 */
@Service
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceTokenUtil tokenUtil;

    public DeviceServiceImpl(DeviceRepository deviceRepository, DeviceTokenUtil tokenUtil) {
        this.deviceRepository = deviceRepository;
        this.tokenUtil = tokenUtil;
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
                    tokenUtil.generateToken(device.getDeviceId().toString(), hashFingerprint(request.getDeviceId())),
                    device.getRegisteredAt());
        }

        // Create new device with 7-day free trial
        Device newDevice = new Device(fingerprintHash, DeviceStatus.TRIAL);
        newDevice.setDeviceModel(request.getDeviceModel());
        newDevice.setOsVersion(request.getOsVersion());
        newDevice.setPlatform(request.getPlatform());
        newDevice.setExpiresAt(LocalDateTime.now().plusDays(7));

        // Save to database
        Device savedDevice = deviceRepository.save(newDevice);

        return new DeviceRegistrationResponse(
                savedDevice.getDeviceId(),
                savedDevice.getDeviceStatus(),
                tokenUtil.generateToken(savedDevice.getDeviceId().toString(), fingerprintHash),
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
        boolean allowed = (device.getDeviceStatus() == DeviceStatus.ACTIVE || device.getDeviceStatus() == DeviceStatus.TRIAL);
        String message = determineValidationMessage(device.getDeviceStatus());

        return new DeviceValidationResponse(
                device.getDeviceId(),
                device.getDeviceStatus(),
                tokenUtil.generateToken(device.getDeviceId().toString(), providedFingerprintHash),
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
        return tokenUtil.hashFingerprint(fingerprint);
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
            case TRIAL -> "Device is in free trial period. Please subscribe to continue access later.";
            case INACTIVE -> "Device is registered but not activated. Please activate your subscription.";
            case BLOCKED -> "Device has been blocked. Please contact support.";
            case EXPIRED -> "Device subscription has expired. Please renew your subscription to continue.";
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
    public java.util.UUID resolveDeviceId(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            throw new IllegalArgumentException("Device identity cannot be null or empty");
        }

        String trimmedIdentity = identity.trim();

        // 1. Try as UUID
        try {
            UUID uuid = UUID.fromString(trimmedIdentity);
            Optional<Device> device = deviceRepository.findByDeviceId(uuid);
            if (device.isPresent()) {
                return device.get().getDeviceId();
            }
        } catch (IllegalArgumentException e) {
            // Not a valid UUID format, proceed to fingerprint hash
        }

        // 2. Try as Fingerprint (MAC)
        String fingerprintHash = hashFingerprint(trimmedIdentity);
        return deviceRepository.findByFingerprintHash(fingerprintHash)
                .map(Device::getDeviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with identity: " + identity));
    }
}
