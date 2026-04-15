package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.dto.response.CheckoutResponse;
import com.iptv.wiseplayer.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PaymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    private final String testDeviceId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();

        // Mock Admin Authentication to bypass the validateAccess check in controller cleanly
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "admin", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void downloadInvoicePdf_ShouldReturnPdfStream_WhenValidRequest() throws Exception {
        // Arrange
        String invoiceNumber = "INV-12345678";
        byte[] mockPdfBytes = "Mock PDF Content".getBytes();
        when(paymentService.generateInvoicePdf(invoiceNumber, testDeviceId)).thenReturn(mockPdfBytes);

        // Act & Assert
        mockMvc.perform(get("/api/payment/invoice/{invoiceNumber}/pdf", invoiceNumber)
                        .param("deviceId", testDeviceId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"invoice-INV-12345678.pdf\""))
                .andExpect(content().bytes(mockPdfBytes));
    }

    @Test
    void getCurrentInvoice_ShouldReturnNoContent_WhenInvoiceIsNull() throws Exception {
        // Arrange
        when(paymentService.getCurrentInvoice(testDeviceId)).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/payment/invoice/current")
                        .param("deviceId", testDeviceId))
                .andExpect(status().isNoContent());
    }

    @Test
    void getActivePlans_ShouldReturnPlans() throws Exception {
        // Arrange
        com.iptv.wiseplayer.dto.response.PlanResponse plan = new com.iptv.wiseplayer.dto.response.PlanResponse();
        plan.setName("Premium");
        when(paymentService.getActivePlans()).thenReturn(Collections.singletonList(plan));

        // Act & Assert
        mockMvc.perform(get("/api/payment/public/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Premium"));
    }
}
