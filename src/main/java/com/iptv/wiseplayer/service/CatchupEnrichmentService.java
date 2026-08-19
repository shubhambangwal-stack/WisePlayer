package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.iptv.XtreamSeriesInfo;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import com.iptv.wiseplayer.dto.response.WatchProgressResponse;
import com.iptv.wiseplayer.security.DeviceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enriches Xtream Codes DTO lists with per-device watch-progress data in a
 * single DB round-trip, so the frontend receives stream + progress in one
 * response without needing a separate API call.
 *
 * <p>The device is identified via {@link DeviceContext#getCurrentDeviceId()},
 * which reads the authenticated JWT. This means the progress is ALWAYS scoped
 * to the physical device making the request — even when multiple devices share
 * the same playlist.</p>
 */
@Service
public class CatchupEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(CatchupEnrichmentService.class);
    private static final String PROGRESS_KEY = "watch_progress";

    private final WatchProgressService watchProgressService;
    private final DeviceContext deviceContext;

    public CatchupEnrichmentService(WatchProgressService watchProgressService,
                                    DeviceContext deviceContext) {
        this.watchProgressService = watchProgressService;
        this.deviceContext = deviceContext;
    }

    // ── VOD Enrichment ─────────────────────────────────────────────────────────

    /**
     * Enriches a list of VOD streams with watch-progress for the currently
     * authenticated device in a single DB round-trip.
     *
     * @param streams the list of VOD streams to enrich (mutated in place)
     */
    public void enrichVodStreams(List<XtreamVodStream> streams) {
        UUID deviceId = deviceContext.getCurrentDeviceId();
        if (deviceId == null || streams == null || streams.isEmpty()) {
            log.debug("[CatchupEnrich] VOD enrichment skipped — deviceId={}, streamCount={}",
                    deviceId, streams == null ? 0 : streams.size());
            return;
        }

        List<Integer> streamIds = streams.stream()
                .map(XtreamVodStream::getStreamId)
                .collect(Collectors.toList());

        Map<Integer, WatchProgressResponse> progressMap =
                watchProgressService.getBulkProgress(deviceId, streamIds, "VOD");

        if (progressMap.isEmpty()) {
            log.debug("[CatchupEnrich] VOD — no saved progress for deviceId={}, {} streams queried",
                    deviceId, streamIds.size());
            return;
        }

        int enrichedCount = 0;
        for (XtreamVodStream stream : streams) {
            WatchProgressResponse progress = progressMap.get(stream.getStreamId());
            if (progress != null) {
                stream.setAdditionalProperty(PROGRESS_KEY, progress);
                enrichedCount++;
            }
        }
        log.info("[CatchupEnrich] VOD — deviceId={}, enriched {}/{} streams with watch_progress",
                deviceId, enrichedCount, streams.size());
    }

    // ── Series Episode Enrichment ──────────────────────────────────────────────

    /**
     * Enriches all episodes in a {@link XtreamSeriesInfo} with watch-progress
     * for the currently authenticated device in a single DB round-trip.
     *
     * @param seriesInfo the full series info object (mutated in place)
     */
    public void enrichSeriesEpisodes(XtreamSeriesInfo seriesInfo) {
        UUID deviceId = deviceContext.getCurrentDeviceId();
        if (deviceId == null || seriesInfo == null || seriesInfo.getEpisodes() == null) {
            log.debug("[CatchupEnrich] Series enrichment skipped — deviceId={}", deviceId);
            return;
        }

        List<Integer> episodeIds = seriesInfo.getEpisodes().values().stream()
                .flatMap(List::stream)
                .map(ep -> parseEpisodeId(ep.getId()))
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (episodeIds.isEmpty()) return;

        Map<Integer, WatchProgressResponse> progressMap =
                watchProgressService.getBulkProgress(deviceId, episodeIds, "SERIES");

        if (progressMap.isEmpty()) {
            log.debug("[CatchupEnrich] SERIES — no saved progress for deviceId={}, {} episodes queried",
                    deviceId, episodeIds.size());
            return;
        }

        int enrichedCount = 0;
        for (Map.Entry<String, List<XtreamSeriesInfo.Episode>> entry : seriesInfo.getEpisodes().entrySet()) {
            for (XtreamSeriesInfo.Episode episode : entry.getValue()) {
                Integer episodeId = parseEpisodeId(episode.getId());
                if (episodeId != null) {
                    WatchProgressResponse progress = progressMap.get(episodeId);
                    if (progress != null) {
                        episode.setAdditionalProperty(PROGRESS_KEY, progress);
                        enrichedCount++;
                    }
                }
            }
        }
        log.info("[CatchupEnrich] SERIES — deviceId={}, enriched {}/{} episodes with watch_progress",
                deviceId, enrichedCount, episodeIds.size());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Integer parseEpisodeId(String id) {
        if (id == null || id.isBlank()) return null;
        try {
            return Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
