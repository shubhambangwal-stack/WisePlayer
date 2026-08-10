package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.StreamService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for stream play authorization.
 */
@RestController
@RequestMapping("/api/stream")
@Tag(name = "Stream Playback", description = "Endpoints for authorizing and resolving stream URLs")
public class StreamController {

    private final StreamService streamService;
    private final DeviceContext deviceContext;

    public StreamController(StreamService streamService, DeviceContext deviceContext) {
        this.streamService = streamService;
        this.deviceContext = deviceContext;
    }

    @Operation(summary = "Authorize Stream Playback", description = "Validates access rights and returns a secure stream proxy URL.")
    @PostMapping("/play")
    public ResponseEntity<?> authorizePlay(@RequestBody Map<String, String> requestBody, jakarta.servlet.http.HttpServletRequest request) {
        String streamId = requestBody.get("streamId");
        String playlistIdStr = requestBody.get("playlistId");

        if (streamId == null || streamId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "streamId is required"));
        }
        if (playlistIdStr == null || playlistIdStr.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "playlistId is required"));
        }

        UUID playlistId = UUID.fromString(playlistIdStr);
        // Authorize to ensure they have access
        streamService.authorizeAndGetUrl(deviceContext.getCurrentDeviceId(), playlistId, streamId);
        
        String token = request.getHeader("X-Device-Token");
        String fingerprint = request.getHeader("X-Device-Fingerprint");
        
        String proxyUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/stream/proxy")
                .queryParam("playlistId", playlistIdStr)
                .queryParam("streamId", streamId)
                .queryParam("token", token)
                .queryParam("fingerprint", fingerprint)
                .toUriString();

        return ResponseEntity.ok(Map.of("success", true, "url", proxyUrl));
    }

    @Operation(summary = "Proxy Stream Playback", description = "Proxies the stream to bypass User-Agent restrictions.")
    @GetMapping("/proxy")
    public void proxyStream(@RequestParam("playlistId") UUID playlistId,
                            @RequestParam("streamId") String streamId,
                            jakarta.servlet.http.HttpServletResponse response) {
        String upstreamUrl = streamService.authorizeAndGetUrl(deviceContext.getCurrentDeviceId(), playlistId, streamId);
        
        try {
            java.net.URL url = new java.net.URL(upstreamUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "okhttp/4.9.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();
            
            response.setStatus(connection.getResponseCode());
            String contentType = connection.getContentType();
            if (contentType != null) {
                response.setContentType(contentType);
            }
            
            try (java.io.InputStream in = connection.getInputStream();
                 java.io.OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                out.flush();
            }
        } catch (Exception e) {
            response.setStatus(500);
        }
    }
}
