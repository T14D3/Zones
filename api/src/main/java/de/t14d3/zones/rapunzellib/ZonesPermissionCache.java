package de.t14d3.zones.rapunzellib;

import de.t14d3.rapunzellib.objects.RPlayer;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small per-player permission cache backed by {@link de.t14d3.rapunzellib.objects.RExtras}.
 *
 * <p>This is intentionally tiny and platform-agnostic: it caches only boolean results for a
 * permission node for a short TTL to reduce repeated {@code hasPermission} calls in very hot paths
 * (e.g. block-break spam).</p>
 */
public final class ZonesPermissionCache {
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();

    /**
     * Evaluates {@code player.hasPermission(permission)} with a per-player TTL cache.
     *
     * <p>The cache is stored in {@link RPlayer#extras()} using {@link ZonesExtraKeys#PERMISSION_CACHE}.</p>
     *
     * @param player     player to check
     * @param permission permission node
     * @param ttlNanos   time-to-live in nanoseconds; {@code <= 0} disables caching
     * @return {@code true} if permitted
     */
    public static boolean hasPermissionCached(RPlayer player, String permission, long ttlNanos) {
        if (player == null) return false;
        if (permission == null || permission.isBlank()) return false;
        if (ttlNanos <= 0) return player.hasPermission(permission);

        try {
            ZonesPermissionCache cache = player.extras()
                    .get(ZonesExtraKeys.PERMISSION_CACHE)
                    .orElse(null);

            if (cache == null) {
                cache = new ZonesPermissionCache();
                player.extras().put(ZonesExtraKeys.PERMISSION_CACHE, cache);
            }

            Objects.requireNonNull(cache, "cache");

            long now = System.nanoTime();
            Entry cached = cache.entries.get(permission);
            if (cached != null && cached.expiresAtNanos > now) return cached.value;

            boolean computed = player.hasPermission(permission);
            long expiresAt = now + ttlNanos;
            if (expiresAt < now) expiresAt = Long.MAX_VALUE;
            cache.entries.put(permission, new Entry(computed, expiresAt));
            return computed;
        } catch (UnsupportedOperationException ignored) {
            return player.hasPermission(permission);
        }
    }

    private static final class Entry {
        private final boolean value;
        private final long expiresAtNanos;

        private Entry(boolean value, long expiresAtNanos) {
            this.value = value;
            this.expiresAtNanos = expiresAtNanos;
        }
    }
}
