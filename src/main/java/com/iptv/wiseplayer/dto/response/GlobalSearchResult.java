package com.iptv.wiseplayer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Unified result item returned by the global search API.
 * Represents one stream/show from Live TV, VOD, or Series.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "A single search result item from across the full playlist (Live, VOD, or Series)")
public class GlobalSearchResult {

    public enum ContentType {
        LIVE, VOD, SERIES
    }

    @Schema(description = "Type of content: LIVE, VOD, or SERIES")
    private ContentType contentType;

    @Schema(description = "Stream or Series ID")
    private int streamId;

    @Schema(description = "Display name")
    private String name;

    @Schema(description = "Thumbnail / icon URL")
    private String streamIcon;

    @Schema(description = "Category ID the item belongs to")
    private String categoryId;

    @Schema(description = "Numeric rating (when available)")
    private String rating;

    @Schema(description = "Container extension for VOD (e.g. mkv, mp4)")
    private String containerExtension;

    @Schema(description = "Genre (Series only)")
    private String genre;

    @Schema(description = "Plot / synopsis (Series only)")
    private String plot;

    @Schema(description = "Cast (Series only)")
    private String cast;

    @Schema(description = "Director (Series only)")
    private String director;

    @Schema(description = "Release date (Series only)")
    private String releaseDate;

    // ─── Constructors ────────────────────────────────────────────────────────

    public GlobalSearchResult() {}

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public int getStreamId() { return streamId; }
    public void setStreamId(int streamId) { this.streamId = streamId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStreamIcon() { return streamIcon; }
    public void setStreamIcon(String streamIcon) { this.streamIcon = streamIcon; }

    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

    public String getRating() { return rating; }
    public void setRating(String rating) { this.rating = rating; }

    public String getContainerExtension() { return containerExtension; }
    public void setContainerExtension(String containerExtension) { this.containerExtension = containerExtension; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public String getPlot() { return plot; }
    public void setPlot(String plot) { this.plot = plot; }

    public String getCast() { return cast; }
    public void setCast(String cast) { this.cast = cast; }

    public String getDirector() { return director; }
    public void setDirector(String director) { this.director = director; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
}
