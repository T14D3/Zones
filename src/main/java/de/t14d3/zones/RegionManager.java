package de.t14d3.zones;

import de.t14d3.zones.datasource.DataSourceManager;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Direction;
import de.t14d3.zones.utils.Utils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RegionManager {

    private final PermissionManager pm;
    private final DataSourceManager dataSourceManager;
    private final Zones plugin;
    private static RegionManager instance;

    private final Int2ObjectOpenHashMap<Region> loadedRegions = new Int2ObjectOpenHashMap<>();
    private final Map<World, Int2ObjectOpenHashMap<Region>> worldRegions = new HashMap<>();
    private final Map<World, Long2ObjectOpenHashMap<List<Region>>> chunkRegions = new HashMap<>();

    public RegionManager(Zones plugin, PermissionManager permissionManager) {
        this.pm = permissionManager;
        this.plugin = plugin;
        this.dataSourceManager = new DataSourceManager(plugin);
    }

    public DataSourceManager getDataSourceManager() {
        return dataSourceManager; // Add getter for DataSourceManager
    }

    public void saveRegions() {
        dataSourceManager.saveRegions(loadedRegions.values().stream().toList());
        CacheUtils.getInstance().invalidateInteractionCaches();
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
        loadedRegions.clear();
        worldRegions.clear();
        Bukkit.getWorlds().forEach(world -> worldRegions.put(world, new Int2ObjectOpenHashMap<>()));
        dataSourceManager.loadRegions();
    }

    public void loadRegions(World world) {
        worldRegions.computeIfAbsent(world, k -> new Int2ObjectOpenHashMap<>());
        worldRegions.get(world).clear();
        dataSourceManager.loadRegions();
    }

    /**
     * Get all currently loaded regions and their corresponding key.
     *
     * @return A map of region keys and their corresponding {@link de.t14d3.zones.Region} objects.
     */
    public Int2ObjectOpenHashMap<Region> regions() {
        return this.loadedRegions;
    }

    public Int2ObjectOpenHashMap<Region> regions(World world) {
        return this.worldRegions.get(world);
    }



    // Save a region to the YAML file
    public void saveRegion(RegionKey key, Region region) {
        String keyString = key.toString();
        dataSourceManager.saveRegion(keyString, region);
    }

    /**
     * Deletes an existing region
     *
     * @param regionKey The key of the region to delete
     */
    public void deleteRegion(RegionKey regionKey) {
        regions().remove(regionKey.getValue());
        triggerSave();
        CacheUtils.getInstance().invalidateInteractionCaches();
    }

    /**
     * Creates a new region from two Locations, a UUID for the owner and the owner's permissions
     *
     * @param name             Name of the new Region
     * @param min              First corner of the region
     * @param max              Second corner of the region
     * @param playerUUID       Region owner's UUID
     * @param ownerPermissions Owner's permissions map
     * @return The newly created region
     */
    public Region createNewRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        RegionKey key = RegionKey.generate();

        Map<String, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, members, key, 0);

        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this);
        });

        CacheUtils.getInstance().invalidateInteractionCaches();
        saveRegion(key, newRegion);
        loadedRegions.put(key.getValue(), newRegion);
        worldRegions.computeIfAbsent(newRegion.getWorld(), k -> new Int2ObjectOpenHashMap<>())
                .put(newRegion.getKey().getValue(), newRegion);
        indexRegion(newRegion); // Add the new region to the spatial index
        return newRegion;
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
    public Region createNewRegion(String name, Location min, Location max, UUID playerUUID) {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("role", "owner");
        return createNewRegion(name, min, max, playerUUID, permissions);
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
        indexRegion(region); // Add the region to the spatial index
        return region;
    }

    public Region create2DRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
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
    public Region createSubRegion(String name, BlockVector min, BlockVector max, World world, UUID playerUUID, Map<String, String> ownerPermissions, Region parentRegion) {
        RegionKey regionKey = RegionKey.generate();

        Map<String, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, world, members, regionKey, parentRegion.getKey(), 0);

        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this);
        });

        CacheUtils.getInstance().invalidateInteractionCaches();
        saveRegion(regionKey, newRegion);
        loadedRegions.put(regionKey.getValue(), newRegion);
        worldRegions.computeIfAbsent(newRegion.getWorld(), k -> new Int2ObjectOpenHashMap<>())
                .put(newRegion.getKey().getValue(), newRegion);
        indexRegion(newRegion); // Add the new region to the spatial index
        return newRegion;
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
        CacheUtils.getInstance().invalidateInteractionCache(uuid);
        CacheUtils.getInstance().invalidateCache(uuid.toString());
        Region region = regions().get(key.getValue());
        region.addMemberPermission(uuid, permission, value, this);
    }

    public void addMemberPermission(String who, String permission, String value, RegionKey key) {
        CacheUtils.getInstance().invalidateInteractionCache(who);
        CacheUtils.getInstance().invalidateCache(who);
        Region region = regions().get(key.getValue());
        region.addMemberPermission(who, permission, value, this);
    }

    public boolean overlapsExistingRegion(BlockVector min, BlockVector max, World world) {
        return overlapsExistingRegion(min, max, world, null);
    }

    public boolean overlapsExistingRegion(BlockVector min, BlockVector max, World world, @Nullable RegionKey keyToIgnore) {
        for (Region region : worldRegions.get(world).values()) {
            if (region.intersects(min, max) && !region.getKey().equals(keyToIgnore)) {
                return true;
            }
        }
        return false;
    }

    public boolean overlapsExistingRegion(Location min, Location max) {
        return overlapsExistingRegion(min.toVector().toBlockVector(), max.toVector().toBlockVector(), min.getWorld());
    }

    /**
     * Gets a list of regions that overlap with the given location.
     *
     * @param location The location to check for overlaps.
     * @return A list of regions that overlap with the given location.
     */
    public List<Region> getRegionsAt(@NotNull Location location) {
        List<Region> foundRegions = new ArrayList<>();
        World world = location.getWorld();
        if (world == null) return foundRegions;

        BlockVector loc = location.toVector().toBlockVector();
        int xChunk = loc.getBlockX() >> 4;
        int zChunk = loc.getBlockZ() >> 4;
        long key = ((long) xChunk << 32) | (zChunk & 0xFFFFFFFFL);

        List<Region> candidates = chunkRegions.getOrDefault(world, new Long2ObjectOpenHashMap<>())
                .getOrDefault(key, Collections.emptyList());
        for (Region region : candidates) {
            if (region.contains(loc)) foundRegions.add(region);
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

    private void indexRegion(Region region) {
        World world = region.getWorld();
        Long2ObjectOpenHashMap<List<Region>> worldChunks = chunkRegions.computeIfAbsent(world,
                k -> new Long2ObjectOpenHashMap<>());

        BlockVector min = region.getMin();
        BlockVector max = region.getMax();

        int minXChunk = min.getBlockX() >> 4;
        int minZChunk = min.getBlockZ() >> 4;
        int maxXChunk = max.getBlockX() >> 4;
        int maxZChunk = max.getBlockZ() >> 4;

        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                worldChunks.computeIfAbsent(key, k -> new ArrayList<>()).add(region);
            }
        }
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
    public void redefineBounds(Region region, BlockVector min, BlockVector max) {
        BlockVector oldMin = region.getMin();
        BlockVector oldMax = region.getMax();
        region.setMin(min);
        region.setMax(max);
        updateRegionInSpatialIndex(region, oldMin, oldMax);
        triggerSave();
    }

    public void updateRegionInSpatialIndex(Region region, BlockVector oldMin, BlockVector oldMax) {
        World world = region.getWorld();
        Map<Long, List<Region>> worldChunks = chunkRegions.get(world);
        if (worldChunks == null) return;

        // Remove from old chunks
        removeRegionFromChunks(region, oldMin, oldMax, worldChunks);

        // Add to new chunks
        addRegionToChunks(region, region.getMin(), region.getMax(), worldChunks);
    }

    private void removeRegionFromChunks(Region region, BlockVector min, BlockVector max, Map<Long, List<Region>> worldChunks) {
        int minXChunk = min.getBlockX() >> 4;
        int minZChunk = min.getBlockZ() >> 4;
        int maxXChunk = max.getBlockX() >> 4;
        int maxZChunk = max.getBlockZ() >> 4;

        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                List<Region> regions = worldChunks.get(key);
                if (regions != null) {
                    regions.remove(region);
                    if (regions.isEmpty()) worldChunks.remove(key);
                }
            }
        }
    }

    private void addRegionToChunks(Region region, BlockVector min, BlockVector max, Map<Long, List<Region>> worldChunks) {
        int minXChunk = min.getBlockX() >> 4;
        int minZChunk = min.getBlockZ() >> 4;
        int maxXChunk = max.getBlockX() >> 4;
        int maxZChunk = max.getBlockZ() >> 4;

        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                worldChunks.computeIfAbsent(key, k -> new ArrayList<>()).add(region);
            }
        }
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
        if (overlapsExistingRegion(newRegion.getMin().toBlockVector(), newRegion.getMax().toBlockVector(),
                region.getWorld(), region.getKey())) {
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
        loadedRegions.put(region.getKey().getValue(), region);
        worldRegions.computeIfAbsent(region.getWorld(), k -> new Int2ObjectOpenHashMap<>())
                .put(region.getKey().getValue(), region);
        indexRegion(region); // Ensure the region is indexed
    }

    public static Region getRegion(RegionKey key) {
        if (instance == null) {
            throw new IllegalStateException("RegionManager is not yet initialized!");
        }
        return instance.loadedRegions.get(key.getValue());
    }
}
