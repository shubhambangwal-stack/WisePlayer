package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.service.iptv.M3uPlaylistParser.M3uChannel;
import com.iptv.wiseplayer.service.iptv.M3uPlaylistParser.M3uPlaylistData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class M3uPlaylistParserTest {

    private final M3uPlaylistParser parser = new M3uPlaylistParser();

    @Test
    void parsesHeaderTvgUrlAndChannels() {
        String m3u = """
                #EXTM3U url-tvg="http://epg.example.com/guide.xml.gz" tvg-shift="1"
                #EXTINF:-1 tvg-id="cnn.us" tvg-logo="http://img/cnn.png" group-title="News" catchup="xc" catchup-days="7",CNN
                http://host/live/user/pass/555.ts
                #EXTINF:-1 tvg-id="espn.us" group-title="Sports",ESPN
                http://host/live/user/pass/556.ts
                """;

        M3uPlaylistData data = parser.parse(m3u);

        assertEquals("http://epg.example.com/guide.xml.gz", data.getTvgUrl());
        assertTrue(data.hasEpg());
        assertEquals(2, data.getChannels().size());

        M3uChannel cnn = data.getChannels().get(0);
        assertEquals("CNN", cnn.getName());
        assertEquals("http://img/cnn.png", cnn.getLogo());
        assertEquals("News", cnn.getGroupTitle());
        assertEquals("cnn.us", cnn.getTvgId());
        assertEquals("http://host/live/user/pass/555.ts", cnn.getStreamUrl());
        assertTrue(cnn.isCatchupFlag());
        assertEquals("xc", cnn.getMethodRaw());
        assertEquals(Integer.valueOf(7), cnn.getDays());
    }

    @Test
    void parsesCatchupSourceAndTimeshiftHints() {
        String m3u = """
                #EXTM3U
                #EXTINF:-1 tvg-id="cnn.us" timeshift="3" catchup="shift" catchup-days="3" catchup-source="https://m3u-server/arch-{utc}-{duration}.m3u8",CNN
                http://m3u-server/stream.m3u8
                #EXTINF:-1 tvg-id="bbc.uk" tvg-rec="5",BBC
                http://host2/bbc/stream.ts
                """;

        M3uPlaylistData data = parser.parse(m3u);

        M3uChannel cnn = data.getChannels().get(0);
        assertTrue(cnn.isCatchupFlag());
        assertEquals("shift", cnn.getMethodRaw());
        assertEquals("https://m3u-server/arch-{utc}-{duration}.m3u8", cnn.getSource());
        assertEquals(Integer.valueOf(3), cnn.getDays());

        M3uChannel bbc = data.getChannels().get(1);
        assertTrue(bbc.isCatchupFlag());
        assertEquals(Integer.valueOf(5), bbc.getDays());
        assertNull(bbc.getMethodRaw());
    }

    @Test
    void skipsAuxiliaryLinesWhenFindingStreamUrl() {
        String m3u = """
                #EXTM3U
                #EXTINF:-1 catchup="default",Channel A
                #EXTGRP:News
                #EXTVLCOPT:http-user-agent=Mozilla/5.0
                http://host/a.m3u8
                """;

        M3uPlaylistData data = parser.parse(m3u);

        assertEquals(1, data.getChannels().size());
        assertEquals("http://host/a.m3u8", data.getChannels().get(0).getStreamUrl());
    }

    @Test
    void extractsChannelIdFromStreamUrl() {
        assertEquals("1234", M3uPlaylistParser.extractChannelId("http://host/live/user/pass/1234.ts", "ignored"));
        assertEquals("151", M3uPlaylistParser.extractChannelId("http://ch01.spr24.net/151/index.m3u8?token=x", null));
        assertEquals("cnn.us", M3uPlaylistParser.extractChannelId("http://host/stream.ts", "cnn.us"));
    }

    @Test
    void extractsCredentialsFromXtreamStyleStreamUrl() {
        String[] creds = M3uPlaylistParser.extractCredentials("http://host/live/user/pass/1234.ts");
        assertArrayEquals(new String[]{"user", "pass"}, creds);

        assertNull(M3uPlaylistParser.extractCredentials("http://ch01.spr24.net/151/index.m3u8"));
    }

    @Test
    void derivesOriginFromUrl() {
        assertEquals("http://host:8080", M3uPlaylistParser.extractOrigin("http://host:8080/get.php?username=u&password=p"));
        assertEquals("https://m3u-server", M3uPlaylistParser.extractOrigin("https://m3u-server/stream.m3u8"));
    }
}