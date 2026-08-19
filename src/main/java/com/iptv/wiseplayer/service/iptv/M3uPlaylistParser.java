package com.iptv.wiseplayer.service.iptv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses M3U / M3U_plus playlists into a structured model, extracting catch-up
 * attributes ({@code catchup}, {@code catchup-days}, {@code catchup-source},
 * {@code tvg-rec}, {@code timeshift}, {@code archive}) and the EPG source
 * ({@code url-tvg}) when present.
 *
 * <p>This is a pure data-extraction parser. Capability decisions
 * (e.g. "is this channel catch-up playable?") are made by
 * {@link CatchUpService}.
 */
@Component
public class M3uPlaylistParser {

    private static final Logger log = LoggerFactory.getLogger(M3uPlaylistParser.class);

    private static final Pattern ATTR_PATTERN = Pattern.compile("([A-Za-z0-9_-]+)=\"([^\"]*)\"");
    private static final String[] CATCHUP_MARKERS = {
            "catchup", "catchup-days", "catchup-source", "tvg-rec", "timeshift", "archive"
    };
    /** Generic stream-name tokens that should not be treated as the channel id. */
    private static final java.util.Set<String> GENERIC_STREAM_NAMES = java.util.Set.of(
            "index", "stream", "mpegts", "live", "playlist", "main", "mono", "video", "channel", "ts");

    /** Parsed channel with its raw attributes and catch-up hints. */
    public static class M3uChannel {
        private String name;
        private String logo;
        private String groupTitle;
        private String streamUrl;
        private String tvgId;
        private Map<String, String> attributes = new HashMap<>();

        // Catch-up hints (raw, from the playlist)
        private boolean catchupFlag;
        private String methodRaw;
        private String source;
        private Integer days;

        public String getName() {
            return name;
        }

        public String getLogo() {
            return logo;
        }

        public String getGroupTitle() {
            return groupTitle;
        }

        public String getStreamUrl() {
            return streamUrl;
        }

        public String getTvgId() {
            return tvgId;
        }

        public void setTvgId(String tvgId) {
            this.tvgId = tvgId;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setLogo(String logo) {
            this.logo = logo;
        }

        public void setGroupTitle(String groupTitle) {
            this.groupTitle = groupTitle;
        }

        public void setStreamUrl(String streamUrl) {
            this.streamUrl = streamUrl;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public boolean isCatchupFlag() {
            return catchupFlag;
        }

        void setCatchupFlag(boolean catchupFlag) {
            this.catchupFlag = catchupFlag;
        }

        public String getMethodRaw() {
            return methodRaw;
        }

        void setMethodRaw(String methodRaw) {
            this.methodRaw = methodRaw;
        }

        public String getSource() {
            return source;
        }

        void setSource(String source) {
            this.source = source;
        }

        public Integer getDays() {
            return days;
        }

        void setDays(Integer days) {
            this.days = days;
        }
    }

    /** Parsed playlist: header attributes, EPG url and channels. */
    public static class M3uPlaylistData {
        private Map<String, String> headerAttributes = new HashMap<>();
        private String tvgUrl;
        private List<M3uChannel> channels = new ArrayList<>();

        public Map<String, String> getHeaderAttributes() {
            return headerAttributes;
        }

        public String getTvgUrl() {
            return tvgUrl;
        }

        public void setTvgUrl(String tvgUrl) {
            this.tvgUrl = tvgUrl;
        }

        public List<M3uChannel> getChannels() {
            return channels;
        }

        /** True when any channel carries an EPG-relevant identifier. */
        public boolean hasEpg() {
            return tvgUrl != null && !tvgUrl.isBlank();
        }
    }

    public M3uPlaylistData parse(String content) {
        M3uPlaylistData data = new M3uPlaylistData();
        if (content == null || content.isBlank()) {
            return data;
        }

        String[] lines = content.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("#EXTM3U")) {
                data.getHeaderAttributes().putAll(parseAttributes(line));
                data.setTvgUrl(resolveTvgUrl(data.getHeaderAttributes().get("url-tvg")));
            } else if (line.startsWith("#EXTINF")) {
                M3uChannel channel = parseChannel(line, i, lines);
                if (channel != null) {
                    data.getChannels().add(channel);
                }
            }
        }
        log.debug("Parsed {} channels from M3U content", data.getChannels().size());
        return data;
    }

