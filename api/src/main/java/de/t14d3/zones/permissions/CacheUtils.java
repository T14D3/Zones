package de.t14d3.zones.permissions;

import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.utils.DebugLoggerManager;

import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheUtils {
    private final int ttl;
    private final int limit;
    private final int checkInterval;
    private ScheduledFuture<?> cacheTask;
    private static CacheUtils instance;
    private final Zones plugin;

    final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> interactionCache = new ConcurrentHashMap<>();
    public final ConcurrentHashMap<String, ConcurrentLinkedQueue<CacheEntry>> permissionCache = new ConcurrentHashMap<>();

    public CacheUtils(Zones plugin) {
        instance = this;
        this.ttl = plugin.getConfig().getInt("cache.ttl", 300);
        this.checkInterval = plugin.getConfig().getInt("cache.check-interval", 10);
        this.limit = plugin.getConfig().getInt("cache.limit", 0);
        this.plugin = plugin;
    }

    public static CacheUtils getInstance() {
        if (instance == null) throw new IllegalStateException("CacheUtils is not yet initialized!");
        return instance;
    }

    public void startCacheRunnable() {
        CacheRunnable runnable = new CacheRunnable(plugin.getDebugLogger());
        cacheTask = Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(runnable, checkInterval, checkInterval, TimeUnit.MINUTES);
        plugin.getLogger()
                .info("Cache scheduler has been started! (TTL: {} seconds, Interval: {} minutes, Limit: {})", ttl,
                        checkInterval, limit);
    }

    public void invalidateInteractionCache(UUID target) {
        interactionCache.remove(target.toString());
    }

    public void invalidateInteractionCache(String target) {
        interactionCache.remove(target);
    }

    public void invalidateInteractionCaches() {
        interactionCache.clear();
    }

    public void invalidateInteractionCacheForChunk(int chunkX, int chunkZ, String world) {
        Zones.getInstance().getThreadPool().execute(() -> {
            synchronized (interactionCache) {
                interactionCache.forEach((uuid, cacheEntries) -> {
                    for (CacheEntry entry : cacheEntries) {
                        BlockLocation location = (BlockLocation) entry.getFlag();
                        int locX = location.getX() >> 4;
                        int locZ = location.getZ() >> 4;
                        if (chunkX == locX && chunkZ == locZ) {
                            interactionCache.computeIfAbsent(uuid, k -> new ConcurrentLinkedQueue<>()).remove(entry);
                        }
                    }
                });
            }
        });
    }

    public void invalidateCache(String target) {
        permissionCache.remove(target);
    }

    public void invalidateCaches() {
        permissionCache.clear();
    }

    public class CacheRunnable implements Runnable {
        private final DebugLoggerManager logger;

        public CacheRunnable(DebugLoggerManager logger) {
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
