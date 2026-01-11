package de.t14d3.zones;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.datasource.DataSourceManager;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Direction;
import de.t14d3.zones.objects.shapes.CuboidShape;
import de.t14d3.zones.objects.shapes.ShapeProvider;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.utils.Utils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Loads, stores, and queries regions.
 *
 * <p>Regions are kept in memory and additionally indexed per-world for efficient spatial queries.
 * Callers should use the provided {@code withWorldReadLock}/{@code withWorldWriteLock} helpers when
 * interacting with world-backed state.</p>
 */
public class RegionManager {

    private final DataSourceManager dataSourceManager;
    private final Zones plugin;
    // Global facade; per-world managers are stored separately.

    private final ConcurrentHashMap<Integer, Region> loadedRegions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WorldRegionManager> worldManagers = new ConcurrentHashMap<>();

    public RegionManager(Zones plugin) {
        this.plugin = plugin;
        this.dataSourceManager = new DataSourceManager(plugin);
    }

    /**
     * Returns the data source manager responsible for loading/saving regions.
     */
    public DataSourceManager getDataSourceManager() {
        return dataSourceManager;
    }

    /**
     * Forces saving all currently loaded regions.
     */
    public void saveRegions() {
        withAllWorldReadLocks(() -> {
            dataSourceManager.saveRegions(new ArrayList<>(loadedRegions.values()));
            return null;
        });
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
     * Reloads all regions from the configured data source into memory.
     */
    public void loadRegions() {
        clearAll();
        List<Region> regions = dataSourceManager.loadRegions();
        if (regions != null) {
            for (Region region : regions) {
                addRegion(region);
            }
        }
        plugin.getPermissionManager().invalidateAll();
    }

    /**
     * Get all currently loaded regions and their corresponding key.
     *
     * @return A map of region keys and their corresponding {@link de.t14d3.zones.Region} objects.
     */
    public Map<Integer, Region> regions() {
        return Collections.unmodifiableMap(loadedRegions);
    }

    public @Nullable Region getRegion(@Nullable RegionKey key) {
        if (key == null) return null;
        return loadedRegions.get(key.getValue());
    }

    public void clearAll() {
        List<WorldRegionManager> managers = new ArrayList<>(worldManagers.values());
        for (WorldRegionManager manager : managers) {
            manager.withWriteLock(manager::clearLocked);
        }
        loadedRegions.clear();
        worldManagers.clear();
    }

    private <T> T withAllWorldReadLocks(Supplier<T> action) {
        List<String> worldIds = new ArrayList<>(worldManagers.keySet());
        worldIds.sort(String::compareTo);
        return withAllWorldReadLocks(worldIds, 0, action);
    }

    private <T> T withAllWorldReadLocks(List<String> worldIds, int index, Supplier<T> action) {
        if (index >= worldIds.size()) return action.get();
        WorldRegionManager manager = worldManagers.get(worldIds.get(index));
        if (manager == null) return withAllWorldReadLocks(worldIds, index + 1, action);
        return manager.withReadLock(() -> withAllWorldReadLocks(worldIds, index + 1, action));
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
        if (regionKey == null) return;
        Region region = loadedRegions.get(regionKey.getValue());
        if (region == null) return;

        RWorldRef world = region.getWorld();
        if (world != null) {
            WorldRegionManager manager = getOrCreateWorldManager(world);
            manager.withWriteLock(() -> {
                loadedRegions.remove(regionKey.getValue());
                manager.removeRegionLocked(region);
            });
        } else {
            loadedRegions.remove(regionKey.getValue());
        }

        triggerSave();
        plugin.getPermissionManager().invalidateAll();
    }

    /**
     * Creates a new region with the specified shape.
     *
     * @param key     The key of the new region.
     * @param name    The name of the new region.
     * @param shape   The shape of the new region.
     * @param permissions The permissions of the new region.
     * @param parent  The parent region.
     * @param priority The priority of the region.
     * @return The newly created region.
     */
    public Region createNewRegion(String name, ShapeProvider shape, RegionPermissions permissions, RegionKey key, RegionKey parent, int priority) {
        Region newRegion = new Region(name, shape, permissions, new RegionMembership(), key, parent, priority);

        plugin.getPermissionManager().invalidateAll();
        saveRegion(key, newRegion);
        addRegion(newRegion);
        return newRegion;
    }

    public Region createNewRegion(RegionKey key, String name, ShapeProvider shape, RegionPermissions permissions, int priority) {
        return createNewRegion(name, shape, permissions, key, null, priority);
    }

    public Region createNewRegion(String name, RBlockPos min, RBlockPos max, RWorldRef world, RegionPermissions permissions, RegionKey key, RegionKey parent, int priority) {
        CuboidShape shape = new CuboidShape(min, max, world);
        return createNewRegion(name, shape, permissions, key, parent, priority);
    }

    public Region createNewRegion(RegionKey key, String name, RBlockPos min, RBlockPos max, RWorldRef world, RegionPermissions permissions, int priority) {
        return createNewRegion(name, min, max, world, permissions, key, null, priority);
    }

    /**
     * Creates a new region as a sub-region of the given parent region.
     * The region will be owned by the given player with the given permissions.
     *
     * @param name             The name of the new region.
     * @param min              The minimum location of the new region.
     * @param max              The maximum location of the new region.
     * @param playerUUID       The UUID of the player who will own the new region.
     * @param parentRegion     The parent region of the new region.
     */
    public @Nullable Region createSubRegion(String name, RBlockPos min, RBlockPos max, RWorldRef world, UUID playerUUID, Region parentRegion) {
        if (parentRegion == null || min == null || max == null || world == null) return null;
        if (!sameWorld(world, parentRegion.getWorld())) return null;

        WorldRegionManager manager = getOrCreateWorldManager(world);
        boolean insideParent = manager.withReadLock(() -> parentRegion.contains(min) && parentRegion.contains(max));
        if (!insideParent) return null;

        boolean overlaps = manager.withReadLock(() ->
                manager.overlapsExistingRegionLocked(min, max, world, other -> isLineageRelated(parentRegion, other))
        );
        if (overlaps) return null;

        RegionKey regionKey = RegionKey.generate();

        RegionPermissions permissions = new RegionPermissions();
        RegionMembership membership = new RegionMembership();
        membership.player(playerUUID).roles().allow("owner");

        Region newRegion = new Region(name, min, max, world, permissions, membership, regionKey, parentRegion.getKey(),
                0);

        plugin.getPermissionManager().invalidateAll();
        saveRegion(regionKey, newRegion);
        addRegion(newRegion);
        return newRegion;
    }

    public boolean overlapsExistingRegion(RBlockPos min, RBlockPos max, RWorldRef world) {
        return overlapsExistingRegion(min, max, world, null);
    }

    public boolean overlapsExistingRegion(Box box) {
        return overlapsExistingRegion(box.getMin(), box.getMax(), box.getWorld(), null);
    }

    public boolean overlapsExistingRegion(RBlockPos min, RBlockPos max, RWorldRef world, @Nullable RegionKey keyToIgnore) {
        if (min == null || max == null || world == null) return false;
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withReadLock(() -> manager.overlapsExistingRegionLocked(min, max, world, keyToIgnore));
    }

    /**
     * Gets a list of regions that overlap with the given location.
     *
     * @param location The location to check for overlaps.
     * @return A list of regions that overlap with the given location.
     */
    public List<Region> getRegionsAt(RBlockPos location, RWorldRef world) {
        if (world == null || location == null) return Collections.emptyList();
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withReadLock(() -> manager.getRegionsAtLocked(location));
    }

    /**
     * Returns regions near the given location by chunk index.
     *
     * <p>This returns candidates from the chunk index, not necessarily regions within the
     * exact distance.</p>
     */
    public List<Region> getRegionsNear(RBlockPos center, RWorldRef world, int blockRange) {
        if (world == null || center == null) return Collections.emptyList();
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withReadLock(() -> manager.getRegionsNearLocked(center, blockRange));
    }

    /**
     * Gets the region with the highest priority at the given location
     *
     * @param location Location to check
     * @return Region at location, or null if no region found
     */
    public @Nullable Region getEffectiveRegionAt(RBlockPos location, RWorldRef world) {
        if (world == null || location == null) return null;
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withReadLock(() -> manager.getEffectiveRegionAtLocked(location));
    }

    /**
     * Redefines the bounds of a region.
     * Does not have any overlap checks.
     *
     * @param region The region to redefine.
     * @param min    The new minimum location of the region.
     * @param max    The new maximum location of the region.
     */
    public void redefineBounds(Region region, RBlockPos min, RBlockPos max) {
        if (region == null) return;
        RWorldRef world = region.getWorld();
        if (world == null) return;

        WorldRegionManager manager = getOrCreateWorldManager(world);
        manager.withWriteLock(() -> {
            RBlockPos oldMin = region.getMin();
            RBlockPos oldMax = region.getMax();
            region.setMin(min);
            region.setMax(max);
            manager.updateCuboidLocked(region, oldMin, oldMax);
        });
        triggerSave();
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
        if (region == null) return false;
        RWorldRef world = region.getWorld();
        if (world == null) return false;

        WorldRegionManager manager = getOrCreateWorldManager(world);
        boolean success = manager.withWriteLock(() -> {
            RBlockPos oldMin = region.getMin();
            RBlockPos oldMax = region.getMax();
            if (oldMin == null || oldMax == null) return false;

            RBlockPos newMin = oldMin;
            RBlockPos newMax = oldMax;

            switch (direction) {
                case NORTH -> newMin = new RBlockPos(newMin.x(), newMin.y(), newMin.z() - amount);
                case SOUTH -> newMax = new RBlockPos(newMax.x(), newMax.y(), newMax.z() + amount);
                case EAST -> newMax = new RBlockPos(newMax.x() + amount, newMax.y(), newMax.z());
                case WEST -> newMin = new RBlockPos(newMin.x() - amount, newMin.y(), newMin.z());
                case UP -> newMax = new RBlockPos(newMax.x(), newMax.y() + amount, newMax.z());
                case DOWN -> newMin = new RBlockPos(newMin.x(), newMin.y() - amount, newMin.z());
            }

            if (newMin.x() > newMax.x() || newMin.y() > newMax.y() || newMin.z() > newMax.z()) {
                return false;
            }

            Region parent = region.getParentRegion(this);
            if (parent != null) {
                if (!parent.contains(newMin) || !parent.contains(newMax)) {
                    return false;
                }
            }

            if (!allowOverlap && overlapsOutsideLineageInternal(region, newMin, newMax)) {
                return false;
            }

            region.setMin(newMin);
            region.setMax(newMax);
            manager.updateCuboidLocked(region, oldMin, oldMax);
            return true;
        });

        if (success) triggerSave();
        return success;
    }

    public void expandBounds(Region region, Direction direction, int amount) {
        expandBounds(region, direction, amount, true);
    }

    private boolean overlapsOutsideLineageInternal(@Nullable Region region, RBlockPos min, RBlockPos max) {
        if (region == null) return false;
        RWorldRef world = region.getWorld();
        if (world == null) return false;
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withReadLock(() ->
                manager.overlapsExistingRegionLocked(min, max, world, other -> isLineageRelated(region, other))
        );
    }

    public boolean overlapsOutsideLineage(@Nullable Region region, RBlockPos min, RBlockPos max) {
        return overlapsOutsideLineageInternal(region, min, max);
    }

    /**
     * Adds a region to the loaded regions map.
     * Requires an existing region object
     *
     * @param region The region to add.
     * @see #createNewRegion
     */
    public void addRegion(Region region) {
        if (region == null) return;
        RWorldRef world = region.getWorld();
        if (world == null) {
            loadedRegions.put(region.getKey().getValue(), region);
            return;
        }

        WorldRegionManager manager = getOrCreateWorldManager(world);
        manager.withWriteLock(() -> {
            loadedRegions.put(region.getKey().getValue(), region);
            manager.addRegionLocked(region);
        });
    }

    public <T> T withWorldReadLock(@Nullable RWorldRef world, Supplier<T> action) {
        if (action == null) throw new IllegalArgumentException("action");
        if (world == null) return action.get();
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withReadLock(action);
    }

    public void withWorldReadLock(@Nullable RWorldRef world, Runnable action) {
        withWorldReadLock(world, () -> {
            action.run();
            return null;
        });
    }

    public <T> T withWorldWriteLock(@Nullable RWorldRef world, Supplier<T> action) {
        if (action == null) throw new IllegalArgumentException("action");
        if (world == null) return action.get();
        WorldRegionManager manager = getOrCreateWorldManager(world);
        return manager.withWriteLock(action);
    }

    public void withWorldWriteLock(@Nullable RWorldRef world, Runnable action) {
        withWorldWriteLock(world, () -> {
            action.run();
            return null;
        });
    }

    private boolean isLineageRelated(@Nullable Region a, @Nullable Region b) {
        if (a == null || b == null) return false;
        if (!sameWorld(a.getWorld(), b.getWorld())) return false;
        if (a.getKey().equals(b.getKey())) return true;
        return isAncestorOf(a, b) || isAncestorOf(b, a);
    }

    private boolean isAncestorOf(@Nullable Region candidate, @Nullable Region possibleDescendant) {
        if (candidate == null || possibleDescendant == null) return false;
        if (!sameWorld(candidate.getWorld(), possibleDescendant.getWorld())) return false;

        Set<Integer> visited = null;
        Region current = possibleDescendant.getParentRegion(this);
        while (current != null) {
            if (current.getKey().equals(candidate.getKey())) return true;
            if (visited == null) visited = new HashSet<>();
            if (!visited.add(current.getKey().getValue())) break;
            current = current.getParentRegion(this);
        }
        return false;
    }

    private static boolean sameWorld(@Nullable RWorldRef a, @Nullable RWorldRef b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.identifier(), b.identifier());
    }

    private WorldRegionManager getOrCreateWorldManager(RWorldRef world) {
        String worldId = world == null ? "" : world.identifier();

        WorldRegionManager existing = worldManagers.get(worldId);
        if (existing != null) return existing;

        WorldRegionManager created = new WorldRegionManager(worldId);
        WorldRegionManager race = worldManagers.putIfAbsent(worldId, created);
        return race != null ? race : created;
    }

}