    private M3uChannel parseChannel(String extinfLine, int lineIndex, String[] lines) {
        M3uChannel channel = new M3uChannel();
        Map<String, String> attrs = parseAttributes(extinfLine);
        channel.getAttributes().putAll(attrs);

        channel.setName(extinfLine.substring(extinfLine.lastIndexOf(",") + 1).trim());
        channel.setLogo(attrs.getOrDefault("tvg-logo", ""));
        channel.setGroupTitle(attrs.getOrDefault("group-title", ""));
        channel.setTvgId(attrs.getOrDefault("tvg-id", ""));

        // Find the media URL (skip auxiliary lines like #EXTGRP, #EXTVLCOPT, #KODIPROP)
        String streamUrl = null;
        for (int j = lineIndex + 1; j < lines.length; j++) {
            String next = lines[j].trim();
            if (next.isEmpty() || next.startsWith("#")) {
                continue;
            }
            if (!next.startsWith("http://") && !next.startsWith("https://") && !next.startsWith("rtmp://")
                    && !next.startsWith("rtsp://")) {
                continue;
            }
            streamUrl = next;
            break;
        }
        if (streamUrl == null) {
            return null;
        }
        channel.setStreamUrl(streamUrl);

        applyCatchUpHints(channel, attrs);
        return channel;
    }

    private void applyCatchUpHints(M3uChannel channel, Map<String, String> attrs) {
        boolean flag = false;
        String methodRaw = null;
        String source = null;
        Integer days = null;

        // catchup="xc" / "flussonic" / "shift" / "default" / "1"
        String catchup = attrs.get("catchup");
        if (catchup != null && !catchup.isBlank()) {
            flag = true;
            methodRaw = catchup;
        }

        // catchup-source="http://host/{utc}..."  (explicit URL template)
        String catchupSource = attrs.get("catchup-source");
        if (catchupSource != null && !catchupSource.isBlank()) {
            flag = true;
            source = catchupSource;
        }

        // catchup-days="7"
        String catchupDays = attrs.get("catchup-days");
        if (catchupDays != null && !catchupDays.isBlank()) {
            flag = true;
            days = parseDays(catchupDays);
        }

        // timeshift="3" -> days hint (SIPTV providers)
        String timeshift = attrs.get("timeshift");
        if (timeshift != null && !timeshift.isBlank()) {
            flag = true;
            Integer shiftDays = parseDays(timeshift);
            if (shiftDays != null && (days == null || days < shiftDays)) {
                days = shiftDays;
            }
            if (methodRaw == null) {
                methodRaw = "shift";
            }
        }

        // tvg-rec="7" / archive="1" / archive="true" (recording-based providers)
        String tvgRec = attrs.get("tvg-rec");
        if (tvgRec != null && !tvgRec.isBlank()) {
            flag = true;
            Integer recDays = parseDays(tvgRec);
            if (recDays != null && (days == null || days < recDays)) {
                days = recDays;
            }
        }
        String archive = attrs.get("archive");
        if (archive != null && !archive.isBlank() && isTruthy(archive)) {
            flag = true;
            if (methodRaw == null) {
                methodRaw = "default";
            }
        }

        // If the playlist only carries a catchup-source but no catchup= value,
        // default to "default" method for placeholder substitution.
        if (flag && methodRaw == null && source != null) {
            methodRaw = "default";
        }

        channel.catchupFlag = flag;
        channel.methodRaw = methodRaw;
        channel.source = source;
        channel.days = days;
    }

