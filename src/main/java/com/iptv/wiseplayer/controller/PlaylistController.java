package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    public ResponseEntity<List<PlaylistResponse>> getPlaylists() {
        List<PlaylistResponse> response = playlistService.getPlaylists(deviceContext.getCurrentDeviceId());
        return ResponseEntity.ok(response);
    }

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
