package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService subscriptionService;

    @Test
    void testActivateSubscription() throws Exception {
        mockMvc.perform(post("/api/subscription/activate")
                .contentType("application/json")
                .content("{\"deviceId\": \"test-device\", \"planName\": \"ANNUAL\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSubscriptionStatus() throws Exception {
        mockMvc.perform(get("/api/subscription/status")
                .param("deviceId", "test-device"))
                .andExpect(status().isOk());
    }
}
