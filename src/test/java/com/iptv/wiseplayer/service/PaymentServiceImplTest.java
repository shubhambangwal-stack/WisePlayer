package com.iptv.wiseplayer.service;

import com.iptv.wiseplayer.domain.entity.Payments;
import com.iptv.wiseplayer.domain.enums.PaymentStatus;
import com.iptv.wiseplayer.exception.ResourceNotFoundException;
import com.iptv.wiseplayer.repository.PaymentRepository;
import com.iptv.wiseplayer.repository.PlanConfigRepository;
import com.iptv.wiseplayer.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private DeviceService deviceService;

    @Mock
    private PlanConfigRepository planConfigRepository;
    
    @Mock
    private SubscriptionService subscriptionService;
    
    @Mock
    private com.iptv.wiseplayer.service.CreditService creditService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private final UUID testDeviceId = UUID.randomUUID();
    private final UUID testPaymentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void generateInvoicePdf_ShouldReturnPdfBytes_WhenInvoiceExists() {
        // Arrange
        String invoiceNumber = "INV-" + testPaymentId.toString().substring(0, 8).toUpperCase();

        Payments mockPayment = new Payments();
        mockPayment.setId(testPaymentId);
        mockPayment.setDeviceId(testDeviceId);
        mockPayment.setCreatedAt(LocalDateTime.now());
        mockPayment.setStatus(PaymentStatus.SUCCESS);
        mockPayment.setAmount(new BigDecimal("15.99"));
        mockPayment.setPlanName("PREMIUM");
        mockPayment.setPaypalOrderId("PAYPAL-123");

        when(deviceService.resolveDeviceId(testDeviceId.toString())).thenReturn(testDeviceId);
        when(paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(testDeviceId))
                .thenReturn(Collections.singletonList(mockPayment));

        // Act
        byte[] pdfBytes = paymentService.generateInvoicePdf(invoiceNumber, testDeviceId.toString());

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF byte array should not be empty");
        // PDF headers start with %PDF-
        assertEquals("%PDF-", new String(pdfBytes, 0, 5));
    }

    @Test
    void generateInvoicePdf_ShouldThrowException_WhenInvoiceNotFound() {
        // Arrange
        String invoiceNumber = "INV-NOTEXIST";

        when(deviceService.resolveDeviceId(testDeviceId.toString())).thenReturn(testDeviceId);
        when(paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(testDeviceId))
                .thenReturn(Collections.emptyList());

        // Act & Assert
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            paymentService.generateInvoicePdf(invoiceNumber, testDeviceId.toString());
        });

        assertEquals("Invoice not found or does not belong to this device.", exception.getMessage());
    }
}
