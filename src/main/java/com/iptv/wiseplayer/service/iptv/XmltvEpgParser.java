package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.EpgProgram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal XXE-safe XMLTV (EPG) parser.
 *
 * <p>Parses {@code <programme>} elements and their {@code <title>}/{@code <desc>}
 * children. Programme start/stop timestamps follow the XMLTV format
 * {@code yyyyMMddHHmmss ±HHMM}.
 */
@Component
public class XmltvEpgParser {

    private static final Logger log = LoggerFactory.getLogger(XmltvEpgParser.class);

    private static final DateTimeFormatter XMLTV_FORMAT = new DateTimeFormatterBuilder()
            .appendPattern("yyyyMMddHHmmss")
            .optionalStart()
            .appendOffset("+HHMM", "+0000")
            .optionalEnd()
            .toFormatter();

    /**
     * Parses XMLTV content and returns all programmes within the requested
     * window, for every channel in the file.
     *
     * @param xml          the raw XMLTV content
     * @param windowStart  Unix seconds; programmes starting before this are skipped
     * @param windowEnd    Unix seconds; programmes ending after this are kept
     * @return parsed programmes (never null), empty when parsing fails
     */
    public List<EpgProgram> parse(String xml, long windowStart, long windowEnd) {
        if (xml == null || xml.isBlank()) {
            return new ArrayList<>();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));

            NodeList programmes = doc.getElementsByTagName("programme");
            List<EpgProgram> result = new ArrayList<>();
            for (int i = 0; i < programmes.getLength(); i++) {
                Element el = (Element) programmes.item(i);
                Long start = parseXmltvTimestamp(el.getAttribute("start"));
                Long end = parseXmltvTimestamp(el.getAttribute("stop"));
                if (start == null || end == null || end <= windowStart || start > windowEnd) {
                    continue;
                }
                EpgProgram program = new EpgProgram();
                program.setChannelId(el.getAttribute("channel"));
                program.setId(el.getAttribute("channel") + "-" + start);
                program.setStartTs(start);
                program.setEndTs(end);
                program.setTitle(firstChildText(el, "title"));
                program.setDescription(firstChildText(el, "desc"));
                result.add(program);
            }
            log.debug("Parsed {} XMLTV programmes in window [{}, {}]", result.size(), windowStart, windowEnd);
            return result;
        } catch (Exception e) {
            log.warn("Failed to parse XMLTV EPG content: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private String firstChildText(Element parent, String tag) {
        NodeList children = parent.getElementsByTagName(tag);
        if (children.getLength() == 0) {
            return null;
        }
        return children.item(0).getTextContent();
    }

    private Long parseXmltvTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            // XMLTV: "20260601123000 +0200" -> parse LocalDateTime + offset
            String trimmed = value.trim();
            String dateTimePart = trimmed.substring(0, 14);
            LocalDateTime local = LocalDateTime.parse(dateTimePart, DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            if (trimmed.length() > 15) {
                String offsetPart = trimmed.substring(15).trim();
                ZoneOffset offset = ZoneOffset.of(offsetPart);
                return local.toEpochSecond(offset);
            }
            return local.toEpochSecond(ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Invalid XMLTV timestamp '{}': {}", value, e.getMessage());
            return null;
        }
    }
}