package com.iptv.wiseplayer.job;

import com.iptv.wiseplayer.service.SubscriptionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled job to manage subscription lifecycle.
 */
@Component
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Run nightly at 00:01 to expire overdue subscriptions.
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void checkExpiredSubscriptions() {
        subscriptionService.expireOverdueSubscriptions(LocalDateTime.now());
    }
}
