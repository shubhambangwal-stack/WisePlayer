package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.UpdatePlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.domain.enums.OwnerType;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.ResellerPlaylistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reseller/playlists")
@Tag(name = "Reseller Playlist Management", description = "Endpoints for resellers to manage playlists")
public class ResellerPlaylistController {

    private final ResellerPlaylistService resellerPlaylistService;
    private final AdminRepository adminRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;

    public ResellerPlaylistController(ResellerPlaylistService resellerPlaylistService, AdminRepository adminRepository, com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository) {
        this.resellerPlaylistService = resellerPlaylistService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    private UUID getCurrentResellerId() {
        String identifier = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(identifier)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(identifier)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Reseller not found for: " + identifier));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @GetMapping
    @Operation(summary = "Get reseller playlists")
    public ResponseEntity<List<PlaylistResponse>> getPlaylists() {
        return ResponseEntity.ok(resellerPlaylistService.getPlaylists(getCurrentResellerId(), OwnerType.RESELLER));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @GetMapping("/{playlistId}")
    @Operation(summary = "Get playlist by ID")
    public ResponseEntity<?> getPlaylistById(@PathVariable UUID playlistId) {
        PlaylistResponse response = resellerPlaylistService.getPlaylistById(getCurrentResellerId(), OwnerType.RESELLER, playlistId);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PostMapping("/xtream")
    @Operation(summary = "Create Xtream playlist")
    public ResponseEntity<?> createXtreamPlaylist(@Valid @RequestBody XtreamPlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.createXtreamPlaylist(getCurrentResellerId(), OwnerType.RESELLER, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PostMapping("/m3u")
    @Operation(summary = "Create M3U playlist")
    public ResponseEntity<?> createM3uPlaylist(@Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.createM3uPlaylist(getCurrentResellerId(), OwnerType.RESELLER, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PatchMapping("/{playlistId}")
    @Operation(summary = "Update playlist (partial update — only include fields to change)")
    public ResponseEntity<?> updatePlaylist(@PathVariable UUID playlistId,
                                            @RequestBody UpdatePlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.updatePlaylist(getCurrentResellerId(), OwnerType.RESELLER, playlistId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist updated successfully", "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PutMapping("/{playlistId}/assign")
    @Operation(summary = "Assign playlist to a device")
    public ResponseEntity<?> assignPlaylist(@PathVariable UUID playlistId, @Valid @RequestBody AssignPlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.assignPlaylist(getCurrentResellerId(), OwnerType.RESELLER, playlistId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist assigned successfully", "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @DeleteMapping("/{playlistId}/assign")
    @Operation(summary = "Unassign playlist from its current device")
    public ResponseEntity<?> unassignPlaylist(@PathVariable UUID playlistId) {
        PlaylistResponse response = resellerPlaylistService.unassignPlaylist(getCurrentResellerId(), OwnerType.RESELLER, playlistId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist unassigned successfully", "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @PatchMapping("/{playlistId}/pin")
    @Operation(summary = "Toggle pin state of a playlist")
    public ResponseEntity<?> togglePin(@PathVariable UUID playlistId) {
        PlaylistResponse response = resellerPlaylistService.togglePin(getCurrentResellerId(), OwnerType.RESELLER, playlistId);
        String message = response.isPinned() ? "Playlist pinned successfully" : "Playlist unpinned successfully";
        return ResponseEntity.ok(Map.of("success", true, "message", message, "data", response));
    }

    @PreAuthorize("hasAuthority('ROLE_RESELLER')")
    @DeleteMapping("/{playlistId}")
    @Operation(summary = "Delete playlist")
    public ResponseEntity<?> deletePlaylist(@PathVariable UUID playlistId) {
        resellerPlaylistService.deletePlaylist(getCurrentResellerId(), OwnerType.RESELLER, playlistId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist deleted successfully"));
    }
}
