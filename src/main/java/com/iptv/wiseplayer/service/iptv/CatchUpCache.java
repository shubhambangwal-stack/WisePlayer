package com.iptv.wiseplayer.service.iptv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Tiny in-memory TTL cache used to reduce API calls to upstream IPTV providers
 * for EPG data and catch-up availability snapshots.
 */
@Component
public class CatchUpCache {

    private static final Logger log = LoggerFactory.getLogger(CatchUpCache.class);

    private static final class Entry {
        private final Object value;
        private final long expiresAt;

        Entry(Object value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        Entry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired()) {
            cache.remove(key);
            return null;
        }
        return (T) entry.value;
    }

    public void put(String key, Object value, long ttlMillis) {
        cache.put(key, new Entry(value, System.currentTimeMillis() + ttlMillis));
    }

    /**
     * Returns the cached value or loads it via the supplier. The supplier result
     * is cached for {@code ttlMillis}. A {@code null} supplier result is not
     * cached (so transient provider failures never poison the cache).
     */
    public <T> T getOrLoad(String key, long ttlMillis, Supplier<T> loader) {
        T cached = get(key);
        if (cached != null) {
            return cached;
        }
        T value = loader.get();
        if (value != null) {
            put(key, value, ttlMillis);
        }
        return value;
    }

    public void evict(String prefix) {
        cache.keySet().removeIf(k -> k.startsWith(prefix));
        log.debug("Evicted cache entries with prefix '{}'", prefix);
    }

    public void clear() {
        cache.clear();
    }
}