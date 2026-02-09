package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
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
}
