package com.iptv.wiseplayer.service.iptv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.util.XtreamUrlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class M3uService {

    private static final Logger logger = LoggerFactory.getLogger(M3uService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final XtreamUrlParser xtreamUrlParser;
    private final XtreamClient xtreamClient;

    public M3uService(ObjectMapper objectMapper, XtreamUrlParser xtreamUrlParser, XtreamClient xtreamClient) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.xtreamUrlParser = xtreamUrlParser;
        this.xtreamClient = xtreamClient;
    }

    public JsonNode getCategories(String m3uUrl) {
        logger.info("getCategories called for URL: {}", m3uUrl);
        String content = fetchM3uContent(m3uUrl);
        Set<String> categories = new LinkedHashSet<>();

        String[] lines = content.split("\n");
        logger.info("Processing {} lines for categories", lines.length);
        for (String line : lines) {
            if (line.startsWith("#EXTINF")) {
                String groupTitle = extractAttribute(line, "group-title");
                if (groupTitle != null && !groupTitle.isEmpty()) {
                    categories.add(groupTitle);
                }
            }
        }

        logger.info("Found {} categories", categories.size());
        ArrayNode root = objectMapper.createArrayNode();
        for (String category : categories) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("category_id", category);
            node.put("category_name", category);
            root.add(node);
        }
        return root;
    }

    public JsonNode getChannels(String m3uUrl, String categoryId) {
        logger.info("getChannels called for URL: {}, categoryId: {}", m3uUrl, categoryId);
        String content = fetchM3uContent(m3uUrl);
        ArrayNode channels = objectMapper.createArrayNode();

        String[] lines = content.split("\n");
        logger.info("Processing {} lines for channels", lines.length);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("#EXTINF")) {
                String groupTitle = extractAttribute(line, "group-title");
                if (categoryId == null || categoryId.equalsIgnoreCase(groupTitle)) {
                    String logo = extractAttribute(line, "tvg-logo");
                    String name = line.substring(line.lastIndexOf(",") + 1).trim();

                    String streamUrl = "";
                    if (i + 1 < lines.length && !lines[i + 1].startsWith("#")) {
                        streamUrl = lines[i + 1].trim();
                    }

                    ObjectNode channel = objectMapper.createObjectNode();
                    channel.put("num", channels.size() + 1);
                    channel.put("name", name);
                    channel.put("stream_id", streamUrl); // For M3U, the stream_id IS the URL
                    channel.put("stream_icon", logo);
                    channel.put("category_id", groupTitle);
                    channels.add(channel);
                }
            }
        }
        logger.info("Parsed {} channels for category: {}", channels.size(), categoryId);
        return channels;
    }

    private String fetchM3uContent(String url) {
        logger.info("fetchM3uContent starting for URL: {}", url);
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(org.springframework.http.HttpHeaders.USER_AGENT, "okhttp/4.9.0");
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            logger.info("Sending HTTP GET to: {}", url);
            String content = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class).getBody();
            int len = content != null ? content.length() : 0;
            logger.info("HTTP response received, content length: {}", len);
            
            if (content == null || !content.trim().startsWith("#EXTM3U")) {
                logger.warn("M3U fetch from {} returned empty or non-M3U content. Attempting Xtream fallback. Content preview: {}", 
                        url, (content != null && content.length() > 100 ? content.substring(0, 100) : content));
                XtreamUrlParser.XtreamDetails details = xtreamUrlParser.parse(url);
                if (details != null) {
                    try {
                        return generateM3uFromXtream(details);
                    } catch (Exception ex) {
                        logger.error("Failed fallback M3U generation for URL {}: {}", url, ex.getMessage(), ex);
                    }
                } else {
                    logger.warn("URL is not Xtream-compatible, cannot fallback to Xtream API: {}", url);
                }
            }
            return content != null ? content : "";
        } catch (Exception e) {
            logger.error("Error fetching M3U content from {}: {}. Attempting Xtream fallback.", url, e.getMessage(), e);
            XtreamUrlParser.XtreamDetails details = xtreamUrlParser.parse(url);
            if (details != null) {
                try {
                    return generateM3uFromXtream(details);
                } catch (Exception ex) {
                    logger.error("Failed fallback M3U generation after network error for URL {}: {}", url, ex.getMessage(), ex);
                }
            } else {
                logger.warn("URL is not Xtream-compatible, cannot fallback to Xtream API: {}", url);
            }
            return "";
        }
    }

    private String generateM3uFromXtream(XtreamUrlParser.XtreamDetails details) {
        String serverUrl = details.getServerUrl();
        String username = details.getUsername();
        String password = details.getPassword();

        logger.info("Generating fallback M3U using Xtream API on server: {}", serverUrl);

        // 1. Fetch categories
        logger.info("Fetching categories from Xtream API...");
        List<XtreamCategory> categories = xtreamClient.getLiveCategories(serverUrl, username, password);
        Map<String, String> categoryMap = new HashMap<>();
        if (categories != null) {
            logger.info("Retrieved {} categories from Xtream API", categories.size());
            for (XtreamCategory cat : categories) {
                categoryMap.put(String.valueOf(cat.getCategoryId()), cat.getCategoryName());
            }
        } else {
            logger.warn("No categories returned by Xtream API");
        }

        // 2. Fetch all live streams
        logger.info("Fetching all live streams from Xtream API...");
        List<XtreamLiveStream> streams = xtreamClient.getLiveStreams(serverUrl, username, password, null);
        if (streams == null || streams.isEmpty()) {
            logger.warn("No streams returned by Xtream API for fallback");
            return "";
        }
        logger.info("Retrieved {} streams from Xtream API", streams.size());

        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        for (XtreamLiveStream stream : streams) {
            String name = stream.getName() != null ? stream.getName() : "Unknown";
            int streamId = stream.getStreamId();
            String catId = stream.getCategoryId();
            String catName = categoryMap.getOrDefault(catId, "Uncategorized");
            String logo = stream.getStreamIcon() != null ? stream.getStreamIcon() : "";

            String normalizedServer = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
            String streamUrl = String.format("%s/live/%s/%s/%d.ts", normalizedServer, username, password, streamId);

            sb.append(String.format("#EXTINF:-1 tvg-id=\"%d\" tvg-logo=\"%s\" group-title=\"%s\",%s\n",
                    streamId, logo, catName, name));
            sb.append(streamUrl).append("\n");
        }

        logger.info("Successfully generated fallback M3U playlist with {} channels", streams.size());
        return sb.toString();
    }

    private String extractAttribute(String line, String attribute) {
        Pattern pattern = Pattern.compile(attribute + "=\"(.*?)\"");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
