package com.iptv.wiseplayer.dto.iptv;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO for the Xtream Codes get_series_info response.
 * The response contains top-level series info, a seasons map (keyed by season number string),
 * and an episodes map (keyed by season number string, values are lists of episodes).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XtreamSeriesInfo {

    @JsonProperty("info")
    private Info info;

    /**
     * Map of season number (as string) -> Season metadata.
     */
    @JsonProperty("seasons")
    private Object seasons; // Some providers return a list, others a map - use raw Object

    /**
     * Map of season number (as string) -> List of episodes.
     */
    @JsonProperty("episodes")
    private Map<String, List<Episode>> episodes;

    // ── Info ──────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Info {
        @JsonProperty("name")
        private String name;

        @JsonProperty("cover")
        private String cover;

        @JsonProperty("plot")
        private String plot;

        @JsonProperty("cast")
        private String cast;

        @JsonProperty("director")
        private String director;

        @JsonProperty("genre")
        private String genre;

        @JsonProperty("releaseDate")
        private String releaseDate;

        @JsonProperty("last_modified")
        private String lastModified;

        @JsonProperty("rating")
        private String rating;

        @JsonProperty("rating_5based")
        private Object rating5Based;

        @JsonProperty("backdrop_path")
        private List<String> backdropPath;

        @JsonProperty("youtube_trailer")
        private String youtubeTrailer;

        @JsonProperty("episode_run_time")
        private String episodeRunTime;

        @JsonProperty("category_id")
        private String categoryId;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCover() { return cover; }
        public void setCover(String cover) { this.cover = cover; }

        public String getPlot() { return plot; }
        public void setPlot(String plot) { this.plot = plot; }

        public String getCast() { return cast; }
        public void setCast(String cast) { this.cast = cast; }

        public String getDirector() { return director; }
        public void setDirector(String director) { this.director = director; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

        public String getLastModified() { return lastModified; }
        public void setLastModified(String lastModified) { this.lastModified = lastModified; }

        public String getRating() { return rating; }
        public void setRating(String rating) { this.rating = rating; }

        public Object getRating5Based() { return rating5Based; }
        public void setRating5Based(Object rating5Based) { this.rating5Based = rating5Based; }

        public List<String> getBackdropPath() { return backdropPath; }
        public void setBackdropPath(List<String> backdropPath) { this.backdropPath = backdropPath; }

        public String getYoutubeTrailer() { return youtubeTrailer; }
        public void setYoutubeTrailer(String youtubeTrailer) { this.youtubeTrailer = youtubeTrailer; }

        public String getEpisodeRunTime() { return episodeRunTime; }
        public void setEpisodeRunTime(String episodeRunTime) { this.episodeRunTime = episodeRunTime; }

        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    }

    // ── Episode ───────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Episode {
        @JsonProperty("id")
        private String id;

        @JsonProperty("episode_num")
        private int episodeNum;

        @JsonProperty("title")
        private String title;

        @JsonProperty("container_extension")
        private String containerExtension;

        @JsonProperty("info")
        private EpisodeInfo info;

        @JsonProperty("added")
        private String added;

        @JsonProperty("season")
        private int season;

        @JsonProperty("direct_source")
        private String directSource;

        @JsonIgnore
        private Map<String, Object> additionalProperties = new HashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getAdditionalProperties() {
            return this.additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperty(String name, Object value) {
            this.additionalProperties.put(name, value);
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public int getEpisodeNum() { return episodeNum; }
        public void setEpisodeNum(int episodeNum) { this.episodeNum = episodeNum; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getContainerExtension() { return containerExtension; }
        public void setContainerExtension(String containerExtension) { this.containerExtension = containerExtension; }

        public EpisodeInfo getInfo() { return info; }
        public void setInfo(EpisodeInfo info) { this.info = info; }

        public String getAdded() { return added; }
        public void setAdded(String added) { this.added = added; }

        public int getSeason() { return season; }
        public void setSeason(int season) { this.season = season; }

        public String getDirectSource() { return directSource; }
        public void setDirectSource(String directSource) { this.directSource = directSource; }
    }

    // ── EpisodeInfo ───────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EpisodeInfo {
        @JsonProperty("movie_image")
        private String movieImage;

        @JsonProperty("plot")
        private String plot;

        @JsonProperty("releasedate")
        private String releaseDate;

        @JsonProperty("rating")
        private Object rating;

        @JsonProperty("duration_secs")
        private long durationSecs;

        @JsonProperty("duration")
        private String duration;

        public String getMovieImage() { return movieImage; }
        public void setMovieImage(String movieImage) { this.movieImage = movieImage; }

        public String getPlot() { return plot; }
        public void setPlot(String plot) { this.plot = plot; }

        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

        public Object getRating() { return rating; }
        public void setRating(Object rating) { this.rating = rating; }

        public long getDurationSecs() { return durationSecs; }
        public void setDurationSecs(long durationSecs) { this.durationSecs = durationSecs; }

        public String getDuration() { return duration; }
        public void setDuration(String duration) { this.duration = duration; }
    }

    // ── Root getters/setters ──────────────────────────────────────────────────

    public Info getInfo() { return info; }
    public void setInfo(Info info) { this.info = info; }

    public Object getSeasons() { return seasons; }
    public void setSeasons(Object seasons) { this.seasons = seasons; }

    public Map<String, List<Episode>> getEpisodes() { return episodes; }
    public void setEpisodes(Map<String, List<Episode>> episodes) { this.episodes = episodes; }
}
