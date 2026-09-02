package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.enums.DeviceStatus;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
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
    private final com.iptv.wiseplayer.repository.ActivationRequestRepository activationRequestRepository;

    public AdminDashboardService(DeviceRepository deviceRepository,
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            com.iptv.wiseplayer.repository.ActivationRequestRepository activationRequestRepository) {
        this.deviceRepository = deviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.activationRequestRepository = activationRequestRepository;
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // Device Stats
        stats.put("totalDevices", deviceRepository.count());
        stats.put("activeDevices", deviceRepository.countByDeviceStatus(DeviceStatus.ACTIVE));
        stats.put("trialDevices", deviceRepository.countByPlanName("TRIAL"));
        stats.put("expiredDevices", deviceRepository.countByExpiresAtBefore(java.time.LocalDateTime.now()));

        // Subscription Stats
        stats.put("totalSubscriptions", subscriptionRepository.count());
        stats.put("activeSubscriptions", subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE));

        // Revenue Stats
        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.put("totalPayments", paymentRepository.countByStatus(PaymentStatus.SUCCESS));

        // Activation Requests
        stats.put("pendingActivations", activationRequestRepository.countByStatus("PENDING"));

        return stats;
    }
}
