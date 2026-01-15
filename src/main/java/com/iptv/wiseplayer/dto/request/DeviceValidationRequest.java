package com.iptv.wiseplayer.dto.request;

import java.util.UUID;

/**
 * Request DTO for device validation.
 * Contains device ID and fingerprint for verification.
 */
public class DeviceValidationRequest {

    private UUID deviceId;
    private String fingerprint;

    // Constructors
    public DeviceValidationRequest() {
    }

    public DeviceValidationRequest(UUID deviceId, String fingerprint) {
        this.deviceId = deviceId;
        this.fingerprint = fingerprint;
    }

    // Getters and Setters
    public UUID getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId) {
        this.deviceId = deviceId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
