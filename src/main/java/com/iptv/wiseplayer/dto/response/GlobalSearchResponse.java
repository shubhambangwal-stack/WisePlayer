package com.iptv.wiseplayer.dto.response;

import java.util.List;

/**
 * Paginated wrapper for global search results.
 */
public class GlobalSearchResponse {

    private int page;
    private int size;
    private long totalResults;
    private int totalPages;
    private String query;
    private String filterType;   // "ALL", "LIVE", "VOD", "SERIES"
    private List<GlobalSearchResult> results;

    public GlobalSearchResponse() {}

    public GlobalSearchResponse(String query, String filterType, int page, int size,
                                 long totalResults, List<GlobalSearchResult> results) {
        this.query = query;
        this.filterType = filterType;
        this.page = page;
        this.size = size;
        this.totalResults = totalResults;
        this.totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalResults / size);
        this.results = results;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public long getTotalResults() { return totalResults; }
    public void setTotalResults(long totalResults) { this.totalResults = totalResults; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getFilterType() { return filterType; }
    public void setFilterType(String filterType) { this.filterType = filterType; }

    public List<GlobalSearchResult> getResults() { return results; }
    public void setResults(List<GlobalSearchResult> results) { this.results = results; }
}
