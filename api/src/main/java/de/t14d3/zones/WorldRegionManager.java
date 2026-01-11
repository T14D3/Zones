package de.t14d3.zones;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.objects.shapes.CuboidShape;
import de.t14d3.zones.objects.shapes.GlobalShape;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * World-scoped region manager for fast lookups and isolated locking (Folia-friendly).
 *
 * <p>This class is intentionally stateful and uses a per-world lock to keep the
 * hot-path lookups safe under concurrent reads/writes.</p>
 */
final class WorldRegionManager {
    private final String worldId;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Int2ObjectOpenHashMap<Region> regions = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<List<Region>> chunkRegions = new Long2ObjectOpenHashMap<>();
    private final Set<Region> globalRegions = new HashSet<>();

    WorldRegionManager(String worldId) {
        this.worldId = worldId == null ? "" : worldId;
    }

    String worldId() {
        return worldId;
    }

    <T> T withReadLock(Supplier<T> action) {
        var read = lock.readLock();
        read.lock();
        try {
            return action.get();
        } finally {
            read.unlock();
        }
    }

    void withReadLock(Runnable action) {
        withReadLock(() -> {
            action.run();
            return null;
        });
    }

    <T> T withWriteLock(Supplier<T> action) {
        var write = lock.writeLock();
        write.lock();
        try {
            return action.get();
        } finally {
            write.unlock();
        }
    }

    void withWriteLock(Runnable action) {
        withWriteLock(() -> {
            action.run();
            return null;
        });
    }

    int sizeLocked() {
        return regions.size();
    }

    boolean isEmptyLocked() {
        return regions.isEmpty();
    }

    void clearLocked() {
        regions.clear();
        chunkRegions.clear();
        globalRegions.clear();
    }

    void addRegionLocked(Region region) {
        if (region == null) return;
        regions.put(region.getKey().getValue(), region);

        if (region.getShape() instanceof GlobalShape) {
            globalRegions.add(region);
            return;
        }
        if (region.getShape() instanceof CuboidShape) {
            indexCuboidLocked(region);
        }
    }

    void removeRegionLocked(Region region) {
        if (region == null) return;
        regions.remove(region.getKey().getValue());

        if (region.getShape() instanceof GlobalShape) {
            globalRegions.remove(region);
            return;
        }
        if (region.getShape() instanceof CuboidShape) {
            RBlockPos min = region.getMin();
            RBlockPos max = region.getMax();
            if (min != null && max != null) {
                removeRegionFromChunksLocked(region, min, max);
            }
        }
    }

    void indexCuboidLocked(Region region) {
        if (!(region.getShape() instanceof CuboidShape)) return;
        RBlockPos min = region.getMin();
        RBlockPos max = region.getMax();
        if (min == null || max == null) return;
        addRegionToChunksLocked(region, min, max);
    }

    void updateCuboidLocked(Region region, RBlockPos oldMin, RBlockPos oldMax) {
        if (!(region.getShape() instanceof CuboidShape)) return;
        if (oldMin != null && oldMax != null) {
            removeRegionFromChunksLocked(region, oldMin, oldMax);
        }
        RBlockPos min = region.getMin();
        RBlockPos max = region.getMax();
        if (min == null || max == null) return;
        addRegionToChunksLocked(region, min, max);
    }

    boolean overlapsExistingRegionLocked(
            RBlockPos min,
            RBlockPos max,
            RWorldRef world,
            @Nullable RegionKey keyToIgnore
    ) {
        return overlapsExistingRegionLocked(min, max, world,
                region -> region != null && region.getKey().equals(keyToIgnore));
    }

    boolean overlapsExistingRegionLocked(
            RBlockPos min,
            RBlockPos max,
            RWorldRef world,
            @Nullable java.util.function.Predicate<Region> ignore
    ) {
        if (min == null || max == null || world == null) return false;
        for (Region region : regions.values()) {
            if (region.intersects(min, max, world)) {
                if (ignore != null && ignore.test(region)) continue;
                return true;
            }
        }
        return false;
    }

    List<Region> getRegionsAtLocked(RBlockPos location) {
        if (location == null) return Collections.emptyList();
        ArrayList<Region> found = new ArrayList<>();

        int xChunk = location.x() >> 4;
        int zChunk = location.z() >> 4;
        long key = ((long) xChunk << 32) | (zChunk & 0xFFFFFFFFL);

        List<Region> candidates = chunkRegions.get(key);
        if (candidates != null) {
            for (Region region : candidates) {
                if (region.contains(location)) found.add(region);
            }
        }

        if (!globalRegions.isEmpty()) found.addAll(globalRegions);
        return found;
    }

    List<Region> getRegionsNearLocked(RBlockPos center, int blockRange) {
        if (center == null) return Collections.emptyList();
        int r = Math.max(0, blockRange);

        int minXChunk = (center.x() - r) >> 4;
        int minZChunk = (center.z() - r) >> 4;
        int maxXChunk = (center.x() + r) >> 4;
        int maxZChunk = (center.z() + r) >> 4;

        LinkedHashSet<Region> found = new LinkedHashSet<>();
        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                List<Region> candidates = chunkRegions.get(key);
                if (candidates != null) found.addAll(candidates);
            }
        }

        if (!globalRegions.isEmpty()) found.addAll(globalRegions);
        if (found.isEmpty()) return Collections.emptyList();
        return new ArrayList<>(found);
    }

    @Nullable Region getEffectiveRegionAtLocked(RBlockPos location) {
        List<Region> regions = getRegionsAtLocked(location);
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

    private void removeRegionFromChunksLocked(Region region, RBlockPos min, RBlockPos max) {
        int minXChunk = min.x() >> 4;
        int minZChunk = min.z() >> 4;
        int maxXChunk = max.x() >> 4;
        int maxZChunk = max.z() >> 4;

        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                List<Region> regions = chunkRegions.get(key);
                if (regions != null) {
                    regions.remove(region);
                    if (regions.isEmpty()) chunkRegions.remove(key);
                }
            }
        }
    }

    private void addRegionToChunksLocked(Region region, RBlockPos min, RBlockPos max) {
        int minXChunk = min.x() >> 4;
        int minZChunk = min.z() >> 4;
        int maxXChunk = max.x() >> 4;
        int maxZChunk = max.z() >> 4;

        for (int x = minXChunk; x <= maxXChunk; x++) {
            for (int z = minZChunk; z <= maxZChunk; z++) {
                long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
                chunkRegions.computeIfAbsent(key, k -> new ArrayList<>()).add(region);
            }
        }
    }
}
