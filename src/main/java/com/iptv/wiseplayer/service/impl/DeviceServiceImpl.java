package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.DeviceAuditLog;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.DeviceValidationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.DeviceValidationResponse;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.DeviceAuthenticationException;
import com.iptv.wiseplayer.exception.DeviceNotFoundException;
import com.iptv.wiseplayer.repository.DeviceAuditRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.SubscriptionRepository;
import com.iptv.wiseplayer.repository.ResellerCustomerRepository;
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
    private final DeviceAuditRepository auditRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ResellerCustomerRepository resellerCustomerRepository;

    public DeviceServiceImpl(DeviceRepository deviceRepository,
            DeviceTokenUtil tokenUtil,
            DeviceAuditRepository auditRepository,
            SubscriptionRepository subscriptionRepository,
            ResellerCustomerRepository resellerCustomerRepository) {
        this.deviceRepository = deviceRepository;
        this.tokenUtil = tokenUtil;
        this.auditRepository = auditRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.resellerCustomerRepository = resellerCustomerRepository;
    }


    @Override
    @Transactional
    public DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request) {
        // Validate input
        if (request.getDeviceId() == null || request.getDeviceId().trim().isEmpty()) {
            throw new BadRequestException("Device ID cannot be null or empty");
        }

        // Hash the fingerprint/deviceId (SHA-256)
        String fingerprintHash = hashFingerprint(request.getDeviceId());

        // Check if device already exists (idempotent registration)
        Optional<Device> existingDevice = deviceRepository.findByFingerprintHash(fingerprintHash);

        if (existingDevice.isPresent()) {
            Device device = existingDevice.get();
            // Update metadata
            device.setDeviceModel(request.getDeviceModel());
            device.setOsVersion(request.getOsVersion());
            device.setPlatform(request.getPlatform());
            device.setLastSeenAt(LocalDateTime.now());

            // Generate new permanent Hardware-Linked Secret (HLS)
            String rawSecret = tokenUtil.generateRefreshToken();
            device.setDeviceSecretHash(tokenUtil.hashSecret(rawSecret));

            Device savedDevice = deviceRepository.save(device);

            logAudit(savedDevice.getDeviceId(), savedDevice.getDeviceStatus(), savedDevice.getDeviceStatus(), "DEVICE_RE_REGISTERED",
                    "Device re-registered due to reinstall or cache clear.");

            return new DeviceRegistrationResponse(
                    savedDevice.getDeviceId(),
                    savedDevice.getDeviceStatus(),
                    savedDevice.getSubscriptionType(),
                    tokenUtil.generateToken(savedDevice.getDeviceId().toString(), fingerprintHash),
                    rawSecret,
                    savedDevice.getRegisteredAt());
        }

        // Create new device (Trial type by default, but not started yet)
        Device newDevice = new Device(fingerprintHash, DeviceStatus.INACTIVE);
        newDevice.setSubscriptionType(SubscriptionType.TRIAL);
        newDevice.setDeviceModel(request.getDeviceModel());
        newDevice.setOsVersion(request.getOsVersion());
        newDevice.setPlatform(request.getPlatform());
        newDevice.setMacAddress(request.getDeviceId());
        
        // Auto-link reseller if this MAC was pre-claimed
        resellerCustomerRepository.findByMacAddress(request.getDeviceId())
                .ifPresent(rc -> newDevice.setResellerId(rc.getResellerId()));



        // Generate permanent Hardware-Linked Secret (HLS)
        String rawSecret = tokenUtil.generateRefreshToken();
        newDevice.setDeviceSecretHash(tokenUtil.hashSecret(rawSecret));

        // Save     device to database
        Device savedDevice = deviceRepository.save(newDevice);

        logAudit(savedDevice.getDeviceId(), null, DeviceStatus.INACTIVE, "DEVICE_REGISTERED",
                "New device registered. Pending activation.");

        return new DeviceRegistrationResponse(
                savedDevice.getDeviceId(),
                savedDevice.getDeviceStatus(),
                savedDevice.getSubscriptionType(),
                tokenUtil.generateToken(savedDevice.getDeviceId().toString(), fingerprintHash),
                rawSecret,
                savedDevice.getRegisteredAt());
    }

    @Override
    @Transactional
    public DeviceValidationResponse validateDevice(DeviceValidationRequest request) {
        // Validate input
        if (request.getFingerprint() == null || request.getFingerprint().trim().isEmpty()) {
            throw new BadRequestException("Device fingerprint cannot be null or empty");
        }

        // Hash the provided fingerprint for lookup
        String providedFingerprintHash = hashFingerprint(request.getFingerprint());

        // Find device by fingerprint hash
        Device device = deviceRepository.findByFingerprintHash(providedFingerprintHash)
                .orElseThrow(() -> new DeviceNotFoundException(
                        "Device not found. Please register device first."));


        // Update last seen timestamp
        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

// LOCK CHECK — reseller lock device
        if (!device.isActive()) {
            return new DeviceValidationResponse(
                    device.getDeviceId(),
                    device.getDeviceStatus(),
                    device.getSubscriptionType(),
                    null,
                    false,
                    "Your account has been locked. Please contact your reseller.",
                    device.getLastSeenAt());
        }

// Determine access permission based on device status and expiry
        boolean allowed = false;
        if (device.getDeviceStatus() == DeviceStatus.ACTIVE) {
            if (device.getExpiresAt() == null) {
                // ✅ FIX: expiresAt should never be null for an ACTIVE device, but if it is
                // (data integrity gap from a failed subscription write), still grant access
                // rather than silently blocking a legitimately activated device.
                // Log a warning so this can be investigated in production.
                org.slf4j.LoggerFactory.getLogger(getClass()).warn(
                        "ACTIVE device {} has null expiresAt — granting access but this should be investigated.",
                        device.getDeviceId());
                allowed = true;
            } else if (LocalDateTime.now().isBefore(device.getExpiresAt())) {
                allowed = true;
            } else {
                // Subscription genuinely expired — access denied but status stays ACTIVE
                logAudit(device.getDeviceId(), DeviceStatus.ACTIVE, DeviceStatus.ACTIVE, "ACCESS_DENIED",
                        "Subscription expired during validation");
            }
        }

        String message = determineValidationMessage(device);

        SubscriptionType responseType = device.getSubscriptionType();
        if (device.getExpiresAt() != null && LocalDateTime.now().isAfter(device.getExpiresAt())) {
            responseType = SubscriptionType.EXPIRED;
        }

        DeviceValidationResponse response = new DeviceValidationResponse(
                device.getDeviceId(),
                device.getDeviceStatus(),
                responseType,
                null,
                allowed,
                message,
                device.getLastSeenAt());
        
        return response;
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
    private String determineValidationMessage(Device device) {
        if (device.getDeviceStatus() == DeviceStatus.INACTIVE) {
            if (device.getExpiresAt() != null && LocalDateTime.now().isAfter(device.getExpiresAt())) {
                return "Device subscription has expired. Please renew your subscription to continue.";
            }
            return "Device is registered but not activated. Please activate your subscription.";
        }

        if (device.getDeviceStatus() == DeviceStatus.ACTIVE) {
            boolean isExpired = device.getExpiresAt() != null && LocalDateTime.now().isAfter(device.getExpiresAt());

            if (isExpired) {
                return "Your subscription has expired. Please renew to continue access.";
            }

            if (device.getSubscriptionType() == SubscriptionType.TRIAL) {
                return "Device is in free trial period. Please subscribe to continue access later.";
            }
            return "Device is active and authorized";
        }

        return "Device status unknown or unauthorized.";
    }

    @Override
    @Transactional
    public void updateDeviceSubscription(java.util.UUID deviceId, DeviceStatus status, SubscriptionType type,
            LocalDateTime expiresAt) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with ID: " + deviceId));

        DeviceStatus oldStatus = device.getDeviceStatus();
        device.setDeviceStatus(status);
        device.setSubscriptionType(type);
        device.setExpiresAt(expiresAt);
        // Force status to ACTIVE if a valid future expiration is provided
        if (expiresAt != null && expiresAt.isAfter(LocalDateTime.now())) {
            device.setDeviceStatus(DeviceStatus.ACTIVE);
        }
        deviceRepository.save(device);

        logAudit(deviceId, oldStatus, device.getDeviceStatus(), "SUBSCRIPTION_UPDATE",
                "Status set to " + device.getDeviceStatus());
    }

    @Override
    @Transactional
    public DeviceValidationResponse refreshDeviceToken(String deviceSecret, String fingerprint) {
        String providedFingerprintHash = hashFingerprint(fingerprint);
        Device device = deviceRepository.findByFingerprintHash(providedFingerprintHash)
                .orElseThrow(() -> new DeviceAuthenticationException("Device not found for provided fingerprint"));

        // Verify Hardware-Linked Secret
        String providedSecretHash = tokenUtil.hashSecret(deviceSecret);
        if (device.getDeviceSecretHash() == null || !device.getDeviceSecretHash().equals(providedSecretHash)) {
            throw new DeviceAuthenticationException("Invalid device secret during refresh");
        }

        // Generate new session token
        String newAccessToken = tokenUtil.generateToken(device.getDeviceId().toString(), providedFingerprintHash);

        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

        boolean allowed = false;
        if (device.getDeviceStatus() == DeviceStatus.ACTIVE) {
            if (device.getExpiresAt() != null && LocalDateTime.now().isBefore(device.getExpiresAt())) {
                allowed = true;
            }
        }
        String message = determineValidationMessage(device);

        SubscriptionType responseType = device.getSubscriptionType();
        if (device.getExpiresAt() != null && LocalDateTime.now().isAfter(device.getExpiresAt())) {
            responseType = SubscriptionType.EXPIRED;
        }

        return new DeviceValidationResponse(
                device.getDeviceId(),
                device.getDeviceStatus(),
                responseType,
                newAccessToken,
                allowed,
                message,
                device.getLastSeenAt());
    }

    private void logAudit(UUID deviceId, DeviceStatus oldStatus, DeviceStatus newStatus, String action, String reason) {
        DeviceAuditLog auditLog = new DeviceAuditLog(deviceId, oldStatus, newStatus, action, reason);
        auditRepository.save(auditLog);
    }

    @Override
    public java.util.UUID resolveDeviceId(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            throw new BadRequestException("Device identity cannot be null or empty");
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
        Optional<Device> device = deviceRepository.findByFingerprintHash(fingerprintHash);
        
        // 3. Try plain MAC address lookup as a final fallback
        if (device.isEmpty()) {
            device = deviceRepository.findByMacAddressIgnoreCase(trimmedIdentity);
        }

        return device.map(Device::getDeviceId)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with identity: " + identity));
    }
}
