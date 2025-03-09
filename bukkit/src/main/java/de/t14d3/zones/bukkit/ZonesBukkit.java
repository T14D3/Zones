package de.t14d3.zones.bukkit;

import com.sk89q.worldedit.WorldEdit;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.bukkit.commands.RootCommand;
import de.t14d3.zones.bukkit.listeners.*;
import de.t14d3.zones.integrations.FAWEIntegration;
import de.t14d3.zones.integrations.PlaceholderAPI;
import de.t14d3.zones.integrations.WorldEditSession;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.utils.DebugLoggerManager;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.utils.Utils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZonesBukkit extends JavaPlugin {

    private static ZonesBukkit instance;
    public boolean debug = false;
    private ZonesPlatform platform;
    private Zones zones;
    private BukkitPermissionManager permissionManager;
    private RegionManager regionManager;
    private Messages messages;
    private DebugLoggerManager debugLogger;
    private BukkitTypes types;

    public static ZonesBukkit getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {


        this.platform = new BukkitPlatform(this);
        instance = this;

        this.types = new BukkitTypes();
        this.zones = new Zones(platform);
        this.debug = zones.debug;
        // Configure CommandAPI
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this)
                .verboseOutput(debug)
                .skipReloadDatapacks(true)
                .silentLogs(!debug)
                .usePluginNamespace()
        );

        this.regionManager = zones.getRegionManager();
        this.messages = zones.getMessages();
        this.debugLogger = zones.getDebugLogger();

        this.permissionManager = new BukkitPermissionManager(zones);
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();
        this.regionManager.loadRegions();

        this.saveDefaultConfig();

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new PlayerEventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(zones), this);
        this.getServer().getPluginManager().registerEvents(new WorldEventListener(zones), this);
        this.getServer().getPluginManager().registerEvents(new ChunkEventListener(), this);
        ExplosivesListener explosivesListener = new ExplosivesListener(this);
        BlockEventListener blockEventListener = new BlockEventListener(zones);

        // Register mode permissions
        for (Utils.SelectionMode mode : Utils.SelectionMode.values()) {
            getServer().getPluginManager().addPermission(
                    new Permission("zones.mode." + mode.getName().toLowerCase() + ".main", PermissionDefault.OP));
            getServer().getPluginManager().addPermission(
                    new Permission("zones.mode." + mode.getName().toLowerCase() + ".sub", PermissionDefault.OP));
        }

        permissionManager.getPermissions().forEach(permission -> {
            this.getServer().getPluginManager().addPermission(
                    new Permission(permission.getName(), permission.getDescription(),
                            permission.getLevel() >= 2 ? PermissionDefault.OP : PermissionDefault.TRUE)
            );
        });


        // Register saving task
        if (getSavingMode() == Utils.SavingModes.PERIODIC) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                regionManager.saveRegions();
                getLogger().info("Zones have been saved.");
            }, 20L, getConfig().getInt("zone-saving.period", 60) * 20L);
        }


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
        CacheUtils.getInstance().startCacheRunnable();

        RootCommand rootCommand = new RootCommand(this);

        getLogger().info("Zones plugin has been enabled! Loaded " + regionManager.regions().size() + " regions.");
    }

    @Override
    public void onDisable() {
        // Save regions to datasource before plugin shutdown
        regionManager.saveRegions();
        regionManager.regions().clear();
        CommandAPI.onDisable();
        regionManager.getDataSourceManager().close();

        getLogger().info("Zones plugin is disabling and regions are saved.");
    }

    // Getters
    public RegionManager getRegionManager() {
        return regionManager;
    }

    public BukkitPermissionManager getPermissionManager() {
        return permissionManager;
    }

    public Utils.SavingModes getSavingMode() {
        return Utils.SavingModes.fromString(this.getConfig().getString("zone-saving.mode", "MODIFIED"));
    }

    public DebugLoggerManager getDebugLogger() {
        return debugLogger;
    }

    public Messages getMessages() {
        return messages;
    }

    public ZonesPlatform getPlatform() {
        return platform;
    }

    public Zones getZones() {
        return zones;
    }

    public Types getTypes() {
        return types;
    }
}
