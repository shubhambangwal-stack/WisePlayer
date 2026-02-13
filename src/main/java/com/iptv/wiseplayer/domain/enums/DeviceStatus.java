package com.iptv.wiseplayer.domain.enums;

/**
 * Device lifecycle states for IPTV application.
 * Represents the current status of a registered device.
 */
public enum DeviceStatus {
    /**
     * Device registered but not yet activated or subscription expired.
     */
    INACTIVE,

    /**
     * Device has a valid active subscription.
     */
    ACTIVE
}
