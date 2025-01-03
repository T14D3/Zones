package de.t14d3.zones;

import de.t14d3.zones.utils.Direction;
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
    public Map<String, Region> loadedRegions = new HashMap<>();

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
     */
    public void loadRegions() {
        if (regionsConfig.contains("regions")) {
            for (String key : Objects.requireNonNull(regionsConfig.getConfigurationSection("regions")).getKeys(false)) {
                String name = regionsConfig.getString("regions." + key + ".name");
                Location min = loadLocation("regions." + key + ".min");
                Location max = loadLocation("regions." + key + ".max");
                String parent = regionsConfig.getString("regions." + key + ".parent");

                Map<UUID, Map<String, String>> members = loadMembers(key);
                Region region = new Region(name != null ? name : "Invalid Name", min, max, members, key, parent);
                loadedRegions.put(key, region);
            }
        }
    }

    /**
     * Get all currently loaded regions and their corresponding key.
     *
     * @return A map of region keys and their corresponding {@link de.t14d3.zones.Region} objects.
     */
    public Map<String, Region> regions() {
        return this.loadedRegions;
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
            newRegion.addMemberPermission(playerUUID, permission, value, this);
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
            newRegion.addMemberPermission(playerUUID, permission, value, this);
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

    /**
     * Adds a permission to a player's membership in a region.
     *
     * @param uuid       The UUID of the player.
     * @param permission The permission to add.
     * @param value      The value of the permission.
     * @param key        The key of the region.
     */
    public void addMemberPermission(UUID uuid, String permission, String value, String key) {
        permissionManager.invalidateCache(uuid);
        Region region = this.regions().get(key);
        region.addMemberPermission(uuid, permission, value, this);
    }

    /**
     * Checks if a region overlaps with any existing regions.
     *
     * @param region The region to check for overlaps.
     * @return True if the region overlaps with any existing regions, false otherwise.
     * @see #overlapsExistingRegion(Location, Location)
     */
    public boolean overlapsExistingRegion(Region region) {
        BoundingBox box = BoundingBox.of(region.getMin(), region.getMax());
        return overlapsExistingRegion(box); // No overlaps found
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
        BoundingBox thisBox = BoundingBox.of(min, max);
        return overlapsExistingRegion(thisBox);
    }

    /**
     * Checks if the given bounding box overlaps with any existing regions.
     *
     * @param thisBox The bounding box to check for overlaps.
     * @return True if the bounding box overlaps with any existing regions, false otherwise.
     * @see #overlapsExistingRegion(Region)
     */
    public boolean overlapsExistingRegion(BoundingBox thisBox) {
        Map<String, Region> regions = regions();
        for (Region otherRegion : regions.values()) {
            BoundingBox otherBox = BoundingBox.of(otherRegion.getMin(), otherRegion.getMax());
            if (thisBox.overlaps(otherBox)) {
                return true; // Found an overlap
            }
        }
        return false; // No overlaps found
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

    /**
     * Redefines the bounds of a region.
     * Does not have any overlap checks.
     *
     * @param region The region to redefine.
     * @param min    The new minimum location of the region.
     * @param max    The new maximum location of the region.
     */
    public void redefineBounds(Region region, Location min, Location max) {
        region.setMin(min, this);
        region.setMax(max, this);
    }


    /**
     * Expands the bounds of a region in a given direction by a given amount.
     * Only succeeds if the region does not overlap with any other regions
     * or the <code>allowOverlap</code> parameter is set to true.
     *
     * @param region       The region to expand.
     * @param direction    The direction to expand in.
     * @param amount       The amount to expand by.
     * @param allowOverlap Whether to allow overlaps or not.
     * @return Whether the expansion was successful.
     * @see #expandBounds(Region, Direction, int)
     */
    public boolean expandBounds(Region region, Direction direction, int amount, boolean allowOverlap) {
        if (allowOverlap) {
            expandBounds(region, direction, amount);
            return true;
        }
        BoundingBox newRegion = BoundingBox.of(region.getMin(), region.getMax());
        newRegion.expand(direction.toBlockFace(), amount);
        if (overlapsExistingRegion(newRegion)) {
            return false;
        }
        expandBounds(region, direction, amount);
        return true;
    }

    /**
     * Expands the bounds of a region in a given direction by a given amount.
     * Does not have any overlap checks.
     *
     * @param region    The region to expand.
     * @param direction The direction to expand in.
     * @param amount    The amount to expand by.
     * @see #expandBounds(Region, Direction, int, boolean)
     */
    public void expandBounds(Region region, Direction direction, int amount) {
        BoundingBox newRegion = BoundingBox.of(region.getMin(), region.getMax());
        newRegion.expand(direction.toBlockFace(), amount);
        region.setMin(newRegion.getMin().toBlockVector());
        region.setMax(newRegion.getMax().toBlockVector());
        saveRegions();
    }

    public Map<String, String> getMemberPermissions(Player player, Region region) {
        return region.getMemberPermissions(player.getUniqueId());
    }

    public Map<String, String> getMemberPermissions(UUID uuid, Region region) {
        return region.getMemberPermissions(uuid);
    }
}
