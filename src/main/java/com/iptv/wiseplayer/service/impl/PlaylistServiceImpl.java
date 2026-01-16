package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.DeviceService;
import com.iptv.wiseplayer.service.PlaylistService;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlaylistServiceImpl implements PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceService deviceService;
    private final EncryptionUtil encryptionUtil;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
            DeviceRepository deviceRepository,
            DeviceService deviceService,
            EncryptionUtil encryptionUtil) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    @Transactional
    public void saveXtreamPlaylist(XtreamPlaylistRequest request) {
        UUID deviceId = deviceService.getDeviceIdByFingerprint(request.getDeviceId());

        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setDeviceId(deviceId);
                    return p;
                });

        playlist.setType(com.iptv.wiseplayer.domain.enums.PlaylistType.XTREAM);
        playlist.setServerUrl(encryptionUtil.encrypt(request.getServerUrl()));
        playlist.setUsername(encryptionUtil.encrypt(request.getUsername()));
        playlist.setPassword(encryptionUtil.encrypt(request.getPassword()));
        playlist.setM3uUrl(null); // Clear other type fields

        playlistRepository.save(playlist);
    }

    @Override
    @Transactional
    public void saveM3uPlaylist(M3uPlaylistRequest request) {
        UUID deviceId = deviceService.getDeviceIdByFingerprint(request.getDeviceId());

        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setDeviceId(deviceId);
                    return p;
                });

        playlist.setType(com.iptv.wiseplayer.domain.enums.PlaylistType.M3U);
        playlist.setM3uUrl(encryptionUtil.encrypt(request.getM3uUrl()));
        playlist.setServerUrl(null); // Clear other type fields
        playlist.setUsername(null);
        playlist.setPassword(null);

        playlistRepository.save(playlist);
    }

    @Override
    public PlaylistResponse getPlaylist(String deviceIdFingerprint) {
        UUID deviceId = deviceService.getDeviceIdByFingerprint(deviceIdFingerprint);

        // Check if device is ACTIVE
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Internal Error: Device ID resolved but entity not found"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Access Denied: Device subscription is not active. Current status: " + device.getDeviceStatus());
        }

        Playlist playlist = playlistRepository.findByDeviceId(deviceId)
                .orElseThrow(
                        () -> new PlaylistNotFoundException("Playlist not found for device: " + deviceIdFingerprint));

        // Decrypt fields
        String serverUrl = encryptionUtil.decrypt(playlist.getServerUrl());
        String username = encryptionUtil.decrypt(playlist.getUsername());
        String password = encryptionUtil.decrypt(playlist.getPassword());
        String m3uUrl = encryptionUtil.decrypt(playlist.getM3uUrl());

        return new PlaylistResponse(
                playlist.getId(),
                playlist.getDeviceId(),
                playlist.getType(),
                serverUrl,
                username,
                password,
                m3uUrl);
    }
}
