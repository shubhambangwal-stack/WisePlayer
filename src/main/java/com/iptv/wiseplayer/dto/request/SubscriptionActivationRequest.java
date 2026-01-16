package com.iptv.wiseplayer.dto.request;

import com.iptv.wiseplayer.domain.enums.SubscriptionPlan;

/**
 * Request to activate a subscription for a device.
 */
public class SubscriptionActivationRequest {

    private String deviceId;
    private SubscriptionPlan plan;

    public SubscriptionActivationRequest() {
    }

    public SubscriptionActivationRequest(String deviceId, SubscriptionPlan plan) {
        this.deviceId = deviceId;
        this.plan = plan;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }
}
