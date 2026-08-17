package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.EpgProgram;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XmltvEpgParserTest {

    private final XmltvEpgParser parser = new XmltvEpgParser();

    @Test
    void parsesProgrammesWithinWindow() {
        long windowStart = Instant.parse("2026-06-01T00:00:00Z").getEpochSecond();
        long windowEnd = Instant.parse("2026-06-01T23:59:59Z").getEpochSecond();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <tv>
                  <programme start="20260601100000 +0200" stop="20260601110000 +0200" channel="cnn.us">
                    <title>Morning News</title>
                    <desc>Headlines</desc>
                  </programme>
                  <programme start="20260602000000 +0000" stop="20260602010000 +0000" channel="cnn.us">
                    <title>Tomorrow Show</title>
                  </programme>
                </tv>
                """;

        List<EpgProgram> programs = parser.parse(xml, windowStart, windowEnd);

        assertEquals(1, programs.size());
        EpgProgram morning = programs.get(0);
        assertEquals("cnn.us", morning.getChannelId());
        assertEquals("Morning News", morning.getTitle());
        assertEquals("Headlines", morning.getDescription());
        assertEquals(Instant.parse("2026-06-01T08:00:00Z").getEpochSecond(), morning.getStartTs());
        assertEquals(Instant.parse("2026-06-01T09:00:00Z").getEpochSecond(), morning.getEndTs());
    }

    @Test
    void returnsEmptyForMalformedContent() {
        assertTrue(parser.parse("not xml at all", 0, 1000).isEmpty());
        assertTrue(parser.parse("", 0, 1000).isEmpty());
        assertTrue(parser.parse(null, 0, 1000).isEmpty());
    }
}