package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Subscription;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT s FROM Subscription s WHERE " +
           "(:deviceId IS NULL OR :deviceId = '' OR CAST(s.deviceId AS string) LIKE LOWER(CONCAT('%', :deviceId, '%'))) AND " +
           "(:plan IS NULL OR :plan = '' OR LOWER(s.planName) LIKE LOWER(CONCAT('%', :plan, '%'))) AND " +
           "(:status IS NULL OR s.status = :status)")
    Page<Subscription> searchSubscriptions(
            @Param("deviceId") String deviceId,
            @Param("plan") String plan,
            @Param("status") SubscriptionStatus status,
            Pageable pageable);

    /**
     * Find active subscription for a device.
     */
    Optional<Subscription> findByDeviceIdAndStatus(UUID deviceId, SubscriptionStatus status);

    Optional<Subscription> findByDeviceId(UUID deviceId);
    
    void deleteAllByDeviceId(UUID deviceId);

    /**
     * Find expired subscriptions that are still marked as ACTIVE or TRIAL.
     */
    @Query("SELECT s FROM Subscription s WHERE (s.status = 'ACTIVE' OR s.status = 'TRIAL') AND s.endDate < :now")
    List<Subscription> findExpiredSubscriptions(LocalDateTime now);

    long countByStatus(SubscriptionStatus status);

    long countByStatusAndStartDateBetween(SubscriptionStatus status, LocalDateTime from, LocalDateTime to);
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' AND s.deviceId IN (SELECT d.deviceId FROM Device d WHERE d.resellerId = :resellerId)")
    long countActiveByResellerId(@org.springframework.data.repository.query.Param("resellerId") java.util.UUID resellerId);
}
