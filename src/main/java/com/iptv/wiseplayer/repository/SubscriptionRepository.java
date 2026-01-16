package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Subscription;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Subscription entity.
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Find active subscription for a device.
     */
    Optional<Subscription> findByDeviceIdAndStatus(UUID deviceId, SubscriptionStatus status);

    /**
     * Find expired subscriptions that are still marked as ACTIVE.
     */
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.endDate < :now")
    List<Subscription> findExpiredActiveSubscriptions(LocalDateTime now);
}
