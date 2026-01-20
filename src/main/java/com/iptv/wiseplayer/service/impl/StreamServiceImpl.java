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

    @Override
    public String authorizeAndGetUrl(UUID deviceId, String streamId) {
        // 1. Validate device and subscription
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Active subscription required to play content");
        }

        // 2. Fetch playlist
        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new PlaylistNotFoundException("No playlist found for your device"));

        // 3. Generate URL
        if (playlist.getType() == PlaylistType.XTREAM) {
            String serverUrl = encryptionUtil.decrypt(playlist.getServerUrl());
            String username = encryptionUtil.decrypt(playlist.getUsername());
            String password = encryptionUtil.decrypt(playlist.getPassword());

            // Xtream URL format: http://server:port/live/username/password/stream_id.ts
            // Note: We should ensure serverUrl does not end with /
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            return String.format("%s/live/%s/%s/%s.ts", serverUrl, username, password, streamId);
        }

        // TODO: Handle M3U parsing/lookup
        throw new UnsupportedOperationException("Stream play not yet supported for M3U playlists");
    }
}
