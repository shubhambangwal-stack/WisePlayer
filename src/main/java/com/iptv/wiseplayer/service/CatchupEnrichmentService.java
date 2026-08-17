package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.iptv.XtreamSeriesInfo;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import com.iptv.wiseplayer.dto.response.WatchProgressResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enriches Xtream Codes DTO lists with per-stream watch-progress data
 * so that the frontend receives stream + progress in a single response,
 * without needing a separate {@code GET /api/progress} API call.
 *
 * <p><b>How the frontend uses this:</b></p>
 * <ul>
 *   <li>For VOD streams: each {@link XtreamVodStream} in the list will have
 *       an additional {@code watch_progress} key in its
 *       {@code additionalProperties} map if the user has a saved position.</li>
 *   <li>For Series episodes: each {@link XtreamSeriesInfo.Episode} will have
 *       a {@code watch_progress} key in the same way.</li>
 * </ul>
 *
 * <p>Items with no saved progress simply have no {@code watch_progress} key —
 * the frontend treats their absence as "not started".</p>
 */
@Service
public class CatchupEnrichmentService {

    private static final String PROGRESS_KEY = "watch_progress";

    private final WatchProgressService watchProgressService;

    public CatchupEnrichmentService(WatchProgressService watchProgressService) {
        this.watchProgressService = watchProgressService;
    }

    // ── VOD Enrichment ─────────────────────────────────────────────────────────

    /**
     * Enriches a list of VOD streams with watch-progress data in a single DB round-trip.
     *
     * <p>The progress is injected via the stream's {@code additionalProperties} map
     * (Jackson's {@code @JsonAnySetter} mechanism), which means it will appear as
     * a first-class JSON field called {@code "watch_progress"} in the response.</p>
     *
     * @param playlistId the playlist context (user/device)
     * @param streams    the list of VOD streams to enrich (mutated in place)
     */
    public void enrichVodStreams(UUID playlistId, List<XtreamVodStream> streams) {
        if (streams == null || streams.isEmpty()) return;

        List<Integer> streamIds = streams.stream()
                .map(XtreamVodStream::getStreamId)
                .collect(Collectors.toList());

        Map<Integer, WatchProgressResponse> progressMap =
                watchProgressService.getBulkProgress(playlistId, streamIds, "VOD");

        if (progressMap.isEmpty()) return;

        for (XtreamVodStream stream : streams) {
            WatchProgressResponse progress = progressMap.get(stream.getStreamId());
            if (progress != null) {
                stream.setAdditionalProperty(PROGRESS_KEY, progress);
            }
        }
    }

    // ── Series Episode Enrichment ──────────────────────────────────────────────

    /**
     * Enriches a {@link XtreamSeriesInfo} (all its episodes across all seasons)
     * with watch-progress data in a single DB round-trip.
     *
     * <p>The episode {@code id} field from the Xtream API is a string, but the
     * actual stream ID used for playback must be parsed as an integer. Episodes
     * whose {@code id} cannot be parsed are silently skipped.</p>
     *
     * @param playlistId the playlist context (user/device)
     * @param seriesInfo the full series info object (mutated in place)
     */
    public void enrichSeriesEpisodes(UUID playlistId, XtreamSeriesInfo seriesInfo) {
        if (seriesInfo == null || seriesInfo.getEpisodes() == null) return;

        // Collect all episode IDs across all seasons
        List<Integer> episodeIds = seriesInfo.getEpisodes().values().stream()
                .flatMap(List::stream)
                .map(ep -> parseEpisodeId(ep.getId()))
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (episodeIds.isEmpty()) return;

        Map<Integer, WatchProgressResponse> progressMap =
                watchProgressService.getBulkProgress(playlistId, episodeIds, "SERIES");

        if (progressMap.isEmpty()) return;

        // Inject progress into each episode via additionalProperties
        seriesInfo.getEpisodes().values().forEach(episodeList ->
            episodeList.forEach(episode -> {
                Integer episodeId = parseEpisodeId(episode.getId());
                if (episodeId != null) {
                    WatchProgressResponse progress = progressMap.get(episodeId);
                    if (progress != null) {
                        episode.setAdditionalProperty(PROGRESS_KEY, progress);
                    }
                }
            })
        );
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
