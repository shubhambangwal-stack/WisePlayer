package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.dto.iptv.XtreamEpgProgram;
import com.iptv.wiseplayer.dto.iptv.EpgProgram;
import com.iptv.wiseplayer.dto.iptv.EpgResponse;
import com.iptv.wiseplayer.dto.iptv.CatchUpChannelStatus;
import com.iptv.wiseplayer.dto.iptv.CatchUpStatus;
import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.CatchUpMethod;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.iptv.M3uPlaylistParser.M3uChannel;
import com.iptv.wiseplayer.service.iptv.M3uPlaylistParser.M3uPlaylistData;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatchUpServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;
    @Mock
    private SecureCredentialStore credentialStore;
    @Mock
    private XtreamCatalogService xtreamCatalog;
    @Mock
    private XtreamStreamResolver streamResolver;
    @Mock
    private M3uService m3uService;
    @Mock
    private EncryptionUtil encryptionUtil;

    private CatchUpCache cache;
    private CatchUpService catchUpService;

    @BeforeEach
    void setUp() {
        cache = new CatchUpCache();
        catchUpService = new CatchUpService(
                playlistRepository, credentialStore, xtreamCatalog, streamResolver, m3uService, cache, encryptionUtil);
    }

    // ── Xtream: playlist-level status ─────────────────────────────────────────

    @Test
    void xtreamPlaylistStatus_detectedFromTvArchive() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream archived = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        archived.setStreamId(101);
        archived.setTvArchive(1);
        archived.setTvArchiveDuration(7);
        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream plain = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        plain.setStreamId(102);
        plain.setTvArchive(0);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(archived, plain));

        CatchUpStatus status = catchUpService.getPlaylistStatus(playlistId);

        assertTrue(status.isSupported());
        assertEquals(CatchUpMethod.XC, status.getMethod());
        assertEquals(Integer.valueOf(7), status.getDays());
        assertEquals("XTREAM", status.getProvider());
    }

    @Test
    void xtreamPlaylistStatus_unsupportedWhenNoArchiveChannels() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream plain = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        plain.setStreamId(102);
        plain.setTvArchive(0);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(plain));

        CatchUpStatus status = catchUpService.getPlaylistStatus(playlistId);

        assertFalse(status.isSupported());
        assertEquals(CatchUpMethod.NONE, status.getMethod());
    }

    // ── Xtream: per-channel status ────────────────────────────────────────────

    @Test
    void xtreamChannelStatus_supportedAndPlayable() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream archived = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        archived.setStreamId(101);
        archived.setTvArchive(1);
        archived.setTvArchiveDuration(3);
        archived.setEpgChannelId("cnn.us");

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(archived));
        when(streamResolver.resolveStreamUrl(eq(playlistId), eq(101), eq(XtreamStreamResolver.StreamType.LIVE)))
                .thenReturn("http://host/live/user/pass/101.ts");

        CatchUpChannelStatus status = catchUpService.getChannelStatus(playlistId, "101");

        assertTrue(status.isSupported());
        assertTrue(status.isPlayable());
        assertEquals(CatchUpMethod.XC, status.getMethod());
        assertEquals(Integer.valueOf(3), status.getDays());
        assertEquals("http://host/live/user/pass/101.ts", status.getLiveUrl());
    }

    @Test
    void xtreamChannelStatus_unsupportedWhenNotArchivedOrUnknown() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream plain = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        plain.setStreamId(102);
        plain.setTvArchive(0);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(plain));

        CatchUpChannelStatus status = catchUpService.getChannelStatus(playlistId, "101");

        assertFalse(status.isSupported());
        assertFalse(status.isPlayable());
        assertEquals(CatchUpMethod.NONE, status.getMethod());
    }

    // ── Xtream: EPG ───────────────────────────────────────────────────────────

    @Test
    void xtreamEpg_includesPastProgramsAnnotatedWithCatchup() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream archived = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        archived.setStreamId(101);
        archived.setTvArchive(1);
        archived.setTvArchiveDuration(7);

        long now = Instant.now().getEpochSecond();
        XtreamEpgProgram past = new XtreamEpgProgram();
        past.setTitle("Yesterday Show");
        past.setStart(now - 3600 * 26);
        past.setEnd(now - 3600 * 25);
        XtreamEpgProgram running = new XtreamEpgProgram();
        running.setTitle("Now Show");
        running.setStart(now - 1800);
        running.setEnd(now + 1800);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(archived));
        when(xtreamCatalog.getSimpleDataTable(eq(playlistId), eq(101), anyLong(), anyLong()))
                .thenReturn(List.of(past, running));

        EpgResponse response = catchUpService.getEpg(playlistId, "101", null, null);

        assertTrue(response.isCatchupSupported());
        assertEquals(2, response.getPrograms().size());
        EpgProgram yesterday = response.getPrograms().get(0);
        assertTrue(yesterday.isPast());
        assertTrue(yesterday.isCatchupAvailable());
        EpgProgram nowShow = response.getPrograms().get(1);
        assertTrue(nowShow.isRunning());
        assertTrue(nowShow.isCatchupAvailable());
        assertNotNull(nowShow.getProgressPercent());
        assertNotNull(nowShow.getRemainingSeconds());
    }

    // ── Xtream: catch-up URL ──────────────────────────────────────────────────

    @Test
    void xtreamResolveUrl_buildsArchiveStreamUrl() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream archived = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        archived.setStreamId(101);
        archived.setTvArchive(1);
        archived.setTvArchiveDuration(7);

        long now = Instant.now().getEpochSecond();
        long start = now - 3600;
        long end = now;

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(archived));
        when(credentialStore.getCredentials(playlistId))
                .thenReturn(new SecureCredentialStore.Credentials("http://line.pro:8080/xmltv.php", "user", "pass"));

        String url = catchUpService.resolveCatchUpUrl(playlistId, "101", start, end, "ts");

        assertEquals(String.format("http://line.pro:8080/live/user/pass/101.ts?start=%d&end=%d", start, end), url);
    }

    @Test
    void xtreamResolveUrl_rejectsOutOfWindowTime() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream archived = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        archived.setStreamId(101);
        archived.setTvArchive(1);
        archived.setTvArchiveDuration(7);

        long now = Instant.now().getEpochSecond();
        long start = now - 20L * 86400;
        long end = now - 19L * 86400;

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(archived));

        assertThrows(BadRequestException.class,
                () -> catchUpService.resolveCatchUpUrl(playlistId, "101", start, end, "ts"));
    }

    @Test
    void xtreamResolveUrl_rejectsUnsupportedChannel() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "X", "enc-server", "enc-user", "enc-pass");
        playlist.setId(playlistId);

        com.iptv.wiseplayer.dto.iptv.XtreamLiveStream plain = new com.iptv.wiseplayer.dto.iptv.XtreamLiveStream();
        plain.setStreamId(102);
        plain.setTvArchive(0);

        long now = Instant.now().getEpochSecond();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(xtreamCatalog.getLiveStreams(eq(playlistId), isNull())).thenReturn(List.of(plain));

        assertThrows(BadRequestException.class,
                () -> catchUpService.resolveCatchUpUrl(playlistId, "101", now - 600, now, "ts"));
    }

    // ── M3U: playlist-level status ────────────────────────────────────────────

    @Test
    void m3uPlaylistStatus_detectedFromCatchupAttributes() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "M", "http://host/list.m3u");
        playlist.setId(playlistId);
        playlist.setType(PlaylistType.M3U);

        M3uPlaylistData data = new M3uPlaylistData();
        M3uChannel channel = channelWith("xc", "7", null, "http://host/live/user/pass/1234.ts");
        data.getChannels().add(channel);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(encryptionUtil.decrypt("http://host/list.m3u")).thenReturn("http://host/list.m3u");
        when(m3uService.getParsedPlaylist("http://host/list.m3u")).thenReturn(data);

        CatchUpStatus status = catchUpService.getPlaylistStatus(playlistId);

        assertTrue(status.isSupported());
        assertEquals(CatchUpMethod.XC, status.getMethod());
        assertEquals(Integer.valueOf(7), status.getDays());
    }

    @Test
    void m3uPlaylistStatus_unsupportedWhenNoCatchupData() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "M", "http://host/list.m3u");
        playlist.setId(playlistId);
        playlist.setType(PlaylistType.M3U);

        M3uPlaylistData data = new M3uPlaylistData();
        M3uChannel channel = channelWith(null, null, null, "http://host/channel.ts");
        data.getChannels().add(channel);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(encryptionUtil.decrypt("http://host/list.m3u")).thenReturn("http://host/list.m3u");
        when(m3uService.getParsedPlaylist("http://host/list.m3u")).thenReturn(data);

        CatchUpStatus status = catchUpService.getPlaylistStatus(playlistId);

        assertFalse(status.isSupported());
    }

    // ── M3U: catch-up URL from template ───────────────────────────────────────

    @Test
    void m3uResolveUrl_substitutesCatchupSourceTemplate() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "M", "http://host/list.m3u");
        playlist.setId(playlistId);
        playlist.setType(PlaylistType.M3U);

        M3uPlaylistData data = new M3uPlaylistData();
        M3uChannel channel = channelWith("default", "7",
                "http://ch01.host/151/archive-{utc}-{duration}.m3u8?token=x", "http://ch01.host/151/index.m3u8");
        data.getChannels().add(channel);

        long now = Instant.now().getEpochSecond();
        long start = now - 3600;
        long end = now;

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(encryptionUtil.decrypt("http://host/list.m3u")).thenReturn("http://host/list.m3u");
        when(m3uService.getParsedPlaylist("http://host/list.m3u")).thenReturn(data);

        String url = catchUpService.resolveCatchUpUrl(playlistId, "151", start, end, "m3u8");

        assertEquals(String.format("http://ch01.host/151/archive-%d-%d.m3u8?token=x", start, end - start), url);
    }

    @Test
    void m3uResolveUrl_buildsDefaultMethodUrl() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "M", "http://host/list.m3u");
        playlist.setId(playlistId);
        playlist.setType(PlaylistType.M3U);

        M3uPlaylistData data = new M3uPlaylistData();
        M3uChannel channel = channelWith("default", "7", null, "http://host/live/user/pass/1234.ts");
        data.getChannels().add(channel);

        long now = Instant.now().getEpochSecond();
        long start = now - 3600;
        long end = now;

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(encryptionUtil.decrypt("http://host/list.m3u")).thenReturn("http://host/list.m3u");
        when(m3uService.getParsedPlaylist("http://host/list.m3u")).thenReturn(data);

        String url = catchUpService.resolveCatchUpUrl(playlistId, "1234", start, end, "ts");

        assertEquals(String.format("http://host/user/pass/1234/%d.%d.ts", start, end), url);
    }

    @Test
    void m3uResolveUrl_rejectsChannelWithoutPlayableSource() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "M", "http://host/list.m3u");
        playlist.setId(playlistId);
        playlist.setType(PlaylistType.M3U);

        // catchup flag present but no source and no credentials embedded -> not playable
        M3uPlaylistData data = new M3uPlaylistData();
        M3uChannel channel = channelWith("default", "7", null, "http://ch01.host/151/index.m3u8");
        data.getChannels().add(channel);

        long now = Instant.now().getEpochSecond();

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(encryptionUtil.decrypt("http://host/list.m3u")).thenReturn("http://host/list.m3u");
        when(m3uService.getParsedPlaylist("http://host/list.m3u")).thenReturn(data);

        assertThrows(BadRequestException.class,
                () -> catchUpService.resolveCatchUpUrl(playlistId, "151", now - 600, now, "ts"));
    }

    // ── M3U: EPG ──────────────────────────────────────────────────────────────

    @Test
    void m3uEpg_returnsProgramsAndLiveEdge() {
        UUID playlistId = UUID.randomUUID();
        Playlist playlist = new Playlist(UUID.randomUUID(), "M", "http://host/list.m3u");
        playlist.setId(playlistId);
        playlist.setType(PlaylistType.M3U);

        M3uPlaylistData data = new M3uPlaylistData();
        M3uChannel channel = channelWith("xc", "7", "http://host/archive-{utc}-{duration}.m3u8", "http://host/live/user/pass/1234.ts");
        channel.setTvgId("cnn.us");
        data.getChannels().add(channel);

        long now = Instant.now().getEpochSecond();
        EpgProgram program = new EpgProgram();
        program.setTitle("Past News");
        program.setStartTs(now - 3600);
        program.setEndTs(now - 1800);

        when(playlistRepository.findById(playlistId)).thenReturn(Optional.of(playlist));
        when(encryptionUtil.decrypt("http://host/list.m3u")).thenReturn("http://host/list.m3u");
        when(m3uService.getParsedPlaylist("http://host/list.m3u")).thenReturn(data);
        when(m3uService.getEpg(eq("http://host/list.m3u"), eq("cnn.us"), anyString(), anyLong(), anyLong()))
                .thenReturn(List.of(program));

        EpgResponse response = catchUpService.getEpg(playlistId, "1234", null, null);

        assertTrue(response.isCatchupSupported());
        assertEquals(1, response.getPrograms().size());
        assertTrue(response.getPrograms().get(0).isCatchupAvailable());
        assertEquals(now, response.getLiveEdge());
    }

    private M3uChannel channelWith(String catchup, String days, String source, String streamUrl) {
        M3uChannel channel = new M3uChannel();
        channel.setName("Test Channel");
        channel.setStreamUrl(streamUrl);
        if (catchup != null) {
            channel.getAttributes().put("catchup", catchup);
        }
        if (days != null) {
            channel.getAttributes().put("catchup-days", days);
        }
        if (source != null) {
            channel.getAttributes().put("catchup-source", source);
        }
        // Simulate the parser's hint computation
        channel.setCatchupFlag(catchup != null || days != null || source != null);
        channel.setMethodRaw(catchup);
        channel.setSource(source);
        channel.setDays(days != null ? Integer.valueOf(days) : null);
        return channel;
    }
}