package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.LiveTvService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LiveTvController.class)
@AutoConfigureMockMvc(addFilters = false)
class LiveTvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LiveTvService liveTvService;

    @MockBean
    private XtreamStreamResolver streamResolver;

    @MockBean
    private DeviceContext deviceContext;

    @Test
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/live/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetChannels() throws Exception {
        mockMvc.perform(get("/api/live/channels")
                .param("categoryId", "1"))
                .andExpect(status().isOk());
    }
}
