package de.t14d3.zones;

import de.t14d3.zones.datasource.DataSourceManager;
import de.t14d3.zones.objects.*;
import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Utils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RegionManager {

    private final DataSourceManager dataSourceManager;
    private final Zones plugin;
    private static RegionManager instance;
    private final ZonesPlatform platform;

    private final Int2ObjectOpenHashMap<Region> loadedRegions = new Int2ObjectOpenHashMap<>();
    private final Map<UUID, Int2ObjectOpenHashMap<Region>> worldRegions = new HashMap<>();
    private final Map<UUID, Long2ObjectOpenHashMap<List<Region>>> chunkRegions = new HashMap<>();

    public RegionManager(Zones plugin, PermissionManager permissionManager) {
        this.plugin = plugin;
        this.dataSourceManager = new DataSourceManager(plugin);
        this.platform = plugin.getPlatform();
        instance = this;
    }

    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
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
        platform.getWorlds().forEach(world -> worldRegions.put(world.getUID(), new Int2ObjectOpenHashMap<>()));
        dataSourceManager.loadRegions();
    }

    public void loadRegions(World world) {
        worldRegions.computeIfAbsent(world.getUID(), k -> new Int2ObjectOpenHashMap<>());
        worldRegions.get(world.getUID()).clear();
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
        return this.worldRegions.get(world.getUID());
    }

    // Save a region to the configured data source
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
     * Creates a new region with the specified key, name, minimum and maximum locations, members and their permissions.
     *
     * @param key     The key of the new region.
     * @param name    The name of the new region.
     * @param min     The minimum location of the new region.
     * @param max     The maximum location of the new region.
     * @param members The members of the new region.
     * @return The newly created region.
     */
    public Region createNewRegion(String name, BlockLocation min, BlockLocation max, World world, Map<String, List<RegionFlagEntry>> members, RegionKey key, RegionKey parent, int priority) {
        Region newRegion = new Region(name, min, max, world, members, key, 0);

        CacheUtils.getInstance().invalidateInteractionCaches();
        saveRegion(key, newRegion);
        loadedRegions.put(key.getValue(), newRegion);
        worldRegions.computeIfAbsent(newRegion.getWorld().getUID(), k -> new Int2ObjectOpenHashMap<>())
                .put(newRegion.getKey().getValue(), newRegion);
        indexRegion(newRegion);
        return newRegion;
    }

    public Region createNewRegion(RegionKey key, String name, BlockLocation min, BlockLocation max, World world, Map<String, List<RegionFlagEntry>> members, int priority) {
        return createNewRegion(name, min, max, world, members, key, null, priority);
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
    public Region createSubRegion(String name, BlockLocation min, BlockLocation max, World world, UUID playerUUID, List<RegionFlagEntry> ownerPermissions, Region parentRegion) {
        RegionKey regionKey = RegionKey.generate();

        Map<String, List<RegionFlagEntry>> members = new HashMap<>();
        members.put(playerUUID.toString(), ownerPermissions);
        Region newRegion = new Region(name, min, max, world, members, regionKey, parentRegion.getKey(), 0);

        CacheUtils.getInstance().invalidateInteractionCaches();
        saveRegion(regionKey, newRegion);
        loadedRegions.put(regionKey.getValue(), newRegion);
        worldRegions.computeIfAbsent(newRegion.getWorld().getUID(), k -> new Int2ObjectOpenHashMap<>())
                .put(newRegion.getKey().getValue(), newRegion);
        indexRegion(newRegion);
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

    public void addMemberPermissions(String who, String permission, List<String> values, RegionKey key) {
        CacheUtils.getInstance().invalidateInteractionCache(who);
        CacheUtils.getInstance().invalidateCache(who);
        Region region = regions().get(key.getValue());
        for (String value : values) {
            region.addMemberPermission(who, permission, value, this);
        }
    }

    public boolean overlapsExistingRegion(BlockLocation min, BlockLocation max, World world) {
        return overlapsExistingRegion(min, max, world, null);
    }

    public boolean overlapsExistingRegion(Box box) {
        return overlapsExistingRegion(box.getMin(), box.getMax(), box.getWorld(), null);
    }

    public boolean overlapsExistingRegion(BlockLocation min, BlockLocation max, World world, @Nullable RegionKey keyToIgnore) {
        for (Region region : worldRegions.get(world.getUID()).values()) {
            if (region.intersects(min, max, world) && !region.getKey().equals(keyToIgnore)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a list of regions that overlap with the given location.
     *
     * @param location The location to check for overlaps.
     * @return A list of regions that overlap with the given location.
     */
    public List<Region> getRegionsAt(BlockLocation location, World world) {
        List<Region> foundRegions = new ArrayList<>();
        if (world == null) return foundRegions;

        int xChunk = location.getX() >> 4;
        int zChunk = location.getZ() >> 4;
        long key = ((long) xChunk << 32) | (zChunk & 0xFFFFFFFFL);

        List<Region> candidates = chunkRegions.getOrDefault(world.getUID(), new Long2ObjectOpenHashMap<>())
                .getOrDefault(key, Collections.emptyList());
        for (Region region : candidates) {
            if (region.contains(location)) foundRegions.add(region);
        }
        return foundRegions;
    }

    /**
     * Gets the region with the highest priority at the given location
     *
     * @param location Location to check
     * @return Region at location, or null if no region found
     */
    public @Nullable Region getEffectiveRegionAt(BlockLocation location, World world) {
        List<Region> regions = getRegionsAt(location, world);
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
        Long2ObjectOpenHashMap<List<Region>> worldChunks = chunkRegions.computeIfAbsent(world.getUID(),
                k -> new Long2ObjectOpenHashMap<>());

        BlockLocation min = region.getMin();
        BlockLocation max = region.getMax();

        int minXChunk = min.getX() >> 4;
        int minZChunk = min.getZ() >> 4;
        int maxXChunk = max.getX() >> 4;
        int maxZChunk = max.getZ() >> 4;

        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                worldChunks.computeIfAbsent(key, k -> new ArrayList<>()).add(region);
            }
        }
    }

    /**
     * Redefines the bounds of a region.
     * Does not have any overlap checks.
     *
     * @param region The region to redefine.
     * @param min    The new minimum location of the region.
     * @param max    The new maximum location of the region.
     */
    public void redefineBounds(Region region, BlockLocation min, BlockLocation max) {
        BlockLocation oldMin = region.getMin();
        BlockLocation oldMax = region.getMax();
        region.setMin(min);
        region.setMax(max);
        updateRegionInSpatialIndex(region, oldMin, oldMax);
        triggerSave();
    }

    public void updateRegionInSpatialIndex(Region region, BlockLocation oldMin, BlockLocation oldMax) {
        World world = region.getWorld();
        Map<Long, List<Region>> worldChunks = chunkRegions.get(world.getUID());
        if (worldChunks == null) return;

        // Remove from old chunks
        removeRegionFromChunks(region, oldMin, oldMax, worldChunks);

        // Add to new chunks
        addRegionToChunks(region, region.getMin(), region.getMax(), worldChunks);
    }

    private static void removeRegionFromChunks(Region region, BlockLocation min, BlockLocation max, Map<Long, List<Region>> worldChunks) {
        int minXChunk = min.getX() >> 4;
        int minZChunk = min.getZ() >> 4;
        int maxXChunk = max.getX() >> 4;
        int maxZChunk = max.getZ() >> 4;

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

    private static void addRegionToChunks(Region region, BlockLocation min, BlockLocation max, Map<Long, List<Region>> worldChunks) {
        int minXChunk = min.getX() >> 4;
        int minZChunk = min.getZ() >> 4;
        int maxXChunk = max.getX() >> 4;
        int maxZChunk = max.getZ() >> 4;

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
        BlockLocation newMin = region.getMin().clone();
        BlockLocation newMax = region.getMax().clone();

        switch (direction) {
            case NORTH:
                newMin.setZ(newMin.getZ() - amount);
                break;
            case SOUTH:
                newMax.setZ(newMax.getZ() + amount);
                break;
            case EAST:
                newMax.setX(newMax.getX() + amount);
                break;
            case WEST:
                newMin.setX(newMin.getX() - amount);
                break;
            case UP:
                newMax.setY(newMax.getY() + amount);
                break;
            case DOWN:
                newMin.setY(newMin.getY() - amount);
                break;
        }

        if (allowOverlap) {
            region.setMin(newMin);
            region.setMax(newMax);
            triggerSave();
            return true;
        }

        if (overlapsExistingRegion(newMin, newMax, region.getWorld(), region.getKey())) {
            return false;
        }

        region.setMin(newMin);
        region.setMax(newMax);
        triggerSave();
        return true;
    }

    public void expandBounds(Region region, Direction direction, int amount) {
        BlockLocation newMin = region.getMin().clone();
        BlockLocation newMax = region.getMax().clone();

        switch (direction) {
            case NORTH:
                newMin.setZ(newMin.getZ() - amount);
                break;
            case SOUTH:
                newMax.setZ(newMax.getZ() + amount);
                break;
            case EAST:
                newMax.setX(newMax.getX() + amount);
                break;
            case WEST:
                newMin.setX(newMin.getX() - amount);
                break;
            case UP:
                newMax.setY(newMax.getY() + amount);
                break;
            case DOWN:
                newMin.setY(newMin.getY() - amount);
                break;
        }

        region.setMin(newMin);
        region.setMax(newMax);
        triggerSave();
    }

    /**
     * Adds a region to the loaded regions map.
     * Requires an existing region object
     *
     * @param region The region to add.
     * @see #createNewRegion
     */
    public void addRegion(Region region) {
        loadedRegions.put(region.getKey().getValue(), region);
        worldRegions.computeIfAbsent(region.getWorld().getUID(), k -> new Int2ObjectOpenHashMap<>())
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
