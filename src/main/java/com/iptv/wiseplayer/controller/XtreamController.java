package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import com.iptv.wiseplayer.service.iptv.XtreamAuthService;
import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/xtream")
@Tag(name = "Xtream Codes Integration", description = "Endpoints for interacting with Xtream Codes IPTV providers")
public class XtreamController {

    private final XtreamAuthService authService;
    private final XtreamCatalogService catalogService;
    private final XtreamStreamResolver streamResolver;

    public XtreamController(XtreamAuthService authService,
            XtreamCatalogService catalogService,
            XtreamStreamResolver streamResolver) {
        this.authService = authService;
        this.catalogService = catalogService;
        this.streamResolver = streamResolver;
    }

    @Operation(summary = "Check Xtream Authentication", description = "Verifies Xtream Codes credentials.")
    @GetMapping("/auth")
    public ResponseEntity<XtreamAuthResponse> checkAuth(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(authService.checkAuth(playlistId));
    }

    @Operation(summary = "Get Live TV Categories", description = "Retrieves live TV categories from Xtream provider.")
    @GetMapping("/categories")
    public ResponseEntity<List<XtreamCategory>> getCategories(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(catalogService.getLiveCategories(playlistId));
    }

    @Operation(summary = "Get Live Streams", description = "Retrieves live streams for a specific category.")
    @GetMapping("/streams")
    public ResponseEntity<List<XtreamLiveStream>> getStreams(@RequestParam UUID playlistId,
            @RequestParam String categoryId) {
        return ResponseEntity.ok(catalogService.getLiveStreams(playlistId, categoryId));
    }

    @Operation(summary = "Get VOD Categories", description = "Retrieves VOD (Video on Demand) categories.")
    @GetMapping("/vod/categories")
    public ResponseEntity<List<XtreamCategory>> getVodCategories(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(catalogService.getVodCategories(playlistId));
    }

    @Operation(summary = "Get VOD Streams", description = "Retrieves VOD streams for a specific category.")
    @GetMapping("/vod/streams")
    public ResponseEntity<List<XtreamVodStream>> getVodStreams(@RequestParam UUID playlistId,
            @RequestParam String categoryId) {
        return ResponseEntity.ok(catalogService.getVodStreams(playlistId, categoryId));
    }

    @Operation(summary = "Get Series Categories", description = "Retrieves Series categories.")
    @GetMapping("/series/categories")
    public ResponseEntity<List<XtreamCategory>> getSeriesCategories(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(catalogService.getSeriesCategories(playlistId));
    }

    @Operation(summary = "Get Series", description = "Retrieves Series for a specific category.")
    @GetMapping("/series")
    public ResponseEntity<List<XtreamSeries>> getSeries(@RequestParam UUID playlistId,
            @RequestParam String categoryId) {
        return ResponseEntity.ok(catalogService.getSeries(playlistId, categoryId));
    }

    @Operation(summary = "Get Stream Play URL", description = "Resolves the playback URL for a specific stream ID.")
    @GetMapping("/play")
    public ResponseEntity<?> getPlayUrl(@RequestParam UUID playlistId,
            @RequestParam int streamId) {
        String url = streamResolver.resolveStreamUrl(playlistId, streamId);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
