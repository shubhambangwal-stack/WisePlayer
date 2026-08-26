package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.OwnerType;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.UpdatePlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.AdminPlaylistService;
import com.iptv.wiseplayer.service.iptv.XtreamClient;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AdminPlaylistServiceImpl implements AdminPlaylistService {

    private final PlaylistRepository playlistRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;
    private final XtreamClient xtreamClient;

    public AdminPlaylistServiceImpl(PlaylistRepository playlistRepository, DeviceRepository deviceRepository, EncryptionUtil encryptionUtil, XtreamClient xtreamClient) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
        this.xtreamClient = xtreamClient;
    }

    // ─── Read ────────────────────────────────────────────────────────────────

    @Override
    public List<PlaylistResponse> getAllPlaylists() {
        return playlistRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PlaylistResponse getPlaylistById(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));
        return mapToResponse(playlist);
    }

    // ─── Create ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PlaylistResponse createXtreamPlaylist(UUID adminId, XtreamPlaylistRequest request) {
        xtreamClient.authenticate(request.getServerUrl(), request.getUsername(), request.getPassword())
                .orElseThrow(() -> new BadRequestException("Invalid Xtream credentials or inactive account"));

        Playlist playlist = new Playlist();
        playlist.setName(request.getName());
        playlist.setType(PlaylistType.XTREAM);
        playlist.setServerUrl(encryptionUtil.encrypt(request.getServerUrl()));
        playlist.setUsername(encryptionUtil.encrypt(request.getUsername()));
        playlist.setPassword(encryptionUtil.encrypt(request.getPassword()));
        playlist.setOwnerType(OwnerType.ADMIN);
        playlist.setOwnerId(adminId);

        Playlist saved = playlistRepository.save(playlist);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PlaylistResponse createM3uPlaylist(UUID adminId, M3uPlaylistRequest request) {
        Playlist playlist = new Playlist();
        playlist.setName(request.getName());
        playlist.setType(PlaylistType.M3U);
        playlist.setM3uUrl(encryptionUtil.encrypt(request.getM3uUrl()));
        playlist.setOwnerType(OwnerType.ADMIN);
        playlist.setOwnerId(adminId);

        Playlist saved = playlistRepository.save(playlist);
        return mapToResponse(saved);
    }

    // ─── Update ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PlaylistResponse updatePlaylist(UUID playlistId, UpdatePlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));

        if (request.getName() != null && !request.getName().isBlank()) {
            playlist.setName(request.getName());
        }

        if (playlist.getType() == PlaylistType.XTREAM) {
            if (request.getServerUrl() != null && !request.getServerUrl().isBlank()) {
                playlist.setServerUrl(encryptionUtil.encrypt(request.getServerUrl()));
            }
            if (request.getUsername() != null && !request.getUsername().isBlank()) {
                playlist.setUsername(encryptionUtil.encrypt(request.getUsername()));
            }
            if (request.getPassword() != null && !request.getPassword().isBlank()) {
                playlist.setPassword(encryptionUtil.encrypt(request.getPassword()));
            }
        } else if (playlist.getType() == PlaylistType.M3U) {
            if (request.getM3uUrl() != null && !request.getM3uUrl().isBlank()) {
                playlist.setM3uUrl(encryptionUtil.encrypt(request.getM3uUrl()));
            }
        }

        return mapToResponse(playlistRepository.save(playlist));
    }

    // ─── Assign / Unassign ───────────────────────────────────────────────────

    @Override
    @Transactional
    public PlaylistResponse assignPlaylist(UUID playlistId, AssignPlaylistRequest request) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));

        Device device = resolveDevice(request.getDeviceId());

        playlist.setDeviceId(device.getDeviceId());
        return mapToResponse(playlistRepository.save(playlist));
    }

    @Override
    @Transactional
    public PlaylistResponse unassignPlaylist(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));

        playlist.setDeviceId(null);
        return mapToResponse(playlistRepository.save(playlist));
    }

    // ─── Pin ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PlaylistResponse togglePin(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));

        playlist.setPinned(!playlist.isPinned());
        return mapToResponse(playlistRepository.save(playlist));
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deletePlaylist(UUID playlistId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found"));
        playlistRepository.delete(playlist);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Resolve a device by either its UUID or its MAC address.
     *
     * @param deviceIdentifier a UUID string (36 chars) or a MAC address (XX:XX:XX:XX:XX:XX)
     * @return the matching Device
     * @throws ResourceNotFoundException if no device matches
     */
    private Device resolveDevice(String deviceIdentifier) {
        Optional<Device> device;
        if (deviceIdentifier != null && deviceIdentifier.contains("-")) {
            // Treat as UUID
            try {
                device = deviceRepository.findById(UUID.fromString(deviceIdentifier));
            } catch (IllegalArgumentException e) {
                throw new ResourceNotFoundException("Device not found: invalid UUID format");
            }
        } else {
            // Treat as MAC address
            device = deviceRepository.findByMacAddressIgnoreCase(deviceIdentifier);
        }
        return device.orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    private PlaylistResponse mapToResponse(Playlist playlist) {
        String serverUrl = playlist.getServerUrl() != null ? encryptionUtil.decrypt(playlist.getServerUrl()) : null;
        String username = playlist.getUsername() != null ? encryptionUtil.decrypt(playlist.getUsername()) : null;
        String password = playlist.getPassword() != null ? encryptionUtil.decrypt(playlist.getPassword()) : null;
        String m3uUrl = playlist.getM3uUrl() != null ? encryptionUtil.decrypt(playlist.getM3uUrl()) : null;

        return new PlaylistResponse(
                playlist.getId(),
                playlist.getDeviceId(),
                playlist.getName(),
                playlist.getType(),
                serverUrl,
                username,
                password,
                m3uUrl,
                playlist.isPinned(),
                playlist.getPinHash() != null);
    }
}
