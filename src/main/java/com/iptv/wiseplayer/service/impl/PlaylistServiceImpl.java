package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.PlaylistService;
import com.iptv.wiseplayer.service.iptv.XtreamClient;
import com.iptv.wiseplayer.security.DeviceTokenUtil;
import com.iptv.wiseplayer.util.EncryptionUtil;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of PlaylistService.
 * Handles encrypted storage and retrieval of playlists.
 */
@Service
public class PlaylistServiceImpl implements PlaylistService {

    private static final Logger log = LoggerFactory.getLogger(PlaylistServiceImpl.class);

    private final PlaylistRepository playlistRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;
    private final XtreamClient xtreamClient;
    private final com.iptv.wiseplayer.util.XtreamUrlParser xtreamUrlParser;
    private final DeviceTokenUtil tokenUtil;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
            DeviceRepository deviceRepository,
            EncryptionUtil encryptionUtil,
            XtreamClient xtreamClient,
            com.iptv.wiseplayer.util.XtreamUrlParser xtreamUrlParser,
            DeviceTokenUtil tokenUtil) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
        this.xtreamClient = xtreamClient;
        this.xtreamUrlParser = xtreamUrlParser;
        this.tokenUtil = tokenUtil;
    }

    @Override
    @Transactional
    public PlaylistResponse saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request) {
        log.info("Saving Xtream playlist '{}' for device {}", request.getName(), deviceId);
        // Validate credentials before saving
        xtreamClient.authenticate(request.getServerUrl(), request.getUsername(), request.getPassword())
                .orElseThrow(() -> {
                    log.warn("Xtream validation failed for '{}' on device {}", request.getName(), deviceId);
                    return new BadRequestException("Invalid Xtream credentials or inactive account");
                });

        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setDeviceId(deviceId);
                    p.setName(request.getName());
                    return p;
                });

        playlist.setType(PlaylistType.XTREAM);
        playlist.setServerUrl(encryptionUtil.encrypt(request.getServerUrl()));
        playlist.setUsername(encryptionUtil.encrypt(request.getUsername()));
        playlist.setPassword(encryptionUtil.encrypt(request.getPassword()));
        playlist.setM3uUrl(null);

        Playlist saved = playlistRepository.save(playlist);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PlaylistResponse saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request) {
        // Smart Promotion Check
        var xtreamDetails = xtreamUrlParser.parse(request.getM3uUrl());
        if (xtreamDetails != null) {
            XtreamPlaylistRequest xtreamRequest = new XtreamPlaylistRequest();
            xtreamRequest.setName(request.getName());
            xtreamRequest.setServerUrl(xtreamDetails.getServerUrl());
            xtreamRequest.setUsername(xtreamDetails.getUsername());
            xtreamRequest.setPassword(xtreamDetails.getPassword());
            return saveXtreamPlaylist(deviceId, xtreamRequest);
        }

        // Validate M3U URL with a lightweight HEAD request
        log.info("Validating M3U URL for playlist '{}'", request.getName());
        validateM3uUrl(request.getM3uUrl());

        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getName()))
                .findFirst()
                .orElseGet(() -> {
                    Playlist p = new Playlist();
                    p.setDeviceId(deviceId);
                    p.setName(request.getName());
                    return p;
                });

        playlist.setType(PlaylistType.M3U);
        playlist.setM3uUrl(encryptionUtil.encrypt(request.getM3uUrl()));
        playlist.setServerUrl(null);
        playlist.setUsername(null);
        playlist.setPassword(null);

        Playlist saved = playlistRepository.save(playlist);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PlaylistResponse savePublicM3uPlaylist(String deviceId, M3uPlaylistRequest request) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BadRequestException("Device ID is required");
        }

        String identity = deviceId.trim();
        Device device = null;

        // 1. Try as UUID
        try {
            UUID uuid = UUID.fromString(identity);
            device = deviceRepository.findByDeviceId(uuid).orElse(null);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try as fingerprint
        }

        // 2. Try as Fingerprint (MAC) if UUID didn't work
        if (device == null) {
            String fingerprintHash = tokenUtil.hashFingerprint(identity);
            device = deviceRepository.findByFingerprintHash(fingerprintHash)
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found with identity: " + identity));
        }

        // REQUIRED: MUST BE ACTIVE AND NOT EXPIRED
        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Upload failed: Device is not active. Current status: " + device.getDeviceStatus());
        }

        if (device.getExpiresAt() != null && LocalDateTime.now().isAfter(device.getExpiresAt())) {
            throw new AccessDeniedException("Upload failed: Device subscription has expired");
        }

        // Construct a standard M3uPlaylistRequest to reuse existing logic (including
        // Xtream Promotion)
        M3uPlaylistRequest m3uRequest = new M3uPlaylistRequest();
        m3uRequest.setName(request.getName());
        m3uRequest.setM3uUrl(request.getM3uUrl());

        return saveM3uPlaylist(device.getDeviceId(), m3uRequest);
    }

    @Override
    public java.util.List<PlaylistResponse> getPlaylists(UUID deviceId) {
        // Validation check for device status
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internal Security Error: Authenticated device not found in database"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Access Denied: Your device status is " + device.getDeviceStatus());
        }

        return playlistRepository.findByDeviceId(deviceId).stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());
    }

    private PlaylistResponse mapToResponse(Playlist playlist) {
        // Decrypt fields for response
        String serverUrl = playlist.getServerUrl() != null ? encryptionUtil.decrypt(playlist.getServerUrl())
                : null;
        String username = playlist.getUsername() != null ? encryptionUtil.decrypt(playlist.getUsername())
                : null;
        String password = playlist.getPassword() != null ? encryptionUtil.decrypt(playlist.getPassword())
                : null;
        String m3uUrl = playlist.getM3uUrl() != null ? encryptionUtil.decrypt(playlist.getM3uUrl()) : null;

        return new PlaylistResponse(
                playlist.getId(),
                playlist.getDeviceId(),
                playlist.getName(),
                playlist.getType(),
                serverUrl,
                username,
                password,
                m3uUrl);
    }

    private void validateM3uUrl(String urlString) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(urlString).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "okhttp/4.9.0");
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 400) {
                if (responseCode == 405 || responseCode == 403) {
                    log.warn("M3U HEAD request returned {} for {}. Attempting soft pass.", responseCode, urlString);
                    return;
                }
                log.error("M3U validation failed with status {} for URL: {}", responseCode, urlString);
                throw new BadRequestException("Invalid M3U URL or server is unreachable. HTTP Status: " + responseCode);
            }
        } catch (java.net.MalformedURLException e) {
            log.error("Malformed M3U URL: {}", urlString);
            throw new BadRequestException("Invalid M3U URL format");
        } catch (java.io.IOException e) {
            log.error("Network error during M3U validation: {}", e.getMessage());
            throw new BadRequestException("Error connecting to the M3U URL: " + e.getMessage());
        }
    }
}
