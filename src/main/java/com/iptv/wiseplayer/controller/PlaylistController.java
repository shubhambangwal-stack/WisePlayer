package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.PlaylistPinRequest;
import com.iptv.wiseplayer.dto.request.SetDevicePinRequest;
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
import org.springframework.web.bind.annotation.RequestParam;

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

    @Operation(summary = "Get All Playlists", description = "Retrieves all saved playlists for the authenticated device. Pinned playlist is listed first. If a playlist has a PIN set, the correct 4-digit PIN must be supplied.")
    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getPlaylists(@RequestParam(value = "pin", required = false) String pin) {
        List<PlaylistResponse> response = playlistService.getPlaylists(deviceContext.getCurrentDeviceId(), pin);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get All Public Playlists",
        description = "Retrieves all saved playlists for a specific MAC address/device ID. "
            + "If the device has a PIN set, the correct 4-digit PIN must be supplied via the 'pin' query parameter.")
    @GetMapping("/public/{deviceId}")
    public ResponseEntity<?> getPublicPlaylists(
            @PathVariable String deviceId,
            @RequestParam(value = "pin", required = false) String pin) {
        List<PlaylistResponse> response = playlistService.getPublicPlaylistsWithPin(deviceId, pin);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", response));
    }

    @Operation(
        summary = "Set Public Access PIN",
        description = "Sets or updates the 4-digit PIN that protects the public playlist endpoint for this device. "
            + "Requires an active device token. Send an empty-body DELETE to /pin to remove the PIN.")
    @PostMapping("/pin")
    public ResponseEntity<?> setDevicePin(@Valid @RequestBody SetDevicePinRequest request) {
        if (request.getConfirmPin() == null || !request.getConfirmPin().equals(request.getPin())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "PIN and Confirm PIN do not match"));
        }
        playlistService.setDevicePin(deviceContext.getCurrentDeviceId(), request.getPin());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Public access PIN set successfully"));
    }

    @Operation(
        summary = "Remove Public Access PIN",
        description = "Removes the PIN protection from this device's public playlist endpoint.")
    @DeleteMapping("/pin")
    public ResponseEntity<?> removeDevicePin() {
        playlistService.removeDevicePin(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Public access PIN removed successfully"));
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

    @Operation(summary = "Delete Public Playlist",
        description = "Deletes a specific playlist by ID. If the playlist has a PIN set, the correct PIN must be supplied via ?pin= query parameter. Use pin=0000 when no PIN is configured.")
    @DeleteMapping("/public/{deviceId}/{playlistId}")
    public ResponseEntity<?> deletePublicPlaylist(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId,
            @RequestParam(value = "pin", required = false) String pin) {
        playlistService.deletePublicPlaylistWithPin(deviceId, playlistId, pin);
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

    // ── Playlist-level PIN management ────────────────────────────────────

    @Operation(
        summary = "Set Playlist PIN",
        description = "Sets or replaces a 4-digit PIN on a specific playlist for the authenticated device. "
            + "Once set, this PIN is required when deleting the playlist.")
    @PostMapping("/{playlistId}/playlist-pin")
    public ResponseEntity<?> setPlaylistPin(
            @PathVariable java.util.UUID playlistId,
            @Valid @RequestBody PlaylistPinRequest request) {
        if (request.getConfirmPin() == null || !request.getConfirmPin().equals(request.getPin())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "PIN and Confirm PIN do not match"));
        }
        PlaylistResponse response = playlistService.setPlaylistPin(
                deviceContext.getCurrentDeviceId(), playlistId, request.getPin());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist PIN set successfully",
                "data", response));
    }

    @Operation(
        summary = "Remove Playlist PIN",
        description = "Removes the PIN protection from a specific playlist for the authenticated device.")
    @DeleteMapping("/{playlistId}/playlist-pin")
    public ResponseEntity<?> removePlaylistPin(@PathVariable java.util.UUID playlistId) {
        PlaylistResponse response = playlistService.removePlaylistPin(
                deviceContext.getCurrentDeviceId(), playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist PIN removed successfully",
                "data", response));
    }

    @Operation(
        summary = "Verify Playlist PIN",
        description = "Verifies the supplied 4-digit PIN against the playlist's stored PIN. "
            + "Returns {valid: true} when correct. Useful before showing sensitive actions on the client.")
    @PostMapping("/{playlistId}/verify-pin")
    public ResponseEntity<?> verifyPlaylistPin(
            @PathVariable java.util.UUID playlistId,
            @Valid @RequestBody PlaylistPinRequest request) {
        boolean valid = playlistService.verifyPlaylistPin(
                deviceContext.getCurrentDeviceId(), playlistId, request.getPin());
        if (!valid) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "valid", false,
                    "message", "Incorrect PIN"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", true,
                "message", "PIN verified successfully"));
    }

    // ── Public Playlist-level PIN management ───────────────────────────

    @Operation(
        summary = "Set Public Playlist PIN",
        description = "Sets or replaces a 4-digit PIN on a specific public playlist. "
            + "Once set, this PIN is required when deleting the playlist.")
    @PostMapping("/public/{deviceId}/{playlistId}/playlist-pin")
    public ResponseEntity<?> setPublicPlaylistPin(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId,
            @Valid @RequestBody PlaylistPinRequest request) {
        if (request.getConfirmPin() == null || !request.getConfirmPin().equals(request.getPin())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "PIN and Confirm PIN do not match"));
        }
        PlaylistResponse response = playlistService.setPublicPlaylistPin(deviceId, playlistId, request.getPin());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist PIN set successfully",
                "data", response));
    }

    @Operation(
        summary = "Remove Public Playlist PIN",
        description = "Removes the PIN from a specific public playlist.")
    @DeleteMapping("/public/{deviceId}/{playlistId}/playlist-pin")
    public ResponseEntity<?> removePublicPlaylistPin(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId) {
        PlaylistResponse response = playlistService.removePublicPlaylistPin(deviceId, playlistId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Playlist PIN removed successfully",
                "data", response));
    }

    @Operation(
        summary = "Verify Public Playlist PIN",
        description = "Verifies the supplied PIN against a public playlist's stored PIN.")
    @PostMapping("/public/{deviceId}/{playlistId}/verify-pin")
    public ResponseEntity<?> verifyPublicPlaylistPin(
            @PathVariable String deviceId,
            @PathVariable java.util.UUID playlistId,
            @Valid @RequestBody PlaylistPinRequest request) {
        boolean valid = playlistService.verifyPublicPlaylistPin(deviceId, playlistId, request.getPin());
        if (!valid) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "valid", false,
                    "message", "Incorrect PIN"));
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "valid", true,
                "message", "PIN verified successfully"));
    }
}
