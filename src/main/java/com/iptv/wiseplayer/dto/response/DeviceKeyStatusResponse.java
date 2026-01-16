package com.iptv.wiseplayer.dto.response;

/**
 * Response for checking the status of an activation key.
 */
public class DeviceKeyStatusResponse {

    private String status; // PENDING, EXPIRED, NOT_FOUND
    private String deviceId;

    public DeviceKeyStatusResponse() {
    }

    public DeviceKeyStatusResponse(String status, String deviceId) {
        this.status = status;
        this.deviceId = deviceId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
