package de.t14d3.zones;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {

    private final File regionsFile;
    private final FileConfiguration regionsConfig;
    final PermissionManager permissionManager;
    private final Zones plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages;

    public RegionManager(Zones plugin, PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        this.plugin = plugin;
        this.messages = plugin.getMessages();

        regionsFile = new File(plugin.getDataFolder(), "regions.yml");

        if (!regionsFile.exists()) {
            regionsFile.getParentFile().mkdirs();
            plugin.saveResource("regions.yml", false);
        }

        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    public void saveRegions() {
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads regions from the YAML file into memory.
     * @return A map of region keys and their corresponding {@link de.t14d3.zones.Region} objects.
     */
    public Map<String, Region> regions() {
        Map<String, Region> regions = new HashMap<>();
        if (regionsConfig.contains("regions")) {
            for (String key : regionsConfig.getConfigurationSection("regions").getKeys(false)) {
                String name = regionsConfig.getString("regions." + key + ".name");
                Location min = loadLocation("regions." + key + ".min");
                Location max = loadLocation("regions." + key + ".max");
                String parent = regionsConfig.getString("regions." + key + ".parent");

                Map<UUID, Map<String, String>> members = loadMembers(key);
                Region region = new Region(name, min, max, members, key, parent);
                regions.put(key, region);
            }
        }
        return regions;
    }

    private Map<UUID, Map<String, String>> loadMembers(String key) {
        Map<UUID, Map<String, String>> members = new HashMap<>();
        if (regionsConfig.contains("regions." + key + ".members")) {
            for (String uuidStr : regionsConfig.getConfigurationSection("regions." + key + ".members").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, String> permissions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (String perm : regionsConfig.getConfigurationSection("regions." + key + ".members." + uuidStr).getKeys(false)) {
                    permissions.put(perm, regionsConfig.getString("regions." + key + ".members." + uuidStr + "." + perm));
                }
                members.put(uuid, permissions);
            }
        }
        return members;
    }

    // Save a region to the YAML file
    public void saveRegion(String key, Region region) {
        regionsConfig.set("regions." + key + ".name", region.getName());
        saveLocation("regions." + key + ".min", region.getMin());
        saveLocation("regions." + key + ".max", region.getMax());
        if (region.getParent() != null) {
            regionsConfig.set("regions." + key + ".parent", region.getParent());
        }


        saveMembers(key, region.getMembers());
        saveRegions();
    }

    private void saveMembers(String key, Map<UUID, Map<String, String>> members) {
        for (Map.Entry<UUID, Map<String, String>> entry : members.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, String> perm : entry.getValue().entrySet()) {
                regionsConfig.set("regions." + key + ".members." + uuid + "." + perm.getKey(), perm.getValue());
            }
        }
    }

    // Load a location from the YAML file
    private Location loadLocation(String path) {
        String world = regionsConfig.getString(path + ".world");
        double x = regionsConfig.getDouble(path + ".x");
        double y = regionsConfig.getDouble(path + ".y");
        double z = regionsConfig.getDouble(path + ".z");
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    // Save a location to the YAML file
    private void saveLocation(String path, Location loc) {
        regionsConfig.set(path + ".world", loc.getWorld().getName());
        regionsConfig.set(path + ".x", loc.getX());
        regionsConfig.set(path + ".y", loc.getY());
        regionsConfig.set(path + ".z", loc.getZ());
    }

    /**
     * Deletes an existing region
     * @param regionKey The key of the region to delete
     */
    public void deleteRegion(String regionKey) {
        regions().remove(regionKey);
        saveRegions();
        permissionManager.invalidateAllCaches();
    }

    /**
     * Creates a new region from two Locations, a UUID for the owner and the owner's permissions
     *
     * @param name Name of the new Region
     * @param min First corner of the region
     * @param max Second corner of the region
     * @param playerUUID Region owner's UUID
     * @param ownerPermissions Owner's permissions map
     *
     */
    public void createNewRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        String regionKey;
        do {
            regionKey = UUID.randomUUID().toString().substring(0, 8);
        } while (regions().containsKey(regionKey));

        Map<UUID, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, members, regionKey);

        String finalRegionKey = regionKey;
        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this, finalRegionKey);
        });

        permissionManager.invalidateAllCaches();
        saveRegion(regionKey, newRegion);
    }

    /**
     * Creates a new region as a sub-region of the given parent region.
     * The region will be owned by the given player with the given permissions.
     *
     * @param name             The name of the new region.
     * @param min              The minimum location of the new region.
     * @param max              The maximum location of the new region.
     * @param playerUUID       The UUID of the player who will own the new region.
     * @param ownerPermissions The permissions that the player will have for the new region.
     * @param parentRegion     The parent region of the new region.
     */
    public void createSubRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions, Region parentRegion) {
        String regionKey;
        do {
            regionKey = UUID.randomUUID().toString().substring(0, 8);
        } while (regions().containsKey(regionKey));

        Map<UUID, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, members, regionKey, parentRegion.getKey());

        String finalRegionKey = regionKey;
        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this, finalRegionKey);
        });

        permissionManager.invalidateAllCaches();
        saveRegion(regionKey, newRegion);
    }

    /**
     * Creates a new region with the given name and minimum and maximum locations
     * The region will be owned by the given player
     *
     * @param name       The name of the new region.
     * @param min        The minimum location of the new region.
     * @param max        The maximum location of the new region.
     * @param playerUUID The UUID of the player who will own the new region.
     */
    public void createNewRegion(String name, Location min, Location max, UUID playerUUID) {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("role", "owner");
        createNewRegion(name, min, max, playerUUID, permissions);
    }

    public void create2DRegion(String name, Location min, Location max, UUID playerUUID) {
        min.setY(-63);
        max.setY(319);
        createNewRegion(name, min, max, playerUUID);
    }

    public void create2DRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        min.setY(-63);
        max.setY(319);
        createNewRegion(name, min, max, playerUUID, ownerPermissions);
    }

    public void addMemberPermission(UUID uuid, String permission, String value, String key) {
        permissionManager.invalidateCache(uuid);
        Region region = this.regions().get(key);
        region.addMemberPermission(uuid, permission, value, this, key);
    }

    /**
     * Checks if a region overlaps with any existing regions.
     *
     * @param region The region to check for overlaps.
     * @return True if the region overlaps with any existing regions, false otherwise.
     * @see #overlapsExistingRegion(Location, Location)
     */
    public boolean overlapsExistingRegion(Region region) {
        Map<String, Region> regions = regions();
        for (Region otherRegion : regions.values()) {
            BoundingBox otherBox = BoundingBox.of(otherRegion.getMin(), otherRegion.getMax());
            BoundingBox thisBox = BoundingBox.of(region.getMin(), region.getMax());
            if (thisBox.overlaps(otherBox)) {
                return true; // Found an overlap
            }
        }
        return false; // No overlaps found
    }

    /**
     * Checks if a region overlaps with any existing regions.
     *
     * @param min The minimum location of the region.
     * @param max The maximum location of the region.
     * @return True if the region overlaps with any existing regions, false otherwise.
     * @see #overlapsExistingRegion(Region)
     */
    public boolean overlapsExistingRegion(Location min, Location max) {
        Region region = new Region(null, min, max, null, null);
        return overlapsExistingRegion(region);
    }

    /**
     * Gets a list of regions that overlap with the given location.
     *
     * @param location The location to check for overlaps.
     * @return A list of regions that overlap with the given location.
     */
    public List<Region> getRegionsAt(Location location) {
        List<Region> foundRegions = new ArrayList<>();
        Map<String, Region> regions = regions();

        for (Region region : regions.values()) {
            BoundingBox regionBox = BoundingBox.of(region.getMin(), region.getMax());
            // Check if the location's bounding box overlaps with the region's bounding box
            if (regionBox.contains(location.toVector())) {
                foundRegions.add(region);
            }
        }

        return foundRegions;
    }

    public Map<String, String> getMemberPermissions(Player player, Region region) {
        return region.getMemberPermissions(player.getUniqueId());
    }

    public Map<String, String> getMemberPermissions(UUID uuid, Region region) {
        return region.getMemberPermissions(uuid);
    }
}
