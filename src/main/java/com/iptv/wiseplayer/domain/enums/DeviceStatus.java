package com.iptv.wiseplayer.domain.enums;

/**
 * Device lifecycle states for IPTV application.
 * Represents the current status of a registered device.
 */
public enum DeviceStatus {
    /**
     * Device registered but not yet activated.
     * Initial state after registration.
     */
    INACTIVE,

    /**
     * Device has a valid subscription and is allowed to access content.
     */
    ACTIVE,

    /**
     * Device has been manually blocked or blocked due to security concerns.
     * Access is denied regardless of subscription status.
     */
    BLOCKED,

    /**
     * Device is in the 7-day free trial period.
     */
    TRIAL,

    /**
     * Device subscription has expired.
     * Access is denied until subscription is renewed.
     */
    EXPIRED
}
