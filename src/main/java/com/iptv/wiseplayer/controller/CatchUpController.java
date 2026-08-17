package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.dto.iptv.CatchUpChannelStatus;
import com.iptv.wiseplayer.dto.iptv.CatchUpStatus;
import com.iptv.wiseplayer.dto.iptv.EpgResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.iptv.CatchUpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Catch-up / Archive / Timeshift endpoints.
 *
 * <p>All responses are derived strictly from provider data. When a playlist or
 * channel does not actually expose archive data, the API reports "unsupported"
 * instead of fabricating options, so the client can hide/grey out the
 * catch-up UI.
 */
@RestController
@RequestMapping("/api/catchup")
@Tag(name = "Catch-Up / Archive", description = "Endpoints for IPTV catch-up, archive and timeshift playback")
public class CatchUpController {

    private final CatchUpService catchUpService;
    private final PlaylistRepository playlistRepository;
    private final DeviceContext deviceContext;

    public CatchUpController(CatchUpService catchUpService, PlaylistRepository playlistRepository,
            DeviceContext deviceContext) {
        this.catchUpService = catchUpService;
        this.playlistRepository = playlistRepository;
        this.deviceContext = deviceContext;
    }

    @Operation(summary = "Playlist Catch-Up Status",
            description = "Returns whether the playlist provides catch-up/archive data, which method is used, "
                    + "and how many days of catch-up are available.")
    @GetMapping("/status")
    public ResponseEntity<CatchUpStatus> getStatus(@RequestParam UUID playlistId) {
        assertPlaylistOwned(playlistId);
        return ResponseEntity.ok(catchUpService.getPlaylistStatus(playlistId));
    }

    @Operation(summary = "Channel Catch-Up Status",
            description = "Returns per-channel catch-up availability. 'supported' is only true when the provider "
                    + "actually exposes archive data for this channel and a playable catch-up URL can be built.")
    @GetMapping("/channel")
    public ResponseEntity<CatchUpChannelStatus> getChannelStatus(@RequestParam UUID playlistId,
            @RequestParam String channelId) {
        assertPlaylistOwned(playlistId);
        return ResponseEntity.ok(catchUpService.getChannelStatus(playlistId, channelId));
    }

    @Operation(summary = "EPG with Catch-Up",
            description = "Returns the EPG for a channel. When catch-up is available, past programmes are included "
                    + "and flagged with catchupAvailable. Also returns the live edge so the client can jump back "
                    + "to live playback.")
    @GetMapping("/epg")
    public ResponseEntity<EpgResponse> getEpg(@RequestParam UUID playlistId,
            @RequestParam String channelId,
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end) {
        assertPlaylistOwned(playlistId);
        return ResponseEntity.ok(catchUpService.getEpg(playlistId, channelId, start, end));
    }

    @Operation(summary = "Resolve Catch-Up Stream URL",
            description = "Returns the playable catch-up stream URL for a programme window. Returns an error when "
                    + "catch-up is not available or the requested time is outside the catch-up window, so the "
                    + "client can fall back gracefully to the live stream.")
    @GetMapping("/play")
    public ResponseEntity<Map<String, Object>> resolvePlayUrl(@RequestParam UUID playlistId,
            @RequestParam String channelId,
            @RequestParam long start,
            @RequestParam long end,
            @RequestParam(value = "extension", required = false, defaultValue = "ts") String extension) {
        assertPlaylistOwned(playlistId);
        String url = catchUpService.resolveCatchUpUrl(playlistId, channelId, start, end, extension);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "channelId", channelId,
                "start", start,
                "end", end));
    }

    private void assertPlaylistOwned(UUID playlistId) {
        UUID deviceId = deviceContext.getCurrentDeviceId();
        if (deviceId == null) {
            throw new ResourceNotFoundException("Playlist not found or does not belong to your device");
        }
        boolean owned = playlistRepository.findByDeviceId(deviceId).stream()
                .map(Playlist::getId)
                .anyMatch(playlistId::equals);
        if (!owned) {
            throw new ResourceNotFoundException("Playlist not found or does not belong to your device");
        }
    }
}