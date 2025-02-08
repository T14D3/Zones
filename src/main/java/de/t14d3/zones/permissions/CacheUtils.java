package de.t14d3.zones.permissions;

import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.DebugLoggerManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheUtils {
    private final int ttl;
    private final int limit;
    private final PermissionManager permissionManager;
    protected BukkitRunnable cacheRunnable;

    public CacheUtils(Zones plugin, PermissionManager permissionManager) {
        this.ttl = plugin.getConfig().getInt("cache.ttl", 300);
        int checkInterval = plugin.getConfig().getInt("cache.check-interval", 10) * 20 * 60;
        this.limit = plugin.getConfig().getInt("cache.limit", 0);
        this.permissionManager = permissionManager;
        this.cacheRunnable = new cacheRunnable(plugin.getDebugLogger());
        this.cacheRunnable.runTaskTimerAsynchronously(plugin, checkInterval, checkInterval);
        plugin.getLogger()
                .info("Cache scheduler has been started! (TTL: " + ttl + " seconds, Interval: " + checkInterval + " ticks, Limit: " + limit + ")");
    }

    private class cacheRunnable extends BukkitRunnable {
        private final DebugLoggerManager logger;

        public cacheRunnable(DebugLoggerManager logger) {
            this.logger = logger;
        }

        @Override
        public void run() {
            logger.log("Running cache scheduler...");
            synchronized (permissionManager.interactionCache) {
                synchronized (permissionManager.permissionCache) {
                    AtomicInteger initialSize = new AtomicInteger();
                    permissionManager.interactionCache.forEach(
                            (uuid, entries) -> initialSize.addAndGet(entries.size())
                    );
                    AtomicInteger initialPermSize = new AtomicInteger();
                    permissionManager.permissionCache.forEach(
                            (uuid, entries) -> initialPermSize.addAndGet(entries.size())
                    );
                    logger.log(
                            "Cache size: " + initialSize.get() + " (interaction cache) and " + initialPermSize.get() + " (permission cache)");
                    AtomicInteger size = new AtomicInteger();
                    AtomicInteger permSize = new AtomicInteger();
                    final long current = System.currentTimeMillis() >> 10;
                    if (limit > 0) {
                        if (initialSize.get() > limit) {
                            permissionManager.interactionCache.clear();
                        }
                        if (initialPermSize.get() > limit) {
                            permissionManager.permissionCache.clear();
                        }
                    }
                    permissionManager.interactionCache.values().forEach(
                            entries -> entries.removeIf(entry -> current - entry.timestamp > ttl)
                    );
                    permissionManager.interactionCache.forEach((key, value) -> {
                        if (value.isEmpty()) {
                            permissionManager.interactionCache.remove(key);
                        } else {
                            size.addAndGet(value.size());
                        }
                    });
                    permissionManager.permissionCache.values().forEach(
                            entries -> entries.removeIf(entry -> current - entry.timestamp > ttl)
                    );
                    permissionManager.permissionCache.forEach((key, value) -> {
                        if (value.isEmpty()) {
                            permissionManager.permissionCache.remove(key);
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
