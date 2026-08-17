package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Envelope returned by the Xtream Codes {@code get_short_epg} action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XtreamShortEpg {

    @JsonProperty("epg_listings")
    private List<XtreamEpgProgram> epgListings = new ArrayList<>();

    @JsonProperty("lastUpdate")
    private String lastUpdate;

    public List<XtreamEpgProgram> getEpgListings() {
        return epgListings;
    }

    public void setEpgListings(List<XtreamEpgProgram> epgListings) {
        this.epgListings = epgListings;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}