package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import com.iptv.wiseplayer.service.iptv.XtreamAuthService;
import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/xtream")
@Tag(name = "Xtream Codes Integration", description = "Endpoints for interacting with Xtream Codes IPTV providers")
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

    @Operation(summary = "Check Xtream Authentication", description = "Verifies Xtream Codes credentials.")
    @GetMapping("/auth")
    public ResponseEntity<XtreamAuthResponse> checkAuth(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(authService.checkAuth(playlistId));
    }
}
