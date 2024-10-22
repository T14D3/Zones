package de.t14d3.zones;

import de.t14d3.zones.listeners.CommandListener;
import de.t14d3.zones.listeners.PlayerInteractListener;
import de.t14d3.zones.listeners.PlayerQuitListener;
import de.t14d3.zones.listeners.TabCompleteListener;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Zones extends JavaPlugin {

    private RegionManager regionManager;
    private PermissionManager permissionManager;

    public Map<UUID, Pair<Location, Location>> selection = new HashMap<>();

    @Override
    public void onEnable() {
        // Initialize RegionManager
        this.regionManager = new RegionManager(this);
        this.permissionManager = new PermissionManager(regionManager);

        // Load regions from regions.yml
        Map<String, RegionManager.Region> regions = regionManager.loadRegions();

        this.saveDefaultConfig();


        // Register listeners
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(regionManager, permissionManager, this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getLogger().info("Zones plugin has been enabled and regions are loaded.");

        // Register command executor
        this.getCommand("zone").setExecutor(new CommandListener(this , regionManager));
        this.getCommand("zone").setTabCompleter(new TabCompleteListener());

    }

    @Override
    public void onDisable() {
        // Save regions to regions.yml before plugin shutdown
        regionManager.saveRegions();

        getLogger().info("Zones plugin is disabling and regions are saved.");
    }

    // Getter for RegionManager
    public RegionManager getRegionManager() {
        return regionManager;
    }

}
