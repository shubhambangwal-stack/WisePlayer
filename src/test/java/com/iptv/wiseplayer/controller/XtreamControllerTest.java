package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.service.iptv.XtreamAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(controllers = XtreamController.class)
@AutoConfigureMockMvc(addFilters = false)
class XtreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XtreamAuthService authService;

    @Test
    void testCheckAuth() throws Exception {
        // Just mock it so it doesn't return null if DTO assumes non-null
        mockMvc.perform(get("/api/xtream/auth")
                .param("playlistId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }
}
