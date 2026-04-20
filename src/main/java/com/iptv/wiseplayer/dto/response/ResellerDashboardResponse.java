package com.iptv.wiseplayer.dto.response;

import java.util.List;
import com.iptv.wiseplayer.domain.entity.Device;
import java.math.BigDecimal;

public class ResellerDashboardResponse {
    private long totalUsers;
    private long activeSubscriptions;
    private long pendingRequests;
    private BigDecimal credits;
    private List<Device> recentUsers;

    public ResellerDashboardResponse() {}

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveSubscriptions() { return activeSubscriptions; }
    public void setActiveSubscriptions(long activeSubscriptions) { this.activeSubscriptions = activeSubscriptions; }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }

    public List<Device> getRecentUsers() { return recentUsers; }
    public void setRecentUsers(List<Device> recentUsers) { this.recentUsers = recentUsers; }
}
