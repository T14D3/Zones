package de.t14d3.zones;

import de.t14d3.zones.utils.Direction;
import de.t14d3.zones.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RegionManager {

    private final PermissionManager pm;
    private final File regionsFile;
    private final FileConfiguration regionsConfig;
    private final Zones plugin;
    Map<Location, List<String>> regionCache = new HashMap<>();
    private final Map<RegionKey, Region> loadedRegions = new HashMap<>();

    private final Map<ChunkKey, Set<RegionKey>> chunkIndex = new HashMap<>();

    public RegionManager(Zones plugin, PermissionManager permissionManager) {
        this.pm = permissionManager;
        this.plugin = plugin;

        regionsFile = new File(plugin.getDataFolder(), "regions.yml");

        if (!regionsFile.exists()) {
            regionsFile.getParentFile().mkdirs();
            plugin.saveResource("regions.yml", false);
        }

        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    public void saveRegions() {
        regionsConfig.set("regions", null);
        for (Map.Entry<RegionKey, Region> entry : loadedRegions.entrySet()) {
            RegionKey key = entry.getKey();
            Region region = entry.getValue();
            regionsConfig.set("regions." + key + ".name", region.getName());
            regionsConfig.set("regions." + key + ".priority", region.getPriority());
            saveLocation("regions." + key + ".min", region.getMin());
            saveLocation("regions." + key + ".max", region.getMax());
            if (region.getParent() != null) {
                regionsConfig.set("regions." + key + ".parent", region.getParent());
            }
            saveMembers(key, region.getMembers());
        }
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save regions.yml");
        }
        pm.invalidateInteractionCaches();
        regionCache.clear();
    }

    /**
     * Triggers saving the regions.
     * Respects the saving mode.
     *
     * @see #saveRegions() #saveRegions() to force-save
     */
    public void triggerSave() {
        if (plugin.getSavingMode() == Utils.SavingModes.MODIFIED) {
            saveRegions();
        }
    }

    /**
     * Loads regions from the YAML file into memory.
     */
    public void loadRegions() {
        if (regionsConfig.contains("regions")) {
            loadedRegions.clear();
            for (String key : Objects.requireNonNull(regionsConfig.getConfigurationSection("regions")).getKeys(false)) {
                String name = regionsConfig.getString("regions." + key + ".name");
                int priority = regionsConfig.getInt("regions." + key + ".priority", 0);
                Location min = loadLocation("regions." + key + ".min");
                Location max = loadLocation("regions." + key + ".max");
                RegionKey parent = RegionKey.fromString(regionsConfig.getString("regions." + key + ".parent"));

                Map<String, Map<String, String>> members = loadMembers(
                        regionsConfig.getConfigurationSection("regions." + key + ".members"));
                RegionKey regionKey = RegionKey.fromString(key);
                Region region = new Region(name != null ? name : "Invalid Name", min, max, members, regionKey, parent,
                        priority);
                loadedRegions.put(regionKey, region);
            }
            regionCache.clear();
            pm.invalidateInteractionCaches();
        }
    }

    /**
     * Get all currently loaded regions and their corresponding key.
     *
     * @return A map of region keys and their corresponding {@link de.t14d3.zones.Region} objects.
     */
    public Map<RegionKey, Region> regions() {
        return this.loadedRegions;
    }

    private Map<String, Map<String, String>> loadMembers(@Nullable ConfigurationSection section) {
        Map<String, Map<String, String>> members = new HashMap<>();
        if (section == null) return members;
        for (String uuidStr : section.getKeys(false)) {
            Map<String, String> permissions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String perm : section.getConfigurationSection(uuidStr).getKeys(false)) {
                permissions.put(perm, section.getString(uuidStr + "." + perm));
            }
            members.put(uuidStr, permissions);
        }
        return members;
    }

    // Save a region to the YAML file
    public void saveRegion(RegionKey key, Region region) {
        regionsConfig.set("regions." + key + ".name", region.getName());
        regionsConfig.set("regions." + key + ".priority", region.getPriority());
        saveLocation("regions." + key + ".min", region.getMin());
        saveLocation("regions." + key + ".max", region.getMax());
        if (region.getParent() != null) {
            regionsConfig.set("regions." + key + ".parent", region.getParent());
        }

        saveMembers(key, region.getMembers());
        triggerSave();
    }

    private void saveMembers(RegionKey key, Map<String, Map<String, String>> members) {
        for (Map.Entry<String, Map<String, String>> entry : members.entrySet()) {
            String who = entry.getKey();
            for (Map.Entry<String, String> perm : entry.getValue().entrySet()) {
                regionsConfig.set("regions." + key + ".members." + who + "." + perm.getKey(), perm.getValue());
            }
        }
    }

    // Load a location from the YAML file
    private Location loadLocation(String path) {
        String world = regionsConfig.getString(path + ".world");
        double x = regionsConfig.getDouble(path + ".x");
        double y = regionsConfig.getDouble(path + ".y");
        double z = regionsConfig.getDouble(path + ".z");
        assert world != null;
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
     *
     * @param regionKey The key of the region to delete
     */
    public void deleteRegion(RegionKey regionKey) {
        regions().remove(regionKey);
        triggerSave();
        pm.invalidateInteractionCaches();
    }

    /**
     * Creates a new region from two Locations, a UUID for the owner and the owner's permissions
     *
     * @param name             Name of the new Region
     * @param min              First corner of the region
     * @param max              Second corner of the region
     * @param playerUUID       Region owner's UUID
     * @param ownerPermissions Owner's permissions map
     * @return The key of the newly created region
     */
    public RegionKey createNewRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        RegionKey key;
        do {
            key = RegionKey.fromString(UUID.randomUUID().toString().substring(0, 8));
        } while (regions().containsKey(key));

        Map<String, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, members, key, 0);

        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this);
        });

        pm.invalidateInteractionCaches();
        saveRegion(key, newRegion);
        loadedRegions.put(key, newRegion);
        return key;
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

    /**
     * Creates a new region with the specified key, name, minimum and maximum locations, members and their permissions.
     *
     * @param key     The key of the new region.
     * @param name    The name of the new region.
     * @param min     The minimum location of the new region.
     * @param max     The maximum location of the new region.
     * @param members The members of the new region.
     * @return The newly created region.
     */
    public Region createNewRegion(RegionKey key, String name, Location min, Location max, Map<String, Map<String, String>> members, int priority) {
        Region region = new Region(name, min, max, members, key, priority);
        saveRegion(key, region);
        return region;
    }

    public void create2DRegion(String name, Location min, Location max, UUID playerUUID) {
        min.setY(-63);
        max.setY(319);
        createNewRegion(name, min, max, playerUUID);
    }

    public RegionKey create2DRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        min.setY(-63);
        max.setY(319);
        return createNewRegion(name, min, max, playerUUID, ownerPermissions);
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
        RegionKey regionKey;
        do {
            regionKey = RegionKey.fromString(UUID.randomUUID().toString().substring(0, 8));
        } while (regions().containsKey(regionKey));

        Map<String, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, members, regionKey, parentRegion.getKey(), 0);

        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this);
        });

        pm.invalidateInteractionCaches();
        regionCache.clear();
        saveRegion(regionKey, newRegion);
        loadedRegions.put(regionKey, newRegion);
    }

    /**
     * Adds a permission to a player's membership in a region.
     *
     * @param uuid       The UUID of the player.
     * @param permission The permission to add.
     * @param value      The value of the permission.
     * @param key        The key of the region.
     */
    public void addMemberPermission(UUID uuid, String permission, String value, RegionKey key) {
        pm.invalidateInteractionCache(uuid);
        pm.invalidateCache(uuid.toString());
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
     * @see #overlapsExistingRegion(BoundingBox, RegionKey)
     */
    public boolean overlapsExistingRegion(BoundingBox thisBox) {
        for (Region otherRegion : regions().values()) {
            BoundingBox otherBox = BoundingBox.of(otherRegion.getMin(), otherRegion.getMax());
            if (thisBox.overlaps(otherBox)) {
                return true; // Found an overlap
            }
        }
        return false; // No overlaps found
    }

    /**
     * Checks if the given bounding box overlaps with any existing regions.
     * Does not check neither the given region nor child regions.
     *
     * @param thisBox     The bounding box to check for overlaps.
     * @param keyToIgnore The key of the region to ignore.
     * @return True if the bounding box overlaps with any existing regions, false otherwise.
     * @see #overlapsExistingRegion(BoundingBox)
     */
    public boolean overlapsExistingRegion(BoundingBox thisBox, RegionKey keyToIgnore) {
        for (Region otherRegion : regions().values()) {
            if (otherRegion.getKey().equals(keyToIgnore) || otherRegion.getParent() != null) {
                continue;
            }
            BoundingBox otherBox = BoundingBox.of(otherRegion.getMin(), otherRegion.getMax());
            if (thisBox.overlaps(otherBox)) {
                return true; // Found an overlap
            }
        }
        return false; // No overlaps found
    }

    public CompletableFuture<Boolean> overlapsExistingRegionAsync(Region region) {
        return CompletableFuture.supplyAsync(() -> overlapsExistingRegion(region));
    }

    public CompletableFuture<Boolean> overlapsExistingRegionAsync(Location min, Location max) {
        return CompletableFuture.supplyAsync(() -> overlapsExistingRegion(min, max));
    }

    public CompletableFuture<Boolean> overlapsExistingRegionAsync(BoundingBox thisBox) {
        return CompletableFuture.supplyAsync(() -> overlapsExistingRegion(thisBox));
    }

    public CompletableFuture<Boolean> overlapsExistingRegionAsync(BoundingBox thisBox, RegionKey keyToIgnore) {
        return CompletableFuture.supplyAsync(() -> overlapsExistingRegion(thisBox, keyToIgnore));
    }


    private void updateRegionChunks(Region region) {
        // Remove region from all previously tracked chunks
        for (ChunkKey chunkKey : region.getOverlappingChunks()) {
            Set<RegionKey> regionsInChunk = chunkIndex.get(chunkKey);
            if (regionsInChunk != null) {
                regionsInChunk.remove(region.getKey());
                if (regionsInChunk.isEmpty()) {
                    chunkIndex.remove(chunkKey);
                }
            }
        }
        region.getOverlappingChunks().clear();

        World world = region.getMin().getWorld();
        BoundingBox regionBox = BoundingBox.of(region.getMin(), region.getMax());

        // Calculate chunk range the region spans
        int minChunkX = (int) Math.floor(regionBox.getMinX() / 16);
        int maxChunkX = (int) Math.floor(regionBox.getMaxX() / 16);
        int minChunkZ = (int) Math.floor(regionBox.getMinZ() / 16);
        int maxChunkZ = (int) Math.floor(regionBox.getMaxZ() / 16);

        // Add region to all overlapping chunks
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                ChunkKey chunkKey = new ChunkKey(x, z, world.getUID());
                region.getOverlappingChunks().add(chunkKey);
                chunkIndex.computeIfAbsent(chunkKey, k -> new HashSet<>()).add(region.getKey());
            }
        }
    }

    /**
     * Gets a list of regions that overlap with the given location.
     *
     * @param location The location to check for overlaps.
     * @return A list of regions that overlap with the given location.
     */
    public List<Region> getRegionsAt(Location location) {
        List<Region> foundRegions = new ArrayList<>();

        // Calculate chunk key for the location
        World world = location.getWorld();
        if (world == null) return foundRegions;

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;

        ChunkKey chunkKey = new ChunkKey(chunkX, chunkZ, world.getUID());

        // Get regions in this chunk
        Set<RegionKey> regionKeys = chunkIndex.get(chunkKey);
        if (regionKeys == null) return foundRegions;

        // Check each region in chunk
        for (RegionKey regionKey : regionKeys) {
            Region region = loadedRegions.get(regionKey);
            if (region != null && region.contains(location)) {
                foundRegions.add(region);
            }
        }

        return foundRegions;
    }

    /**
     * Gets the region with the highest priority at the given location
     *
     * @param location Location to check
     * @return Region at location, or null if no region found
     */
    public @Nullable Region getEffectiveRegionAt(Location location) {
        List<Region> regions = getRegionsAt(location);
        int priority = Integer.MIN_VALUE;
        Region effectiveRegion = null;
        for (Region region : regions) {
            if (region.getPriority() > priority) {
                effectiveRegion = region;
                priority = region.getPriority();
            }
        }
        return effectiveRegion;
    }

    /**
     * Gets Regions at location async
     *
     * @param location Location to check
     * @return List of regions at location
     */
    public CompletableFuture<List<Region>> getRegionsAtAsync(Location location) {
        return CompletableFuture.supplyAsync(() -> getRegionsAt(location));
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
     * or the {@code allowOverlap} parameter is set to true.
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
        if (overlapsExistingRegionAsync(newRegion, region.getKey()).join()) {
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
        triggerSave();
    }

    public Map<String, String> getMemberPermissions(Player player, Region region) {
        return region.getMemberPermissions(player.getUniqueId().toString());
    }

    public Map<String, String> getMemberPermissions(UUID uuid, Region region) {
        return region.getMemberPermissions(uuid.toString());
    }

    /**
     * Adds a region to the loaded regions map.
     * Requires an existing region object, to create a new region use {@link #createNewRegion(RegionKey, String, Location, Location, Map, int)}.
     *
     * @param region The region to add.
     * @see #createNewRegion
     */
    public void addRegion(Region region) {
        loadedRegions.put(region.getKey(), region);
    }
}
