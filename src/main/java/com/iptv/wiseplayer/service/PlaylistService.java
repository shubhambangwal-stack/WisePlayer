package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import java.util.UUID;

public interface PlaylistService {
    PlaylistResponse saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request);

    PlaylistResponse saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request);

    PlaylistResponse savePublicM3uPlaylist(String deviceId, M3uPlaylistRequest request);

    java.util.List<PlaylistResponse> getPlaylists(UUID deviceId);

    java.util.List<PlaylistResponse> getPublicPlaylists(String deviceId);

    /**
     * Retrieves public playlists after verifying the device PIN.
     * If the device has no PIN set, access is granted freely (backward-compatible).
     *
     * @param deviceId the device MAC address, fingerprint, or UUID
     * @param pin      the 4-digit PIN to verify (may be null if no PIN is set)
     */
    java.util.List<PlaylistResponse> getPublicPlaylistsWithPin(String deviceId, String pin);

    /**
     * Sets (or replaces) the 4-digit public access PIN for the authenticated device.
     *
     * @param deviceId the UUID of the authenticated device
     * @param pin      the raw 4-digit PIN to hash and store
     */
    void setDevicePin(UUID deviceId, String pin);

    /**
     * Removes the public access PIN from the authenticated device.
     *
     * @param deviceId the UUID of the authenticated device
     */
    void removeDevicePin(UUID deviceId);

    void deletePublicPlaylist(String deviceId, UUID playlistId);

    PlaylistResponse updatePublicM3uPlaylist(String deviceId, UUID playlistId, M3uPlaylistRequest request);
}


