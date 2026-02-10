package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for playlist management.
 * Protected by DeviceSecurityFilter.
 */
@RestController
@RequestMapping("/api/playlist")
@Tag(name = "Playlist Management", description = "Endpoints for adding, validating, and retrieving M3U and Xtream playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final DeviceContext deviceContext;

    public PlaylistController(PlaylistService playlistService, DeviceContext deviceContext) {
        this.playlistService = playlistService;
        this.deviceContext = deviceContext;
    }

    @Operation(summary = "Save Xtream Playlist", description = "Validates and saves a new Xtream Codes playlist.")
    @PostMapping("/xtream")
    public ResponseEntity<?> saveXtreamPlaylist(@RequestBody XtreamPlaylistRequest request) {
        playlistService.saveXtreamPlaylist(deviceContext.getCurrentDeviceId(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Xtream playlist saved successfully"));
    }

    @Operation(summary = "Save M3U Playlist", description = "Validates and saves a new M3U playlist URL.")
    @PostMapping("/m3u")
    public ResponseEntity<?> saveM3uPlaylist(@RequestBody M3uPlaylistRequest request) {
        playlistService.saveM3uPlaylist(deviceContext.getCurrentDeviceId(), request);
        return ResponseEntity.ok(Map.of("success", true, "message", "M3U playlist saved successfully"));
    }

    @Operation(summary = "Get All Playlists", description = "Retrieves all saved playlists for the authenticated device.")
    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getPlaylists() {
        List<PlaylistResponse> response = playlistService.getPlaylists(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Validate Playlist", description = "Validates playlist credentials without saving.")
    @PostMapping("/validate")
    public ResponseEntity<?> validatePlaylist(@RequestBody Map<String, Object> payload) {
        String type = (String) payload.get("type");
        if ("XTREAM".equalsIgnoreCase(type)) {
            com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest request = new com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest();
            request.setName((String) payload.get("name"));
            request.setServerUrl((String) payload.get("serverUrl"));
            request.setUsername((String) payload.get("username"));
            request.setPassword((String) payload.get("password"));
            playlistService.validatePlaylist(deviceContext.getCurrentDeviceId(), request);
        } else if ("M3U".equalsIgnoreCase(type)) {
            com.iptv.wiseplayer.dto.request.M3uPlaylistRequest request = new com.iptv.wiseplayer.dto.request.M3uPlaylistRequest();
            request.setName((String) payload.get("name"));
            request.setM3uUrl((String) payload.get("m3uUrl"));
            playlistService.validatePlaylist(deviceContext.getCurrentDeviceId(), request);
        } else {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Invalid playlist type"));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist validated and saved successfully"));
    }
}
