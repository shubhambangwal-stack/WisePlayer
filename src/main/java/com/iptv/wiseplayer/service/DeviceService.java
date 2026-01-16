package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.DeviceRegistrationRequest;
import com.iptv.wiseplayer.dto.request.DeviceValidationRequest;
import com.iptv.wiseplayer.dto.response.DeviceRegistrationResponse;
import com.iptv.wiseplayer.dto.response.DeviceValidationResponse;

/**
 * Service interface for device management operations.
 * Handles device registration and validation.
 */
public interface DeviceService {

    /**
     * Register a new device or return existing device if fingerprint already
     * exists.
     * Registration is idempotent - same fingerprint returns existing device.
     *
     * @param request Device registration request with fingerprint and metadata
     * @return Device registration response with device ID and status
     * @throws IllegalArgumentException if fingerprint is null or empty
     */
    DeviceRegistrationResponse registerDevice(DeviceRegistrationRequest request);

    /**
     * Validate device by checking device ID and fingerprint match.
     * Updates last seen timestamp on successful validation.
     *
     * @param request Device validation request with device ID and fingerprint
     * @return Device validation response with status and access permission
     * @throws com.iptv.wiseplayer.exception.DeviceNotFoundException     if device
     *                                                                   not found
     * @throws com.iptv.wiseplayer.exception.InvalidFingerprintException if
     *                                                                   fingerprint
     *                                                                   doesn't
     *                                                                   match
     */
    DeviceValidationResponse validateDevice(DeviceValidationRequest request);

    /**
     * Update device subscription status.
     * Called by Subscription module when subscription status changes.
     *
     * @param deviceId  Device ID
     * @param status    New device status
     * @param expiresAt New expiration date (nullable)
     */
    void updateDeviceSubscription(java.util.UUID deviceId, com.iptv.wiseplayer.domain.enums.DeviceStatus status,
            java.time.LocalDateTime expiresAt);

    /**
     * Resolve internal Device UUID from raw fingerprint (MAC).
     *
     * @param fingerprint Raw fingerprint
     * @return Device UUID
     * @throws com.iptv.wiseplayer.exception.DeviceNotFoundException if not found
     */
    java.util.UUID getDeviceIdByFingerprint(String fingerprint);
}
