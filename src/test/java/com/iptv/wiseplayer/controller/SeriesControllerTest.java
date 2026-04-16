package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SeriesController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeriesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XtreamCatalogService catalogService;

    @MockBean
    private XtreamStreamResolver streamResolver;

    @Test
    void testGetSeriesCategories() throws Exception {
        when(catalogService.getSeriesCategories(any())).thenReturn(List.of());
        mockMvc.perform(get("/api/series")
                .param("playlistId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetSeriesByCategory() throws Exception {
        when(catalogService.getSeries(any(), anyString())).thenReturn(List.of());
        mockMvc.perform(get("/api/series")
                .param("playlistId", UUID.randomUUID().toString())
                .param("categoryId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void testResolveStreamUrl() throws Exception {
        when(streamResolver.resolveStreamUrl(any(), any(), any())).thenReturn("http://stream.url");
        mockMvc.perform(get("/api/series")
                .param("playlistId", UUID.randomUUID().toString())
                .param("streamId", "42"))
                .andExpect(status().isOk());
    }
}
