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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AdminReportServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private ActivationRequestRepository activationRequestRepository;

    @InjectMocks
    private AdminReportService adminReportService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getRevenueReport_ShouldReturnCorrectData() {
        // Arrange
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        when(paymentRepository.sumTotalRevenueBetween(from, to)).thenReturn(new BigDecimal("1000.00"));
        when(paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.SUCCESS, from, to)).thenReturn(50L);
        when(paymentRepository.countByStatusAndCreatedAtBetween(PaymentStatus.FAILED, from, to)).thenReturn(5L);

        // Act
        Map<String, Object> report = adminReportService.getRevenueReport(from, to);

        // Assert
        assertEquals(new BigDecimal("1000.00"), report.get("totalRevenue"));
        assertEquals(50L, report.get("successfulPayments"));
        assertEquals(5L, report.get("failedPayments"));
    }

    @Test
    void getDeviceReport_ShouldReturnCorrectData() {
        // Arrange
        LocalDateTime from = LocalDateTime.now().minusDays(30);
        LocalDateTime to = LocalDateTime.now();
        when(deviceRepository.countByRegisteredAtBetween(from, to)).thenReturn(100L);

        // Act
        Map<String, Object> report = adminReportService.getDeviceReport(from, to);

        // Assert
        assertEquals(100L, report.get("newDevices"));
    }

    @Test
    void getResellerReport_ShouldReturnCorrectData() {
        // Arrange
        UUID resellerId = UUID.randomUUID();
        Admin reseller = new Admin();
        reseller.setId(resellerId);
        reseller.setUsername("reseller1");
        reseller.setFullName("Reseller One");
        reseller.setRole(AdminRole.RESELLER);

        Page<Admin> resellerPage = new PageImpl<>(Collections.singletonList(reseller));
        when(adminRepository.findAllByRoleIn(eq(Collections.singletonList(AdminRole.RESELLER)), any())).thenReturn(resellerPage);
        
        when(deviceRepository.countByResellerId(resellerId)).thenReturn(10L);
        when(deviceRepository.countByResellerIdAndDeviceStatus(eq(resellerId), any())).thenReturn(8L);
        when(activationRequestRepository.countByResellerIdAndStatus(resellerId, "PENDING")).thenReturn(2L);

        // Act
        Page<Map<String, Object>> result = adminReportService.getResellerReport(PageRequest.of(0, 10));

        // Assert
        assertFalse(result.isEmpty());
        Map<String, Object> data = result.getContent().get(0);
        assertEquals(resellerId, data.get("id"));
        assertEquals("reseller1", data.get("username"));
        assertEquals(10L, data.get("totalUsers"));
        assertEquals(8L, data.get("activeUsers"));
        assertEquals(2L, data.get("pendingRequests"));
    }
}
