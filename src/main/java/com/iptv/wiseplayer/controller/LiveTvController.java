package com.iptv.wiseplayer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.LiveTvService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller for Live TV browsing.
 */
@RestController
@RequestMapping("/api/live")
@Tag(name = "Live TV", description = "Endpoints for browsing Live TV categories, channels, and streams")
public class LiveTvController {

    private final LiveTvService liveTvService;
    private final com.iptv.wiseplayer.service.iptv.XtreamStreamResolver streamResolver;
    private final DeviceContext deviceContext;

    public LiveTvController(LiveTvService liveTvService,
            com.iptv.wiseplayer.service.iptv.XtreamStreamResolver streamResolver,
            DeviceContext deviceContext) {
        this.liveTvService = liveTvService;
        this.streamResolver = streamResolver;
        this.deviceContext = deviceContext;
    }

    @Operation(summary = "Handle Live TV Request", description = "Dispatches request based on parameters: categories, streams, or play url.")
    @GetMapping
    public ResponseEntity<?> handleRequest(
            @RequestParam UUID playlistId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) Integer streamId) {

        // 1. Play Stream (if streamId is present)
        if (streamId != null) {
            String url = streamResolver.resolveStreamUrl(playlistId, streamId,
                    com.iptv.wiseplayer.service.iptv.XtreamStreamResolver.StreamType.LIVE);
            return ResponseEntity.ok(java.util.Map.of("url", url));
        }

        // 2. Get Channels/Streams (if categoryId is present)
        if (categoryId != null) {
            JsonNode channels = liveTvService.getChannels(deviceContext.getCurrentDeviceId(), playlistId, categoryId);
            return ResponseEntity.ok(channels);
        }

        // 3. Default: Get Categories
        JsonNode categories = liveTvService.getCategories(deviceContext.getCurrentDeviceId(), playlistId);
        return ResponseEntity.ok(categories);
    }
}
