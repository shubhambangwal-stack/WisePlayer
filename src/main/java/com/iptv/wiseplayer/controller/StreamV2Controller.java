package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.StreamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for V2 stream endpoints, specifically secure IPTV Catchup/Timeshift redirection.
 */
@RestController
@RequestMapping("/api/v2/stream")
@Tag(name = "Stream Playback V2", description = "V2 endpoints for secure stream routing, authorization, and timeshift redirection")
public class StreamV2Controller {

    private final StreamService streamService;
    private final DeviceContext deviceContext;

    public StreamV2Controller(StreamService streamService, DeviceContext deviceContext) {
        this.streamService = streamService;
        this.deviceContext = deviceContext;
    }

    /**
     * Endpoint to request a secure IPTV catchup/timeshift stream via 302 Redirection.
     * Evaluates JWT user session, fetches encrypted provider credentials, checks limits, and redirects.
     * Uses non-blocking CompletableFuture run in a dedicated thread pool to avoid worker starvation.
     *
     * @param playlistId the ID of the playlist containing the stream subscription (Xtream only)
     * @param channelId  the target IPTV channel ID
     * @param timestamp  the requested client time in ISO-8601 string format (e.g. 2026-06-01T11:30:00)
     * @param duration   the requested length of time in minutes (defaults to 60)
     * @param extension  optional target path extension (e.g. ts or m3u8, defaults to ts)
     * @return 302 Found redirecting directly to the upstream IPTV timeshift URL, or standard HTTP error status
     */
    @Operation(
            summary = "Get Catchup/Timeshift Stream Redirect",
            description = "Resolves an IPTV catchup stream URL securely and redirects the client (302) without exposing upstream credentials."
    )
    @GetMapping("/timeshift")
    public CompletableFuture<ResponseEntity<Void>> getTimeshiftRedirect(
            @RequestParam("playlistId") UUID playlistId,
            @RequestParam("channelId") String channelId,
            @RequestParam("timestamp") String timestamp,
            @RequestParam(value = "duration", required = false, defaultValue = "60") Integer duration,
            @RequestParam(value = "extension", required = false, defaultValue = "ts") String extension) {

        UUID deviceId = deviceContext.getCurrentDeviceId();
        if (deviceId == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
            );
        }

        return streamService.getTimeshiftUrlAsync(deviceId, playlistId, channelId, timestamp, duration, extension)
                .thenApply(redirectUrl -> ResponseEntity
                        .status(HttpStatus.FOUND)
                        .location(URI.create(redirectUrl))
                        .build());
    }
}
