package com.iptv.wiseplayer.dto.response;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;

/**
 * Response for device activation status check.
 */
public class DeviceActivationResponse {

    private boolean success;
    private String message;
    private DeviceStatus deviceStatus;

    public DeviceActivationResponse() {
    }

    public DeviceActivationResponse(boolean success, String message, DeviceStatus deviceStatus) {
        this.success = success;
        this.message = message;
        this.deviceStatus = deviceStatus;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }
}
