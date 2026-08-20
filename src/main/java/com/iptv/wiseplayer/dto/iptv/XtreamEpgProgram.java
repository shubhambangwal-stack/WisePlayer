package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A single EPG entry returned by the Xtream Codes
 * {@code get_simple_data_table} / {@code get_short_epg} actions.
 *
 * <p><strong>Note on encoding:</strong> Xtream Codes returns {@code title} and
 * {@code description} as Base64-encoded strings. Use {@link #getDecodedTitle()}
 * and {@link #getDecodedDescription()} to get human-readable values.
 * Providers that skip encoding are handled transparently — the decoder falls
 * back to the raw value when the result is not valid UTF-8 text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XtreamEpgProgram {

    @JsonProperty("id")
    private String id;

    @JsonProperty("epg_id")
    private String epgId;

    /** Raw (Base64-encoded) title — use {@link #getDecodedTitle()} for display. */
    @JsonProperty("title")
    private String title;

    @JsonProperty("lang")
    private String lang;

    @JsonProperty("start")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FlexibleTimestampDeserializer.class)
    private long start;

    @JsonProperty("end")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FlexibleTimestampDeserializer.class)
    private long end;

    /**
     * Flexible Jackson deserializer that handles both numeric Unix timestamps
     * (seconds) and formatted date-time strings (e.g. "2026-08-20 08:30:00").
     */
    public static class FlexibleTimestampDeserializer extends com.fasterxml.jackson.databind.JsonDeserializer<Long> {
        private static final java.time.format.DateTimeFormatter FORMATTER =
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        @Override
        public Long deserialize(com.fasterxml.jackson.core.JsonParser p,
                                com.fasterxml.jackson.databind.DeserializationContext ctxt)
                throws java.io.IOException {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return 0L;
            }
            text = text.trim();
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                // Try parsing as "yyyy-MM-dd HH:mm:ss"
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(text, FORMATTER);
                    return ldt.toEpochSecond(java.time.ZoneOffset.UTC);
                } catch (Exception e) {
                    // Try ISO-8601 fallback
                    try {
                        return java.time.Instant.parse(text).getEpochSecond();
                    } catch (Exception ex) {
                        return 0L;
                    }
                }
            }
        }
    }

    /** Raw (Base64-encoded) description — use {@link #getDecodedDescription()} for display. */
    @JsonProperty("description")
    private String description;

    @JsonProperty("has_archive")
    private boolean hasArchive;

    @JsonProperty("channel_id")
    private String channelId;

    // ── Decoded accessors ────────────────────────────────────────────────────

    /**
     * Returns the human-readable title, Base64-decoded from the raw value
     * supplied by the Xtream Codes API. Falls back to the raw value when
     * decoding fails (e.g. provider omitted encoding).
     */
    public String getDecodedTitle() {
        return decodeBase64Safe(title);
    }

    /**
     * Returns the human-readable description, Base64-decoded from the raw
     * value supplied by the Xtream Codes API. Returns {@code null} when no
     * description is present.
     */
    public String getDecodedDescription() {
        return decodeBase64Safe(description);
    }

    /**
     * Safely decodes a potentially Base64-encoded string.
     *
     * <p>Strategy:
     * <ol>
     *   <li>If the value is null or blank, return it as-is.</li>
     *   <li>Attempt Base64 decoding.</li>
     *   <li>If the decoded bytes are valid UTF-8 and the result is printable
     *       text, return the decoded string.</li>
     *   <li>Otherwise return the original raw value (provider skipped encoding).</li>
     * </ol>
     */
    public static String decodeBase64Safe(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(raw.trim());
            String text = new String(decoded, StandardCharsets.UTF_8);
            // Sanity-check: decoded text should not contain control characters
            // that would indicate a failed decode attempt.
            if (isPrintable(text)) {
                return text;
            }
        } catch (IllegalArgumentException ignored) {
            // Not valid Base64 — treat as plain text
        }
        return raw;
    }

    /** Returns {@code true} when the string contains only printable characters. */
    private static boolean isPrintable(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 && c != '\t' && c != '\n' && c != '\r') {
                return false;
            }
        }
        return true;
    }

    // ── Raw getters / setters (for Jackson) ─────────────────────────────────

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEpgId() { return epgId; }
    public void setEpgId(String epgId) { this.epgId = epgId; }

    /** Returns the raw (possibly Base64-encoded) title. Prefer {@link #getDecodedTitle()}. */
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }

    public long getStart() { return start; }
    public void setStart(long start) { this.start = start; }

    public long getEnd() { return end; }
    public void setEnd(long end) { this.end = end; }

    /** Returns the raw (possibly Base64-encoded) description. Prefer {@link #getDecodedDescription()}. */
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isHasArchive() { return hasArchive; }
    public void setHasArchive(boolean hasArchive) { this.hasArchive = hasArchive; }

    public String getChannelId() { return channelId; }
    public void setChannelId(String channelId) { this.channelId = channelId; }
}