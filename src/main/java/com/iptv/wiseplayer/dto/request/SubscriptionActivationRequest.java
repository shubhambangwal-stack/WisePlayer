package com.iptv.wiseplayer.dto.request;

/**
 * Request to activate a subscription for a device.
 */
public class SubscriptionActivationRequest {

    private String deviceId;
    private String planName;

    public SubscriptionActivationRequest() {
    }

    public SubscriptionActivationRequest(String deviceId, String planName) {
        this.deviceId = deviceId;
        this.planName = planName;
    }

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
