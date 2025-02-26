package de.t14d3.zones;

import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ThisEscapedInObjectConstruction")
public class Zones {
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

        this.configManager = new ConfigManager(this, new File(platform.getDataFolder(), "config.yml"));

        boolean debug = configManager.getBoolean("debug", false) || Objects.equals(System.getenv("ZONES_DEBUG"),
                "true");

        this.debugLogger = new DebugLoggerManager(this, debug);

        new ConfigUpdater(this);
        this.cacheUtils = new CacheUtils(this);
        Flags flags = new Flags();

        Properties messagesConfig = new Properties();
        File messagesFile = new File(platform.getDataFolder(), "messages.properties");
        if (!messagesFile.exists()) {
            try {
                //noinspection DataFlowIssue // File is always included in jar
                Files.copy(getClass().getResourceAsStream("/messages.properties"), messagesFile.toPath());
            } catch (Exception e1) {
                getLogger().error("Failed to copy default messages.properties: {}", e1.getMessage());
            }
        }
        try {
            messagesConfig.load(new FileInputStream(messagesFile));
        } catch (Exception e) {
            getLogger().error("Failed to load messages.properties: {}", e.getMessage());

        }
        this.messages = new Messages(messagesConfig, this);

        PlayerRepository playerRepository = new PlayerRepository();

        this.permissionManager = platform.getPermissionManager();
        this.regionManager = new RegionManager(this, permissionManager);
        var threadConfig = configManager.getConfig().node("advanced", "threadpool");
        this.executor = new ThreadPoolExecutor(
                0,
                threadConfig.node("max-size").getInt(Runtime.getRuntime().availableProcessors() * 2),
                threadConfig.node("keepalive").getLong(60L),
                TimeUnit.SECONDS,
                new SynchronousQueue<>()
        );

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

    public Utils.SavingModes getSavingMode() {
        return null;
    }
}
