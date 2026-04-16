package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.service.iptv.XtreamCatalogService;
import com.iptv.wiseplayer.service.iptv.XtreamStreamResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MoviesController.class)
@AutoConfigureMockMvc(addFilters = false)
class MoviesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XtreamCatalogService catalogService;

    @MockBean
    private XtreamStreamResolver streamResolver;

    @Test
    void testGetMovieCategories() throws Exception {
        mockMvc.perform(get("/api/movies/categories")
                .param("playlistId", UUID.randomUUID().toString()))
                .andExpect(status().isOk());
    }
}
