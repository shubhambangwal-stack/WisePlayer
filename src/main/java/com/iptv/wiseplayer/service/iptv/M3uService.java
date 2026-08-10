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
        String content = fetchM3uContent(m3uUrl);
        Set<String> categories = new LinkedHashSet<>();

        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("#EXTINF")) {
                String groupTitle = extractAttribute(line, "group-title");
                if (groupTitle != null && !groupTitle.isEmpty()) {
                    categories.add(groupTitle);
                }
            }
        }

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
        String content = fetchM3uContent(m3uUrl);
        ArrayNode channels = objectMapper.createArrayNode();

        String[] lines = content.split("\n");
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
        return channels;
    }

    private String fetchM3uContent(String url) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(org.springframework.http.HttpHeaders.USER_AGENT, "okhttp/4.9.0");
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);

            String content = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class).getBody();
            if (content == null || !content.trim().startsWith("#EXTM3U")) {
                logger.warn("M3U fetch from {} returned empty or non-M3U content. Attempting Xtream fallback.", url);
                XtreamUrlParser.XtreamDetails details = xtreamUrlParser.parse(url);
                if (details != null) {
                    try {
                        return generateM3uFromXtream(details);
                    } catch (Exception ex) {
                        logger.error("Failed fallback M3U generation for URL {}: {}", url, ex.getMessage());
                    }
                }
            }
            return content != null ? content : "";
        } catch (Exception e) {
            logger.error("Error fetching M3U content from {}: {}. Attempting Xtream fallback.", url, e.getMessage());
            XtreamUrlParser.XtreamDetails details = xtreamUrlParser.parse(url);
            if (details != null) {
                try {
                    return generateM3uFromXtream(details);
                } catch (Exception ex) {
                    logger.error("Failed fallback M3U generation after network error for URL {}: {}", url, ex.getMessage());
                }
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
        List<XtreamCategory> categories = xtreamClient.getLiveCategories(serverUrl, username, password);
        Map<String, String> categoryMap = new HashMap<>();
        if (categories != null) {
            for (XtreamCategory cat : categories) {
                categoryMap.put(String.valueOf(cat.getCategoryId()), cat.getCategoryName());
            }
        }

        // 2. Fetch all live streams
        List<XtreamLiveStream> streams = xtreamClient.getLiveStreams(serverUrl, username, password, null);
        if (streams == null || streams.isEmpty()) {
            logger.warn("No streams returned by Xtream API for fallback");
            return "";
        }

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
