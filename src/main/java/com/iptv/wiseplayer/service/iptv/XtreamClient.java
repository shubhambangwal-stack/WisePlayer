package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.dto.iptv.XtreamVodStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Client for interacting with Xtream Codes player_api.php.
 * Compliant with production requirements (okhttp User-Agent, typed DTOs).
 */
@Component
public class XtreamClient {

    private static final Logger logger = LoggerFactory.getLogger(XtreamClient.class);
    private static final String USER_AGENT = "okhttp/4.9.0";

    private final RestTemplate restTemplate;

    public XtreamClient() {
        this.restTemplate = new RestTemplate();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, USER_AGENT);
        return headers;
    }

    /**
     * Authenticates with Xtream Codes server.
     */
    public Optional<XtreamAuthResponse> authenticate(String serverUrl, String username, String password) {
        String url = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/player_api.php")
                .queryParam("username", username)
                .queryParam("password", password)
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            XtreamAuthResponse response = restTemplate.exchange(url, HttpMethod.GET, entity, XtreamAuthResponse.class)
                    .getBody();

            if (response != null && response.getUserInfo() != null) {
                return Optional.of(response);
            }
            logger.warn("Authentication failed for {}: No user info in response", username);
        } catch (Exception e) {
            logger.error("Error authenticating with Xtream server: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Fetches live categories from Xtream server.
     */
    public List<XtreamCategory> getLiveCategories(String serverUrl, String username, String password) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_live_categories")
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            return restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamCategory>>() {
                    }).getBody();
        } catch (Exception e) {
            logger.error("Error fetching live categories: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches live streams for a category.
     */
    public List<XtreamLiveStream> getLiveStreams(String serverUrl, String username, String password,
            String categoryId) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_live_streams")
                .queryParam("category_id", categoryId)
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            return restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamLiveStream>>() {
                    }).getBody();
        } catch (Exception e) {
            logger.error("Error fetching live streams: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private UriComponentsBuilder buildBaseUrl(String serverUrl, String username, String password) {
        return UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/player_api.php")
                .queryParam("username", username)
                .queryParam("password", password);
    }

    /**
     * Fetches VOD categories from Xtream server.
     */
    public List<XtreamCategory> getVodCategories(String serverUrl, String username, String password) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_vod_categories")
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            return restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamCategory>>() {
                    }).getBody();
        } catch (Exception e) {
            logger.error("Error fetching VOD categories: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches VOD streams for a category.
     */
    public List<XtreamVodStream> getVodStreams(String serverUrl, String username, String password,
            String categoryId) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_vod_streams")
                .queryParam("category_id", categoryId)
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            return restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamVodStream>>() {
                    }).getBody();
        } catch (Exception e) {
            logger.error("Error fetching VOD streams: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches Series categories from Xtream server.
     */
    public List<XtreamCategory> getSeriesCategories(String serverUrl, String username, String password) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_series_categories")
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            return restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamCategory>>() {
                    }).getBody();
        } catch (Exception e) {
            logger.error("Error fetching Series categories: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetches Series for a category.
     */
    public List<XtreamSeries> getSeries(String serverUrl, String username, String password,
            String categoryId) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_series")
                .queryParam("category_id", categoryId)
                .toUriString();

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            return restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamSeries>>() {
                    }).getBody();
        } catch (Exception e) {
            logger.error("Error fetching Series: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
