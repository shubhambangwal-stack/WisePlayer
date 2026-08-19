package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.StreamService;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class StreamServiceImpl implements StreamService {

    private final PlaylistRepository playlistRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;

    public StreamServiceImpl(PlaylistRepository playlistRepository,
            DeviceRepository deviceRepository,
            EncryptionUtil encryptionUtil) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StreamServiceImpl.class);

    @Override
    public String authorizeAndGetUrl(UUID deviceId, UUID playlistId, String streamId) {
        log.info("Starting stream authorization. Device: {}, PlaylistId: {}, StreamId: {}", deviceId, playlistId, streamId);

        // 1. Validate device and subscription
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> {
                    log.error("Device not found: {}", deviceId);
                    return new RuntimeException("Device not found");
                });

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            log.warn("Access denied for device {}: Status is {}", deviceId, device.getDeviceStatus());
            throw new AccessDeniedException("Active subscription required to play content");
        }

        // 2. Fetch playlist
        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getId().equals(playlistId))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Playlist {} not found or does not belong to device {}", playlistId, deviceId);
                    return new PlaylistNotFoundException("Playlist not found or does not belong to your device");
                });

        // 3. Generate URL
        if (playlist.getType() == PlaylistType.XTREAM) {
            String serverUrl = encryptionUtil.decrypt(playlist.getServerUrl());
            String username = encryptionUtil.decrypt(playlist.getUsername());
            String password = encryptionUtil.decrypt(playlist.getPassword());

            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            String resolvedUrl = String.format("%s/live/%s/%s/%s.ts", serverUrl, username, password, streamId);
            log.info("Resolved Xtream playlist stream URL: {}", resolvedUrl);
            return resolvedUrl;
        } else if (playlist.getType() == PlaylistType.M3U) {
            log.info("Processing M3U playlist stream. StreamId/URL: {}", streamId);
            
            // If the streamId itself is an M3U stream link containing username & password, convert it to Xtream structure
            if (streamId != null && streamId.contains("username=") && streamId.contains("password=")) {
                try {
                    java.net.URI uri = new java.net.URI(streamId);
                    String query = uri.getQuery();
                    if (query != null) {
                        java.util.Map<String, String> params = new java.util.HashMap<>();
                        for (String pair : query.split("&")) {
                            int idx = pair.indexOf("=");
                            if (idx > 0) {
                                params.put(pair.substring(0, idx), pair.substring(idx + 1));
                            }
                        }
                        
                        if (params.containsKey("username") && params.containsKey("password")) {
                            String host = uri.getScheme() + "://" + uri.getHost();
                            if (uri.getPort() != -1) {
                                host += ":" + uri.getPort();
                            }
                            
                            // Extract stream_id from path if present (e.g. /live/user/pass/123.ts -> 123)
                            String path = uri.getPath();
                            String extractedStreamId = streamId;
                            if (path != null) {
                                String[] segments = path.split("/");
                                if (segments.length > 0) {
                                    String lastSegment = segments[segments.length - 1];
                                    if (lastSegment.contains(".")) {
                                        extractedStreamId = lastSegment.substring(0, lastSegment.indexOf("."));
                                    } else {
                                        extractedStreamId = lastSegment;
                                    }
                                }
                            }
                            
                            String resolvedUrl = String.format("%s/live/%s/%s/%s.ts", host, params.get("username"), params.get("password"), extractedStreamId);
                            log.info("Converted M3U stream URL to direct Xtream format: {}", resolvedUrl);
                            return resolvedUrl;
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse and convert M3U streamId to Xtream format: {}. Error: {}", streamId, e.getMessage());
                }
            }
            
            log.info("Returning original M3U stream URL: {}", streamId);
            return streamId;
        }

        throw new UnsupportedOperationException("Stream play not supported for this playlist type");
    }

    @Override
    public CompletableFuture<String> getTimeshiftUrlAsync(UUID deviceId, UUID playlistId, String channelId, String timestamp, Integer duration, String extension) {
        return null;
    }
}
