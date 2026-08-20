package com.iptv.wiseplayer.service.iptv;

import com.iptv.wiseplayer.domain.entity.Playlist;
import com.iptv.wiseplayer.domain.enums.CatchUpMethod;
import com.iptv.wiseplayer.domain.enums.PlaylistType;
import com.iptv.wiseplayer.dto.iptv.CatchUpChannelStatus;
import com.iptv.wiseplayer.dto.iptv.CatchUpStatus;
import com.iptv.wiseplayer.dto.iptv.EpgProgram;
import com.iptv.wiseplayer.dto.iptv.EpgResponse;
import com.iptv.wiseplayer.dto.iptv.XtreamEpgProgram;
import com.iptv.wiseplayer.dto.iptv.XtreamLiveStream;
import com.iptv.wiseplayer.exception.BadRequestException;
import com.iptv.wiseplayer.repository.PlaylistRepository;
import com.iptv.wiseplayer.service.iptv.M3uPlaylistParser.M3uChannel;
import com.iptv.wiseplayer.service.iptv.M3uPlaylistParser.M3uPlaylistData;
import com.iptv.wiseplayer.util.EncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Orchestrates catch-up / archive / timeshift support across both Xtream Codes
 * and M3U_plus sources.
 *
 * <p>Capability detection is strictly data-driven: catch-up is only reported
 * as supported/playable when the provider actually exposes archive data
 * (M3U catch-up attributes, or Xtream {@code tv_archive}) and a playable
 * catch-up URL can be constructed. When no data exists, the service returns an
 * "unsupported" result instead of fabricating options.
 */
@Service
public class CatchUpService {

    private static final Logger log = LoggerFactory.getLogger(CatchUpService.class);

    private static final long STATUS_TTL = Duration.ofMinutes(10).toMillis();
    private static final long EPG_TTL = Duration.ofMinutes(4).toMillis();
    private static final long EMPTY_STREAMS_TTL = Duration.ofMinutes(2).toMillis();
    private static final int EPG_DEFAULT_LOOKAHEAD_HOURS = 6;
    private static final int DEFAULT_CATCHUP_DAYS = 1;

    private final PlaylistRepository playlistRepository;
    private final SecureCredentialStore credentialStore;
    private final XtreamCatalogService xtreamCatalog;
    private final XtreamStreamResolver streamResolver;
    private final M3uService m3uService;
    private final CatchUpCache cache;
    private final EncryptionUtil encryptionUtil;

    public CatchUpService(PlaylistRepository playlistRepository,
            SecureCredentialStore credentialStore,
            XtreamCatalogService xtreamCatalog,
            XtreamStreamResolver streamResolver,
            M3uService m3uService,
            CatchUpCache cache,
            EncryptionUtil encryptionUtil) {
        this.playlistRepository = playlistRepository;
        this.credentialStore = credentialStore;
        this.xtreamCatalog = xtreamCatalog;
        this.streamResolver = streamResolver;
        this.m3uService = m3uService;
        this.cache = cache;
        this.encryptionUtil = encryptionUtil;
    }

    // ── Playlist-level status ──────────────────────────────────────────────────

    /**
     * Returns the playlist-level catch-up availability, cached for 10 minutes
     * and snapshotted on the playlist row so it survives restarts.
     */
    public CatchUpStatus getPlaylistStatus(UUID playlistId) {
        String key = "status:" + playlistId;
        CatchUpStatus cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        Playlist playlist = loadPlaylist(playlistId);
        if (playlist.getCatchupCheckedAt() != null
                && playlist.getCatchupCheckedAt().isAfter(LocalDateTime.now().minus(Duration.ofMinutes(10)))) {
            CatchUpStatus stored = fromPersisted(playlist);
            cache.put(key, stored, STATUS_TTL);
            return stored;
        }

        CatchUpStatus detected = detectPlaylistStatus(playlist);
        persistStatus(playlist, detected);
        cache.put(key, detected, STATUS_TTL);
        return detected;
    }

