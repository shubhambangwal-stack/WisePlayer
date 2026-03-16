package com.iptv.wiseplayer.dto.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import com.iptv.wiseplayer.domain.entity.Device;

@Getter
@Setter
public class ResellerDashboardResponse {
    private long totalUsers;
    private long activeSubscriptions;
    private long pendingRequests;
    private List<Device> recentUsers;
}
