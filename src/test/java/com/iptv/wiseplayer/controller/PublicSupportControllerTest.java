package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.service.SupportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PublicSupportController.class)
@AutoConfigureMockMvc(addFilters = false)
class PublicSupportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SupportService supportService;

    @Test
    void testSubmitTicket() throws Exception {
        mockMvc.perform(multipart("/api/public/support/ticket")
                .param("subject", "Test subject")
                .param("description", "Test description")
                .param("email", "test@test.com"))
                .andExpect(status().isOk());
    }
}
