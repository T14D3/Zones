package de.t14d3.zones;

import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ThisEscapedInObjectConstruction")
public class Zones {
    private final Flags flags = new Flags();
    private final CacheUtils cacheUtils;
    private final Messages messages;
    private static Zones instance;

    private final RegionManager regionManager;
    private final PermissionManager permissionManager;
    private final ZonesPlatform platform;
    private final DebugLoggerManager debugLogger;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ConfigManager configManager;
    private final ThreadPoolExecutor executor;

    public boolean debug = false;

    public Zones(ZonesPlatform platform) {
        instance = this;
        this.platform = platform;
        this.debugLogger = new DebugLoggerManager(this, true);

        this.configManager = new ConfigManager(this);
        new ConfigUpdater(this);
        this.cacheUtils = new CacheUtils(this);

        // TODO: Make this platform agnostic
        Properties messagesConfig = new Properties();
        try {
            messagesConfig.load(new FileInputStream(new File("plugins/Zones/messages.properties")));
        } catch (IOException e) {
            getLogger().error("Failed to load messages.properties");
        }
        this.messages = new Messages(messagesConfig, this);

        this.permissionManager = platform.getPermissionManager();
        this.regionManager = new RegionManager(this, permissionManager);
        this.executor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        Types types = platform.getTypes();
        types.populateTypes();
    }

    public static Zones getInstance() {
        return instance;
    }

    public ZonesPlatform getPlatform() {
        return platform;
    }

    public DebugLoggerManager getDebugLogger() {
        return debugLogger;
    }

    public Logger getLogger() {
        return logger;
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

    public CacheUtils getCacheUtils() {
        return cacheUtils;
    }
}
