package com.iptv.wiseplayer.dto.response;

import java.math.BigDecimal;

public class ResellerStatsResponse {
    private long totalUsers;
    private long activeSubscriptions;
    private double growthPercentage;
    private BigDecimal remainingCredits;
    private String partnerLevel;
    private String peakActivationTime;

    public ResellerStatsResponse() {}

    public ResellerStatsResponse(long totalUsers, long activeSubscriptions, double growthPercentage, 
                                BigDecimal remainingCredits, String partnerLevel, String peakActivationTime) {
        this.totalUsers = totalUsers;
        this.activeSubscriptions = activeSubscriptions;
        this.growthPercentage = growthPercentage;
        this.remainingCredits = remainingCredits;
        this.partnerLevel = partnerLevel;
        this.peakActivationTime = peakActivationTime;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveSubscriptions() { return activeSubscriptions; }
    public void setActiveSubscriptions(long activeSubscriptions) { this.activeSubscriptions = activeSubscriptions; }

    public double getGrowthPercentage() { return growthPercentage; }
    public void setGrowthPercentage(double growthPercentage) { this.growthPercentage = growthPercentage; }

    public BigDecimal getRemainingCredits() { return remainingCredits; }
    public void setRemainingCredits(BigDecimal remainingCredits) { this.remainingCredits = remainingCredits; }

    public String getPartnerLevel() { return partnerLevel; }
    public void setPartnerLevel(String partnerLevel) { this.partnerLevel = partnerLevel; }

    public String getPeakActivationTime() { return peakActivationTime; }
    public void setPeakActivationTime(String peakActivationTime) { this.peakActivationTime = peakActivationTime; }

    public static ResellerStatsResponseBuilder builder() {
        return new ResellerStatsResponseBuilder();
    }

    public static class ResellerStatsResponseBuilder {
        private long totalUsers;
        private long activeSubscriptions;
        private double growthPercentage;
        private BigDecimal remainingCredits;
        private String partnerLevel;
        private String peakActivationTime;

        public ResellerStatsResponseBuilder totalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }
        public ResellerStatsResponseBuilder activeSubscriptions(long activeSubscriptions) {
            this.activeSubscriptions = activeSubscriptions;
            return this;
        }
        public ResellerStatsResponseBuilder growthPercentage(double growthPercentage) {
            this.growthPercentage = growthPercentage;
            return this;
        }
        public ResellerStatsResponseBuilder remainingCredits(BigDecimal remainingCredits) {
            this.remainingCredits = remainingCredits;
            return this;
        }
        public ResellerStatsResponseBuilder partnerLevel(String partnerLevel) {
            this.partnerLevel = partnerLevel;
            return this;
        }
        public ResellerStatsResponseBuilder peakActivationTime(String peakActivationTime) {
            this.peakActivationTime = peakActivationTime;
            return this;
        }
        public ResellerStatsResponse build() {
            return new ResellerStatsResponse(totalUsers, activeSubscriptions, growthPercentage, 
                                           remainingCredits, partnerLevel, peakActivationTime);
        }
    }
}
