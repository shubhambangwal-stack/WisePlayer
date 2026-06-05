package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.StreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StreamV2Controller.class)
@AutoConfigureMockMvc(addFilters = false)
class StreamV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StreamService streamService;

    @MockBean
    private DeviceContext deviceContext;

    @Test
    void testTimeshiftRedirect_success() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();
        String channelId = "555";
        String timestamp = "2026-06-01T11:30:00";
        String expectedRedirectUrl = "http://line.vpnworld.pro/timeshift/user/pass/60/2026-06-01:11-30/555.ts";

        when(deviceContext.getCurrentDeviceId()).thenReturn(deviceId);
        when(streamService.getTimeshiftUrlAsync(eq(deviceId), eq(playlistId), eq(channelId), eq(timestamp), eq(60), eq("ts")))
                .thenReturn(CompletableFuture.completedFuture(expectedRedirectUrl));

        // Start asynchronous request
        MvcResult mvcResult = mockMvc.perform(get("/api/v2/stream/timeshift")
                        .param("playlistId", playlistId.toString())
                        .param("channelId", channelId)
                        .param("timestamp", timestamp)
                        .param("duration", "60")
                        .param("extension", "ts"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Dispatch async process and verify redirect
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", expectedRedirectUrl));
    }

    @Test
    void testTimeshiftRedirect_unauthorized() throws Exception {
        UUID playlistId = UUID.randomUUID();

        when(deviceContext.getCurrentDeviceId()).thenReturn(null);

        mockMvc.perform(get("/api/v2/stream/timeshift")
                        .param("playlistId", playlistId.toString())
                        .param("channelId", "123")
                        .param("timestamp", "2026-06-01T11:30:00"))
                .andExpect(status().isUnauthorized());
    }
}
