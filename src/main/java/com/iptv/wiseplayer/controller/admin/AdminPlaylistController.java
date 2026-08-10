package com.iptv.wiseplayer.controller.admin;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.UpdatePlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.service.AdminPlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/playlists")
@Tag(name = "Admin Playlist Management", description = "Admin endpoints for managing playlists across all devices")
public class AdminPlaylistController {

    private final AdminPlaylistService adminPlaylistService;
    private final AdminRepository adminRepository;
    private final SuperAdminRepository superAdminRepository;

    public AdminPlaylistController(AdminPlaylistService adminPlaylistService,
                                   AdminRepository adminRepository,
                                   SuperAdminRepository superAdminRepository) {
        this.adminPlaylistService = adminPlaylistService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    /**
     * Resolves the current admin's UUID from the Spring Security context.
     * Tries AdminRepository first, then SuperAdminRepository.
     */
    private UUID getCurrentAdminId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(identifier)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(identifier)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found for: " + identifier));
    }

    @Operation(summary = "Get all playlists")
    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getAllPlaylists() {
        return ResponseEntity.ok(adminPlaylistService.getAllPlaylists());
    }

    @Operation(summary = "Get playlist by ID")
    @GetMapping("/{playlistId}")
    public ResponseEntity<?> getPlaylistById(@PathVariable UUID playlistId) {
        PlaylistResponse response = adminPlaylistService.getPlaylistById(playlistId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @Operation(summary = "Create Xtream playlist")
    @PostMapping("/xtream")
    public ResponseEntity<?> createXtreamPlaylist(@Valid @RequestBody XtreamPlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.createXtreamPlaylist(getCurrentAdminId(), request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @Operation(summary = "Create M3U playlist")
    @PostMapping("/m3u")
    public ResponseEntity<?> createM3uPlaylist(@Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.createM3uPlaylist(getCurrentAdminId(), request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @Operation(summary = "Update playlist (partial update — only include fields to change)")
    @PatchMapping("/{playlistId}")
    public ResponseEntity<?> updatePlaylist(@PathVariable UUID playlistId,
                                            @RequestBody UpdatePlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.updatePlaylist(playlistId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist updated successfully", "data", response));
    }

    @Operation(summary = "Assign playlist to device")
    @PutMapping("/{playlistId}/assign")
    public ResponseEntity<?> assignPlaylist(@PathVariable UUID playlistId, @Valid @RequestBody AssignPlaylistRequest request) {
        PlaylistResponse response = adminPlaylistService.assignPlaylist(playlistId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist assigned successfully", "data", response));
    }

    @Operation(summary = "Unassign playlist from its current device")
    @DeleteMapping("/{playlistId}/assign")
    public ResponseEntity<?> unassignPlaylist(@PathVariable UUID playlistId) {
        PlaylistResponse response = adminPlaylistService.unassignPlaylist(playlistId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist unassigned successfully", "data", response));
    }

    @Operation(summary = "Toggle pin state of a playlist")
    @PatchMapping("/{playlistId}/pin")
    public ResponseEntity<?> togglePin(@PathVariable UUID playlistId) {
        PlaylistResponse response = adminPlaylistService.togglePin(playlistId);
        String message = response.isPinned() ? "Playlist pinned successfully" : "Playlist unpinned successfully";
        return ResponseEntity.ok(Map.of("success", true, "message", message, "data", response));
    }

    @Operation(summary = "Delete playlist")
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<?> deletePlaylist(@PathVariable UUID playlistId) {
        adminPlaylistService.deletePlaylist(playlistId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist deleted successfully"));
    }
}
