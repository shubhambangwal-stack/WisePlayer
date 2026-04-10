package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Device entity.
 * Provides database access methods for device management.
 */
@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {

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

        org.springframework.data.domain.Page<Device> findAllByResellerId(UUID resellerId,
                        org.springframework.data.domain.Pageable pageable);

        long countByRegisteredAtBetween(java.time.LocalDateTime from, java.time.LocalDateTime to);

        long countByResellerIdAndRegisteredAtBetween(UUID resellerId, java.time.LocalDateTime from,
                        java.time.LocalDateTime to);
}
