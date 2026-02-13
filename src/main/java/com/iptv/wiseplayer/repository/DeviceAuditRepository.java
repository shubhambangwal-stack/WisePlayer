package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.DeviceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceAuditRepository extends JpaRepository<DeviceAuditLog, Long> {
    List<DeviceAuditLog> findByDeviceIdOrderByTimestampDesc(UUID deviceId);
}
