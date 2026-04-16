package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.DeviceKeyService;
import com.iptv.wiseplayer.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DeviceController.class)
@AutoConfigureMockMvc(addFilters = false)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    @MockBean
    private DeviceKeyService deviceKeyService;

    @MockBean
    private DeviceContext deviceContext;

    @Test
    void testRegisterDevice() throws Exception {
        mockMvc.perform(post("/api/device/register")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testValidateDevice() throws Exception {
        mockMvc.perform(post("/api/device/validate")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGenerateKey() throws Exception {
        mockMvc.perform(post("/api/device/key")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testActivateDevice() throws Exception {
        mockMvc.perform(post("/api/device/activate")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetKeyStatus() throws Exception {
        mockMvc.perform(get("/api/device/key/status"))
                .andExpect(status().isOk());
    }
}