    private CatchUpStatus detectPlaylistStatus(Playlist playlist) {
        String provider = playlist.getType().name();
        try {
            if (playlist.getType() == PlaylistType.XTREAM) {
                List<XtreamLiveStream> streams = getLiveStreamsCached(playlist.getId());
                // Use hasAnyCatchup() — a channel can support catch-up via a provider-level
                // method (catchup=true) even when tv_archive=0.
                boolean anyArchive = streams.stream().anyMatch(XtreamLiveStream::hasAnyCatchup);
                if (!anyArchive) {
                    return CatchUpStatus.unsupported(provider);
                }
                int maxDays = streams.stream()
                        .filter(XtreamLiveStream::hasAnyCatchup)
                        .map(XtreamLiveStream::effectiveCatchupDays)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);
                Integer days = maxDays > 0 ? maxDays : null;
                // Determine the dominant method across all catch-up channels.
                CatchUpMethod dominantMethod = streams.stream()
                        .filter(XtreamLiveStream::hasAnyCatchup)
                        .map(XtreamLiveStream::getCatchupMethod)
                        .filter(m -> m != null && !m.isBlank())
                        .map(CatchUpMethod::fromCode)
                        .filter(m -> m != CatchUpMethod.NONE)
                        .findFirst()
                        .orElse(CatchUpMethod.XC);
                return new CatchUpStatus(true, dominantMethod, days, null, provider, Instant.now());
            }

            if (playlist.getType() == PlaylistType.M3U) {
                M3uPlaylistData data = m3uService.getParsedPlaylist(decryptM3uUrl(playlist));
                List<M3uChannel> playable = data.getChannels().stream()
                        .filter(this::isM3uChannelPlayable)
                        .toList();
                if (playable.isEmpty()) {
                    return CatchUpStatus.unsupported(provider);
                }
                int maxDays = playable.stream()
                        .map(M3uChannel::getDays)
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);
                Integer days = maxDays > 0 ? maxDays : null;
                CatchUpMethod method = playable.stream()
                        .map(M3uChannel::getMethodRaw)
                        .filter(Objects::nonNull)
                        .map(CatchUpMethod::fromCode)
                        .filter(m -> m != CatchUpMethod.NONE)
                        .findFirst()
                        .orElse(CatchUpMethod.DEFAULT);
                String source = playable.stream()
                        .map(M3uChannel::getSource)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
                return new CatchUpStatus(true, method, days, source, provider, Instant.now());
            }
        } catch (Exception e) {
            log.warn("Failed to detect catch-up support for playlist {}: {}", playlist.getId(), e.getMessage());
        }
        return CatchUpStatus.unsupported(provider);
    }

    private CatchUpStatus fromPersisted(Playlist playlist) {
        if (!playlist.isCatchupSupported()) {
            return CatchUpStatus.unsupported(playlist.getType().name());
        }
        CatchUpMethod method = playlist.getCatchupMethod() != null
                ? CatchUpMethod.fromCode(playlist.getCatchupMethod())
                : CatchUpMethod.DEFAULT;
        return new CatchUpStatus(true, method, playlist.getCatchupDays(), playlist.getCatchupSource(),
                playlist.getType().name(),
                playlist.getCatchupCheckedAt() != null ? playlist.getCatchupCheckedAt().toInstant(ZoneOffset.UTC) : Instant.now());
    }

    private void persistStatus(Playlist playlist, CatchUpStatus status) {
        try {
            playlist.setCatchupSupported(status.isSupported());
            playlist.setCatchupMethod(status.isSupported() && status.getMethod() != null
                    ? status.getMethod().getCode() : null);
            playlist.setCatchupDays(status.getDays());
            playlist.setCatchupSource(status.getSource());
            playlist.setCatchupCheckedAt(LocalDateTime.now());
            playlistRepository.save(playlist);
        } catch (Exception e) {
            log.warn("Failed to persist catch-up snapshot for playlist {}: {}", playlist.getId(), e.getMessage());
        }
    }

    // ── Per-channel status ─────────────────────────────────────────────────────

    /**
     * Returns per-channel catch-up availability (cached 10 minutes).
     */
    public CatchUpChannelStatus getChannelStatus(UUID playlistId, String channelId) {
        String key = "channel:" + playlistId + ":" + channelId;
        CatchUpChannelStatus cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        Playlist playlist = loadPlaylist(playlistId);
        CatchUpChannelStatus status;
        if (playlist.getType() == PlaylistType.XTREAM) {
            status = xtreamChannelStatus(playlistId, channelId);
        } else if (playlist.getType() == PlaylistType.M3U) {
            status = m3uChannelStatus(playlist, channelId);
        } else {
            status = unsupportedChannel(channelId);
        }
        cache.put(key, status, STATUS_TTL);
        return status;
    }

    private CatchUpChannelStatus xtreamChannelStatus(UUID playlistId, String channelId) {
        List<XtreamLiveStream> streams = getLiveStreamsCached(playlistId);
        XtreamLiveStream match = streams.stream()
                .filter(s -> String.valueOf(s.getStreamId()).equals(channelId))
                .findFirst()
                .orElse(null);

        CatchUpChannelStatus status = new CatchUpChannelStatus();
        status.setChannelId(channelId);

        // Always populate epg_channel_id — it is needed for EPG lookups even
        // when the channel has no catch-up archive.
        if (match != null) {
            status.setEpgChannelId(match.getEpgChannelId());
        }

        if (match == null || !match.hasAnyCatchup()) {
            // Channel has no catch-up, but we still populate the status object
            // so the EPG layer can use the epgChannelId for schedule lookups.
            return unsupportedChannel(channelId, match != null ? match.getEpgChannelId() : null);
        }

        // Resolve method: prefer the per-channel catchup_method field, fall back to XC.
        CatchUpMethod method = CatchUpMethod.NONE;
        if (match.getCatchupMethod() != null && !match.getCatchupMethod().isBlank()) {
            method = CatchUpMethod.fromCode(match.getCatchupMethod());
        }
        if (method == CatchUpMethod.NONE) {
            method = CatchUpMethod.XC;
        }

        status.setSupported(true);
        status.setPlayable(true);
        status.setMethod(method);
        status.setDays(match.effectiveCatchupDays());
        try {
            status.setLiveUrl(streamResolver.resolveStreamUrl(playlistId, match.getStreamId(),
                    XtreamStreamResolver.StreamType.LIVE));
        } catch (Exception e) {
            log.warn("Could not resolve live URL for channel {}: {}", channelId, e.getMessage());
        }
        return status;
    }

    private CatchUpChannelStatus m3uChannelStatus(Playlist playlist, String channelId) {
        M3uPlaylistData data = m3uService.getParsedPlaylist(decryptM3uUrl(playlist));
        M3uChannel match = data.getChannels().stream()
                .filter(c -> matchesM3uChannel(c, channelId))
                .findFirst()
                .orElse(null);
        if (match == null || !match.isCatchupFlag()) {
            return unsupportedChannel(channelId);
        }

        CatchUpChannelStatus status = new CatchUpChannelStatus();
        status.setChannelId(channelId);
        status.setEpgChannelId(match.getTvgId());
        status.setSource(match.getSource());
        status.setLiveUrl(match.getStreamUrl());
        status.setDays(match.getDays());
        status.setMethod(CatchUpMethod.fromCode(match.getMethodRaw()));
        boolean playable = isM3uChannelPlayable(match);
        status.setSupported(playable);
        status.setPlayable(playable);
        return status;
    }

    private boolean matchesM3uChannel(M3uChannel channel, String channelId) {
        if (channelId == null) {
            return false;
        }
        return channelId.equals(M3uPlaylistParser.extractChannelId(channel.getStreamUrl(), channel.getTvgId()))
                || channelId.equals(channel.getStreamUrl())
                || channelId.equals(channel.getTvgId())
                || channelId.equals(channel.getName());
    }

    private boolean isM3uChannelPlayable(M3uChannel channel) {
        if (!channel.isCatchupFlag()) {
            return false;
        }
        // Explicit catch-up URL template from the provider is always playable.
        if (channel.getSource() != null) {
            return true;
        }
        // Otherwise a URL can only be built when credentials are known.
        return channel.getMethodRaw() != null
                && M3uPlaylistParser.extractCredentials(channel.getStreamUrl()) != null;
    }

    private CatchUpChannelStatus unsupportedChannel(String channelId) {
        return unsupportedChannel(channelId, null);
    }

    private CatchUpChannelStatus unsupportedChannel(String channelId, String epgChannelId) {
        CatchUpChannelStatus status = new CatchUpChannelStatus();
        status.setChannelId(channelId);
        status.setSupported(false);
        status.setPlayable(false);
        status.setMethod(CatchUpMethod.NONE);
        // Preserve epgChannelId even for unsupported channels so EPG guide
        // data can still be fetched from the Xtream or XMLTV source.
        status.setEpgChannelId(epgChannelId);
        return status;
    }

    // ── EPG ────────────────────────────────────────────────────────────────────

    /**
     * Returns the EPG for a channel across the requested window.
     *
     * <p>EPG guide data (schedule listings) is fetched <em>independently</em> of
     * catch-up availability. A channel with {@code tv_archive=0} (e.g. a live
     * sports channel) still returns its programme schedule; each programme is
     * annotated with {@code catchupAvailable=false} when the channel does not
     * support archive playback.
     *
     * <p>Cached for 4 minutes.
     */
    public EpgResponse getEpg(UUID playlistId, String channelId, Long start, Long end) {
        String key = "epg:" + playlistId + ":" + channelId + ":" + start + ":" + end;
        EpgResponse cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        Playlist playlist = loadPlaylist(playlistId);
        long now = Instant.now().getEpochSecond();
        CatchUpChannelStatus status = getChannelStatus(playlistId, channelId);

        EpgResponse response = new EpgResponse();
        response.setChannelId(channelId);
        response.setServerTime(now);
        response.setCatchupSupported(status.isSupported());
        response.setCatchupPlayable(status.isPlayable());
        response.setCatchupMethod(status.getMethod());
        response.setCatchupDays(status.getDays());
        response.setLiveUrl(status.getLiveUrl());
        response.setLiveEdge(now);

        // Use a generous window for EPG even when catch-up is unavailable:
        // always show at least DEFAULT_CATCHUP_DAYS of past + EPG_DEFAULT_LOOKAHEAD_HOURS ahead.
        int days = status.getDays() != null ? Math.max(status.getDays(), DEFAULT_CATCHUP_DAYS) : DEFAULT_CATCHUP_DAYS;
        long windowStart = start != null ? start : now - days * 86400L;
        long windowEnd = end != null ? end : now + (long) EPG_DEFAULT_LOOKAHEAD_HOURS * 3600L;

        // Fetch EPG unconditionally — guide data is independent of catch-up support.
        List<EpgProgram> programs = fetchPrograms(playlist, channelId, status, windowStart, windowEnd);
        annotatePrograms(programs, status, now);
        response.setPrograms(programs);

        cache.put(key, response, EPG_TTL);
        return response;
    }

    /**
     * Fetches EPG programmes for the given channel across the time window.
     *
     * <p>EPG guide data is fetched <em>unconditionally</em> — independent of
     * whether catch-up playback is supported. A live-only channel (e.g. SONY SIX
     * with {@code tv_archive=0}) still has a programme schedule that is useful
     * to display. The {@code catchupAvailable} flag on each programme is
     * set by {@link #annotatePrograms} based on the catch-up status.
     */
    private List<EpgProgram> fetchPrograms(Playlist playlist, String channelId, CatchUpChannelStatus status,
            long windowStart, long windowEnd) {
        try {
            if (playlist.getType() == PlaylistType.XTREAM) {
                return fetchXtreamEpg(playlist, channelId, status, windowStart, windowEnd);
            }

            if (playlist.getType() == PlaylistType.M3U) {
                M3uPlaylistData data = m3uService.getParsedPlaylist(decryptM3uUrl(playlist));
                M3uChannel channel = data.getChannels().stream()
                        .filter(c -> matchesM3uChannel(c, channelId))
                        .findFirst()
                        .orElse(null);
                if (channel == null) {
                    return new ArrayList<>();
                }
                List<EpgProgram> programs = new ArrayList<>(m3uService.getEpg(
                        decryptM3uUrl(playlist), channel.getTvgId(), channel.getName(), windowStart, windowEnd));
                for (EpgProgram program : programs) {
                    program.setChannelId(channelId);
                }
                programs.sort(Comparator.comparingLong(EpgProgram::getStartTs));
                return programs;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch EPG for channel {}: {}", channelId, e.getMessage());
        }
        return new ArrayList<>();
    }

    /**
     * 3-tier EPG fallback chain for Xtream Codes sources:
     *
     * <ol>
     *   <li><b>Tier 1 – get_simple_data_table</b>: full archive + schedule window.
     *       Used for channels with {@code tv_archive=1}. Returns past, current,
     *       and upcoming programmes.</li>
     *   <li><b>Tier 2 – get_short_epg</b>: current + next programmes only.
     *       Used when the data table is empty (live-only channels like SONY SIX HD
     *       that have no archive but do have a live schedule from the provider).</li>
     *   <li><b>Tier 3 – XMLTV via epg_channel_id</b>: cross-reference against the
     *       provider's XMLTV feed when both Xtream API endpoints return nothing.
     *       Relies on the {@code epg_channel_id} field (e.g. {@code "SonySIX.in"}).</li>
     * </ol>
     */
    private List<EpgProgram> fetchXtreamEpg(Playlist playlist, String channelId,
            CatchUpChannelStatus status, long windowStart, long windowEnd) {
        int numericId;
        try {
            numericId = Integer.parseInt(channelId);
        } catch (NumberFormatException e) {
            log.warn("Non-numeric channel id for Xtream EPG: {}", channelId);
            return new ArrayList<>();
        }

        // ── Tier 1: get_simple_data_table (archive window) ───────────────────
        List<XtreamEpgProgram> raw = Collections.emptyList();
        if (status.isSupported()) {
            // Only call the archive endpoint when the channel has catch-up support;
            // the endpoint returns nothing useful for live-only channels.
            raw = xtreamCatalog.getSimpleDataTable(playlist.getId(), numericId, windowStart, windowEnd);
            log.debug("Tier-1 (data table) returned {} entries for channel {}", raw.size(), channelId);
        }

        // ── Tier 2: get_short_epg (current + upcoming) ───────────────────────
        if (raw.isEmpty()) {
            raw = xtreamCatalog.getShortEpg(playlist.getId(), numericId, 72);
            log.debug("Tier-2 (short EPG) returned {} entries for channel {}", raw.size(), channelId);
        }

        // ── Map Xtream → EpgProgram (with Base64 decode) ─────────────────────
        if (!raw.isEmpty()) {
            return mapXtreamPrograms(raw, channelId, windowStart, windowEnd);
        }

        // ── Tier 3: XMLTV via epg_channel_id ─────────────────────────────────
        String epgChannelId = resolveEpgChannelId(playlist.getId(), channelId, status);
        if (epgChannelId != null && !epgChannelId.isBlank()) {
            log.debug("Tier-3 (XMLTV) lookup via epg_channel_id='{}' for channel {}", epgChannelId, channelId);

            // 1. Try Xtream server's native xmltv.php endpoint
            SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlist.getId());
            String xtreamXmltvUrl = normalizeBaseUrl(creds.serverUrl()) + "/xmltv.php?username=" + creds.username() + "&password=" + creds.password();

            List<EpgProgram> xmltvPrograms = m3uService.getEpgFromXmlUrl(
                    xtreamXmltvUrl, epgChannelId, null, windowStart, windowEnd);

            // 2. If Xtream server's xmltv.php is empty, try public open EPG sources
            if (xmltvPrograms.isEmpty()) {
                xmltvPrograms = fetchExternalGlobalEpg(epgChannelId, windowStart, windowEnd);
            }

            if (!xmltvPrograms.isEmpty()) {
                log.debug("Tier-3 (XMLTV) returned {} entries for channel {}", xmltvPrograms.size(), channelId);
                xmltvPrograms.forEach(p -> p.setChannelId(channelId));
                xmltvPrograms.sort(Comparator.comparingLong(EpgProgram::getStartTs));
                return xmltvPrograms;
            }
        }

        log.debug("All EPG tiers returned empty for channel {}", channelId);
        return new ArrayList<>();
    }

    /**
     * Fallback to public open XMLTV EPG sources when neither the Xtream API
     * nor the Xtream server's xmltv.php contains guide listings for an epg_channel_id.
     */
    private List<EpgProgram> fetchExternalGlobalEpg(String epgChannelId, long windowStart, long windowEnd) {
        if (epgChannelId == null || epgChannelId.isBlank()) {
            return Collections.emptyList();
        }
        List<String> externalSources = new ArrayList<>();
        if (epgChannelId.toLowerCase().endsWith(".in")) {
            externalSources.add("https://iptv-org.github.io/epg/guides/in/beetelevision.com.epg.xml");
            externalSources.add("https://iptv-org.github.io/epg/guides/in.xml");
        } else if (epgChannelId.toLowerCase().endsWith(".uk")) {
            externalSources.add("https://iptv-org.github.io/epg/guides/uk.xml");
        } else if (epgChannelId.toLowerCase().endsWith(".us")) {
            externalSources.add("https://iptv-org.github.io/epg/guides/us.xml");
        }
        externalSources.add("https://iptv-org.github.io/epg/guides/general.xml");

        for (String sourceUrl : externalSources) {
            try {
                List<EpgProgram> programs = m3uService.getEpgFromXmlUrl(sourceUrl, epgChannelId, null, windowStart, windowEnd);
                if (!programs.isEmpty()) {
                    log.info("Found {} EPG programmes for epg_channel_id='{}' in external guide {}",
                            programs.size(), epgChannelId, sourceUrl);
                    return programs;
                }
            } catch (Exception e) {
                log.debug("External EPG fetch failed for {}: {}", sourceUrl, e.getMessage());
            }
        }
        return Collections.emptyList();
    }

    /**
     * Maps raw Xtream EPG entries to {@link EpgProgram}, applying Base64
     * decoding to title and description, and filtering by the time window.
     */
    private List<EpgProgram> mapXtreamPrograms(List<XtreamEpgProgram> raw, String channelId,
            long windowStart, long windowEnd) {
        List<EpgProgram> result = new ArrayList<>();
        for (XtreamEpgProgram p : raw) {
            if (p.getEnd() <= windowStart || p.getStart() > windowEnd) {
                continue;
            }
            EpgProgram program = new EpgProgram();
            program.setId(p.getId() != null ? p.getId() : channelId + "-" + p.getStart());
            program.setChannelId(channelId);
            // Use decoded (human-readable) values — handles Base64-encoded titles.
            program.setTitle(p.getDecodedTitle());
            program.setDescription(p.getDecodedDescription());
            program.setStartTs(p.getStart());
            program.setEndTs(p.getEnd());
            result.add(program);
        }
        result.sort(Comparator.comparingLong(EpgProgram::getStartTs));
        return result;
    }

    /**
     * Resolves the {@code epg_channel_id} for the given channel. Checks
     * the status object first (already populated for Xtream channels), then
     * falls back to scanning the cached live-stream list.
     */
    private String resolveEpgChannelId(UUID playlistId, String channelId, CatchUpChannelStatus status) {
        if (status.getEpgChannelId() != null && !status.getEpgChannelId().isBlank()) {
            return status.getEpgChannelId();
        }
        // Fallback: scan the cached stream list
        List<XtreamLiveStream> streams = getLiveStreamsCached(playlistId);
        return streams.stream()
                .filter(s -> String.valueOf(s.getStreamId()).equals(channelId))
                .map(XtreamLiveStream::getEpgChannelId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse(null);
    }

    private void annotatePrograms(List<EpgProgram> programs, CatchUpChannelStatus status, long now) {
        for (EpgProgram program : programs) {
            boolean past = program.getEndTs() <= now;
            boolean running = program.getStartTs() <= now && now < program.getEndTs();
            program.setPast(past);
            program.setRunning(running);
            program.setCatchupAvailable(status.isPlayable() && (past || running));
            if (running) {
                long duration = Math.max(1, program.getEndTs() - program.getStartTs());
                int percent = (int) Math.round(((double) (now - program.getStartTs()) / duration) * 100);
                program.setProgressPercent(Math.max(0, Math.min(100, percent)));
                program.setRemainingSeconds(Math.max(0, program.getEndTs() - now));
            }
        }
    }

    // ── Catch-up playback URL ──────────────────────────────────────────────────

    /**
     * Resolves the playable catch-up URL for a channel and time window.
     * Throws {@link BadRequestException} (never fabricates a URL) when catch-up
     * is not actually available for the channel or the window is out of range,
     * so clients can gracefully fall back to the live stream.
     */
    public String resolveCatchUpUrl(UUID playlistId, String channelId, long startTs, long endTs, String extension) {
        if (startTs <= 0 || endTs <= startTs) {
            throw new BadRequestException("A valid start and end timestamp are required for catch-up playback");
        }

        CatchUpChannelStatus status = getChannelStatus(playlistId, channelId);
        if (!status.isSupported() || !status.isPlayable() || status.getMethod() == CatchUpMethod.NONE) {
            throw new BadRequestException("Catch-up is not available for this channel");
        }

        long now = Instant.now().getEpochSecond();
        int days = status.getDays() != null ? Math.max(status.getDays(), DEFAULT_CATCHUP_DAYS) : DEFAULT_CATCHUP_DAYS;
        if (startTs < now - days * 86400L) {
            throw new BadRequestException("Requested time is outside the " + days + " day catch-up window");
        }

        Playlist playlist = loadPlaylist(playlistId);
        String ext = normalizeExtension(extension);

        if (playlist.getType() == PlaylistType.XTREAM) {
            SecureCredentialStore.Credentials creds = credentialStore.getCredentials(playlistId);
            String base = normalizeBaseUrl(creds.serverUrl());
            return String.format("%s/live/%s/%s/%s.%s?start=%d&end=%d",
                    base, creds.username(), creds.password(), channelId, ext, startTs, endTs);
        }

        if (playlist.getType() == PlaylistType.M3U) {
            M3uPlaylistData data = m3uService.getParsedPlaylist(decryptM3uUrl(playlist));
            M3uChannel channel = data.getChannels().stream()
                    .filter(c -> matchesM3uChannel(c, channelId))
                    .findFirst()
                    .orElse(null);
            if (channel == null) {
                throw new BadRequestException("Channel not found in playlist");
            }
            String m3uChannelId = M3uPlaylistParser.extractChannelId(channel.getStreamUrl(), channel.getTvgId());
            String[] creds = M3uPlaylistParser.extractCredentials(channel.getStreamUrl());
            String user = creds != null ? creds[0] : "";
            String pass = creds != null ? creds[1] : "";

            if (channel.getSource() != null) {
                return substituteTemplate(channel.getSource(), startTs, endTs,
                        m3uChannelId != null ? m3uChannelId : channelId, ext, user, pass);
            }

            CatchUpMethod method = CatchUpMethod.fromCode(channel.getMethodRaw());
            if (method == CatchUpMethod.NONE || creds == null) {
                throw new BadRequestException("No playable catch-up source available for this channel");
            }
            return buildFromMethod(method, M3uPlaylistParser.extractOrigin(channel.getStreamUrl()),
                    user, pass, m3uChannelId != null ? m3uChannelId : channelId, startTs, endTs, ext);
        }

        throw new BadRequestException("Catch-up is not supported for this playlist type");
    }

    private String buildFromMethod(CatchUpMethod method, String origin, String user, String pass,
            String channelId, long startTs, long endTs, String ext) {
        if (origin == null || origin.isBlank() || user.isBlank() || pass.isBlank()) {
            throw new BadRequestException("No playable catch-up source available for this channel");
        }
        switch (method) {
            case XC:
                return String.format("%s/live/%s/%s/%s.%s?start=%d&end=%d",
                        origin, user, pass, channelId, ext, startTs, endTs);
            case FLUSSONIC:
                return String.format("%s/flussonic/%s/%s/timeshift_abs/%d/%s.%s",
                        origin, user, pass, startTs, channelId, ext);
            case SHIFT:
                return String.format("%s/shift/%s/%s/%d.%d.%s.%s",
                        origin, user, pass, startTs, endTs, channelId, ext);
            case DEFAULT:
                return String.format("%s/%s/%s/%s/%d.%d.%s",
                        origin, user, pass, channelId, startTs, endTs, ext);
            default:
                throw new BadRequestException("Unsupported catch-up method: " + method);
        }
    }

    /**
     * Substitutes provider catch-up URL template placeholders.
     * Supports {utc}, {utcend}, {start}, {end}, {start-1}, {end-1}, {duration},
     * {Y}/{m}/{d}/{H}/{M}/{S}, {channelId}/{channel-id}, {ext}, {user}, {pass}.
     */
    private String substituteTemplate(String template, long startTs, long endTs, String channelId,
            String ext, String user, String pass) {
        String url = template;
        Map<String, String> tokens = new HashMap<>();
        tokens.put("{utc}", String.valueOf(startTs));
        tokens.put("{utcend}", String.valueOf(endTs));
        tokens.put("{duration}", String.valueOf(endTs - startTs));
        tokens.put("{start}", formatDateTime(startTs, "yyyyMMddHHmmss"));
        tokens.put("{end}", formatDateTime(endTs, "yyyyMMddHHmmss"));
        tokens.put("{start-1}", formatDateTime(startTs, "yyyy-MM-dd HH:mm:ss"));
        tokens.put("{end-1}", formatDateTime(endTs, "yyyy-MM-dd HH:mm:ss"));
        tokens.put("{Y}", formatDateTime(startTs, "yyyy"));
        tokens.put("{m}", formatDateTime(startTs, "MM"));
        tokens.put("{d}", formatDateTime(startTs, "dd"));
        tokens.put("{H}", formatDateTime(startTs, "HH"));
        tokens.put("{M}", formatDateTime(startTs, "mm"));
        tokens.put("{S}", formatDateTime(startTs, "ss"));
        tokens.put("{channelId}", channelId);
        tokens.put("{channel-id}", channelId);
        tokens.put("{chid}", channelId);
        tokens.put("{ext}", ext);
        tokens.put("{user}", user);
        tokens.put("{pass}", pass);
        // Some providers use unquoted %t/%f style tokens or ${start} syntax
        tokens.put("${start}", String.valueOf(startTs));
        tokens.put("${end}", String.valueOf(endTs));

        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            url = url.replace(entry.getKey(), entry.getValue());
        }
        return url;
    }

    private String formatDateTime(long epochSeconds, String pattern) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private List<XtreamLiveStream> getLiveStreamsCached(UUID playlistId) {
        String key = "xtream:streams:" + playlistId;
        List<XtreamLiveStream> streams = cache.get(key);
        if (streams != null) {
            return streams;
        }
        List<XtreamLiveStream> fetched = xtreamCatalog.getLiveStreams(playlistId, null);
        long ttl = fetched.isEmpty() ? EMPTY_STREAMS_TTL : STATUS_TTL;
        cache.put(key, fetched, ttl);
        return fetched;
    }

    private Playlist loadPlaylist(UUID playlistId) {
        return playlistRepository.findById(playlistId)
                .orElseThrow(() -> new BadRequestException("Playlist not found"));
    }

    private String decryptM3uUrl(Playlist playlist) {
        return encryptionUtil.decrypt(playlist.getM3uUrl());
    }

    private String normalizeExtension(String extension) {
        String ext = extension != null && !extension.isBlank() ? extension.trim() : "ts";
        if (ext.startsWith(".")) {
            ext = ext.substring(1);
        }
        return ext;
    }

    private String normalizeBaseUrl(String serverUrl) {
        if (serverUrl == null || serverUrl.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(serverUrl);
            String origin = uri.getScheme() + "://" + uri.getAuthority();
            return origin.endsWith("/") ? origin.substring(0, origin.length() - 1) : origin;
        } catch (Exception e) {
            return serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        }
    }
}