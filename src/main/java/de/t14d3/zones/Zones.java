package de.t14d3.zones;

import de.t14d3.zones.listeners.CommandListener;
import de.t14d3.zones.listeners.PlayerInteractListener;
import de.t14d3.zones.listeners.PlayerQuitListener;
import de.t14d3.zones.listeners.TabCompleteListener;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Zones extends JavaPlugin {

    private RegionManager regionManager;
    private PermissionManager permissionManager;
    private Utils utils;
    public Map<UUID, Pair<Location, Location>> selection = new HashMap<>();

    @Override
    public void onEnable() {
        // Initialize PermissionManager first without RegionManager
        this.permissionManager = new PermissionManager();

        // Initialize RegionManager with PermissionManager
        this.regionManager = new RegionManager(this, permissionManager);

        // Set RegionManager in PermissionManager to complete dependency setup
        this.permissionManager.setRegionManager(regionManager);

        // Initialize utilities
        this.utils = new Utils(this);

        // Load regions from regions.yml
        Map<String, RegionManager.Region> regions = regionManager.loadRegions();

        this.saveDefaultConfig();

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(regionManager, permissionManager, this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getLogger().info("Zones plugin has been enabled and regions are loaded.");

        // Register command executor and tab completer
        Objects.requireNonNull(this.getCommand("zone")).setExecutor(new CommandListener(this, regionManager));
        Objects.requireNonNull(this.getCommand("zone")).setTabCompleter(new TabCompleteListener(this, regionManager));
    }

    @Override
    public void onDisable() {
        // Save regions to regions.yml before plugin shutdown
        regionManager.saveRegions();
        getLogger().info("Zones plugin is disabling and regions are saved.");
    }

    // Getters
    public RegionManager getRegionManager() { return regionManager; }
    public Utils getUtils() { return utils; }
}
