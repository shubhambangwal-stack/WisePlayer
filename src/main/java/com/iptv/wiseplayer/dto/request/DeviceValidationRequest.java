package com.iptv.wiseplayer.dto.request;

/**
 * Request DTO for device validation.
 * Contains fingerprint for verification.
 */
public class DeviceValidationRequest {

    private String fingerprint;
    private String deviceSecret;

    // Constructors
    public DeviceValidationRequest() {
    }

    public DeviceValidationRequest(String fingerprint, String deviceSecret) {
        this.fingerprint = fingerprint;
        this.deviceSecret = deviceSecret;
    }

    // Getters and Setters
    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }
}
