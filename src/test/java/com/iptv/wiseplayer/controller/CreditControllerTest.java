package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.service.CreditService;
import com.iptv.wiseplayer.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CreditController.class)
@AutoConfigureMockMvc(addFilters = false)
class CreditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditService creditService;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private AdminRepository adminRepository;

    @Test
    void testGetCreditPlans() throws Exception {
        mockMvc.perform(get("/api/credit/plans"))
                .andExpect(status().isOk());
    }
}
