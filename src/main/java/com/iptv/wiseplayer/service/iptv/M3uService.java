package com.iptv.wiseplayer.service.iptv;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iptv.wiseplayer.dto.iptv.EpgProgram;
import com.iptv.wiseplayer.dto.iptv.XtreamCategory;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.util.XtreamUrlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class M3uService {

    private static final Logger logger = LoggerFactory.getLogger(M3uService.class);
    private static final long PLAYLIST_CACHE_TTL = Duration.ofMinutes(10).toMillis();
    private static final long EPG_CACHE_TTL = Duration.ofMinutes(4).toMillis();

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final XtreamUrlParser xtreamUrlParser;
    private final XtreamClient xtreamClient;
    private final M3uPlaylistParser parser;
    private final XmltvEpgParser xmltvEpgParser;
    private final CatchUpCache cache;

    public M3uService(ObjectMapper objectMapper, XtreamUrlParser xtreamUrlParser, XtreamClient xtreamClient,
            M3uPlaylistParser parser, XmltvEpgParser xmltvEpgParser, CatchUpCache cache) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = objectMapper;
        this.xtreamUrlParser = xtreamUrlParser;
        this.xtreamClient = xtreamClient;
        this.parser = parser;
        this.xmltvEpgParser = xmltvEpgParser;
        this.cache = cache;
    }

    public JsonNode getCategories(String m3uUrl) {
        logger.info("getCategories called for URL: {}", m3uUrl);
        M3uPlaylistParser.M3uPlaylistData data = getParsedPlaylist(m3uUrl);
        Set<String> categories = new LinkedHashSet<>();
        for (M3uPlaylistParser.M3uChannel channel : data.getChannels()) {
            if (channel.getGroupTitle() != null && !channel.getGroupTitle().isEmpty()) {
                categories.add(channel.getGroupTitle());
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
        M3uPlaylistParser.M3uPlaylistData data = getParsedPlaylist(m3uUrl);
        ArrayNode channels = objectMapper.createArrayNode();

        for (M3uPlaylistParser.M3uChannel channel : data.getChannels()) {
            if (categoryId != null && !categoryId.equalsIgnoreCase(channel.getGroupTitle())) {
                continue;
            }
            ObjectNode node = objectMapper.createObjectNode();
            node.put("num", channels.size() + 1);
            node.put("name", channel.getName());
            node.put("stream_id", channel.getStreamUrl()); // For M3U, the stream_id IS the URL
            node.put("stream_icon", channel.getLogo());
            node.put("category_id", channel.getGroupTitle());
            node.put("tvg_id", channel.getTvgId() != null ? channel.getTvgId() : "");
            node.put("channel_id", M3uPlaylistParser.extractChannelId(channel.getStreamUrl(), channel.getTvgId()));

            // Catch-up / archive hints
            boolean catchup = channel.isCatchupFlag() && isChannelPlayable(channel);
            node.put("catchup", catchup);
            node.put("catchup_method", channel.getMethodRaw() != null ? channel.getMethodRaw() : "");
            node.put("catchup_days", channel.getDays() != null ? String.valueOf(channel.getDays()) : "");
            node.put("catchup_source", channel.getSource() != null ? channel.getSource() : "");
            channels.add(node);
        }
        logger.info("Parsed {} channels for category: {}", channels.size(), categoryId);
        return channels;
    }

    /**
     * Returns the parsed playlist model, cached to avoid refetching the M3U URL
     * for every category / channel / EPG request.
     */
    public M3uPlaylistParser.M3uPlaylistData getParsedPlaylist(String m3uUrl) {
        String key = "m3u:playlist:" + m3uUrl.hashCode();
        M3uPlaylistParser.M3uPlaylistData data = cache.get(key);
        if (data != null) {
            return data;
        }
        String content = fetchM3uContent(m3uUrl);
        data = parser.parse(content);
        cache.put(key, data, PLAYLIST_CACHE_TTL);
        return data;
    }

    /**
     * Fetches EPG programmes for a channel within a time window using the
     * playlist's {@code url-tvg} XMLTV source (when present).
     *
     * @param m3uUrl   the playlist URL
     * @param tvgId    the channel's tvg-id to match against XMLTV programmes
     * @param channelName fallback channel name used when tvg-id is empty
     * @return EPG programmes (never null)
     */
    public List<EpgProgram> getEpg(String m3uUrl, String tvgId, String channelName, long windowStart, long windowEnd) {
        M3uPlaylistParser.M3uPlaylistData data = getParsedPlaylist(m3uUrl);
        if (!data.hasEpg()) {
            return Collections.emptyList();
        }

        String epgUrl = data.getTvgUrl();
        String cacheKey = "m3u:epg:" + epgUrl.hashCode();
        List<EpgProgram> all = cache.getOrLoad(cacheKey, EPG_CACHE_TTL, () -> {
            String xml = fetchEpgContent(epgUrl);
            return xml != null ? xmltvEpgParser.parse(xml, Long.MIN_VALUE / 2, Long.MAX_VALUE / 2) : null;
        });
        if (all == null) {
            return Collections.emptyList();
        }

        List<EpgProgram> result = new ArrayList<>();
        for (EpgProgram program : all) {
            if (matchesChannel(program.getChannelId(), tvgId, channelName)) {
                result.add(program);
            }
        }
        return result;
    }

    private boolean matchesChannel(String epgChannel, String tvgId, String channelName) {
        if (epgChannel == null || epgChannel.isBlank()) {
            return false;
        }
        if (tvgId != null && !tvgId.isBlank() && epgChannel.equalsIgnoreCase(tvgId)) {
            return true;
        }
        if (channelName != null && !channelName.isBlank()) {
            return epgChannel.equalsIgnoreCase(channelName)
                    || epgChannel.contains(channelName)
                    || channelName.contains(epgChannel);
        }
        return false;
    }

    private boolean isChannelPlayable(M3uPlaylistParser.M3uChannel channel) {
        // Playable when the playlist gives an explicit catch-up URL template,
        // or when the provider credentials are known so a URL can be built.
        return channel.getSource() != null
                || (channel.getMethodRaw() != null && credentialsAvailable(channel.getStreamUrl()));
    }

    private boolean credentialsAvailable(String streamUrl) {
        try {
            if (streamUrl == null) {
                return false;
            }
            java.net.URI uri = new java.net.URI(streamUrl);
            return uri.getUserInfo() != null
                    || (streamUrl.contains("/live/") || streamUrl.contains("/series/"));
        } catch (Exception e) {
            return false;
        }
    }

    private String fetchEpgContent(String epgUrl) {
        logger.info("Fetching XMLTV EPG from: {}", epgUrl);
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set(org.springframework.http.HttpHeaders.USER_AGENT, "okhttp/4.9.0");
            headers.setAccept(Arrays.asList(
                    org.springframework.http.MediaType.APPLICATION_XML,
                    org.springframework.http.MediaType.TEXT_XML,
                    org.springframework.http.MediaType.APPLICATION_JSON));
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            String content = restTemplate.exchange(epgUrl, org.springframework.http.HttpMethod.GET, entity, String.class)
                    .getBody();
            if (content != null && !content.trim().startsWith("<")) {
                logger.warn("EPG URL {} did not return XML content", epgUrl);
                return null;
            }
            return content;
        } catch (Exception e) {
            logger.error("Error fetching XMLTV EPG from {}: {}", epgUrl, e.getMessage(), e);
            return null;
        }
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

            // Carry TV archive metadata into the M3U so catch-up works downstream
            String archiveAttrs = "";
            if (stream.getTvArchive() == 1) {
                archiveAttrs = String.format(" catchup=\"xc\" catchup-days=\"%d\"",
                        stream.getTvArchiveDuration() > 0 ? stream.getTvArchiveDuration() : 7);
            }

            sb.append(String.format("#EXTINF:-1 tvg-id=\"%d\" tvg-logo=\"%s\" group-title=\"%s\"%s,%s\n",
                    streamId, logo, catName, archiveAttrs, name));
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