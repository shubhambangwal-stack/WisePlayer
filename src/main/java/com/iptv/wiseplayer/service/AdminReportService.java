package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Admin;
import com.iptv.wiseplayer.domain.enums.AdminRole;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.enums.SubscriptionStatus;
import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.repository.PaymentRepository;
import com.iptv.wiseplayer.repository.SubscriptionRepository;
import com.iptv.wiseplayer.repository.ActivationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminReportService {

    private final PaymentRepository paymentRepository;
    private final DeviceRepository deviceRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AdminRepository adminRepository;
    private final ActivationRequestRepository activationRequestRepository;

    public AdminReportService(PaymentRepository paymentRepository,
                             DeviceRepository deviceRepository,
                             SubscriptionRepository subscriptionRepository,
                             AdminRepository adminRepository,
                             ActivationRequestRepository activationRequestRepository) {
        this.paymentRepository = paymentRepository;
        this.deviceRepository = deviceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.adminRepository = adminRepository;
        this.activationRequestRepository = activationRequestRepository;
    }

    public Map<String, Object> getRevenueReport(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> report = new HashMap<>();
        report.put("totalRevenue", paymentRepository.sumTotalRevenueBetween(from, to));
        report.put("successfulPayments", paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.SUCCESS, from, to));
        report.put("failedPayments", paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.FAILED, from, to));
        return report;
    }

    public Map<String, Object> getDeviceReport(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> report = new HashMap<>();
        report.put("newDevices", deviceRepository.countByRegisteredAtBetween(from, to));
        return report;
    }

    public Map<String, Object> getSubscriptionReport(LocalDateTime from, LocalDateTime to) {
        Map<String, Object> report = new HashMap<>();
        report.put("newSubscriptions", subscriptionRepository.countByStatusAndStartDateBetween(SubscriptionStatus.ACTIVE, from, to));
        return report;
    }

    public List<Map<String, Object>> getResellerReport() {
        List<Admin> resellers = adminRepository.findAllByRoleIn(Arrays.asList(AdminRole.RESELLER, AdminRole.SUB_RESELLER));
        List<Map<String, Object>> report = new ArrayList<>();

        for (Admin reseller : resellers) {
            Map<String, Object> resellerData = new HashMap<>();
            resellerData.put("id", reseller.getId());
            resellerData.put("username", reseller.getUsername());
            resellerData.put("fullName", reseller.getFullName());
            resellerData.put("totalUsers", deviceRepository.countByResellerId(reseller.getId()));
            resellerData.put("activeUsers", deviceRepository.countByResellerIdAndDeviceStatus(reseller.getId(), com.iptv.wiseplayer.domain.enums.DeviceStatus.ACTIVE));
            resellerData.put("pendingRequests", activationRequestRepository.countByResellerIdAndStatus(reseller.getId(), "PENDING"));
            report.add(resellerData);
        }
        return report;
    }
}
