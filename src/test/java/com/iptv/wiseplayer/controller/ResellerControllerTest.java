package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.repository.AdminRepository;
import com.iptv.wiseplayer.repository.SuperAdminRepository;
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

@WebMvcTest(controllers = ResellerController.class)
@AutoConfigureMockMvc(addFilters = false)
class ResellerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResellerService resellerService;

    @MockBean
    private AdminRepository adminRepository;

    @MockBean
    private SuperAdminRepository superAdminRepository;

    @Test
    void testLogin() throws Exception {
        mockMvc.perform(post("/api/reseller/login")
                .contentType("application/json")
                .content("{\"username\": \"reseller1\", \"password\": \"pass123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testRegister() throws Exception {
        mockMvc.perform(post("/api/reseller/register")
                .contentType("application/json")
                .content("{\"username\": \"reseller1\", \"password\": \"pass123\", \"email\": \"r@test.com\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_RESELLER", username = "reseller1")
    void testGetDashboard() throws Exception {
        mockMvc.perform(get("/api/reseller/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_RESELLER", username = "reseller1")
    void testGetUsers() throws Exception {
        mockMvc.perform(get("/api/reseller/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_RESELLER", username = "reseller1")
    void testGetUsersWithFilters() throws Exception {
        mockMvc.perform(get("/api/reseller/users")
                .param("search", "Samsung")
                .param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_RESELLER", username = "reseller1")
    void testGetActivationRequests() throws Exception {
        mockMvc.perform(get("/api/reseller/activation-request"))
                .andExpect(status().isOk());
    }
}
