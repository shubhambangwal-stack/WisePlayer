package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.PlaylistService;
import jakarta.validation.Valid;
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
    public ResponseEntity<?> saveXtreamPlaylist(@Valid @RequestBody XtreamPlaylistRequest request) {
        PlaylistResponse response = playlistService.saveXtreamPlaylist(deviceContext.getCurrentDeviceId(), request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Xtream playlist validated and saved successfully",
                "data", response));
    }

    @Operation(summary = "Save M3U Playlist", description = "Validates and saves a new M3U playlist URL.")
    @PostMapping("/m3u")
    public ResponseEntity<?> saveM3uPlaylist(@Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = playlistService.saveM3uPlaylist(deviceContext.getCurrentDeviceId(), request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "M3U playlist validated and saved successfully",
                "data", response));
    }

    @Operation(summary = "Save Public M3U Playlist", description = "Public endpoint for website users to save M3U playlists.")
    @PostMapping("/public/{deviceId}/m3u")
    public ResponseEntity<?> savePublicM3uPlaylist(
            @PathVariable String deviceId,
            @Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = playlistService.savePublicM3uPlaylist(deviceId, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "M3U playlist validated and uploaded successfully",
                "data", response));
    }

    @Operation(summary = "Get All Playlists", description = "Retrieves all saved playlists for the authenticated device.")
    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getPlaylists() {
        List<PlaylistResponse> response = playlistService.getPlaylists(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get All Public Playlists", description = "Retrieves all saved playlists for a specific MAC address/device ID.")
    @GetMapping("/public/{deviceId}")
    public ResponseEntity<?> getPublicPlaylists(@PathVariable String deviceId) {
        List<PlaylistResponse> response = playlistService.getPublicPlaylists(deviceId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response));
    }

    @Operation(summary = "Update Public M3U Playlist", description = "Updates an existing M3U playlist by ID for a specific MAC address/device ID.")
    @PutMapping("/public/{deviceId}/{playlistId}")
    public ResponseEntity<?> updatePublicM3uPlaylist(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId,
            @Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = playlistService.updatePublicM3uPlaylist(deviceId, playlistId, request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "M3U playlist updated successfully",
                "data", response));
    }

    @Operation(summary = "Delete Public Playlist", description = "Deletes a specific playlist by ID for a MAC address/device ID.")
    @DeleteMapping("/public/{deviceId}/{playlistId}")
    public ResponseEntity<?> deletePublicPlaylist(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId) {
        playlistService.deletePublicPlaylist(deviceId, playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist deleted successfully"));
    }
}
