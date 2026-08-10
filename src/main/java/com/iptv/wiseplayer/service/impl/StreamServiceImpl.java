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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StreamServiceImpl implements StreamService {

    private static final Logger log = LoggerFactory.getLogger(StreamServiceImpl.class);

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

    @Override
    public String authorizeAndGetUrl(UUID deviceId, UUID playlistId, String streamId) {
        log.info("[STREAM-SVC] authorizeAndGetUrl — deviceId={}, playlistId={}, streamId={}", deviceId, playlistId, streamId);

        // 1. Validate device and subscription
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> {
                    log.warn("[STREAM-SVC] Device not found: {}", deviceId);
                    return new RuntimeException("Device not found");
                });

        log.info("[STREAM-SVC] Device found — status={}", device.getDeviceStatus());

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            log.warn("[STREAM-SVC] Access denied — device {} status is {}", deviceId, device.getDeviceStatus());
            throw new AccessDeniedException("Active subscription required to play content");
        }

        // 2. Fetch playlist
        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getId().equals(playlistId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("[STREAM-SVC] Playlist {} not found for device {}", playlistId, deviceId);
                    return new PlaylistNotFoundException("Playlist not found or does not belong to your device");
                });

        log.info("[STREAM-SVC] Playlist found — name='{}', type={}", playlist.getName(), playlist.getType());

        // 3. Generate URL based on playlist type
        if (playlist.getType() == PlaylistType.XTREAM) {
            String serverUrl = encryptionUtil.decrypt(playlist.getServerUrl());
            String username = encryptionUtil.decrypt(playlist.getUsername());
            String password = encryptionUtil.decrypt(playlist.getPassword());

            // Xtream URL format: http://server:port/live/username/password/stream_id.ts
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            String url = String.format("%s/live/%s/%s/%s.ts", serverUrl, username, password, streamId);
            log.info("[STREAM-SVC] XTREAM → Built stream URL: server={}, streamId={}", serverUrl, streamId);
            return url;

        } else if (playlist.getType() == PlaylistType.M3U) {
            // For M3U, the stream_id passed is already the full URL (as returned by M3uService)
            log.info("[STREAM-SVC] M3U → Using raw streamId as URL: {}", streamId);
            return streamId;
        }

        log.error("[STREAM-SVC] Unsupported playlist type: {} for playlist {}", playlist.getType(), playlistId);
        throw new UnsupportedOperationException("Stream play not supported for this playlist type");
    }
}
