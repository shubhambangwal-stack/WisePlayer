package com.iptv.wiseplayer.dto.request;

/**
 * Request to activate a device using its ID and an activation key.
 */
public class DeviceActivationRequest {

    private String deviceId;
    private String activationKey;

    public DeviceActivationRequest() {
    }

    public DeviceActivationRequest(String deviceId, String activationKey) {
        this.deviceId = deviceId;
        this.activationKey = activationKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }
}
