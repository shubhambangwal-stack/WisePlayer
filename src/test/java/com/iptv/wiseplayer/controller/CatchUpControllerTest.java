package com.iptv.wiseplayer.controller;

import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.CatchUpMethod;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.iptv.CatchUpChannelStatus;
import com.iptv.wiseplayer.dto.iptv.CatchUpStatus;
import com.iptv.wiseplayer.dto.iptv.EpgResponse;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.security.DeviceContext;
import com.iptv.wiseplayer.service.iptv.CatchUpService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CatchUpController.class)
@AutoConfigureMockMvc(addFilters = false)
class CatchUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CatchUpService catchUpService;

    @MockBean
    private PlaylistRepository playlistRepository;

    @MockBean
    private DeviceContext deviceContext;

    private UUID ownedPlaylistId() {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(deviceId, "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);
        when(deviceContext.getCurrentDeviceId()).thenReturn(deviceId);
        when(playlistRepository.findByDeviceId(deviceId)).thenReturn(List.of(playlist));
        return playlistId;
    }

    @Test
    void statusReturnsSupportedPlaylist() throws Exception {
        UUID playlistId = ownedPlaylistId();
        CatchUpStatus status = new CatchUpStatus(true, CatchUpMethod.XC, 7, null, "XTREAM", null);
        when(catchUpService.getPlaylistStatus(playlistId)).thenReturn(status);

        mockMvc.perform(get("/api/catchup/status").param("playlistId", playlistId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(true))
                .andExpect(jsonPath("$.method").value("XC"))
                .andExpect(jsonPath("$.days").value(7));
    }

    @Test
    void statusReturnsUnsupportedWhenNoData() throws Exception {
        UUID playlistId = ownedPlaylistId();
        CatchUpStatus status = new CatchUpStatus(false, CatchUpMethod.NONE, null, null, "M3U", null);
        when(catchUpService.getPlaylistStatus(playlistId)).thenReturn(status);

        mockMvc.perform(get("/api/catchup/status").param("playlistId", playlistId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(false))
                .andExpect(jsonPath("$.method").value("NONE"));
    }

    @Test
    void channelStatusIsReturned() throws Exception {
        UUID playlistId = ownedPlaylistId();
        CatchUpChannelStatus channel = new CatchUpChannelStatus();
        channel.setChannelId("101");
        channel.setSupported(true);
        channel.setPlayable(true);
        channel.setMethod(CatchUpMethod.XC);
        channel.setDays(7);
        when(catchUpService.getChannelStatus(playlistId, "101")).thenReturn(channel);

        mockMvc.perform(get("/api/catchup/channel")
                        .param("playlistId", playlistId.toString())
                        .param("channelId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supported").value(true))
                .andExpect(jsonPath("$.playable").value(true))
                .andExpect(jsonPath("$.days").value(7));
    }

    @Test
    void epgReturnsPrograms() throws Exception {
        UUID playlistId = ownedPlaylistId();
        EpgResponse epg = new EpgResponse();
        epg.setChannelId("101");
        epg.setCatchupSupported(true);
        epg.setCatchupPlayable(true);
        epg.setCatchupDays(7);
        epg.setLiveEdge(1717200000L);
        when(catchUpService.getEpg(eq(playlistId), eq("101"), any(), any())).thenReturn(epg);

        mockMvc.perform(get("/api/catchup/epg")
                        .param("playlistId", playlistId.toString())
                        .param("channelId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelId").value("101"))
                .andExpect(jsonPath("$.catchupSupported").value(true))
                .andExpect(jsonPath("$.liveEdge").value(1717200000L));
    }

    @Test
    void playReturnsResolvedUrl() throws Exception {
        UUID playlistId = ownedPlaylistId();
        String expectedUrl = "http://line.pro:8080/live/user/pass/101.ts?start=1717196400&end=1717200000";
        when(catchUpService.resolveCatchUpUrl(eq(playlistId), eq("101"), eq(1717196400L), eq(1717200000L), eq("ts")))
                .thenReturn(expectedUrl);

        mockMvc.perform(get("/api/catchup/play")
                        .param("playlistId", playlistId.toString())
                        .param("channelId", "101")
                        .param("start", "1717196400")
                        .param("end", "1717200000")
                        .param("extension", "ts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }

    @Test
    void playFailsGracefullyWhenUnsupported() throws Exception {
        UUID playlistId = ownedPlaylistId();
        when(catchUpService.resolveCatchUpUrl(eq(playlistId), eq("101"), eq(1717196400L), eq(1717200000L), eq("ts")))
                .thenThrow(new BadRequestException("Catch-up is not available for this channel"));

        mockMvc.perform(get("/api/catchup/play")
                        .param("playlistId", playlistId.toString())
                        .param("channelId", "101")
                        .param("start", "1717196400")
                        .param("end", "1717200000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Catch-up is not available for this channel"));
    }

    @Test
    void playlistNotOwnedByDeviceIsRejected() throws Exception {
        UUID deviceId = UUID.randomUUID();
        UUID playlistId = UUID.randomUUID();
        when(deviceContext.getCurrentDeviceId()).thenReturn(deviceId);
        when(playlistRepository.findByDeviceId(deviceId)).thenReturn(List.of());

        mockMvc.perform(get("/api/catchup/status").param("playlistId", playlistId.toString()))
                .andExpect(status().isNotFound());
    }
}