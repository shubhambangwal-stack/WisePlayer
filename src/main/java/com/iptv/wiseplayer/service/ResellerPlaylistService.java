package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.dto.request.AssignPlaylistRequest;
import com.iptv.wiseplayer.dto.request.M3uPlaylistRequest;
import com.iptv.wiseplayer.dto.request.XtreamPlaylistRequest;
import com.iptv.wiseplayer.dto.response.PlaylistResponse;

import com.iptv.wiseplayer.domain.enums.OwnerType;
import java.util.List;
import java.util.UUID;

public interface ResellerPlaylistService {

    List<PlaylistResponse> getPlaylists(UUID resellerId, OwnerType ownerType);

    PlaylistResponse createXtreamPlaylist(UUID resellerId, OwnerType ownerType, XtreamPlaylistRequest request);

    PlaylistResponse createM3uPlaylist(UUID resellerId, OwnerType ownerType, M3uPlaylistRequest request);

    PlaylistResponse assignPlaylist(UUID resellerId, OwnerType ownerType, UUID playlistId, AssignPlaylistRequest request);

    void deletePlaylist(UUID resellerId, OwnerType ownerType, UUID playlistId);
}
