package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.service.iptv.XtreamAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@RestController
@RequestMapping("/api/xtream")
@Tag(name = "Xtream Codes Integration", description = "Endpoints for interacting with Xtream Codes IPTV providers")
public class XtreamController {

    private final XtreamAuthService authService;

    public XtreamController(XtreamAuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Check Xtream Authentication", description = "Verifies Xtream Codes credentials.")
    @GetMapping("/auth")
    public ResponseEntity<XtreamAuthResponse> checkAuth(@RequestParam UUID playlistId) {
        return ResponseEntity.ok(authService.checkAuth(playlistId));
    }
}
