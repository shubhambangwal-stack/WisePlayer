package com.iptv.wiseplayer.domain.enums;

/**
 * Available subscription plans.
 */
public enum SubscriptionPlan {
    /**
     * Annual Plan: 6 EUR for 12 months (365 days)
     */
    ANNUAL(365),

    /**
     * Lifetime Plan: 10 EUR for unlimited access
     * Represented as a very large number of days (e.g., 100 years).
     */
    LIFETIME(36500);

    private final int days;

    SubscriptionPlan(int days) {
        this.days = days;
    }

    public int getDays() {
        return days;
    }
}
