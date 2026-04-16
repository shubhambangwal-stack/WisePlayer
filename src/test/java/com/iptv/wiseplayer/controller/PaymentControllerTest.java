package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.security.DeviceAuthenticationToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void testCreateCheckoutSession() throws Exception {
        mockMvc.perform(post("/api/payment/checkout")
                .contentType("application/json")
                .content("{\"deviceId\": \"test-device\", \"planName\": \"ANNUAL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreatePublicCheckoutSession() throws Exception {
        mockMvc.perform(post("/api/payment/public/checkout")
                .contentType("application/json")
                .content("{\"deviceId\": \"test-device\", \"planName\": \"ANNUAL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetActivePlans() throws Exception {
        when(paymentService.getActivePlans()).thenReturn(List.of());
        mockMvc.perform(get("/api/payment/public/plans"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllInvoices() throws Exception {
        when(paymentService.getAllInvoicesByDevice("test-device")).thenReturn(List.of());
        mockMvc.perform(get("/api/payment/invoices")
                .param("deviceId", "test-device"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetCurrentInvoice_noContent() throws Exception {
        when(paymentService.getCurrentInvoice("test-device")).thenReturn(null);
        mockMvc.perform(get("/api/payment/invoice/current")
                .param("deviceId", "test-device"))
                .andExpect(status().isNoContent());
    }
}
