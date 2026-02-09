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
    java.util.List<Device> findByDeviceStatusAndExpiresAtBefore(com.iptv.wiseplayer.domain.enums.DeviceStatus status, java.time.LocalDateTime now);

    /**
     * Find device by device ID.
     *
     * @param deviceId UUID of the device
     * @return Optional containing device if found
     */
    Optional<Device> findByDeviceId(UUID deviceId);
}
