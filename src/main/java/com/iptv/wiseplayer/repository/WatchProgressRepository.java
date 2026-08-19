package com.iptv.wiseplayer.repository;

import com.iptv.wiseplayer.domain.entity.WatchProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WatchProgressRepository extends JpaRepository<WatchProgress, UUID> {

    /**
     * Finds the single progress row for a given device + stream combination.
     * Keyed by device_id so two devices sharing the same playlist NEVER collide.
     */
    Optional<WatchProgress> findByDeviceIdAndStreamIdAndStreamType(
            UUID deviceId, int streamId, String streamType);

    /**
     * Bulk lookup: fetches progress for a set of stream IDs in one query.
     * Used when enriching a category / listing response.
     */
    List<WatchProgress> findByDeviceIdAndStreamIdInAndStreamType(
            UUID deviceId, Collection<Integer> streamIds, String streamType);

    /**
     * Deletes a fully-watched entry so the next play restarts from the beginning.
     */
    @Modifying
    @Query("DELETE FROM WatchProgress w WHERE w.deviceId = :deviceId " +
           "AND w.streamId = :streamId AND w.streamType = :streamType")
    void deleteByDeviceIdAndStreamIdAndStreamType(
            @Param("deviceId") UUID deviceId,
            @Param("streamId") int streamId,
            @Param("streamType") String streamType);

    /**
     * Postgres-native UPSERT to prevent race conditions (409 Conflict)
     * when the frontend fires multiple heartbeats simultaneously.
     */
    @Modifying
    @Query(value = "INSERT INTO watch_progress (id, device_id, stream_id, stream_type, position_seconds, duration_seconds, fully_watched, updated_at) " +
                   "VALUES (:id, :deviceId, :streamId, :streamType, :pos, :dur, false, CURRENT_TIMESTAMP) " +
                   "ON CONFLICT (device_id, stream_id, stream_type) " +
                   "DO UPDATE SET position_seconds = :pos, duration_seconds = :dur, fully_watched = false, updated_at = CURRENT_TIMESTAMP",
           nativeQuery = true)
    void upsertProgress(
            @Param("id") UUID id,
            @Param("deviceId") UUID deviceId,
            @Param("streamId") int streamId,
            @Param("streamType") String streamType,
            @Param("pos") long positionSeconds,
            @Param("dur") long durationSeconds);
}
