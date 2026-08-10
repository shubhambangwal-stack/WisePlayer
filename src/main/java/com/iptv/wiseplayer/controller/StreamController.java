package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(StreamController.class);
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

        log.info("Stream playback authorization request received. streamId: {}, playlistId: {}", streamId, playlistIdStr);

        if (streamId == null || streamId.isEmpty()) {
            log.warn("Authorization failed: streamId is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "streamId is required"));
        }
        if (playlistIdStr == null || playlistIdStr.isEmpty()) {
            log.warn("Authorization failed: playlistId is missing or empty");
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "playlistId is required"));
        }

        UUID playlistId = UUID.fromString(playlistIdStr);
        try {
            // Authorize to ensure they have access
            String resolvedDirectUrl = streamService.authorizeAndGetUrl(deviceContext.getCurrentDeviceId(), playlistId, streamId);
            log.info("Stream authorized successfully. Upstream URL resolved: {}", resolvedDirectUrl);
        } catch (Exception e) {
            log.error("Stream authorization failed for device {} on playlist {}: {}", 
                    deviceContext.getCurrentDeviceId(), playlistId, e.getMessage(), e);
            throw e;
        }
        
        String token = request.getHeader("X-Device-Token");
        String fingerprint = request.getHeader("X-Device-Fingerprint");
        log.info("Auth headers retrieved - token present: {}, fingerprint present: {}", (token != null), (fingerprint != null));
        
        String proxyUrl = org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/stream/proxy")
                .queryParam("playlistId", playlistIdStr)
                .queryParam("streamId", streamId)
                .queryParam("token", token)
                .queryParam("fingerprint", fingerprint)
                .toUriString();

        log.info("Returning generated proxy URL: {}", proxyUrl);
        return ResponseEntity.ok(Map.of("success", true, "url", proxyUrl));
    }

    @Operation(summary = "Proxy Stream Playback", description = "Proxies the stream to bypass User-Agent restrictions.")
    @GetMapping("/proxy")
    public void proxyStream(@RequestParam("playlistId") UUID playlistId,
                            @RequestParam("streamId") String streamId,
                            jakarta.servlet.http.HttpServletResponse response) {
        log.info("Proxy request triggered for playlistId: {}, streamId: {}", playlistId, streamId);
        String upstreamUrl = "";
        try {
            upstreamUrl = streamService.authorizeAndGetUrl(deviceContext.getCurrentDeviceId(), playlistId, streamId);
            log.info("Upstream URL for proxy resolved: {}", upstreamUrl);
        } catch (Exception e) {
            log.error("Failed to authorize stream for proxy playback: {}", e.getMessage(), e);
            response.setStatus(403);
            return;
        }
        
        try {
            java.net.URL url = new java.net.URL(upstreamUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "okhttp/4.9.0");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);
            log.info("Connecting to upstream stream: {}", upstreamUrl);
            connection.connect();
            
            int responseCode = connection.getResponseCode();
            log.info("Upstream response code: {}", responseCode);
            response.setStatus(responseCode);
            
            String contentType = connection.getContentType();
            log.info("Upstream content type: {}", contentType);
            if (contentType != null) {
                response.setContentType(contentType);
            }
            
            int totalBytesPiped = 0;
            try (java.io.InputStream in = connection.getInputStream();
                 java.io.OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    totalBytesPiped += bytesRead;
                }
                out.flush();
                log.info("Stream proxying completed successfully. Total bytes piped: {}", totalBytesPiped);
            } catch (Exception streamEx) {
                log.warn("Stream piping interrupted or finished. Bytes piped so far: {}. Error: {}", 
                        totalBytesPiped, streamEx.getMessage());
            }
        } catch (Exception e) {
            log.error("Error proxying stream from upstream: {}", upstreamUrl, e);
            response.setStatus(500);
        }
    }
}
