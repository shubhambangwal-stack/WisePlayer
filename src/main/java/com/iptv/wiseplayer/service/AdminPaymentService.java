package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Payments;
import com.iptv.wiseplayer.domain.entity.Payments;
import com.iptv.wiseplayer.dto.response.AdminPaymentResponse;
import com.iptv.wiseplayer.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class AdminPaymentService {

    private final PaymentRepository paymentRepository;

    public AdminPaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Page<AdminPaymentResponse> getAllPayments(Pageable pageable) {
        return paymentRepository.findAll(pageable).map(this::convertToResponse);
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
        response.setPlan(payment.getPlan());
        response.setPaypalOrderId(payment.getPaypalOrderId());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
