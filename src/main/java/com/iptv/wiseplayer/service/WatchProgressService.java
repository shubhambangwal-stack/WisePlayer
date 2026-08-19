package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.WatchProgress;
import com.iptv.wiseplayer.dto.request.WatchProgressRequest;
import com.iptv.wiseplayer.dto.response.WatchProgressResponse;
import com.iptv.wiseplayer.exception.DeviceAuthenticationException;
import com.iptv.wiseplayer.repository.WatchProgressRepository;
import com.iptv.wiseplayer.security.DeviceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <h3>Why device_id and NOT playlist_id</h3>
 * <p>
 * Multiple physical devices (different MAC addresses) can share the same
 * playlist. If we keyed progress on {@code playlist_id}, all those devices
 * would overwrite each other's watch position. Using {@code device_id} — taken
 * directly from the authenticated JWT via {@link DeviceContext} — guarantees
 * the key is always unique per physical device.
 * </p>
 *
 * <h3>Other design decisions</h3>
 * <ul>
 * <li><b>Upsert, not insert</b> – always exactly one row per (device_id,
 * stream_id, stream_type).</li>
 * <li><b>95 % threshold</b> – when position/duration ≥ 0.95 the row is deleted;
 * the next play starts from the beginning.</li>
 * <li><b>No history log</b> – only the LAST position is stored, never an
 * append-only log.</li>
 * </ul>
 */
@Service
public class WatchProgressService {

        private static final Logger log = LoggerFactory.getLogger(WatchProgressService.class);

        /** Content is "fully watched" once 95 % or more has been seen. */
        private static final double FULLY_WATCHED_THRESHOLD = 0.95;

        private final WatchProgressRepository watchProgressRepository;
        private final DeviceContext deviceContext;

        public WatchProgressService(WatchProgressRepository watchProgressRepository,
                        DeviceContext deviceContext) {
                this.watchProgressRepository = watchProgressRepository;
                this.deviceContext = deviceContext;
        }

        // ── Save / Update ──────────────────────────────────────────────────────────

        /**
         * Saves (upserts) the current playback position for a VOD or Series episode.
         * The device is identified automatically from the authenticated JWT — no
         * client-supplied device or playlist ID is trusted for this lookup.
         *
         * @param request the progress payload from the client
         * @return the persisted progress state
         */
        @Transactional
        public WatchProgressResponse saveProgress(WatchProgressRequest request) {
                UUID deviceId = requireDeviceId();

                int streamId = request.getStreamId();
                String streamType = request.getStreamType();
                long positionSeconds = request.getPositionSeconds();
                long durationSeconds = request.getDurationSeconds();

                double watchedPercent = (durationSeconds > 0)
                                ? ((double) positionSeconds / durationSeconds) * 100.0
                                : 0.0;

                log.debug("[WatchProgress] SAVE → deviceId={}, streamId={}, type={}, position={}s / {}s ({:.1f}%)",
                                deviceId, streamId, streamType, positionSeconds, durationSeconds, watchedPercent);

                // ── Fully-watched: clean up and respond ────────────────────────────────
                if (watchedPercent >= FULLY_WATCHED_THRESHOLD * 100) {
                        log.info("[WatchProgress] FULLY_WATCHED → deviceId={}, streamId={}, type={} — progress row deleted, next play starts from 0",
                                        deviceId, streamId, streamType);
                        watchProgressRepository.deleteByDeviceIdAndStreamIdAndStreamType(
                                        deviceId, streamId, streamType);
                        return new WatchProgressResponse(0L, durationSeconds, watchedPercent, true);
                }

                // ── Upsert using Postgres native query (prevents 409 Conflict race conditions) ─
                watchProgressRepository.upsertProgress(UUID.randomUUID(), deviceId, streamId, streamType, positionSeconds, durationSeconds);

                log.info("[WatchProgress] UPSERTED → deviceId={}, streamId={}, type={}, position={}s ({:.1f}%)",
                                deviceId, streamId, streamType, positionSeconds, watchedPercent);

                return new WatchProgressResponse(positionSeconds, durationSeconds, watchedPercent, false);
        }

        // ── Single lookup ──────────────────────────────────────────────────────────

        /**
         * Returns the saved progress for a single stream for the currently
         * authenticated device, or {@link Optional#empty()} if not started / fully
         * watched.
         */
        @Transactional(readOnly = true)
        public Optional<WatchProgressResponse> getProgress(int streamId, String streamType) {
                UUID deviceId = requireDeviceId();
                Optional<WatchProgressResponse> result = watchProgressRepository
                                .findByDeviceIdAndStreamIdAndStreamType(deviceId, streamId, streamType)
                                .map(this::toResponse);
                log.debug("[WatchProgress] GET → deviceId={}, streamId={}, type={}, found={}",
                                deviceId, streamId, streamType, result.isPresent());
                return result;
        }

        // ── Bulk lookup ────────────────────────────────────────────────────────────

        /**
         * Returns a map of {@code streamId → WatchProgressResponse} for a collection
         * of stream IDs belonging to the currently authenticated device. Entries with
         * no saved progress are absent from the map.
         *
         * <p>
         * Used by {@link CatchupEnrichmentService} to annotate VOD/Series listings
         * in a single DB round-trip.
         * </p>
         */
        @Transactional(readOnly = true)
        public Map<Integer, WatchProgressResponse> getBulkProgress(
                        UUID deviceId, Collection<Integer> streamIds, String streamType) {

                if (streamIds == null || streamIds.isEmpty()) {
                        return Collections.emptyMap();
                }

                List<WatchProgress> rows = watchProgressRepository
                                .findByDeviceIdAndStreamIdInAndStreamType(deviceId, streamIds, streamType);

                log.debug("[WatchProgress] BULK_GET → deviceId={}, type={}, requested={}, found={}",
                                deviceId, streamType, streamIds.size(), rows.size());

                return rows.stream().collect(Collectors.toMap(
                                WatchProgress::getStreamId,
                                this::toResponse));
        }

        // ── Helpers ────────────────────────────────────────────────────────────────

        /**
         * Retrieves the device_id from the current security context.
         * Throws if no authenticated device is present (should never happen in
         * a properly secured endpoint).
         */
        private UUID requireDeviceId() {
                UUID deviceId = deviceContext.getCurrentDeviceId();
                if (deviceId == null) {
                        log.error("[WatchProgress] SECURITY_ERROR → No device_id found in security context. " +
                                        "Check that X-Device-Token is being sent and is valid.");
                        throw new DeviceAuthenticationException("No authenticated device found in security context");
                }
                log.debug("[WatchProgress] Authenticated deviceId={}", deviceId);
                return deviceId;
        }

        private WatchProgressResponse toResponse(WatchProgress wp) {
                double watchedPercent = (wp.getDurationSeconds() > 0)
                                ? ((double) wp.getPositionSeconds() / wp.getDurationSeconds()) * 100.0
                                : 0.0;
                return new WatchProgressResponse(
                                wp.getPositionSeconds(),
                                wp.getDurationSeconds(),
                                watchedPercent,
                                wp.isFullyWatched());
        }
}
