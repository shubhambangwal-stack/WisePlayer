package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionType;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PaymentRepository;
import com.iptv.wiseplayer.repository.SubscriptionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminDashboardService {

    private final DeviceRepository deviceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    public AdminDashboardService(DeviceRepository deviceRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository) {
        this.deviceRepository = deviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Device Stats
        stats.put("totalDevices", deviceRepository.count());
        stats.put("activeDevices", deviceRepository.countByDeviceStatus(DeviceStatus.ACTIVE));
        stats.put("trialDevices", deviceRepository.countBySubscriptionType(SubscriptionType.TRIAL));
        stats.put("expiredDevices", deviceRepository.countBySubscriptionType(SubscriptionType.EXPIRED));

        // Subscription Stats
        stats.put("totalSubscriptions", subscriptionRepository.count());
        stats.put("activeSubscriptions", subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE));

        // Revenue Stats
        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.put("totalPayments", paymentRepository.countByStatus(PaymentStatus.SUCCESS));

        return stats;
    }
}
