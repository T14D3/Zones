package de.t14d3.zones;

import de.t14d3.zones.listeners.CommandListener;
import de.t14d3.zones.listeners.PlayerInteractListener;
import de.t14d3.zones.listeners.PlayerQuitListener;
import de.t14d3.zones.utils.BeaconUtils;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.utils.Utils;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Zones extends JavaPlugin {

    private RegionManager regionManager;
    private PermissionManager permissionManager;
    private Utils utils;
    private BeaconUtils beaconUtils;
    public Map<UUID, Pair<Location, Location>> selection = new HashMap<>();
    private Map<String, String> messages;
    public List<String> types;
    public List<String> blockTypes;
    public List<String> entityTypes;
    public List<String> containerTypes;
    public List<String> redstoneTypes;

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void onEnable() {
        // Initialize PermissionManager first without RegionManager
        this.permissionManager = new PermissionManager();

        // Initialize RegionManager with PermissionManager
        this.regionManager = new RegionManager(this, permissionManager);

        // Set RegionManager in PermissionManager to complete dependency setup
        this.permissionManager.setRegionManager(regionManager);

        // Initialize utilities
        this.utils = new Utils(this);
        this.beaconUtils = new BeaconUtils(this);

        // Load regions from regions.yml
        regionManager.loadRegions();

        this.saveDefaultConfig();

        // Load messages from messages.yml
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false); // Copy default messages.yml from jar
        }

        FileConfiguration messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        Map<String, Object> rawMessages = messagesConfig.getConfigurationSection("messages").getValues(true); // Get raw messages as Map<String, Object>
        messages = new HashMap<>();

        // Convert to Map<String, String>
        for (Map.Entry<String, Object> entry : rawMessages.entrySet()) {
            messages.put(entry.getKey(), entry.getValue().toString()); // Convert each value to String
        }


        // Register listeners
        this.getServer().getPluginManager().registerEvents(new PlayerInteractListener(regionManager, permissionManager, this), this);
        this.getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getLogger().info("Zones plugin has been enabled and regions are loaded.");

        // Populate Types
        Types types = new Types();
        types.populateTypes();
        this.types = types.allTypes;
        this.blockTypes = types.blockTypes;
        this.entityTypes = types.entityTypes;
        this.containerTypes = types.containerTypes;
        this.redstoneTypes = types.redstoneTypes;

        // Register command executor and tab completer
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register("zone", new CommandListener(this, regionManager));
        });
    }

    @Override
    public void onDisable() {
        // Save regions to regions.yml before plugin shutdown
        regionManager.saveRegions();
        getLogger().info("Zones plugin is disabling and regions are saved.");
    }

    // Getters
    public RegionManager getRegionManager() { return regionManager; }
    public PermissionManager getPermissionManager() {return permissionManager; }
    public Map<String, String> getMessages() { return messages; }
    public Utils getUtils() { return utils; }
    public BeaconUtils getBeaconUtils() { return beaconUtils; }
}
