package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
import com.iptv.wiseplayer.service.CreditService;
import com.iptv.wiseplayer.service.PaymentService;
import com.iptv.wiseplayer.service.ResellerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubResellerController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubResellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResellerService resellerService;

    @MockBean
    private CreditService creditService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private SuperAdminRepository superAdminRepository;

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testGetBalance() throws Exception {
        mockMvc.perform(get("/api/sub-reseller/credits/balance"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testGetTransactionHistory() throws Exception {
        mockMvc.perform(get("/api/sub-reseller/credits/transactions"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testGetDashboard() throws Exception {
        mockMvc.perform(get("/api/sub-reseller/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testGetUsers() throws Exception {
        mockMvc.perform(get("/api/sub-reseller/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testGetUsersWithFilters() throws Exception {
        mockMvc.perform(get("/api/sub-reseller/users")
                .param("search", "LG")
                .param("status", "INACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testGetActivationRequests() throws Exception {
        mockMvc.perform(get("/api/sub-reseller/activation-request"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUB_RESELLER", username = "sub1")
    void testPurchaseCredits() throws Exception {
        mockMvc.perform(post("/api/sub-reseller/credits/purchase")
                .contentType("application/json")
                .content("{\"creditAmount\": 10}"))
                .andExpect(status().isOk());
    }
}