    private Integer parseDays(String raw) {
        try {
            double value = Double.parseDouble(raw.trim());
            int days = (int) Math.ceil(value);
            return days > 0 ? days : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean isTruthy(String raw) {
        String v = raw.trim().toLowerCase();
        return v.equals("1") || v.equals("true") || v.equals("yes") || v.equals("on");
    }

    private Map<String, String> parseAttributes(String line) {
        Map<String, String> attrs = new HashMap<>();
        Matcher matcher = ATTR_PATTERN.matcher(line);
        while (matcher.find()) {
            attrs.put(matcher.group(1).toLowerCase(), matcher.group(2));
        }
        return attrs;
    }

    private String resolveTvgUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        // url-tvg may contain several URLs separated by commas or semicolons
        String[] candidates = value.split("[,;]");
        for (String candidate : candidates) {
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * Derives the server origin of a playlist URL (scheme + host + port) which
     * is used as the base for catch-up URL templates.
     */
    public static String extractOrigin(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            String origin = uri.getScheme() + "://" + uri.getHost();
            if (uri.getPort() != -1) {
                origin += ":" + uri.getPort();
            }
            return origin;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extracts a stable channel identifier for catch-up URL building.
     * Prefers the numeric id embedded in the stream URL (e.g. {@code 1234} from
     * {@code .../live/user/pass/1234.ts}). When the stream name is a generic
     * token (index, stream, mpegts...), the preceding path segment is used
     * (e.g. {@code 151} from {@code .../151/index.m3u8}). Falls back to the
     * tvg-id.
     */
    public static String extractChannelId(String streamUrl, String tvgId) {
        String last = null;
        String previous = null;
        if (streamUrl != null && !streamUrl.isBlank()) {
            try {
                URI uri = new URI(streamUrl);
                String path = uri.getPath();
                if (path != null && !path.isEmpty()) {
                    String[] segments = path.split("/");
                    last = segments[segments.length - 1];
                    if (segments.length >= 2) {
                        previous = segments[segments.length - 2];
                    }
                }
            } catch (Exception ignored) {
                // fall through to simple trailing-segment extraction
            }
            if (last == null) {
                int q = streamUrl.indexOf('?');
                String candidate = q >= 0 ? streamUrl.substring(0, q) : streamUrl;
                int dot = candidate.lastIndexOf('.');
                last = dot > 0 ? candidate.substring(0, dot) : candidate;
            }
        }

        if (last != null) {
            int q = last.indexOf('?');
            if (q >= 0) {
                last = last.substring(0, q);
            }
            int dot = last.lastIndexOf('.');
            if (dot > 0) {
                last = last.substring(0, dot);
            }
            if (!last.isEmpty() && !GENERIC_STREAM_NAMES.contains(last.toLowerCase())) {
                return last;
            }
            if (previous != null && !previous.isEmpty()) {
                return previous;
            }
        }
        if (tvgId != null && !tvgId.isBlank()) {
            return tvgId;
        }
        return last != null && !last.isEmpty() ? last : null;
    }

    /**
     * Extracts username / password embedded in an Xtream-style stream URL
     * ({@code /live/{user}/{pass}/{id}.ts} or {@code user:pass@host}).
     */
    public static String[] extractCredentials(String streamUrl) {
        if (streamUrl == null || streamUrl.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(streamUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo != null && userInfo.contains(":")) {
                String[] parts = userInfo.split(":", 2);
                return new String[]{parts[0], parts[1]};
            }
            String path = uri.getPath();
            if (path != null) {
                String[] segments = path.split("/");
                for (int i = 0; i < segments.length - 2; i++) {
                    if (segments[i].equalsIgnoreCase("live") || segments[i].equalsIgnoreCase("series")) {
                        return new String[]{segments[i + 1], segments[i + 2]};
                    }
                }
            }
        } catch (Exception ignored) {
            // not a parseable URL
        }
        return null;
    }
}