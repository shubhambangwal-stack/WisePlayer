package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamEpgProgram;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.dto.iptv.XtreamSeriesInfo;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class XtreamCatalogService {

    private final XtreamClient xtreamClient;
    private final SecureCredentialStore credentialStore;

    public XtreamCatalogService(XtreamClient xtreamClient, SecureCredentialStore credentialStore) {
        this.xtreamClient = xtreamClient;
        this.credentialStore = credentialStore;
    }

    public List<XtreamCategory> getLiveCategories(UUID playlistId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getLiveCategories(creds.serverUrl(), creds.username(), creds.password());
    }

    public List<XtreamLiveStream> getLiveStreams(UUID playlistId, String categoryId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getLiveStreams(creds.serverUrl(), creds.username(), creds.password(), categoryId);
    }

    public List<XtreamCategory> getVodCategories(UUID playlistId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getVodCategories(creds.serverUrl(), creds.username(), creds.password());
    }

    public List<XtreamVodStream> getVodStreams(UUID playlistId, String categoryId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getVodStreams(creds.serverUrl(), creds.username(), creds.password(), categoryId);
    }

    public List<XtreamCategory> getSeriesCategories(UUID playlistId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getSeriesCategories(creds.serverUrl(), creds.username(), creds.password());
    }

    public List<XtreamSeries> getSeries(UUID playlistId, String categoryId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getSeries(creds.serverUrl(), creds.username(), creds.password(), categoryId);
    }

    /**
     * Fetches full series info (seasons + episodes) by series ID.
     * This calls the get_series_info action on the Xtream Codes API.
     */
    public XtreamSeriesInfo getSeriesInfo(UUID playlistId, int seriesId) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getSeriesInfo(creds.serverUrl(), creds.username(), creds.password(), seriesId);
    }

    /**
     * Fetches the current and upcoming EPG programmes for a live stream.
     */
    public List<XtreamEpgProgram> getShortEpg(UUID playlistId, int streamId, int limit) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getShortEpg(creds.serverUrl(), creds.username(), creds.password(), streamId, limit);
    }

    /**
     * Fetches EPG / archive data (get_simple_data_table) for a live stream
     * within a time window. Includes past programmes when archive is available.
     */
    public List<XtreamEpgProgram> getSimpleDataTable(UUID playlistId, int streamId, long start, long end) {
        SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
        return xtreamClient.getSimpleDataTable(creds.serverUrl(), creds.username(), creds.password(), streamId, start, end);
    }
}
