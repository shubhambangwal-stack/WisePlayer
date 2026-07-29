package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

@Repository
public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {

    @Query("SELECT COUNT(a) > 0 FROM AdminAuditLog a WHERE a.performedBy = :performedBy AND a.targetEmail = :targetEmail AND a.action = :action AND a.timestamp >= :since")
    boolean hasRecentAction(@Param("performedBy") UUID performedBy, @Param("targetEmail") String targetEmail, @Param("action") String action, @Param("since") LocalDateTime since);

    /**
     * Returns true if the performer made ANY action of the given type on ANY target since the given time.
     * Used for SuperAdmin who has a UUID (not just an email target) when doing bulk role changes.
     */
    @Query("SELECT COUNT(a) > 0 FROM AdminAuditLog a WHERE a.performedBy = :performedBy AND a.action = :action AND a.timestamp >= :since")
    boolean hasRecentActionByPerformer(@Param("performedBy") UUID performedBy, @Param("action") String action, @Param("since") LocalDateTime since);

    /**
     * Returns all audit log entries for a given performer, ordered by most recent first.
     */
    List<AdminAuditLog> findByPerformedByOrderByTimestampDesc(UUID performedBy);
}
