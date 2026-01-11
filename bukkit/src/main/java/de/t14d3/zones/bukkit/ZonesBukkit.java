package de.t14d3.zones.bukkit;

import com.sk89q.worldedit.WorldEdit;
import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.events.GameEventBridge;
import de.t14d3.rapunzellib.events.GameEventBus;
import de.t14d3.rapunzellib.events.paper.PaperGameEventBridgeInstaller;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.platform.paper.PaperRapunzelBootstrap;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.bukkit.commands.RootCommand;
import de.t14d3.zones.bukkit.integrations.FAWEIntegration;
import de.t14d3.zones.bukkit.integrations.PlaceholderAPI;
import de.t14d3.zones.bukkit.integrations.WorldEditSession;
import de.t14d3.zones.rapunzellib.ZonesRapunzelHooks;
import de.t14d3.zones.utils.DebugLoggerManager;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.utils.Utils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIPaperConfig;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit/Paper plugin entrypoint for Zones.
 *
 * <p>Bootstraps the core {@link Zones} instance and wires Paper/Bukkit integrations (commands, events, visuals).</p>
 */
public final class ZonesBukkit extends JavaPlugin {

    private static ZonesBukkit instance;
    public boolean debug = false;
    private ZonesPlatform platform;
    private Zones zones;
    private BukkitPermissionManager permissionManager;
    private RegionManager regionManager;
    private DebugLoggerManager debugLogger;
    private BukkitTypes types;

    /**
     * Returns the active plugin instance.
     */
    public static ZonesBukkit getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        PaperRapunzelBootstrap.bootstrap(this);

        this.platform = new BukkitPlatform(this);
        instance = this;

        this.types = new BukkitTypes();
        this.zones = new Zones(platform);
        this.debug = zones.debug;
        // Configure CommandAPI
        CommandAPI.onLoad(new CommandAPIPaperConfig(this)
                .verboseOutput(debug)
                .silentLogs(!debug)
        );

        this.regionManager = zones.getRegionManager();
        this.debugLogger = zones.getDebugLogger();

        this.permissionManager = (BukkitPermissionManager) zones.getPermissionManager();
    }

    @Override
    public void onEnable() {

        GameEventBus bus = new GameEventBus(Rapunzel.context().scheduler(), Rapunzel.context().logger());
        Rapunzel.context().services().register(GameEventBus.class, bus);

        GameEventBridge bridge = new PaperGameEventBridgeInstaller().install(Rapunzel.context(), bus, this);
        Rapunzel.context().services().register(GameEventBridge.class, bridge);

        ZonesRapunzelHooks.install(zones);

        CommandAPI.onEnable();
        this.regionManager.loadRegions();

        this.saveDefaultConfig();

        // Register mode permissions
        for (Utils.SelectionMode mode : Utils.SelectionMode.values()) {
            getServer().getPluginManager().addPermission(
                    new Permission("zones.mode." + mode.getName().toLowerCase() + ".main", PermissionDefault.OP));
            getServer().getPluginManager().addPermission(
                    new Permission("zones.mode." + mode.getName().toLowerCase() + ".sub", PermissionDefault.OP));
        }

        permissionManager.getPermissions().forEach(permission -> {
            this.getServer().getPluginManager().addPermission(
                    new Permission(permission.name(), permission.description(),
                            permission.level() >= 2 ? PermissionDefault.OP : PermissionDefault.TRUE)
            );
        });


        // PlaceholderAPI integration
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderAPI(this).register();
            getLogger().info("PlaceholderAPI hooked!");
        }

        if (getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            new FAWEIntegration(this).register();
            getLogger().info("FAWE Integration enabled.");
        } else if (getServer().getPluginManager().getPlugin("WorldEdit") != null) {
            WorldEdit.getInstance().getEventBus().register(new WorldEditSession(zones));
            getLogger().info("WorldEdit Integration enabled.");
        }

        RootCommand rootCommand = new RootCommand(this);

        getLogger().info("Zones plugin has been enabled! Loaded " + regionManager.regions().size() + " regions.");
    }

    @Override
    public void onDisable() {
        // Save regions to datasource before plugin shutdown
        regionManager.saveRegions();
        regionManager.clearAll();
        CommandAPI.onDisable();
        regionManager.getDataSourceManager().close();
        Rapunzel.shutdown(this);

        getLogger().info("Zones plugin is disabling and regions are saved.");
    }

    // Getters

    /**
     * Returns the shared region manager.
     */
    public RegionManager getRegionManager() {
        return regionManager;
    }

    /**
     * Returns the permission manager implementation for Bukkit.
     */
    public BukkitPermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * Returns the configured region saving mode.
     */
    public Utils.SavingModes getSavingMode() {
        return Utils.SavingModes.fromString(this.getConfig().getString("zone-saving.mode", "MODIFIED"));
    }

    /**
     * Returns the debug logger facility.
     */
    public DebugLoggerManager getDebugLogger() {
        return debugLogger;
    }

    /**
     * Returns the message formatter service used by Zones.
     */
    public MessageFormatService getMessages() {
        return zones.getMessages();
    }

    /**
     * Returns the platform bridge.
     */
    public ZonesPlatform getPlatform() {
        return platform;
    }

    /**
     * Returns the core Zones instance.
     */
    public Zones getZones() {
        return zones;
    }

    /**
     * Returns the types registry used by Zones.
     */
    public Types getTypes() {
        return types;
    }
}
