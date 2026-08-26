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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    private final PasswordEncoder passwordEncoder;
    private final com.iptv.wiseplayer.service.iptv.CatchUpService catchUpService;

    public PlaylistServiceImpl(PlaylistRepository playlistRepository,
            DeviceRepository deviceRepository,
            EncryptionUtil encryptionUtil,
            XtreamClient xtreamClient,
            com.iptv.wiseplayer.util.XtreamUrlParser xtreamUrlParser,
            DeviceTokenUtil tokenUtil,
            PasswordEncoder passwordEncoder,
            com.iptv.wiseplayer.service.iptv.CatchUpService catchUpService) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
        this.xtreamClient = xtreamClient;
        this.xtreamUrlParser = xtreamUrlParser;
        this.tokenUtil = tokenUtil;
        this.passwordEncoder = passwordEncoder;
        this.catchUpService = catchUpService;
    }

    @Override
    @Transactional
    public PlaylistResponse saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request) {
        log.info("saveXtreamPlaylist starting. Name: '{}', serverUrl: {}, username: {}", request.getName(), request.getServerUrl(), request.getUsername());
        // Validate credentials before saving
        try {
            xtreamClient.authenticate(request.getServerUrl(), request.getUsername(), request.getPassword())
                    .orElseThrow(() -> {
                        log.warn("Xtream validation returned empty response for '{}' on device {}", request.getName(), deviceId);
                        return new BadRequestException("Invalid Xtream credentials or inactive account");
                    });
            log.info("Xtream credentials authenticated successfully.");
        } catch (Exception authEx) {
            log.error("Error validating Xtream credentials: {}", authEx.getMessage(), authEx);
            throw authEx;
        }

        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getName()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("No existing playlist with name '{}' found, creating a new one.", request.getName());
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
        log.info("Xtream playlist '{}' saved successfully with ID: {}", saved.getName(), saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PlaylistResponse saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request) {
        log.info("saveM3uPlaylist starting. Name: '{}', m3uUrl: {}", request.getName(), request.getM3uUrl());
        // Smart Promotion Check
        var xtreamDetails = xtreamUrlParser.parse(request.getM3uUrl());
        if (xtreamDetails != null) {
            log.info("URL is Xtream-compatible. Promoting M3U to Xtream Codes playlist format.");
            XtreamPlaylistRequest xtreamRequest = new XtreamPlaylistRequest();
            xtreamRequest.setName(request.getName());
            xtreamRequest.setServerUrl(xtreamDetails.getServerUrl());
            xtreamRequest.setUsername(xtreamDetails.getUsername());
            xtreamRequest.setPassword(xtreamDetails.getPassword());
            return saveXtreamPlaylist(deviceId, xtreamRequest);
        }

        // Validate M3U URL with a lightweight HEAD request
        log.info("Validating M3U URL with network request for playlist '{}'", request.getName());
        validateM3uUrl(request.getM3uUrl());

        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getName().equalsIgnoreCase(request.getName()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("No existing playlist with name '{}' found, creating a new one.", request.getName());
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
        log.info("M3U playlist '{}' saved successfully with ID: {}", saved.getName(), saved.getId());
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
            device = deviceRepository.findByFingerprintHash(fingerprintHash).orElse(null);
        }

        // 3. Try plain MAC address lookup as a final fallback
        if (device == null) {
            device = deviceRepository.findByMacAddressIgnoreCase(identity).orElse(null);
        }

        if (device == null) {
            throw new ResourceNotFoundException("Device not found with identity: " + identity);
        }

        // REQUIRED: MUST BE ACTIVE AND NOT EXPIRED
        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException(
                    "Upload failed: Device is not active. Current status: " + device.getDeviceStatus());
        }

        if (device.getExpiresAt() != null && LocalDateTime.now().isAfter(device.getExpiresAt())) {
            if (device.getSubscriptionType() == com.iptv.wiseplayer.domain.enums.SubscriptionType.TRIAL) {
                throw new AccessDeniedException("Upload failed: Free trial has ended. Please subscribe to continue.");
            } else {
                throw new AccessDeniedException("Upload failed: Device subscription has expired. Please renew.");
            }
        }

        if (request.getM3uUrl() == null || request.getM3uUrl().trim().isEmpty()) {
            throw new BadRequestException("M3U URL is required");
        }

        // Smart Promotion Check - only plain M3U URLs get HEAD-validated here;
        // Xtream-style URLs are validated downstream via xtreamClient.authenticate()
        var xtreamDetails = xtreamUrlParser.parse(request.getM3uUrl());
        if (xtreamDetails == null) {
            log.info("Validating M3U URL for public playlist '{}' on device {}", request.getName(), device.getDeviceId());
            validateM3uUrl(request.getM3uUrl());
        }

        // Construct a standard M3uPlaylistRequest to reuse existing logic (including
        // Xtream Promotion)
        M3uPlaylistRequest m3uRequest = new M3uPlaylistRequest();
        m3uRequest.setName(request.getName());
        m3uRequest.setM3uUrl(request.getM3uUrl());

        return saveM3uPlaylist(device.getDeviceId(), m3uRequest);
    }

    @Override
    public java.util.List<PlaylistResponse> getPlaylists(UUID deviceId, String pin) {
        // Validation check for device status
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internal Security Error: Authenticated device not found in database"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Access Denied: Your device status is " + device.getDeviceStatus());
        }

        String deviceStoredHash = device.getPublicPinHash();
        String defaultPinHash = passwordEncoder.encode("0000");
        String effectivePin = (pin == null || pin.isBlank()) ? "0000" : pin;

        java.util.List<Playlist> allPlaylists = playlistRepository.findByDeviceIdOrderByPinnedDescCreatedAtDesc(deviceId);
        java.util.List<PlaylistResponse> accessiblePlaylists = new java.util.ArrayList<>();

        for (Playlist playlist : allPlaylists) {
            boolean isPinValid = false;

            if (com.iptv.wiseplayer.domain.enums.OwnerType.DEVICE.equals(playlist.getOwnerType()) || playlist.getOwnerType() == null) {
                // Device-level PIN for regular users
                String hashToCheck = (deviceStoredHash != null) ? deviceStoredHash : defaultPinHash;
                isPinValid = passwordEncoder.matches(effectivePin, hashToCheck);
            } else if (com.iptv.wiseplayer.domain.enums.OwnerType.RESELLER.equals(playlist.getOwnerType()) || com.iptv.wiseplayer.domain.enums.OwnerType.SUB_RESELLER.equals(playlist.getOwnerType())) {
                // Playlist-level PIN for resellers and subresellers
                String playlistHash = playlist.getPinHash();
                String hashToCheck = (playlistHash != null) ? playlistHash : defaultPinHash;
                isPinValid = passwordEncoder.matches(effectivePin, hashToCheck);
            }

            if (isPinValid) {
                accessiblePlaylists.add(mapToResponse(playlist));
            }
        }

        if (accessiblePlaylists.isEmpty() && !allPlaylists.isEmpty()) {
            log.warn("Incorrect PIN attempt for authenticated device {} with PIN {}", deviceId, effectivePin);
            throw new AccessDeniedException("Incorrect PIN. Access denied.");
        }

        return accessiblePlaylists;
    }

    private Device resolveDevice(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new BadRequestException("Device ID is required");
        }

        String identity = deviceId.trim();
        Device device = null;

        try {
            UUID uuid = UUID.fromString(identity);
            device = deviceRepository.findByDeviceId(uuid).orElse(null);
        } catch (IllegalArgumentException e) {
            // Not a UUID
        }

        if (device == null) {
            String fingerprintHash = tokenUtil.hashFingerprint(identity);
            device = deviceRepository.findByFingerprintHash(fingerprintHash).orElse(null);
        }

        if (device == null) {
            device = deviceRepository.findByMacAddressIgnoreCase(identity).orElse(null);
        }

        if (device == null) {
            throw new ResourceNotFoundException("Device not found with identity: " + identity);
        }

        return device;
    }

    @Override
    public java.util.List<PlaylistResponse> getPublicPlaylists(String deviceId) {
        Device device = resolveDevice(deviceId);
        return getPlaylists(device.getDeviceId(), null);
    }

    @Override
    public java.util.List<PlaylistResponse> getPublicPlaylistsWithPin(String deviceId, String pin) {
        Device device = resolveDevice(deviceId);
        return getPlaylists(device.getDeviceId(), pin);
    }

    @Override
    @Transactional
    public void setDevicePin(UUID deviceId, String pin) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not found: " + deviceId));
        String hashed = passwordEncoder.encode(pin);
        device.setPublicPinHash(hashed);
        deviceRepository.save(device);
        log.info("Public PIN set for device {}", deviceId);
    }

    @Override
    @Transactional
    public void removeDevicePin(UUID deviceId) {
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Device not found: " + deviceId));
        device.setPublicPinHash(null);
        deviceRepository.save(device);
        log.info("Public PIN removed for device {}", deviceId);
    }

    @Override
    @Transactional
    public void deletePublicPlaylist(String deviceId, UUID playlistId) {
        // Legacy: treat as deletePublicPlaylistWithPin with the default PIN
        deletePublicPlaylistWithPin(deviceId, playlistId, "0000");
    }

    // ── Playlist-level PIN management ────────────────────────────────────────

    @Override
    @Transactional
    public PlaylistResponse setPlaylistPin(UUID deviceId, UUID playlistId, String pin) {
        Playlist playlist = playlistRepository.findByIdAndDeviceId(playlistId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));
        playlist.setPinHash(passwordEncoder.encode(pin));
        log.info("Playlist-level PIN set for playlistId={}", playlistId);
        return mapToResponse(playlistRepository.save(playlist));
    }

    @Override
    @Transactional
    public PlaylistResponse removePlaylistPin(UUID deviceId, UUID playlistId) {
        Playlist playlist = playlistRepository.findByIdAndDeviceId(playlistId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));
        playlist.setPinHash(null);
        log.info("Playlist-level PIN removed for playlistId={}", playlistId);
        return mapToResponse(playlistRepository.save(playlist));
    }

    @Override
    public boolean verifyPlaylistPin(UUID deviceId, UUID playlistId, String pin) {
        Playlist playlist = playlistRepository.findByIdAndDeviceId(playlistId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));
        String stored = playlist.getPinHash();
        if (stored == null) {
            // No PIN set — accept the default "0000" or any attempt gracefully
            return "0000".equals(pin) || pin == null;
        }
        return passwordEncoder.matches(pin, stored);
    }

    @Override
    @Transactional
    public PlaylistResponse setPublicPlaylistPin(String deviceId, UUID playlistId, String pin) {
        Device device = resolveDevice(deviceId);
        return setPlaylistPin(device.getDeviceId(), playlistId, pin);
    }

    @Override
    @Transactional
    public PlaylistResponse removePublicPlaylistPin(String deviceId, UUID playlistId) {
        Device device = resolveDevice(deviceId);
        return removePlaylistPin(device.getDeviceId(), playlistId);
    }

    @Override
    public boolean verifyPublicPlaylistPin(String deviceId, UUID playlistId, String pin) {
        Device device = resolveDevice(deviceId);
        return verifyPlaylistPin(device.getDeviceId(), playlistId, pin);
    }

    /**
     * Deletes a public playlist after verifying its PIN.
     * If the playlist has NO pin_hash stored, the deletion is allowed freely (default open).
     * If a pin_hash IS stored, the supplied PIN must match.
     */
    @Override
    @Transactional
    public void deletePublicPlaylistWithPin(String deviceId, UUID playlistId, String pin) {
        Device device = resolveDevice(deviceId);

        Playlist playlist = playlistRepository.findByIdAndDeviceId(playlistId, device.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));

        String storedHash = playlist.getPinHash();
        if (storedHash != null && !storedHash.isBlank()) {
            // PIN is set — must verify
            String effectivePin = (pin == null || pin.isBlank()) ? "0000" : pin;
            if (!passwordEncoder.matches(effectivePin, storedHash)) {
                log.warn("Incorrect PIN attempt for playlist deletion. playlistId={}", playlistId);
                throw new AccessDeniedException("Incorrect PIN. Playlist deletion denied.");
            }
        }

        playlistRepository.delete(playlist);
        log.info("Playlist {} deleted successfully.", playlistId);
    }

    @Override
    @Transactional
    public PlaylistResponse updatePublicM3uPlaylist(String deviceId, UUID playlistId, M3uPlaylistRequest request) {
        Device device = resolveDevice(deviceId);
        
        Playlist playlist = playlistRepository.findByIdAndDeviceId(playlistId, device.getDeviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));
        
        validateM3uUrl(request.getM3uUrl());
        
        playlist.setName(request.getName());
        playlist.setM3uUrl(encryptionUtil.encrypt(request.getM3uUrl()));
        
        Playlist updated = playlistRepository.save(playlist);
        return mapToResponse(updated);
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

        PlaylistResponse response = new PlaylistResponse(
                playlist.getId(),
                playlist.getDeviceId(),
                playlist.getName(),
                playlist.getType(),
                serverUrl,
                username,
                password,
                m3uUrl,
                playlist.isPinned());

        // Attach cached catch-up availability (data-driven; never fabricated).
        try {
            response.setCatchUp(catchUpService.getPlaylistStatus(playlist.getId()));
        } catch (Exception e) {
            log.debug("Could not attach catch-up status for playlist {}: {}", playlist.getId(), e.getMessage());
        }
        return response;
    }

    private void validateM3uUrl(String urlString) {
        try {
            java.net.URL url = new java.net.URL(urlString);
            String host = url.getHost();

            // SSRF Protection: Prevent connecting to localhost or internal network
            java.net.InetAddress[] addresses = java.net.InetAddress.getAllByName(host);
            for (java.net.InetAddress addr : addresses) {
                if (addr.isLoopbackAddress() || addr.isSiteLocalAddress() || addr.isLinkLocalAddress()
                        || addr.isAnyLocalAddress()) {
                    log.error("SSRF attempt blocked: {} resolved to internal IP {}", urlString, addr.getHostAddress());
                    throw new BadRequestException("Invalid URL: Access to internal network is restricted");
                }
            }

            int responseCode = -1;
            try {
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(4000);
                connection.setReadTimeout(4000);
                connection.setRequestProperty("User-Agent", "okhttp/4.9.0");
                responseCode = connection.getResponseCode();

                if (responseCode < 200 || responseCode >= 400) {
                    java.net.HttpURLConnection getConn = (java.net.HttpURLConnection) url.openConnection();
                    getConn.setRequestMethod("GET");
                    getConn.setConnectTimeout(3000);
                    getConn.setReadTimeout(3000);
                    getConn.setRequestProperty("User-Agent", "okhttp/4.9.0");
                    responseCode = getConn.getResponseCode();
                }
            } catch (Exception connEx) {
                log.warn("Network check failed for M3U URL {}: {}. Performing soft pass since host resolved.", urlString, connEx.getMessage());
                return; // Soft pass on connection exception
            }

            if (responseCode < 200 || responseCode >= 400) {
                log.warn("M3U validation returned status {} for URL: {}. Performing soft pass.", responseCode, urlString);
                return; // Soft pass on HTTP errors
            }
        } catch (java.net.UnknownHostException e) {
            log.error("Unknown host for M3U URL: {}", urlString);
            throw new BadRequestException("Invalid M3U URL: Host not found");
        } catch (java.net.MalformedURLException e) {
            log.error("Malformed M3U URL: {}", urlString);
            throw new BadRequestException("Invalid M3U URL format");
        }
    }

    // ── Pin / Unpin ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public PlaylistResponse pinPlaylist(UUID deviceId, UUID playlistId) {
        log.info("Pinning playlist {} for device {}", playlistId, deviceId);

        // Unpin any currently pinned playlist for this device (one-pin-per-device)
        playlistRepository.findByDeviceIdAndPinnedTrue(deviceId).ifPresent(current -> {
            if (!current.getId().equals(playlistId)) {
                current.setPinned(false);
                playlistRepository.save(current);
                log.debug("Auto-unpinned previous playlist {} for device {}", current.getId(), deviceId);
            }
        });

        Playlist target = playlistRepository.findByIdAndDeviceId(playlistId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));

        target.setPinned(true);
        return mapToResponse(playlistRepository.save(target));
    }

    @Override
    @Transactional
    public PlaylistResponse unpinPlaylist(UUID deviceId, UUID playlistId) {
        log.info("Unpinning playlist {} for device {}", playlistId, deviceId);

        Playlist target = playlistRepository.findByIdAndDeviceId(playlistId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Playlist not found or access denied"));

        target.setPinned(false);
        return mapToResponse(playlistRepository.save(target));
    }

    @Override
    public Optional<PlaylistResponse> getPinnedPlaylist(UUID deviceId) {
        // Validate device is active first
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Internal Security Error: Authenticated device not found in database"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Access Denied: Your device status is " + device.getDeviceStatus());
        }

        return playlistRepository.findByDeviceIdAndPinnedTrue(deviceId)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public PlaylistResponse pinPublicPlaylist(String deviceId, UUID playlistId) {
        Device device = resolveDevice(deviceId);
        return pinPlaylist(device.getDeviceId(), playlistId);
    }

    @Override
    @Transactional
    public PlaylistResponse unpinPublicPlaylist(String deviceId, UUID playlistId) {
        Device device = resolveDevice(deviceId);
        return unpinPlaylist(device.getDeviceId(), playlistId);
    }

    @Override
    public Optional<PlaylistResponse> getPublicPinnedPlaylist(String deviceId) {
        Device device = resolveDevice(deviceId);
        return getPinnedPlaylist(device.getDeviceId());
    }
}
