package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import com.iptv.wiseplayer.service.CatchupEnrichmentService;
import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/movie")
@Tag(name = "Movies (VOD)", description = "Endpoints for browsing VOD categories, streams, and playback")
public class MoviesController {

    private final XtreamCatalogService catalogService;
    private final XtreamStreamResolver streamResolver;
    private final CatchupEnrichmentService enrichmentService;

    public MoviesController(XtreamCatalogService catalogService,
                            XtreamStreamResolver streamResolver,
                            CatchupEnrichmentService enrichmentService) {
        this.catalogService = catalogService;
        this.streamResolver = streamResolver;
        this.enrichmentService = enrichmentService;
    }

    @Operation(summary = "Handle VOD Request", description = "Dispatches request based on parameters: categories, streams, or play url. " +
            "Stream listings are automatically enriched with watch_progress (resume position) when available.")
    @GetMapping
    public ResponseEntity<?> handleRequest(
            @RequestParam UUID playlistId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Integer streamId,
            @RequestParam(required = false) String containerExtension) {
        
        System.out.println("MoviesController received request - playlistId: " + playlistId + ", categoryId: " + categoryId + ", streamId: " + streamId + ", containerExtension: " + containerExtension);

        // 1. Play Stream (if streamId is present)
        if (streamId != null) {
            String url = streamResolver.resolveStreamUrl(playlistId, streamId, XtreamStreamResolver.StreamType.VOD, containerExtension);
            return ResponseEntity.ok(Map.of("url", url));
        }

        // 2. Get VOD Streams (if categoryId is present)
        //    → enriched with per-stream watch_progress automatically
        if (categoryId != null) {
            List<XtreamVodStream> streams = catalogService.getVodStreams(playlistId, categoryId);
            enrichmentService.enrichVodStreams(playlistId, streams);
            return ResponseEntity.ok(streams);
        }

        // 3. Default: Get VOD Categories
        List<XtreamCategory> categories = catalogService.getVodCategories(playlistId);
        return ResponseEntity.ok(categories);
    }
}
