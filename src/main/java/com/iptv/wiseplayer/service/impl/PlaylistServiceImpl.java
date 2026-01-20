package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.PlaylistService;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of PlaylistService.
 * Handles encrypted storage and retrieval of playlists.
 */
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;
    private final com.iptv.wiseplayer.service.iptv.XtreamClient xtreamClient;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
            DeviceRepository deviceRepository,
            EncryptionUtil encryptionUtil,
            com.iptv.wiseplayer.service.iptv.XtreamClient xtreamClient) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
        this.xtreamClient = xtreamClient;
    }

    @Override
    @Transactional
    public void saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request) {
        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setDeviceId(deviceId);
                    return p;
                });

        playlist.setType(PlaylistType.XTREAM);
        playlist.setServerUrl(encryptionUtil.encrypt(request.getServerUrl()));
        playlist.setUsername(encryptionUtil.encrypt(request.getUsername()));
        playlist.setPassword(encryptionUtil.encrypt(request.getPassword()));
        playlist.setM3uUrl(null);

        playlistRepository.save(playlist);
    }

    @Override
    @Transactional
    public void saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request) {
        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setDeviceId(deviceId);
                    return p;
                });

        playlist.setType(PlaylistType.M3U);
        playlist.setM3uUrl(encryptionUtil.encrypt(request.getM3uUrl()));
        playlist.setServerUrl(null);
        playlist.setUsername(null);
        playlist.setPassword(null);

        playlistRepository.save(playlist);
    }

    @Override
    public PlaylistResponse getPlaylist(UUID deviceId) {
        // Validation check for device status
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException(
                        "Internal Security Error: Authenticated device not found in database"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Access Denied: Your device status is " + device.getDeviceStatus());
        }

        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new PlaylistNotFoundException("No playlist found for your device"));

        // Decrypt fields for response
        String serverUrl = playlist.getServerUrl() != null ? encryptionUtil.decrypt(playlist.getServerUrl()) : null;
        String username = playlist.getUsername() != null ? encryptionUtil.decrypt(playlist.getUsername()) : null;
        String password = playlist.getPassword() != null ? encryptionUtil.decrypt(playlist.getPassword()) : null;
        String m3uUrl = playlist.getM3uUrl() != null ? encryptionUtil.decrypt(playlist.getM3uUrl()) : null;

        return new PlaylistResponse(
                playlist.getId(),
                playlist.getDeviceId(),
                playlist.getType(),
                serverUrl,
                username,
                password,
                m3uUrl);
    }

    @Override
    public void validatePlaylist(UUID deviceId, Object request) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Validation allowed only for active devices");
        }

        if (request instanceof XtreamPlaylistRequest xtreamRequest) {
            xtreamClient.authenticate(xtreamRequest.getServerUrl(), xtreamRequest.getUsername(),
                    xtreamRequest.getPassword())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid Xtream credentials or inactive account"));

            // If valid, save it
            saveXtreamPlaylist(deviceId, xtreamRequest);
        } else if (request instanceof M3uPlaylistRequest m3uRequest) {
            // M3U validation could be as simple as checking if URL is reachable (optional
            // for now)
            saveM3uPlaylist(deviceId, m3uRequest);
        } else {
            throw new IllegalArgumentException("Unsupported playlist request type");
        }
    }
}
