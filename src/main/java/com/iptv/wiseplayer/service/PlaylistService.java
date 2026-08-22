package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaylistService {

    PlaylistResponse saveXtreamPlaylist(UUID deviceId, XtreamPlaylistRequest request);

    PlaylistResponse saveM3uPlaylist(UUID deviceId, M3uPlaylistRequest request);

    PlaylistResponse savePublicM3uPlaylist(String deviceId, M3uPlaylistRequest request);

    List<PlaylistResponse> getPlaylists(UUID deviceId, String pin);

    List<PlaylistResponse> getPublicPlaylists(String deviceId);

    /**
     * Retrieves public playlists after verifying the device PIN.
     * If the device has no PIN set, access is granted freely (backward-compatible).
     *
     * @param deviceId the device MAC address, fingerprint, or UUID
     * @param pin      the 4-digit PIN to verify (may be null if no PIN is set)
     */
    List<PlaylistResponse> getPublicPlaylistsWithPin(String deviceId, String pin);

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

    // ── Playlist Pin/Unpin (favourite) ───────────────────────────────────────

    /** Pin a playlist for an authenticated device (by UUID). */
    PlaylistResponse pinPlaylist(UUID deviceId, UUID playlistId);

    /** Unpin a playlist for an authenticated device (by UUID). */
    PlaylistResponse unpinPlaylist(UUID deviceId, UUID playlistId);

    PlaylistResponse togglePlaylistLock(UUID deviceId, UUID playlistId, boolean isLocked);

    /** Get the currently pinned playlist for an authenticated device. */
    Optional<PlaylistResponse> getPinnedPlaylist(UUID deviceId);

    /** Pin a playlist via the public (MAC/fingerprint) device identifier. */
    PlaylistResponse pinPublicPlaylist(String deviceId, UUID playlistId);

    /** Unpin a playlist via the public (MAC/fingerprint) device identifier. */
    PlaylistResponse unpinPublicPlaylist(String deviceId, UUID playlistId);

    /** Get the currently pinned playlist via the public device identifier. */
    Optional<PlaylistResponse> getPublicPinnedPlaylist(String deviceId);
}
