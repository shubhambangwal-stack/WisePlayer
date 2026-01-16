package com.iptv.wiseplayer.dto.request;

/**
 * Request DTO for device validation.
 * Contains fingerprint for verification.
 */
public class DeviceValidationRequest {

    private String fingerprint;

    // Constructors
    public DeviceValidationRequest() {
    }

    public DeviceValidationRequest(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    // Getters and Setters
    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }
}
