package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.dto.iptv.XtreamSeriesInfo;
import com.iptv.wiseplayer.service.CatchupEnrichmentService;
import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.iptv.wiseplayer.service.WatchProgressService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/series")
@Tag(name = "Series", description = "Endpoints for browsing Series categories, content, and playback")
public class SeriesController {

    private final XtreamCatalogService catalogService;
    private final XtreamStreamResolver streamResolver;
    private final CatchupEnrichmentService enrichmentService;
    private final WatchProgressService watchProgressService;

    public SeriesController(XtreamCatalogService catalogService,
                            XtreamStreamResolver streamResolver,
                            CatchupEnrichmentService enrichmentService,
                            WatchProgressService watchProgressService) {
        this.catalogService = catalogService;
        this.streamResolver = streamResolver;
        this.enrichmentService = enrichmentService;
        this.watchProgressService = watchProgressService;
    }

    /**
     * Main dispatch endpoint:
     * - No params              → Series categories list
     * - categoryId only        → Series list for that category
     * - seriesId only          → Full series info (seasons + episodes), with watch_progress
     *                            injected into every episode that has a saved position.
     */
    @Operation(
        summary = "Browse Series",
        description = "Dispatches request based on parameters: " +
            "(none) returns categories, categoryId returns series list, " +
            "seriesId returns seasons/episodes enriched with watch_progress (resume positions)."
    )
    @GetMapping
    public ResponseEntity<?> handleRequest(
            @RequestParam UUID playlistId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Integer seriesId) {

        // 1. Get full Series info with seasons/episodes (if seriesId is present)
        //    → episodes are enriched with per-episode watch_progress automatically
        if (seriesId != null) {
            XtreamSeriesInfo info = catalogService.getSeriesInfo(playlistId, seriesId);
            if (info == null) {
                return ResponseEntity.notFound().build();
            }
            enrichmentService.enrichSeriesEpisodes(playlistId, info);
            return ResponseEntity.ok(info);
        }

        // 2. Get Series list for a category (if categoryId is present)
        if (categoryId != null) {
            List<XtreamSeries> series = catalogService.getSeries(playlistId, categoryId);
            return ResponseEntity.ok(series);
        }

        // 3. Default: Get Series Categories
        List<XtreamCategory> categories = catalogService.getSeriesCategories(playlistId);
        return ResponseEntity.ok(categories);
    }

    /**
     * Resolves the playback URL for a Series episode.
     * Requires episodeId (the numeric id from the episode in get_series_info)
     * and containerExtension (e.g. "mkv", "mp4") from the episode metadata.
     *
     * URL format: /series/{username}/{password}/{episodeId}.{containerExtension}
     */
    @Operation(
        summary = "Get Episode Playback URL",
        description = "Resolves the direct playback URL for a series episode. " +
            "The episodeId and containerExtension must be taken from the get_series_info response."
    )
    @GetMapping("/play")
    public ResponseEntity<?> getEpisodePlayUrl(
            @RequestParam UUID playlistId,
            @RequestParam int episodeId,
            @RequestParam(required = false, defaultValue = "mkv") String containerExtension) {

        String url = streamResolver.resolveSeriesEpisodeUrl(playlistId, episodeId, containerExtension);
        
        Map<String, Object> response = new HashMap<>();
        response.put("url", url);
        
        watchProgressService.getProgress(playlistId, episodeId, "SERIES")
                .ifPresent(progress -> response.put("watch_progress", progress));

        return ResponseEntity.ok(response);
    }
}