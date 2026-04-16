package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.PlaylistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PlaylistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlaylistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaylistService playlistService;

    @MockBean
    private DeviceContext deviceContext;

    @Test
    void testSaveXtreamPlaylist() throws Exception {
        mockMvc.perform(post("/api/playlist/xtream")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testSaveM3uPlaylist() throws Exception {
        mockMvc.perform(post("/api/playlist/m3u")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetPlaylists() throws Exception {
        mockMvc.perform(get("/api/playlist"))
                .andExpect(status().isOk());
    }

    @Test
    void testValidatePlaylist() throws Exception {
        mockMvc.perform(post("/api/playlist/validate")
                .contentType("application/json")
                .content("{\"type\": \"M3U\", \"name\": \"test\"}"))
                .andExpect(status().isOk());
    }
}
