package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST Controller for playlist management.
 * Protected by DeviceSecurityFilter.
 */
@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final DeviceContext deviceContext;

    public PlaylistController(PlaylistService playlistService, DeviceContext deviceContext) {
        this.playlistService = playlistService;
        this.deviceContext = deviceContext;
    }

    @PostMapping("/xtream")
    public ResponseEntity<?> saveXtreamPlaylist(@RequestBody XtreamPlaylistRequest request) {
        playlistService.saveXtreamPlaylist(deviceContext.getCurrentDeviceId(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Xtream playlist saved successfully"));
    }

    @PostMapping("/m3u")
    public ResponseEntity<?> saveM3uPlaylist(@RequestBody M3uPlaylistRequest request) {
        playlistService.saveM3uPlaylist(deviceContext.getCurrentDeviceId(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", "M3U playlist saved successfully"));
    }

    @GetMapping
    public ResponseEntity<PlaylistResponse> getPlaylist() {
        PlaylistResponse response = playlistService.getPlaylist(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(response);
    }
}
