package com.iptv.wiseplayer.service.iptv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

/**
 * Client for interacting with Xtream Codes player_api.php.
 */
@Component
public class XtreamClient {

    private static final Logger logger = LoggerFactory.getLogger(XtreamClient.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public XtreamClient(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
    }

    /**
     * Authenticates with Xtream Codes server.
     *
     * @param serverUrl Xtream server URL
     * @param username  Username
     * @param password  Password
     * @return User info if successful, empty otherwise.
     */
    public Optional<JsonNode> authenticate(String serverUrl, String username, String password) {
        String url = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/player_api.php")
                .queryParam("username", username)
                .queryParam("password", password)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root.has("user_info") && "Active".equalsIgnoreCase(root.path("user_info").path("status").asText())) {
                return Optional.of(root);
            }
            logger.warn("Authentication failed or user inactive for {}: {}", username, response);
        } catch (Exception e) {
            logger.error("Error authenticating with Xtream server: {}", e.getMessage());
        }
        return Optional.empty();
    }

    /**
     * Fetches live categories from Xtream server.
     */
    public JsonNode getLiveCategories(String serverUrl, String username, String password) {
        return fetchXtreamData(serverUrl, username, password, "get_live_categories");
    }

    /**
     * Fetches live streams for a category.
     */
    public JsonNode getLiveStreams(String serverUrl, String username, String password, String categoryId) {
        String url = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/player_api.php")
                .queryParam("username", username)
                .queryParam("password", password)
                .queryParam("action", "get_live_streams")
                .queryParam("category_id", categoryId)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Error fetching live streams: {}", e.getMessage());
            return objectMapper.createArrayNode();
        }
    }

    private JsonNode fetchXtreamData(String serverUrl, String username, String password, String action) {
        String url = UriComponentsBuilder.fromHttpUrl(serverUrl)
                .path("/player_api.php")
                .queryParam("username", username)
                .queryParam("password", password)
                .queryParam("action", action)
                .toUriString();

        try {
            String response = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Error fetching action {}: {}", action, e.getMessage());
            return objectMapper.createArrayNode();
        }
    }
}
