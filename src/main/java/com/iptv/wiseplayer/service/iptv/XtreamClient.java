package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.XtreamAuthResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamEpgProgram;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.dto.iptv.XtreamSeries;
import com.iptv.wiseplayer.dto.iptv.XtreamSeriesInfo;
import com.iptv.wiseplayer.dto.iptv.XtreamShortEpg;
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
        logger.info("getLiveCategories starting. Server: {}, URL: {}", serverUrl, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamCategory> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamCategory>>() {
                    }).getBody();
            logger.info("getLiveCategories completed. Retrieved {} categories.", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching live categories: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches live streams for a category.
     */
    public List<XtreamLiveStream> getLiveStreams(String serverUrl, String username, String password,
            String categoryId) {
        UriComponentsBuilder builder = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_live_streams");
        if (categoryId != null && !categoryId.trim().isEmpty()) {
            builder.queryParam("category_id", categoryId);
        }
        String url = builder.toUriString();
        logger.info("getLiveStreams starting. Server: {}, categoryId: {}, URL: {}", serverUrl, categoryId, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamLiveStream> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamLiveStream>>() {
                    }).getBody();
            logger.info("getLiveStreams completed. Retrieved {} streams.", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching live streams: {}", e.getMessage(), e);
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
        logger.info("getVodCategories starting. Server: {}, URL: {}", serverUrl, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamCategory> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamCategory>>() {
                    }).getBody();
            logger.info("getVodCategories completed. Retrieved {} categories.", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching VOD categories: {}", e.getMessage(), e);
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
        logger.info("getVodStreams starting. Server: {}, categoryId: {}, URL: {}", serverUrl, categoryId, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamVodStream> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamVodStream>>() {
                    }).getBody();
            logger.info("getVodStreams completed. Retrieved {} VOD streams.", result != null ? result.size() : 0);
            if (result != null && !result.isEmpty()) {
                logger.info("Sample VOD stream containerExtension: streamId={}, ext={}", result.get(0).getStreamId(), result.get(0).getContainerExtension());
            }
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching VOD streams: {}", e.getMessage(), e);
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
        logger.info("getSeriesCategories starting. Server: {}, URL: {}", serverUrl, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamCategory> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamCategory>>() {
                    }).getBody();
            logger.info("getSeriesCategories completed. Retrieved {} series categories.", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching Series categories: {}", e.getMessage(), e);
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
        logger.info("getSeries starting. Server: {}, categoryId: {}, URL: {}", serverUrl, categoryId, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamSeries> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamSeries>>() {
                    }).getBody();
            logger.info("getSeries completed. Retrieved {} series.", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching Series: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches detailed Series info including seasons and episodes.
     * Uses the get_series_info action with a series_id.
     */
    public XtreamSeriesInfo getSeriesInfo(String serverUrl, String username, String password, int seriesId) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_series_info")
                .queryParam("series_id", seriesId)
                .toUriString();
        logger.info("getSeriesInfo starting. Server: {}, seriesId: {}, URL: {}", serverUrl, seriesId, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            XtreamSeriesInfo result = restTemplate.exchange(url, HttpMethod.GET, entity, XtreamSeriesInfo.class).getBody();
            logger.info("getSeriesInfo completed successfully. Result: {}", result != null ? "Found" : "Null");
            return result;
        } catch (Exception e) {
            logger.error("Error fetching Series info for series_id={}: {}", seriesId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches the current and upcoming EPG programmes for a live stream.
     * Uses the get_short_epg action with a stream_id.
     *
     * @return EPG entries (never null), or an empty list when the provider does
     *         not expose EPG data or the request fails
     */
    public List<XtreamEpgProgram> getShortEpg(String serverUrl, String username, String password, int streamId, int limit) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_short_epg")
                .queryParam("stream_id", streamId)
                .queryParam("limit", limit)
                .toUriString();
        logger.info("getShortEpg starting. Server: {}, streamId: {}, URL: {}", serverUrl, streamId, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            XtreamShortEpg result = restTemplate.exchange(url, HttpMethod.GET, entity, XtreamShortEpg.class).getBody();
            List<XtreamEpgProgram> listings = result != null ? result.getEpgListings() : null;
            logger.info("getShortEpg completed. Retrieved {} EPG entries.", listings != null ? listings.size() : 0);
            return listings != null ? listings : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching short EPG for stream_id={}: {}", streamId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Fetches EPG / archive data for a live stream within a time window.
     * Uses the get_simple_data_table action which returns past programmes
     * (i.e. the TV archive) as well as current and upcoming ones.
     *
     * @param start Unix timestamp (seconds) of the window start
     * @param end   Unix timestamp (seconds) of the window end
     * @return EPG entries (never null), or an empty list when no archive data
     *         exists or the request fails
     */
    public List<XtreamEpgProgram> getSimpleDataTable(String serverUrl, String username, String password,
            int streamId, long start, long end) {
        String url = buildBaseUrl(serverUrl, username, password)
                .queryParam("action", "get_simple_data_table")
                .queryParam("stream_id", streamId)
                .queryParam("start", start)
                .queryParam("end", end)
                .toUriString();
        logger.info("getSimpleDataTable starting. Server: {}, streamId: {}, range: [{}, {}], URL: {}",
                serverUrl, streamId, start, end, url);

        try {
            HttpEntity<Void> entity = new HttpEntity<>(createHeaders());
            List<XtreamEpgProgram> result = restTemplate
                    .exchange(url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<XtreamEpgProgram>>() {
                    }).getBody();
            logger.info("getSimpleDataTable completed. Retrieved {} EPG entries.", result != null ? result.size() : 0);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            logger.error("Error fetching simple data table for stream_id={}: {}", streamId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
