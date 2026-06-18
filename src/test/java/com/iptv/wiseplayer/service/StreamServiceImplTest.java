package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Device;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.exception.AccessDeniedException;
import com.iptv.wiseplayer.exception.PlaylistNotFoundException;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.impl.StreamServiceImpl;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamServiceImplTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private AntiFraudGuardService antiFraudGuardService;

    private StreamServiceImpl streamService;

    @BeforeEach
    void setUp() {
        // Use SyncTaskExecutor to run async tasks synchronously in tests
        streamService = new StreamServiceImpl(
                playlistRepository,
                deviceRepository,
                encryptionUtil,
                antiFraudGuardService,
                new SyncTaskExecutor()
        );
    }

    @Test
    void testGetTimeshiftUrlAsync_success() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceStatus(DeviceStatus.ACTIVE);

        Playlist playlist = new Playlist(deviceId, "Xtream Play", "encrypted-server", "encrypted-user", "encrypted-pass");
        playlist.setId(playlistId);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));
        when(playlistRepository.findByDeviceId(deviceId)).thenReturn(Collections.singletonList(playlist));
        when(encryptionUtil.decrypt("encrypted-server")).thenReturn("http://line.vpnworld.pro:8080/xmltv.php");
        when(encryptionUtil.decrypt("encrypted-user")).thenReturn("testuser");
        when(encryptionUtil.decrypt("encrypted-pass")).thenReturn("testpass");

        // Test with basic ISO datetime
        CompletableFuture<String> future = streamService.getTimeshiftUrlAsync(
                deviceId, playlistId, "101", "2026-06-01T11:30:00", 60, "ts"
        );

        String resultUrl = future.get();
        assertEquals("http://line.vpnworld.pro:8080/timeshift/testuser/testpass/60/2026-06-01:11-30/101.ts", resultUrl);
    }

    @Test
    void testGetTimeshiftUrlAsync_resilientParsing() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceStatus(DeviceStatus.ACTIVE);

        Playlist playlist = new Playlist(deviceId, "Xtream Play", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));
        when(playlistRepository.findByDeviceId(deviceId)).thenReturn(Collections.singletonList(playlist));
        when(encryptionUtil.decrypt(anyString())).thenAnswer(invocation -> invocation.getArgument(0));

        // Test offset datetime (e.g. +05:30)
        CompletableFuture<String> offsetFuture = streamService.getTimeshiftUrlAsync(
                deviceId, playlistId, "101.ts", "2026-06-01T11:30:00+02:00", 120, null
        );
        assertTrue(offsetFuture.get().contains("/timeshift/enc-user/enc-pass/120/2026-06-01:11-30/101.ts"));

        // Test instant/UTC format (with Z)
        CompletableFuture<String> instantFuture = streamService.getTimeshiftUrlAsync(
                deviceId, playlistId, "101", "2026-06-01T11:30:00Z", 120, "ts"
        );
        assertTrue(instantFuture.get().contains("/2026-06-01:11-30/101.ts"));
    }

    @Test
    void testGetTimeshiftUrlAsync_inactiveDevice() {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceStatus(DeviceStatus.INACTIVE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));

        CompletableFuture<String> future = streamService.getTimeshiftUrlAsync(
                deviceId, playlistId, "101", "2026-06-01T11:30:00", 60, "ts"
        );

        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof AccessDeniedException);
    }

    @Test
    void testGetTimeshiftUrlAsync_playlistNotFound() {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();

        Device device = new Device();
        device.setDeviceId(deviceId);
        device.setDeviceStatus(DeviceStatus.ACTIVE);

        when(deviceRepository.findByDeviceId(deviceId)).thenReturn(Optional.of(device));
        when(playlistRepository.findByDeviceId(deviceId)).thenReturn(Collections.emptyList());

        CompletableFuture<String> future = streamService.getTimeshiftUrlAsync(
                deviceId, playlistId, "101", "2026-06-01T11:30:00", 60, "ts"
        );

        ExecutionException exception = assertThrows(ExecutionException.class, future::get);
        assertTrue(exception.getCause() instanceof PlaylistNotFoundException);
    }
}
