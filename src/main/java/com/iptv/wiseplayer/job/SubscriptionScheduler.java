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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SubscriptionScheduler.class);
    private final SubscriptionService subscriptionService;

    public SubscriptionScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Scheduled(fixedDelay = 10000) // Run every 10 seconds for easier testing
    public void checkExpiredSubscriptions() {
        log.info("Starting background subscription expiry check at {}", LocalDateTime.now());
        subscriptionService.expireOverdueSubscriptions(LocalDateTime.now());
    }
}
