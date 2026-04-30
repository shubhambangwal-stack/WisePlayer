// package com.iptv.wiseplayer.service;

// import com.iptv.wiseplayer.domain.entity.Payments;
// import com.iptv.wiseplayer.domain.enums.PaymentStatus;
// import com.iptv.wiseplayer.exception.ResourceNotFoundException;
// import com.iptv.wiseplayer.repository.PaymentRepository;
// import com.iptv.wiseplayer.repository.PlanConfigRepository;
// import com.iptv.wiseplayer.service.impl.PaymentServiceImpl;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;

// import java.math.BigDecimal;
// import java.time.LocalDateTime;
// import java.util.Collections;
// import java.util.UUID;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.when;

// class PaymentServiceImplTest {

// @Mock
// private PaymentRepository paymentRepository;

// @Mock
// private DeviceService deviceService;

// @Mock
// private PlanConfigRepository planConfigRepository;

// @Mock
// private SubscriptionService subscriptionService;

// @Mock
// private com.iptv.wiseplayer.service.CreditService creditService;

// @Mock
// private org.springframework.web.client.RestTemplate restTemplate;

// @InjectMocks
// private PaymentServiceImpl paymentService;

// private final UUID testDeviceId = UUID.randomUUID();
// private final UUID testPaymentId = UUID.randomUUID();

// @BeforeEach
// void setUp() {
// MockitoAnnotations.openMocks(this);
// }

// @Test
// void generateInvoicePdf_ShouldReturnPdfBytes_WhenInvoiceExists() {
// // Arrange
// String invoiceNumber = "INV-" + testPaymentId.toString().substring(0,
// 8).toUpperCase();

// Payments mockPayment = new Payments();
// mockPayment.setId(testPaymentId);
// mockPayment.setDeviceId(testDeviceId);
// mockPayment.setCreatedAt(LocalDateTime.now());
// mockPayment.setStatus(PaymentStatus.SUCCESS);
// mockPayment.setAmount(new BigDecimal("15.99"));
// mockPayment.setPlanName("PREMIUM");
// mockPayment.setPaypalOrderId("PAYPAL-123");

// when(deviceService.resolveDeviceId(testDeviceId.toString())).thenReturn(testDeviceId);
// when(paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(testDeviceId))
// .thenReturn(Collections.singletonList(mockPayment));

// // Act
// byte[] pdfBytes = paymentService.generateInvoicePdf(invoiceNumber,
// testDeviceId.toString());

// // Assert
// assertNotNull(pdfBytes);
// assertTrue(pdfBytes.length > 0, "PDF byte array should not be empty");
// // PDF headers start with %PDF-
// assertEquals("%PDF-", new String(pdfBytes, 0, 5));
// }

// @Test
// void createCheckoutSession_ShouldReturnApproveUrl_WhenSuccessful() {
// // Arrange
// CheckoutRequest request = new CheckoutRequest();
// request.setDeviceId(testDeviceId.toString());
// request.setPlanName("ANNUAL");

// SubscriptionResponse subStatus = new SubscriptionResponse();
// subStatus.setType(SubscriptionType.TRIAL);
// when(subscriptionService.getSubscriptionStatus(anyString())).thenReturn(subStatus);

// SubscriptionPlanConfig planConfig = new SubscriptionPlanConfig();
// planConfig.setName("ANNUAL");
// planConfig.setPrice(new BigDecimal("19.99"));
// planConfig.setCurrency("EUR");
// when(planConfigRepository.findByName("ANNUAL")).thenReturn(java.util.Optional.of(planConfig));

// when(deviceService.resolveDeviceId(anyString())).thenReturn(testDeviceId);

// Payments savedPayment = new Payments();
// savedPayment.setId(UUID.randomUUID());
// when(paymentRepository.save(any(Payments.class))).thenReturn(savedPayment);

// // Mock OAuth Token Response
// java.util.Map<String, Object> tokenResponse = new java.util.HashMap<>();
// tokenResponse.put("access_token", "test-token");
// when(restTemplate.postForEntity(anyString(), any(),
// any())).thenReturn(org.springframework.http.ResponseEntity.ok(tokenResponse));

