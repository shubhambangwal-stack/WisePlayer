package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/playlist")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @PostMapping("/xtream")
    public ResponseEntity<?> saveXtreamPlaylist(@RequestBody XtreamPlaylistRequest request) {
        playlistService.saveXtreamPlaylist(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "Xtream playlist saved successfully"));
    }

    @PostMapping("/m3u")
    public ResponseEntity<?> saveM3uPlaylist(@RequestBody M3uPlaylistRequest request) {
        playlistService.saveM3uPlaylist(request);
        return ResponseEntity.ok(Map.of("success", true, "message", "M3U playlist saved successfully"));
    }

    @GetMapping
    public ResponseEntity<PlaylistResponse> getPlaylist(@RequestParam String deviceId) {
        PlaylistResponse response = playlistService.getPlaylist(deviceId);
        return ResponseEntity.ok(response);
    }
}
