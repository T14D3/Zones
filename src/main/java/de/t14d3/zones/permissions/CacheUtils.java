package de.t14d3.zones.permissions;

import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.DebugLoggerManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheUtils {
    private final int ttl;
    private final int limit;
    private final int checkInterval;
    protected BukkitRunnable cacheRunnable;
    private static CacheUtils instance;
    private final Zones plugin;

    final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> interactionCache = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>>();
    public final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> permissionCache = new ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>>();

    public CacheUtils(Zones plugin) {
        instance = this;
        this.ttl = plugin.getConfig().getInt("cache.ttl", 300);
        this.checkInterval = plugin.getConfig().getInt("cache.check-interval", 10) * 20 * 60;
        this.limit = plugin.getConfig().getInt("cache.limit", 0);
        this.plugin = plugin;
    }

    public static CacheUtils getInstance() {
        return instance;
    }

    public void startCacheRunnable() {
        this.cacheRunnable = new cacheRunnable(plugin.getDebugLogger());
        this.cacheRunnable.runTaskTimerAsynchronously(plugin, checkInterval, checkInterval);
        plugin.getLogger()
                .info("Cache scheduler has been started! (TTL: " + ttl + " seconds, Interval: " + checkInterval + " ticks, Limit: " + limit + ")");
    }

    /**
     * Invalidates the interaction cache for a player.
     * Should be called e.g. when a player logs out.
     *
     * @param target The player to invalidate the cache for.
     */
    public void invalidateInteractionCache(UUID target) {
        interactionCache.remove(target.toString());
    }

    public void invalidateInteractionCache(String target) {
        interactionCache.remove(target);
    }

    /**
     * Invalidates the interaction cache for all players.
     * Should be called e.g. when the plugin is reloaded
     * or when a region area is changed.
     */
    public void invalidateInteractionCaches() {
        interactionCache.clear();
    }

    /**
     * Invalidates the interaction cache for a chunk.
     * Should be called e.g. when a chunk is unloaded.
     */
    public void invalidateInteractionCacheForChunk(int chunkX, int chunkZ, String world) {
        Bukkit.getScheduler().runTaskAsynchronously(Zones.getInstance(), () -> {
            synchronized (interactionCache) {
                interactionCache.forEach((uuid, cacheEntries) -> {
                    for (CacheEntry entry : cacheEntries) {
                        Location location = (Location) entry.getFlag();
                        if (!world.equals(location.getWorld().getName())) {
                            continue;
                        }
                        int locX = location.getBlockX() >> 4;
                        int locZ = location.getBlockZ() >> 4;
                        if (chunkX == locX && chunkZ == locZ) {
                            interactionCache.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>()).remove(entry);
                        }
                    }
                });
            }
        });
    }

    /**
     * Invalidates the flag/permission cache for a player.
     * Should be called when a region's permissions are changed
     *
     * @param target The target player/group to invalidate the cache for.
     */
    public void invalidateCache(String target) {
        permissionCache.remove(target);
    }

    /**
     * Invalidates the flag/permission cache for all players.
     * Should be called when the plugin is reloaded
     * or when a region area is changed.
     */
    public void invalidateCaches() {
        permissionCache.clear();
    }

    public class cacheRunnable extends BukkitRunnable {
        private final DebugLoggerManager logger;

        public cacheRunnable(DebugLoggerManager logger) {
            this.logger = logger;
        }

        @Override
        public void run() {
            logger.log("Running cache scheduler...");
            synchronized (interactionCache) {
                synchronized (permissionCache) {
                    AtomicInteger initialSize = new AtomicInteger();
                    interactionCache.forEach(
                            (uuid, entries) -> initialSize.addAndGet(entries.size())
                    );
                    AtomicInteger initialPermSize = new AtomicInteger();
                    permissionCache.forEach(
                            (uuid, entries) -> initialPermSize.addAndGet(entries.size())
                    );
                    logger.log(
                            "Cache size: " + initialSize.get() + " (interaction cache) and " + initialPermSize.get() + " (permission cache)");
                    AtomicInteger size = new AtomicInteger();
                    AtomicInteger permSize = new AtomicInteger();
                    final long current = System.currentTimeMillis() >> 10;
                    if (limit > 0) {
                        if (initialSize.get() > limit) {
                            interactionCache.clear();
                        }
                        if (initialPermSize.get() > limit) {
                            permissionCache.clear();
                        }
                    }
                    interactionCache.values().forEach(
                            entries -> entries.removeIf(entry -> current - entry.timestamp > ttl)
                    );
                    interactionCache.forEach((key, value) -> {
                        if (value.isEmpty()) {
                            interactionCache.remove(key);
                        } else {
                            size.addAndGet(value.size());
                        }
                    });
                    permissionCache.values().forEach(
                            entries -> entries.removeIf(entry -> current - entry.timestamp > ttl)
                    );
                    permissionCache.forEach((key, value) -> {
                        if (value.isEmpty()) {
                            permissionCache.remove(key);
                        } else {
                            permSize.addAndGet(value.size());
                        }
                    });

                    logger.log("Removed " + (initialSize.get() - size.get()) + " interaction cache entries and "
                            + (initialPermSize.get() - permSize.get()) + " permission cache entries.");
                }
            }
        }
    }
}
