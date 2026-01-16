package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.SubscriptionActivationRequest;
import com.iptv.wiseplayer.dto.response.SubscriptionResponse;

import java.time.LocalDateTime;

/**
 * Service interface for subscription management.
 */
public interface SubscriptionService {

    /**
     * Activate a new subscription for a device.
     * This is called after payment confirmation.
     *
     * @param request Subscription activation details
     * @return Created subscription details
     */
    SubscriptionResponse activateSubscription(SubscriptionActivationRequest request);

    /**
     * Get current subscription status for a device.
     *
     * @param deviceId Device fingerprint
     * @return Subscription details
     */
    SubscriptionResponse getSubscriptionStatus(String deviceId);

    /**
     * Expire subscriptions that have passed their end date.
     * Intended for scheduled jobs.
     *
     * @param now Current timestamp
     */
    void expireOverdueSubscriptions(LocalDateTime now);
}
