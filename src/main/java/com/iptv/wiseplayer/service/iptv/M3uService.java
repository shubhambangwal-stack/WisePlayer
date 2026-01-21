package com.iptv.wiseplayer.service.iptv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public M3uService(ObjectMapper objectMapper) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
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
            String content = restTemplate.getForObject(url, String.class);
            return content != null ? content : "";
        } catch (Exception e) {
            logger.error("Error fetching M3U content from {}: {}", url, e.getMessage());
            return "";
        }
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
