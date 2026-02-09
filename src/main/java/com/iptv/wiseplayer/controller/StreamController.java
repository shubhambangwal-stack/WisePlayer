package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.StreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for stream play authorization.
 */
@RestController
@RequestMapping("/api/stream")
@Tag(name = "Stream Playback", description = "Endpoints for authorizing and resolving stream URLs")
public class StreamController {

    private final StreamService streamService;
    private final DeviceContext deviceContext;

    public StreamController(StreamService streamService, DeviceContext deviceContext) {
        this.streamService = streamService;
        this.deviceContext = deviceContext;
    }

    @Operation(summary = "Authorize Stream Playback", description = "Validates access rights and returns a secure stream URL.")
    @PostMapping("/play")
    public ResponseEntity<?> authorizePlay(@RequestBody Map<String, String> request) {
        String streamId = request.get("streamId");
        String playlistIdStr = request.get("playlistId");

        if (streamId == null || streamId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "streamId is required"));
        }
        if (playlistIdStr == null || playlistIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "playlistId is required"));
        }

        UUID playlistId = UUID.fromString(playlistIdStr);
        String streamUrl = streamService.authorizeAndGetUrl(deviceContext.getCurrentDeviceId(), playlistId, streamId);
        return ResponseEntity.ok(Map.of("success", true, "url", streamUrl));
    }
}
