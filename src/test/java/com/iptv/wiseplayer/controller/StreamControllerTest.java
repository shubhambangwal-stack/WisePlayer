package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.StreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StreamController.class)
@AutoConfigureMockMvc(addFilters = false)
class StreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StreamService streamService;

    @MockBean
    private DeviceContext deviceContext;

    @Test
    void testAuthorizePlay_success() throws Exception {
        UUID playlistId = UUID.randomUUID();
        when(streamService.authorizeAndGetUrl(any(), any(), any())).thenReturn("http://stream.url/live");

        mockMvc.perform(post("/api/stream/play")
                .contentType("application/json")
                .content("{\"streamId\": \"123\", \"playlistId\": \"" + playlistId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testAuthorizePlay_missingStreamId() throws Exception {
        mockMvc.perform(post("/api/stream/play")
                .contentType("application/json")
                .content("{\"playlistId\": \"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testAuthorizePlay_missingPlaylistId() throws Exception {
        mockMvc.perform(post("/api/stream/play")
                .contentType("application/json")
                .content("{\"streamId\": \"123\"}"))
                .andExpect(status().isBadRequest());
    }
}
