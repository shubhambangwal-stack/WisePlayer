package com.iptv.wiseplayer.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CheckoutRequest {
    @NotBlank(message = "Device ID (fingerprint) is required")
    private String deviceId; // Fingerprint

    @NotBlank(message = "Plan name is required")
    private String planName;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }
}
