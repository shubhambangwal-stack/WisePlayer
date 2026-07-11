package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.security.AdminTokenUtil;
import com.iptv.wiseplayer.service.AdminPlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/playlists")
@Tag(name = "Admin Playlist Management", description = "Admin endpoints for managing playlists across all devices")
public class AdminPlaylistController {

    private final AdminPlaylistService adminPlaylistService;
    private final AdminTokenUtil adminTokenUtil;

    public AdminPlaylistController(AdminPlaylistService adminPlaylistService, AdminTokenUtil adminTokenUtil) {
        this.adminPlaylistService = adminPlaylistService;
        this.adminTokenUtil = adminTokenUtil;
    }

    private UUID getAdminId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return adminTokenUtil.extractAdminId(token);
        }
        throw new RuntimeException("Unauthorized");
    }

    @Operation(summary = "Get all playlists")
    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylists() {
        return ResponseEntity.ok(adminPlaylistService.getAllPlaylists());
    }

    @Operation(summary = "Create Xtream playlist")
    @PostMapping("/xtream")
    public ResponseEntity<?> createXtreamPlaylist(HttpServletRequest httpRequest, @Valid @RequestBody XtreamPlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.createXtreamPlaylist(getAdminId(httpRequest), request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @Operation(summary = "Create M3U playlist")
    @PostMapping("/m3u")
    public ResponseEntity<?> createM3uPlaylist(HttpServletRequest httpRequest, @Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.createM3uPlaylist(getAdminId(httpRequest), request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @Operation(summary = "Assign playlist to device")
    @PutMapping("/{playlistId}/assign")
    public ResponseEntity<?> assignPlaylist(@PathVariable UUID playlistId, @Valid @RequestBody AssignPlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.assignPlaylist(playlistId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist assigned successfully", "data", response));
    }

    @Operation(summary = "Delete playlist")
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<?> deletePlaylist(@PathVariable UUID playlistId) {
        adminPlaylistService.deletePlaylist(playlistId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist deleted successfully"));
    }
}
