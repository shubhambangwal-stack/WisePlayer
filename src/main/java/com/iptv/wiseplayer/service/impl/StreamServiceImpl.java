package com.iptv.wiseplayer.service.impl;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.AntiFraudGuardService;
import com.iptv.wiseplayer.service.StreamService;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class StreamServiceImpl implements StreamService {

    private final PlaylistRepository playlistRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;
    private final AntiFraudGuardService antiFraudGuardService;
    private final TaskExecutor timeshiftExecutor;

    public StreamServiceImpl(PlaylistRepository playlistRepository,
            DeviceRepository deviceRepository,
            EncryptionUtil encryptionUtil,
            AntiFraudGuardService antiFraudGuardService,
            @Qualifier("timeshiftExecutor") TaskExecutor timeshiftExecutor) {
        this.playlistRepository = playlistRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
        this.antiFraudGuardService = antiFraudGuardService;
        this.timeshiftExecutor = timeshiftExecutor;
    }

    @Override
    public String authorizeAndGetUrl(UUID deviceId, UUID playlistId, String streamId) {
        // 1. Validate device and subscription
        Device device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
            throw new AccessDeniedException("Active subscription required to play content");
        }

        // 2. Fetch playlist
        Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                .filter(p -> p.getId().equals(playlistId))
                .findFirst()
                .orElseThrow(
                        () -> new PlaylistNotFoundException("Playlist not found or does not belong to your device"));

        // 3. Generate URL
        if (playlist.getType() == PlaylistType.XTREAM) {
            String serverUrl = encryptionUtil.decrypt(playlist.getServerUrl());
            String username = encryptionUtil.decrypt(playlist.getUsername());
            String password = encryptionUtil.decrypt(playlist.getPassword());

            // Xtream URL format: http://server:port/live/username/password/stream_id.ts
            if (serverUrl.endsWith("/")) {
                serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            }
            return String.format("%s/live/%s/%s/%s.ts", serverUrl, username, password, streamId);
        } else if (playlist.getType() == PlaylistType.M3U) {
            // For M3U, the stream_id passed is already the full URL (as returned by
            // M3uService)
            return streamId;
        }

        throw new UnsupportedOperationException("Stream play not supported for this playlist type");
    }

    @Override
    public CompletableFuture<String> getTimeshiftUrlAsync(
            UUID deviceId,
            UUID playlistId,
            String channelId,
            String timestamp,
            Integer duration,
            String extension) {

        return CompletableFuture.supplyAsync(() -> {
            // 1. Anti-fraud guard verification (throws ConnectionLimitException)
            antiFraudGuardService.checkConnectionLimit(deviceId);

            // 2. Validate device and subscription status
            Device device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new RuntimeException("Device not found"));

            if (device.getDeviceStatus() != DeviceStatus.ACTIVE) {
                throw new AccessDeniedException("Active subscription required to play content");
            }

            // 3. Load playlist details
            Playlist playlist = playlistRepository.findByDeviceId(deviceId).stream()
                    .filter(p -> p.getId().equals(playlistId))
                    .findFirst()
                    .orElseThrow(() -> new PlaylistNotFoundException("Playlist not found or does not belong to your device"));

            if (playlist.getType() != PlaylistType.XTREAM) {
                throw new IllegalArgumentException("Timeshift redirect is only supported for Xtream playlists");
            }

            // 4. Decrypt credentials securely
            String serverUrl = encryptionUtil.decrypt(playlist.getServerUrl());
            String username = encryptionUtil.decrypt(playlist.getUsername());
            String password = encryptionUtil.decrypt(playlist.getPassword());

            // 5. Extract host/port domain from server URL
            String baseUrl;
            try {
                URI uri = new URI(serverUrl);
                baseUrl = uri.getScheme() + "://" + uri.getAuthority();
            } catch (Exception e) {
                baseUrl = serverUrl;
                if (baseUrl.endsWith("/")) {
                    baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
                }
            }

            // 6. Resilient parse of standard client ISO strings
            String formattedTime = parseAndFormatTimestamp(timestamp);

            // 7. Format channelId and extension extension
            String targetChannel = channelId;
            String ext = (extension != null && !extension.trim().isEmpty()) ? extension.trim() : "ts";
            if (ext.startsWith(".")) {
                ext = ext.substring(1);
            }
            if (!targetChannel.endsWith(".ts") && !targetChannel.endsWith(".m3u8")) {
                targetChannel = targetChannel + "." + ext;
            }

            int reqDuration = duration != null ? duration : 60;

            // Target URL format:
            // http://line.vpnworld.pro/timeshift/{username}/{password}/{duration}/{yyyy-MM-dd:HH-mm}/{channelId}.ts
            return String.format("%s/timeshift/%s/%s/%d/%s/%s",
                    baseUrl, username, password, reqDuration, formattedTime, targetChannel);
        }, timeshiftExecutor);
    }

    private String parseAndFormatTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Timestamp parameter is required");
        }
        DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH-mm");
        try {
            // Try parsing LocalDateTime (e.g., 2026-06-01T11:30:00)
            return LocalDateTime.parse(timestampStr).format(targetFormatter);
        } catch (Exception e1) {
            try {
                // Try parsing OffsetDateTime (e.g., 2026-06-01T11:30:00+02:00)
                return OffsetDateTime.parse(timestampStr).format(targetFormatter);
            } catch (Exception e2) {
                try {
                    // Try parsing Instant / UTC Zulu (e.g., 2026-06-01T11:30:00Z)
                    return LocalDateTime.ofInstant(Instant.parse(timestampStr), ZoneOffset.UTC).format(targetFormatter);
                } catch (Exception e3) {
                    throw new IllegalArgumentException("Invalid date-time format: " + timestampStr +
                            ". Please use standard ISO-8601 format (e.g., 2026-06-01T11:30:00)", e3);
                }
            }
        }
    }
}
