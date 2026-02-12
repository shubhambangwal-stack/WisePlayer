package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.DeviceAuditLog;
import com.iptv.wiseplayer.domain.entity.DeviceKey;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.request.DeviceActivationRequest;
import com.iptv.wiseplayer.dto.request.DeviceKeyRequest;
import com.iptv.wiseplayer.dto.response.DeviceActivationResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyResponse;
import com.iptv.wiseplayer.dto.response.DeviceKeyStatusResponse;
import com.iptv.wiseplayer.exception.DeviceNotFoundException;
import com.iptv.wiseplayer.repository.DeviceAuditRepository;
import com.iptv.wiseplayer.repository.DeviceKeyRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.service.DeviceKeyService;
import com.iptv.wiseplayer.service.DeviceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class DeviceKeyServiceImpl implements DeviceKeyService {

    private static final int KEY_LENGTH = 6;
    private static final long EXPIRATION_MINUTES = 10;
    private static final SecureRandom secureRandom = new SecureRandom();

    private final DeviceRepository deviceRepository;
    private final DeviceKeyRepository deviceKeyRepository;
    private final DeviceService deviceService;
    private final DeviceAuditRepository auditRepository;

    public DeviceKeyServiceImpl(DeviceRepository deviceRepository,
            DeviceKeyRepository deviceKeyRepository,
            DeviceService deviceService,
            DeviceAuditRepository auditRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceKeyRepository = deviceKeyRepository;
        this.deviceService = deviceService;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public DeviceKeyResponse generateDeviceKey(DeviceKeyRequest request) {
        // 1. Find the device by its physical ID (which is mapped to fingerprintHash
        // logic or similar)
        // Wait, the input is 'deviceId' which we refactored to be the physical ID.
        // But our DB stores fingerprintHash. We need to look up by hashing the input
        // ID.

        String inputDeviceId = request.getDeviceId();
        if (inputDeviceId == null || inputDeviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }

        UUID deviceId = deviceService.resolveDeviceId(inputDeviceId);
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found. Please register device first."));

        // 2. Invalidate/Delete existing keys for this device to ensure only one active
        // key
        deviceKeyRepository.deleteByDeviceId(device.getDeviceId());

        // 3. Generate raw numeric key (e.g., 123456)
        String rawKey = generateRandomNumericKey(KEY_LENGTH);

        // 4. Hash the key for storage
        String keyHash = hashString(rawKey);

        // 5. Create and save verification record
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRATION_MINUTES);
        DeviceKey deviceKey = new DeviceKey(device, keyHash, expiresAt);
        deviceKeyRepository.save(deviceKey);

        // 6. Return response with RAW key (only time it is visible)
        return new DeviceKeyResponse(rawKey, EXPIRATION_MINUTES * 60, expiresAt);
    }

    @Override
    @Transactional
    public DeviceActivationResponse activateDevice(DeviceActivationRequest request) {
        String inputDeviceId = request.getDeviceId();
        String inputKey = request.getActivationKey();

        if (inputDeviceId == null || inputKey == null) {
            throw new IllegalArgumentException("Device ID and Activation Key are required");
        }

        // 1. Find device
        UUID deviceId = deviceService.resolveDeviceId(inputDeviceId);
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found"));

        // 2. Find active key for device
        Optional<DeviceKey> optionalKey = deviceKeyRepository.findByDeviceDeviceId(device.getDeviceId());

        if (optionalKey.isEmpty()) {
            return new DeviceActivationResponse(false,
                    "No activation key found for this device. Please generate a new code.", device.getDeviceStatus());
        }

        DeviceKey deviceKey = optionalKey.get();

        // 3. Verify expiration
        if (deviceKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            // Cleanup expired key
            deviceKeyRepository.delete(deviceKey);
            return new DeviceActivationResponse(false, "Activation code expired. Please generate a new code.",
                    device.getDeviceStatus());
        }

        // 4. Verify key hash
        String inputKeyHash = hashString(inputKey);
        if (!deviceKey.getKeyHash().equals(inputKeyHash)) {
            return new DeviceActivationResponse(false, "Invalid activation code.", device.getDeviceStatus());
        }

        // 5. Activate Device
        DeviceStatus oldStatus = device.getDeviceStatus();
        device.setDeviceStatus(DeviceStatus.ACTIVE);
        device.setActivatedAt(LocalDateTime.now());
        deviceRepository.save(device);

        // Audit Logging
        DeviceAuditLog auditLog = new DeviceAuditLog(device.getDeviceId(), oldStatus, DeviceStatus.ACTIVE,
                "ACTIVATION", "Device activated via 6-digit code");
        auditRepository.save(auditLog);

        // 6. Delete used key
        deviceKeyRepository.delete(deviceKey);

        return new DeviceActivationResponse(true, "Device activated successfully", DeviceStatus.ACTIVE);
    }

    @Override
    public DeviceKeyStatusResponse getKeyStatus(UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with ID: " + deviceId));

        if (device.getDeviceStatus() == DeviceStatus.ACTIVE) {
            return new DeviceKeyStatusResponse("ACTIVATED", deviceId.toString());
        }

        Optional<DeviceKey> optionalKey = deviceKeyRepository.findByDeviceDeviceId(device.getDeviceId());
        if (optionalKey.isPresent()) {
            if (optionalKey.get().getExpiresAt().isBefore(LocalDateTime.now())) {
                return new DeviceKeyStatusResponse("EXPIRED", deviceId.toString());
            } else {
                return new DeviceKeyStatusResponse("PENDING_ACTIVATION", deviceId.toString());
            }
        }

        return new DeviceKeyStatusResponse("NO_CODE", deviceId.toString());
    }

    private String generateRandomNumericKey(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    private String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));

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
}
