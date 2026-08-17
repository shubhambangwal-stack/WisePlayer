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
     * Finds the single progress row for a given playlist + stream combination.
     * This is the primary lookup used when building the "resume" URL.
     */
    Optional<WatchProgress> findByPlaylistIdAndStreamIdAndStreamType(
            UUID playlistId, int streamId, String streamType);

    /**
     * Bulk lookup: fetches progress for a set of stream IDs in one query.
     * Used when enriching a category / listing response (e.g. VOD stream list).
     */
    List<WatchProgress> findByPlaylistIdAndStreamIdInAndStreamType(
            UUID playlistId, Collection<Integer> streamIds, String streamType);

    /**
     * Deletes a fully-watched entry so the next play restarts from the beginning.
     * Called inside {@link com.iptv.wiseplayer.service.WatchProgressService#saveProgress}
     * when the watched percentage crosses the 95 % threshold.
     */
    @Modifying
    @Query("DELETE FROM WatchProgress w WHERE w.playlistId = :playlistId " +
           "AND w.streamId = :streamId AND w.streamType = :streamType")
    void deleteByPlaylistIdAndStreamIdAndStreamType(
            @Param("playlistId") UUID playlistId,
            @Param("streamId") int streamId,
            @Param("streamType") String streamType);
}
