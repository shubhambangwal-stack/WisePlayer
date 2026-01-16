package com.iptv.wiseplayer.dto.request;

/**
 * Request DTO for device registration.
 * Contains device ID (fingerprint) and optional metadata.
 */
public class DeviceRegistrationRequest {

    private String deviceId;
    private String deviceModel;
    private String osVersion;
    private String platform;

    // Constructors
    public DeviceRegistrationRequest() {
    }

    public DeviceRegistrationRequest(String deviceId, String deviceModel, String osVersion, String platform) {
        this.deviceId = deviceId;
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.platform = platform;
    }

    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
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

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
