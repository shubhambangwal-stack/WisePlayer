package com.iptv.wiseplayer.util;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Component
public class XtreamUrlParser {

    /**
     * Parses a possible Xtream-based M3U URL.
     * Example: http://host:port/get.php?username=XXX&password=YYY&type=m3u_plus
     */
    public XtreamDetails parse(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            if (query == null)
                return null;

            Map<String, String> params = parseQuery(query);

            if (params.containsKey("username") && params.containsKey("password")) {
                String host = uri.getScheme() + "://" + uri.getHost();
                if (uri.getPort() != -1) {
                    host += ":" + uri.getPort();
                }

                XtreamDetails details = new XtreamDetails();
                details.setServerUrl(host);
                details.setUsername(params.get("username"));
                details.setPassword(params.get("password"));
                return details;
            }
        } catch (Exception e) {
            // Not a valid URL or not an Xtream URL
        }
        return null;
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                params.put(pair.substring(0, idx), pair.substring(idx + 1));
            }
        }
        return params;
    }

    @Data
    public static class XtreamDetails {
        private String serverUrl;
        private String username;
        private String password;
    }
}
