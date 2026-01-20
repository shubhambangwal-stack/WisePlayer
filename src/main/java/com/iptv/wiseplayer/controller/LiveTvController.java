package com.iptv.wiseplayer.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.LiveTvService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for Live TV browsing.
 */
@RestController
@RequestMapping("/api/live")
public class LiveTvController {

    private final LiveTvService liveTvService;
    private final DeviceContext deviceContext;

    public LiveTvController(LiveTvService liveTvService, DeviceContext deviceContext) {
        this.liveTvService = liveTvService;
        this.deviceContext = deviceContext;
    }

    @GetMapping("/categories")
    public ResponseEntity<JsonNode> getCategories() {
        JsonNode categories = liveTvService.getCategories(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/channels")
    public ResponseEntity<JsonNode> getChannels(@RequestParam String categoryId) {
        JsonNode channels = liveTvService.getChannels(deviceContext.getCurrentDeviceId(), categoryId);
        return ResponseEntity.ok(channels);
    }
}