// // Mock Create Order Response
// java.util.Map<String, Object> orderResponse = new java.util.HashMap<>();
// orderResponse.put("id", "ORDER-123");
// java.util.List<java.util.Map<String, String>> links = new
// java.util.ArrayList<>();
// java.util.Map<String, String> approveLink = new java.util.HashMap<>();
// approveLink.put("rel", "approve");
// approveLink.put("href", "http://approve.url");
// links.add(approveLink);
// orderResponse.put("links", links);
// when(restTemplate.postForEntity(anyString(), any(),
// any())).thenReturn(org.springframework.http.ResponseEntity.ok(orderResponse));

// // Act
// CheckoutResponse response = paymentService.createCheckoutSession(request);

// // Assert
// assertNotNull(response);
// assertEquals("http://approve.url", response.getApproveUrl());
// assertEquals("ORDER-123", response.getOrderId());
// }

// @Test
// void
// createCheckoutSession_ShouldThrowException_WhenAlreadyHasLifetimeSubscription()
// {
// // Arrange
// CheckoutRequest request = new CheckoutRequest();
// request.setDeviceId(testDeviceId.toString());

// SubscriptionResponse subStatus = new SubscriptionResponse();
// subStatus.setType(SubscriptionType.PAID_LIFETIME);
// when(subscriptionService.getSubscriptionStatus(anyString())).thenReturn(subStatus);

// // Act & Assert
// assertThrows(IllegalStateException.class, () ->
// paymentService.createCheckoutSession(request));
// }

// @Test
// void generateInvoicePdf_ShouldThrowException_WhenInvoiceNotFound() {
// // Arrange
// String invoiceNumber = "INV-NOTEXIST";

// when(deviceService.resolveDeviceId(testDeviceId.toString())).thenReturn(testDeviceId);
// when(paymentRepository.findAllByDeviceIdOrderByCreatedAtDesc(testDeviceId))
// .thenReturn(Collections.emptyList());

// // Act & Assert
// Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
// paymentService.generateInvoicePdf(invoiceNumber, testDeviceId.toString());
// });

// assertEquals("Invoice not found or does not belong to this device.",
// exception.getMessage());
// }

// @Test
// void captureOrder_ShouldReturnPayment_WhenSuccessful() {
// // Arrange
// String orderId = "ORDER-123";
// Payments payment = new Payments();
// payment.setId(testPaymentId);
// payment.setPaypalOrderId(orderId);
// payment.setStatus(PaymentStatus.PENDING);
// payment.setDeviceId(testDeviceId);
// payment.setPlanName("ANNUAL");

// when(paymentRepository.findByPaypalOrderId(orderId)).thenReturn(java.util.Optional.of(payment));

// // Mock OAuth Token
// java.util.Map<String, Object> tokenResponse = new java.util.HashMap<>();
// tokenResponse.put("access_token", "test-token");
// when(restTemplate.postForEntity(anyString(), any(),
// any())).thenReturn(org.springframework.http.ResponseEntity.ok(tokenResponse));

// // Mock Capture API
// java.util.Map<String, Object> captureResponse = new java.util.HashMap<>();
// java.util.List<java.util.Map<String, Object>> purchaseUnits = new
// java.util.ArrayList<>();
// java.util.Map<String, Object> pu = new java.util.HashMap<>();
// java.util.Map<String, Object> payments = new java.util.HashMap<>();
// java.util.List<java.util.Map<String, Object>> captures = new
// java.util.ArrayList<>();
// java.util.Map<String, Object> c = new java.util.HashMap<>();
// c.put("id", "CAPTURE-123");
// captures.add(c);
// payments.put("captures", captures);
// pu.put("payments", payments);
// purchaseUnits.add(pu);
// captureResponse.put("purchase_units", purchaseUnits);

// when(restTemplate.postForEntity(anyString(), any(),
// any())).thenReturn(org.springframework.http.ResponseEntity.ok(captureResponse));
// when(paymentRepository.findByPaypalOrderId(orderId)).thenReturn(java.util.Optional.of(payment));

// // Act
// Payments result = paymentService.captureOrder(orderId);

// // Assert
// assertNotNull(result);
// assertEquals(PaymentStatus.SUCCESS, result.getStatus());
// assertEquals("CAPTURE-123", result.getPaypalCaptureId());
// }
// }
