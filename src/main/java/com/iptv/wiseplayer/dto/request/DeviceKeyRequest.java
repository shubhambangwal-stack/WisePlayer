package com.iptv.wiseplayer.dto.request;

/**
 * Request to generate a new activation key for a device.
 */
public class DeviceKeyRequest {

    private String deviceId; // This is the physical ID (MAC based) from the client

    public DeviceKeyRequest() {
    }

    public DeviceKeyRequest(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
