package com.iptv.wiseplayer.service.iptv;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class XtreamStreamResolver {

    private final SecureCredentialStore credentialStore;

    public enum StreamType {
        LIVE, VOD, SERIES
    }

    public XtreamStreamResolver(SecureCredentialStore credentialStore) {
        this.credentialStore = credentialStore;
    }

    /**
     * Resolves a stream URL for Live or VOD streams using default extensions.
     * For Series episodes, use {@link #resolveSeriesEpisodeUrl(UUID, int, String)} instead,
     * since the container extension must come from the episode metadata.
     */
    public String resolveStreamUrl(UUID playlistId, int streamId, StreamType type) {
        // Note: authentication is enforced by Spring Security before this point.
        // Making a live auth call here would reject legitimate channel switches
        // because the upstream provider still counts the previous stream as active.

        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);

        String baseUrl = normalizeBaseUrl(creds.serverUrl());

        String typePath;
        String extension;

        switch (type) {
            case LIVE:
                typePath = "live";
                extension = "ts";
                break;
            case VOD:
                typePath = "movie";
                extension = "mp4";
                break;
            default:
                throw new IllegalArgumentException(
                        "Use resolveSeriesEpisodeUrl() for SERIES streams — container extension is required.");
        }

        return String.format("%s/%s/%s/%s/%d.%s",
                baseUrl, typePath, creds.username(), creds.password(), streamId, extension);
    }

    /**
     * Resolves a playback URL for a Series episode.
     * Series URLs use the format: /series/{username}/{password}/{episode_id}.{container_extension}
     *
     * @param playlistId         the playlist / provider UUID
     * @param episodeId          the numeric episode stream id from the get_series_info response
     * @param containerExtension the container extension (e.g. "mkv", "mp4") from the episode metadata
     * @return the full playback URL
     */
    public String resolveSeriesEpisodeUrl(UUID playlistId, int episodeId, String containerExtension) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);

        String baseUrl = normalizeBaseUrl(creds.serverUrl());
        String ext = (containerExtension != null && !containerExtension.isBlank()) ? containerExtension : "mkv";

        return String.format("%s/series/%s/%s/%d.%s",
                baseUrl, creds.username(), creds.password(), episodeId, ext);
    }

    private String normalizeBaseUrl(String serverUrl) {
        return serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
    }
}
