package de.t14d3.zones;

import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.*;
import de.t14d3.zones.visuals.FindBossbar;
import org.slf4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Zones {
    private Flags flags = new Flags();
    private CacheUtils cacheUtils;
    private Messages messages;
    private static Zones instance;

    private Types types;
    private Utils utils;
    private FindBossbar findBossbar;

    public Map<Player, Box> selection = new HashMap<>();
    private RegionManager regionManager;
    private PermissionManager permissionManager;
    private ZonesPlatform platform;
    private DebugLoggerManager debugLogger;
    private ConfigManager configManager;
    private ThreadPoolExecutor executor;

    public boolean debug = false;

    public Zones() {
        this.cacheUtils = CacheUtils.getInstance();
        this.debugLogger = new DebugLoggerManager(this, true);

        this.configManager = new ConfigManager(this);
        new ConfigUpdater(this);

        this.permissionManager = new PermissionManager(this);
        this.regionManager = new RegionManager(this, permissionManager);
        this.permissionManager.setRegionManager(regionManager);
        this.executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        this.types.populateTypes();
    }

    public static Zones getInstance() {
        return instance;
    }

    public void setPlatform(ZonesPlatform platform) {
        this.platform = platform;
    }

    public ZonesPlatform getPlatform() {
        return platform;
    }

    public DebugLoggerManager getDebugLogger() {
        return debugLogger;
    }

    public Logger getLogger() {
        return null;
    }

    public RegionManager getRegionManager() {
        return regionManager;
    }
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public Messages getMessages() {
        return messages;
    }

    public ConfigManager getConfig() {
        return configManager;
    }

    public File getDataFolder() {
        return platform.getDataFolder();
    }

    public ThreadPoolExecutor getThreadPool() {
        return executor;
    }
}
