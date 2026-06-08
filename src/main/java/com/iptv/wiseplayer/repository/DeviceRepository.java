package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;

import com.iptv.wiseplayer.domain.enums.SubscriptionType;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Device entity.
 * Provides database access methods for device management.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

        @Query("SELECT d FROM Device d WHERE " +
                        "(:deviceId IS NULL OR :deviceId = '' OR CAST(d.deviceId AS string) LIKE LOWER(CONCAT('%', :deviceId, '%')) OR LOWER(d.macAddress) LIKE LOWER(CONCAT('%', :deviceId, '%'))) AND " +
                        "(:status IS NULL OR d.deviceStatus = :status) AND " +
                        "(:subscription IS NULL OR d.subscriptionType = :subscription) AND " +
                        "(:model IS NULL OR :model = '' OR LOWER(d.deviceModel) LIKE LOWER(CONCAT('%', :model, '%'))) AND " +
                        "(:platform IS NULL OR :platform = '' OR LOWER(d.platform) LIKE LOWER(CONCAT('%', :platform, '%')))")

        Page<Device> searchDevices(
                        @Param("deviceId") String deviceId,
                        @Param("status") DeviceStatus status,
                        @Param("subscription") SubscriptionType subscription,
                        @Param("model") String model,
                        @Param("platform") String platform,
                        Pageable pageable);

        /**
         * Find device by fingerprint hash.
         * Used for idempotent registration and validation.
         *
         * @param fingerprintHash SHA-256 hash of device fingerprint
         * @return Optional containing device if found
         */
        Optional<Device> findByFingerprintHash(String fingerprintHash);

        /**
         * Find devices by status and expiration date.
         */
        java.util.List<Device> findByDeviceStatusAndExpiresAtBefore(
                        com.iptv.wiseplayer.domain.enums.DeviceStatus status,
                        java.time.LocalDateTime now);

        /**
         * @param deviceId UUID of the device
         * @return Optional containing device if found
         */
        Optional<Device> findByDeviceId(UUID deviceId);

        /**
         * Find device by refresh token.
         *
         * @param refreshToken Refresh token string
         * @return Optional containing device if found
         */
        Optional<Device> findByRefreshToken(String refreshToken);

        long countByDeviceStatus(com.iptv.wiseplayer.domain.enums.DeviceStatus status);

        long countBySubscriptionType(com.iptv.wiseplayer.domain.enums.SubscriptionType type);

        long countByResellerId(UUID resellerId);

        long countByResellerIdAndDeviceStatus(UUID resellerId, com.iptv.wiseplayer.domain.enums.DeviceStatus status);

        java.util.List<Device> findTop5ByResellerIdOrderByCreatedAtDesc(UUID resellerId);

        java.util.List<Device> findAllByResellerId(UUID resellerId);

        Page<Device> findAllByResellerId(UUID resellerId, Pageable pageable);

        @Query("SELECT d FROM Device d WHERE d.resellerId = :resellerId AND " +
                "(:search IS NULL OR :search = '' OR " +
                "   LOWER(d.macAddress) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                "   CAST(d.deviceId AS string) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                "(:status IS NULL OR d.deviceStatus = :status) AND " +
                "(:subscription IS NULL OR d.subscriptionType = :subscription) AND " +
                "(CAST(:registeredFrom AS java.time.LocalDateTime) IS NULL OR d.registeredAt >= :registeredFrom) AND " +
                "(CAST(:registeredTo AS java.time.LocalDateTime) IS NULL OR d.registeredAt <= :registeredTo) AND " +
                "(CAST(:expiresFrom AS java.time.LocalDateTime) IS NULL OR d.expiresAt >= :expiresFrom) AND " +
                "(CAST(:expiresTo AS java.time.LocalDateTime) IS NULL OR d.expiresAt <= :expiresTo)")
        Page<Device> searchResellerUsers(
                @Param("resellerId") UUID resellerId,
                @Param("search") String search,
                @Param("status") DeviceStatus status,
                @Param("subscription") SubscriptionType subscription,
                @Param("registeredFrom") java.time.LocalDateTime registeredFrom,
                @Param("registeredTo") java.time.LocalDateTime registeredTo,
                @Param("expiresFrom") java.time.LocalDateTime expiresFrom,
                @Param("expiresTo") java.time.LocalDateTime expiresTo,
                Pageable pageable);

        long countByRegisteredAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

        long countByResellerIdAndRegisteredAtBetween(UUID resellerId, java.time.LocalDateTime from,
                        java.time.LocalDateTime to);
}
