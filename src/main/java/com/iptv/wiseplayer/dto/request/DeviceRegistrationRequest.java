package com.iptv.wiseplayer.dto.request;

/**
 * Request DTO for device registration.
 * Contains device fingerprint and optional metadata.
 */
public class DeviceRegistrationRequest {

    private String fingerprint;
    private String deviceModel;
    private String osVersion;

    // Constructors
    public DeviceRegistrationRequest() {
    }

    public DeviceRegistrationRequest(String fingerprint, String deviceModel, String osVersion) {
        this.fingerprint = fingerprint;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
    }

    // Getters and Setters
    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public void setOsVersion(String osVersion) {
        this.osVersion = osVersion;
    }
}
