package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.domain.entity.Payments;
import com.iptv.wiseplayer.dto.response.AdminPaymentResponse;
import com.iptv.wiseplayer.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import com.iptv.wiseplayer.repository.DeviceRepository;
import com.iptv.wiseplayer.util.EncryptionUtil;


@Service
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;
    private final DeviceRepository deviceRepository;
    private final EncryptionUtil encryptionUtil;

    public AdminPaymentService(PaymentRepository paymentRepository,
                               DeviceRepository deviceRepository,
                               EncryptionUtil encryptionUtil) {
        this.paymentRepository = paymentRepository;
        this.deviceRepository = deviceRepository;
        this.encryptionUtil = encryptionUtil;
    }


    public Page<AdminPaymentResponse> getAllPayments(
            String paymentId,
            String deviceId,
            PaymentStatus status,
            Pageable pageable) {
        return paymentRepository.searchPayments(paymentId, deviceId, status, pageable).map(this::convertToResponse);
    }

    public Map<String, Object> getPaymentStats() {
        Map<String, Object> stats = new HashMap<>();
        BigDecimal totalRevenue = paymentRepository.sumTotalRevenue();
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        stats.put("totalTransactions", paymentRepository.count());
        stats.put("successfulTransactions",
                paymentRepository.countByStatus(com.iptv.wiseplayer.domain.enums.PaymentStatus.SUCCESS));
        return stats;
    }

    private AdminPaymentResponse convertToResponse(Payments payment) {
        AdminPaymentResponse response = new AdminPaymentResponse();
        response.setPaymentId(payment.getId());
        response.setDeviceId(payment.getDeviceId());
        response.setStatus(payment.getStatus());
        response.setAmount(payment.getAmount());
        response.setPlanName(payment.getPlanName());
        response.setPaypalOrderId(payment.getPaypalOrderId());
        response.setCreatedAt(payment.getCreatedAt());
        if (payment.getDeviceId() != null) {
            deviceRepository.findByDeviceId(payment.getDeviceId()).ifPresent(device -> {
                if (device.getEncryptedMac() != null) {
                    try {
                        response.setMacAddress(encryptionUtil.decrypt(device.getEncryptedMac()));
                    } catch (Exception e) {
                        response.setMacAddress("N/A");
                    }
                }
            });
        }
        return response;
    }

}
