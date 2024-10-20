package de.t14d3.zones;

import de.t14d3.zones.listeners.PlayerInteractListener;
import de.t14d3.zones.listeners.PlayerQuitListener;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;

public final class Zones extends JavaPlugin {

    private RegionManager regionManager;
    private PermissionManager permissionManager;

    public Map<UUID, Pair<Location, Location>> selection;

    @Override
    public void onEnable() {
        // Initialize RegionManager
        this.regionManager = new RegionManager(this);

        this.permissionManager = new PermissionManager(regionManager);

        // Load regions from regions.yml
        Map<String, RegionManager.Region> regions = regionManager.loadRegions();

        // Register listeners
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(regionManager, permissionManager), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getLogger().info("Zones plugin has been enabled and regions are loaded.");
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

    public Map<UUID, Pair<Location, Location>> getSelection() {
        return selection;
    }
    public void removeSelection(UUID uuid) {
        if (selection.containsKey(uuid)) {
            selection.remove(uuid);
        }
    }
    public void addSelection(UUID uuid, Pair<Location, Location> selection) {
        this.selection.put(uuid, selection);
    }
}
