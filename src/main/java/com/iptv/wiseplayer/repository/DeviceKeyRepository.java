package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.DeviceKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceKeyRepository extends JpaRepository<DeviceKey, UUID> {

    Optional<DeviceKey> findByKeyHash(String keyHash);

    @Modifying
    @Query("DELETE FROM DeviceKey dk WHERE dk.expiresAt < :now")
    void deleteByExpiresAtBefore(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM DeviceKey dk WHERE dk.device.deviceId = :deviceId")
    void deleteByDeviceId(UUID deviceId);

    Optional<DeviceKey> findByDeviceDeviceId(UUID deviceId);
}
