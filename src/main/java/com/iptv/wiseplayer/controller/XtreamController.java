package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.service.iptv.XtreamAuthService;
import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/xtream")
public class XtreamController {

    private final XtreamAuthService authService;
    private final XtreamCatalogService catalogService;
    private final XtreamStreamResolver streamResolver;

    public XtreamController(XtreamAuthService authService,
            XtreamCatalogService catalogService,
            XtreamStreamResolver streamResolver) {
        this.authService = authService;
        this.catalogService = catalogService;
        this.streamResolver = streamResolver;
    }

    @GetMapping("/auth")
    public ResponseEntity<XtreamAuthResponse> checkAuth(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(authService.checkAuth(playlistId));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<XtreamCategory>> getCategories(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(catalogService.getLiveCategories(playlistId));
    }

    @GetMapping("/streams")
    public ResponseEntity<List<XtreamLiveStream>> getStreams(@RequestParam UUID playlistId,
            @RequestParam String categoryId) {
        return ResponseEntity.ok(catalogService.getLiveStreams(playlistId, categoryId));
    }

    @GetMapping("/play")
    public ResponseEntity<?> getPlayUrl(@RequestParam UUID playlistId,
            @RequestParam int streamId) {
        String url = streamResolver.resolveStreamUrl(playlistId, streamId);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
