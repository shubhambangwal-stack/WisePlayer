package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.WatchProgress;
import com.iptv.wiseplayer.dto.request.WatchProgressRequest;
import com.iptv.wiseplayer.dto.response.WatchProgressResponse;
import com.iptv.wiseplayer.repository.WatchProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for the Catchup / Resume feature.
 *
 * <h3>Design decisions</h3>
 * <ul>
 *   <li><b>Upsert, not insert</b> – there is always exactly one row per
 *       (playlistId, streamId, streamType). The {@code saveProgress} method
 *       either creates or overwrites that row.</li>
 *   <li><b>95 % threshold</b> – when {@code positionSeconds / durationSeconds ≥ 0.95}
 *       the entry is deleted so the next play starts from the beginning (exactly like
 *       YouTube). The caller receives {@code fullyWatched = true} in the response.</li>
 *   <li><b>No history log</b> – this service intentionally stores only the LAST position,
 *       never an append-only history table.</li>
 * </ul>
 */
@Service
public class WatchProgressService {

    /** The threshold at which content is considered "fully watched". */
    private static final double FULLY_WATCHED_THRESHOLD = 0.95;

    private final WatchProgressRepository watchProgressRepository;

    public WatchProgressService(WatchProgressRepository watchProgressRepository) {
        this.watchProgressRepository = watchProgressRepository;
    }

    // ── Save / Update ──────────────────────────────────────────────────────────

    /**
     * Saves (upserts) the current playback position for a VOD or Series episode.
     *
     * <p>If the watched percentage crosses the 95 % threshold, the row is
     * removed from the database and a "fully watched" response is returned.
     * The next call to {@link #getProgress} for the same stream will then return
     * an empty Optional, causing the frontend to start from position 0.</p>
     *
     * @param playlistId the playlist that owns the playback session
     * @param request    the progress payload from the client
     * @return a {@link WatchProgressResponse} with the final state
     */
    @Transactional
    public WatchProgressResponse saveProgress(UUID playlistId, WatchProgressRequest request) {
        int streamId = request.getStreamId();
        String streamType = request.getStreamType();
        long positionSeconds = request.getPositionSeconds();
        long durationSeconds = request.getDurationSeconds();

        double watchedPercent = (durationSeconds > 0)
                ? ((double) positionSeconds / durationSeconds) * 100.0
                : 0.0;

        // ── Fully-watched: clean up and respond ────────────────────────────────
        if (watchedPercent >= FULLY_WATCHED_THRESHOLD * 100) {
            watchProgressRepository.deleteByPlaylistIdAndStreamIdAndStreamType(
                    playlistId, streamId, streamType);
            return new WatchProgressResponse(0L, durationSeconds, watchedPercent, true);
        }

        // ── Upsert the single row ──────────────────────────────────────────────
        WatchProgress progress = watchProgressRepository
                .findByPlaylistIdAndStreamIdAndStreamType(playlistId, streamId, streamType)
                .orElseGet(WatchProgress::new);

        progress.setPlaylistId(playlistId);
        progress.setStreamId(streamId);
        progress.setStreamType(streamType);
        progress.setPositionSeconds(positionSeconds);
        progress.setDurationSeconds(durationSeconds);
        progress.setFullyWatched(false);

        watchProgressRepository.save(progress);

        return new WatchProgressResponse(positionSeconds, durationSeconds, watchedPercent, false);
    }

    // ── Single lookup ──────────────────────────────────────────────────────────

    /**
     * Returns the saved progress for a single stream, or {@link Optional#empty()}
     * if the user has never started watching (or already finished it).
     *
     * @param playlistId the playlist context
     * @param streamId   Xtream stream / episode ID
     * @param streamType {@code "VOD"} or {@code "SERIES"}
     */
    @Transactional(readOnly = true)
    public Optional<WatchProgressResponse> getProgress(UUID playlistId, int streamId, String streamType) {
        return watchProgressRepository
                .findByPlaylistIdAndStreamIdAndStreamType(playlistId, streamId, streamType)
                .map(this::toResponse);
    }

    // ── Bulk lookup ────────────────────────────────────────────────────────────

    /**
     * Returns a map of {@code streamId → WatchProgressResponse} for a collection
     * of stream IDs. Stream IDs with no saved progress are simply absent from the map.
     *
     * <p>Used internally by {@link com.iptv.wiseplayer.service.CatchupEnrichmentService}
     * to annotate VOD / Series listings with progress data in a single DB round-trip.</p>
     *
     * @param playlistId the playlist context
     * @param streamIds  the set of stream IDs to query
     * @param streamType {@code "VOD"} or {@code "SERIES"}
     * @return map of streamId → progress (only entries with saved progress are included)
     */
    @Transactional(readOnly = true)
    public Map<Integer, WatchProgressResponse> getBulkProgress(
            UUID playlistId, Collection<Integer> streamIds, String streamType) {

        if (streamIds == null || streamIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<WatchProgress> rows = watchProgressRepository
                .findByPlaylistIdAndStreamIdInAndStreamType(playlistId, streamIds, streamType);

        return rows.stream().collect(Collectors.toMap(
                WatchProgress::getStreamId,
                this::toResponse
        ));
    }

    // ── Mapper ─────────────────────────────────────────────────────────────────

    private WatchProgressResponse toResponse(WatchProgress wp) {
        double watchedPercent = (wp.getDurationSeconds() > 0)
                ? ((double) wp.getPositionSeconds() / wp.getDurationSeconds()) * 100.0
                : 0.0;
        return new WatchProgressResponse(
                wp.getPositionSeconds(),
                wp.getDurationSeconds(),
                watchedPercent,
                wp.isFullyWatched()
        );
    }
}
