package com.iptv.wiseplayer.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ResellerAnalyticsResponse {
    private LocalDate date;
    private long activations;
    private BigDecimal revenue;

    public ResellerAnalyticsResponse() {}

    public ResellerAnalyticsResponse(LocalDate date, long activations, BigDecimal revenue) {
        this.date = date;
        this.activations = activations;
        this.revenue = revenue;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public long getActivations() { return activations; }
    public void setActivations(long activations) { this.activations = activations; }

    public BigDecimal getRevenue() { return revenue; }
    public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }

    public static ResellerAnalyticsResponseBuilder builder() {
        return new ResellerAnalyticsResponseBuilder();
    }

    public static class ResellerAnalyticsResponseBuilder {
        private LocalDate date;
        private long activations;
        private BigDecimal revenue;

        public ResellerAnalyticsResponseBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }
        public ResellerAnalyticsResponseBuilder activations(long activations) {
            this.activations = activations;
            return this;
        }
        public ResellerAnalyticsResponseBuilder revenue(BigDecimal revenue) {
            this.revenue = revenue;
            return this;
        }
        public ResellerAnalyticsResponse build() {
            return new ResellerAnalyticsResponse(date, activations, revenue);
        }
    }
}
