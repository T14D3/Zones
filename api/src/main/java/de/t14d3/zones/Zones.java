package de.t14d3.zones;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.DebugLoggerManager;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.utils.Utils;
import de.t14d3.zones.visuals.FindBossbar;
import de.t14d3.zones.visuals.particles.ParticleVisualManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ThisEscapedInObjectConstruction")
/**
 * Core Zones service container.
 *
 * <p>This class wires up managers (regions, permissions, config, visuals) and exposes
 * them to the platform implementations (Bukkit/Fabric) and integrations.</p>
 */
public class Zones {
    private static Zones instance;

    private final RegionManager regionManager;
    private final PermissionManager permissionManager;
    private final ZonesPlatform platform;
    private final DebugLoggerManager debugLogger;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final YamlConfig config;
    private final ThreadPoolExecutor executor;
    private final FindBossbar findBossbar;
    private final ParticleVisualManager particleVisualManager;

    public boolean debug = false;

    public Zones(ZonesPlatform platform) {
        instance = this;
        this.platform = platform;

        Types types = platform.getTypes();
        types.populateTypes();

        // Load config.yml and merge missing keys/comments from the bundled default.
        this.config = Rapunzel.context().configs()
                .load(new File(platform.getDataFolder(), "config.yml").toPath(), "config.yml");

        boolean debug = config.getBoolean("debug", false) || Objects.equals(System.getenv("ZONES_DEBUG"),
                "true");

        this.debugLogger = new DebugLoggerManager(this, debug);
        // Ensure flags are registered (via class initialization) before regions are loaded/edited.
        new Flags();


        this.regionManager = new RegionManager(this);
        this.permissionManager = platform.createPermissionManager(this);
        this.particleVisualManager = new ParticleVisualManager(this);
        this.executor = new ThreadPoolExecutor(
                0,
                config.getInt("advanced.thread-pool.max-size", Runtime.getRuntime().availableProcessors() * 2),
                config.getInt("advanced.thread-pool.keepalive", 60),
                TimeUnit.SECONDS,
                new SynchronousQueue<>()
        );

        this.findBossbar = new FindBossbar(this);
    }

    /**
     * Returns the global Zones instance for this JVM.
     */
    public static Zones getInstance() {
        return instance;
    }

    /**
     * Returns the platform bridge (Bukkit/Fabric) hosting Zones.
     */
    public ZonesPlatform getPlatform() {
        return platform;
    }

    /**
     * Returns the debug logger facility (may be disabled).
     */
    public DebugLoggerManager getDebugLogger() {
        return debugLogger;
    }

    /**
     * Returns the SLF4J logger for Zones.
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Returns the region manager responsible for loading and querying regions.
     */
    public RegionManager getRegionManager() {
        return regionManager;
    }

    /**
     * Returns the permission manager used for action checks.
     */
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * Returns the message formatting service used to build user-facing messages.
     */
    public MessageFormatService getMessages() {
        return Rapunzel.context().messages();
    }

    /**
     * Returns the config manager wrapper around {@code config.yml}.
     */
    public YamlConfig getConfig() {
        return config;
    }

    /**
     * Returns the platform data folder.
     */
    public File getDataFolder() {
        return platform.getDataFolder();
    }

    /**
     * Returns the thread pool used for asynchronous/auxiliary work.
     */
    public ThreadPoolExecutor getThreadPool() {
        return executor;
    }

    /**
     * Returns the configured region saving mode.
     */
    public Utils.SavingModes getSavingMode() {
        return Utils.SavingModes.fromString(
                config.getString("zone-saving.mode", "MODIFIED")
        );
    }

    /**
     * Returns the bossbar handler used for "find region" visuals.
     */
    public FindBossbar getFindBossbar() {
        return findBossbar;
    }

    public ParticleVisualManager getParticleVisualManager() {
        return particleVisualManager;
    }
}
