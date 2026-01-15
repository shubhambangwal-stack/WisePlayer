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
        if (request.getFingerprint() == null || request.getFingerprint().trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint cannot be null or empty");
        }

        // Hash the fingerprint (SHA-256)
        String fingerprintHash = hashFingerprint(request.getFingerprint());

        // Check if device already exists (idempotent registration)
        Optional<Device> existingDevice = deviceRepository.findByFingerprintHash(fingerprintHash);

        if (existingDevice.isPresent()) {
            Device device = existingDevice.get();
            return new DeviceRegistrationResponse(
                    device.getDeviceId(),
                    device.getDeviceStatus(),
                    device.getRegisteredAt());
        }

        // Create new device
        Device newDevice = new Device(fingerprintHash, DeviceStatus.INACTIVE);
        newDevice.setDeviceModel(request.getDeviceModel());
        newDevice.setOsVersion(request.getOsVersion());

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
        if (request.getDeviceId() == null) {
            throw new IllegalArgumentException("Device ID cannot be null");
        }
        if (request.getFingerprint() == null || request.getFingerprint().trim().isEmpty()) {
            throw new IllegalArgumentException("Device fingerprint cannot be null or empty");
        }

        // Find device by ID
        Device device = deviceRepository.findByDeviceId(request.getDeviceId())
                .orElseThrow(() -> new DeviceNotFoundException(
                        "Device not found with ID: " + request.getDeviceId()));

        // Hash the provided fingerprint and compare
        String providedFingerprintHash = hashFingerprint(request.getFingerprint());

        if (!device.getFingerprintHash().equals(providedFingerprintHash)) {
            throw new InvalidFingerprintException(
                    "Fingerprint mismatch for device: " + request.getDeviceId());
        }

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
            byte[] hashBytes = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));

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
}
