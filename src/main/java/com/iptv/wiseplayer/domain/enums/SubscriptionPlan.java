package com.iptv.wiseplayer.domain.enums;

/**
 * Available subscription plans.
 */
public enum SubscriptionPlan {
    ANNUAL(365),
    LIFETIME(36500);

    private final int days;

    SubscriptionPlan(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
