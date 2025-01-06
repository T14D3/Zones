package de.t14d3.zones;

import de.t14d3.zones.listeners.CommandListener;
import de.t14d3.zones.listeners.PlayerInteractListener;
import de.t14d3.zones.listeners.PlayerQuitListener;
import de.t14d3.zones.utils.BeaconUtils;
import de.t14d3.zones.utils.ParticleHandler;
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
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Zones extends JavaPlugin {

    private RegionManager regionManager;
    private PermissionManager permissionManager;
    private BeaconUtils beaconUtils;
    private ParticleHandler particleHandler;
    public Map<UUID, Pair<Location, Location>> selection = new HashMap<>();
    public Map<UUID, BoundingBox> particles = new HashMap<>();
    private Map<String, String> messages;
    private Types types;

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
        this.beaconUtils = new BeaconUtils(this);
        this.particleHandler = new ParticleHandler(this);
        particleHandler.particleScheduler();

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

        // Populate Types
        types = new Types();
        types.populateTypes();

        // Register command executor and tab completer
        LifecycleEventManager<Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register("zone", new CommandListener(this, regionManager));
        });

        // Register saving task
        if (getSavingMode() == Utils.SavingModes.PERIODIC) {
            getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
                regionManager.saveRegions();
                getLogger().info("Zones have been saved.");
            }, 20L, getConfig().getInt("zone-saving.period", 60) * 20L);
        }

        getLogger().info("Zones plugin has been enabled! Loaded " + regionManager.loadedRegions.size() + " regions.");
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
    public BeaconUtils getBeaconUtils() { return beaconUtils; }
    public ParticleHandler getParticleHandler() {
        return particleHandler;
    }

    public Types getTypes() {
        return this.types;
    }

    public Utils.SavingModes getSavingMode() {
        return Utils.SavingModes.fromString(this.getConfig().getString("zone-saving.mode", "MODIFIED"));
    }
}
