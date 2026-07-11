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
@Tag(name = "Playlist Management", description = "Endpoints for adding, validating, retrieving, and pinning M3U and Xtream playlists")
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

    @Operation(summary = "Get All Playlists", description = "Retrieves all saved playlists for the authenticated device. Pinned playlist is listed first.")
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

    // ── Pin / Unpin Endpoints ────────────────────────────────────────────────

    @Operation(summary = "Pin Playlist", description = "Pins a playlist for the authenticated device. Automatically unpins any previously pinned playlist (one pin per device).")
    @PutMapping("/{playlistId}/pin")
    public ResponseEntity<?> pinPlaylist(@PathVariable java.util.UUID playlistId) {
        PlaylistResponse response = playlistService.pinPlaylist(deviceContext.getCurrentDeviceId(), playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist pinned successfully",
                "data", response));
    }

    @Operation(summary = "Unpin Playlist", description = "Unpins a playlist for the authenticated device.")
    @DeleteMapping("/{playlistId}/pin")
    public ResponseEntity<?> unpinPlaylist(@PathVariable java.util.UUID playlistId) {
        PlaylistResponse response = playlistService.unpinPlaylist(deviceContext.getCurrentDeviceId(), playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist unpinned successfully",
                "data", response));
    }

    @Operation(summary = "Get Pinned Playlist", description = "Returns the currently pinned playlist for the authenticated device, or 404 if none is pinned.")
    @GetMapping("/pinned")
    public ResponseEntity<?> getPinnedPlaylist() {
        return playlistService.getPinnedPlaylist(deviceContext.getCurrentDeviceId())
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of("success", true, "data", p)))
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "No playlist is currently pinned for this device")));
    }

    @Operation(summary = "Pin Public Playlist", description = "Pins a playlist for a public/website device (resolved by MAC, fingerprint, or UUID). Automatically unpins any previously pinned playlist.")
    @PutMapping("/public/{deviceId}/{playlistId}/pin")
    public ResponseEntity<?> pinPublicPlaylist(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId) {
        PlaylistResponse response = playlistService.pinPublicPlaylist(deviceId, playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist pinned successfully",
                "data", response));
    }

    @Operation(summary = "Unpin Public Playlist", description = "Unpins a playlist for a public/website device.")
    @DeleteMapping("/public/{deviceId}/{playlistId}/pin")
    public ResponseEntity<?> unpinPublicPlaylist(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId) {
        PlaylistResponse response = playlistService.unpinPublicPlaylist(deviceId, playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist unpinned successfully",
                "data", response));
    }

    @Operation(summary = "Get Public Pinned Playlist", description = "Returns the currently pinned playlist for a public/website device, or 404 if none is pinned.")
    @GetMapping("/public/{deviceId}/pinned")
    public ResponseEntity<?> getPublicPinnedPlaylist(@PathVariable String deviceId) {
        return playlistService.getPublicPinnedPlaylist(deviceId)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(Map.of("success", true, "data", p)))
                .orElse(ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "No playlist is currently pinned for this device")));
    }
}
