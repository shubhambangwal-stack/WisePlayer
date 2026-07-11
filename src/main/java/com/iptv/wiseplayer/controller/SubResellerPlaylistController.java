package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.OwnerType;
import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
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
@RequestMapping("/api/sub-reseller/playlists")
@Tag(name = "Sub-Reseller Playlist Management", description = "Endpoints for sub-resellers to manage playlists")
public class SubResellerPlaylistController {

    private final ResellerPlaylistService resellerPlaylistService;
    private final AdminRepository adminRepository;
    private final com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository;

    public SubResellerPlaylistController(ResellerPlaylistService resellerPlaylistService, AdminRepository adminRepository, com.iptv.wiseplayer.repository.SuperAdminRepository superAdminRepository) {
        this.resellerPlaylistService = resellerPlaylistService;
        this.adminRepository = adminRepository;
        this.superAdminRepository = superAdminRepository;
    }

    private UUID getCurrentSubResellerId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return adminRepository.findByUsername(username)
                .map(Admin::getId)
                .or(() -> superAdminRepository.findByUsername(username)
                        .map(com.iptv.wiseplayer.domain.entity.SuperAdmin::getId))
                .orElseThrow(() -> new ResourceNotFoundException("Sub-Reseller not found for: " + username));
    }

    @GetMapping
    @Operation(summary = "Get sub-reseller playlists")
    public ResponseEntity<List<PlaylistResponse>> getPlaylists() {
        return ResponseEntity.ok(resellerPlaylistService.getPlaylists(getCurrentSubResellerId(), OwnerType.SUB_RESELLER));
    }

    @PostMapping("/xtream")
    @Operation(summary = "Create Xtream playlist")
    public ResponseEntity<?> createXtreamPlaylist(@Valid @RequestBody XtreamPlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.createXtreamPlaylist(getCurrentSubResellerId(), OwnerType.SUB_RESELLER, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PostMapping("/m3u")
    @Operation(summary = "Create M3U playlist")
    public ResponseEntity<?> createM3uPlaylist(@Valid @RequestBody M3uPlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.createM3uPlaylist(getCurrentSubResellerId(), OwnerType.SUB_RESELLER, request);
        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @PutMapping("/{playlistId}/assign")
    @Operation(summary = "Assign playlist to a device")
    public ResponseEntity<?> assignPlaylist(@PathVariable UUID playlistId, @Valid @RequestBody AssignPlaylistRequest request) {
        PlaylistResponse response = resellerPlaylistService.assignPlaylist(getCurrentSubResellerId(), OwnerType.SUB_RESELLER, playlistId, request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist assigned successfully", "data", response));
    }

    @DeleteMapping("/{playlistId}")
    @Operation(summary = "Delete playlist")
    public ResponseEntity<?> deletePlaylist(@PathVariable UUID playlistId) {
        resellerPlaylistService.deletePlaylist(getCurrentSubResellerId(), OwnerType.SUB_RESELLER, playlistId);
        return ResponseEntity.ok(Map.of("success", true, "message", "Playlist deleted successfully"));
    }
}
