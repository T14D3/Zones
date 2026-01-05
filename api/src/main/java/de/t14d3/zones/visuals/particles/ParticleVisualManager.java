package de.t14d3.zones.visuals.particles;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesParticleRole;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import de.t14d3.zones.visuals.ParticleHandler;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ParticleVisualManager {
    private static final long NO_ENTRY = Long.MIN_VALUE;

    private final Zones zones;
    private final ZonesPlatform platform;
    private final ConcurrentHashMap<UUID, ConcurrentHashMap<String, Overlay>> overlays = new ConcurrentHashMap<>();
    private volatile boolean started;

    public ParticleVisualManager(Zones zones) {
        this.zones = zones;
        this.platform = zones.getPlatform();

        long periodMs = zones.getConfig().getLong("visuals.particles.period-ms", 200);
        Duration period = Duration.ofMillis(Math.max(50, periodMs));
        Rapunzel.context().scheduler().runRepeatingAsync(Duration.ofSeconds(1), period, this::tickSafely);
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Throwable t) {
            zones.getLogger().warn("Particle visuals tick failed", t);
        }
    }

    private void tick() {
        if (!zones.getConfig().getBoolean("visuals.particles.enabled", false)) {
            return;
        }

        int range = zones.getConfig().getInt("visuals.particles.range", 15);
        int budget = zones.getConfig().getInt("visuals.particles.budget-per-player", 2500);
        long rangeSq = (long) range * (long) range;
        long now = System.nanoTime();

        for (RPlayer player : RPlayer.online()) {
            var loc = player.location().orElse(null);
            if (loc == null) continue;

            RBlockPos viewerBlock = loc.blockPos();
            RWorldRef viewerWorld = loc.world();

            Long2LongOpenHashMap points = new Long2LongOpenHashMap(Math.min(budget * 2, 16_384));
            points.defaultReturnValue(NO_ENTRY);

            appendSelection(points, budget, player, viewerBlock, viewerWorld, range, rangeSq);
            appendNearby(points, budget, player, viewerBlock, viewerWorld, range, rangeSq);
            appendOverlays(points, budget, player, viewerBlock, viewerWorld, range, rangeSq, now);

            for (Long2LongMap.Entry entry : points.long2LongEntrySet()) {
                long packed = entry.getLongKey();
                ZonesParticleRole role = decodeRole(entry.getLongValue());

                int x = ParticleHandler.unpackX(packed);
                int y = ParticleHandler.unpackY(packed);
                int z = ParticleHandler.unpackZ(packed);

                platform.spawnParticle(role, new RBlockPos(x, y, z), player);
            }
        }
    }

    public void showOverlay(
            UUID viewerUuid,
            String id,
            Box box,
            ParticleRenderMode mode,
            ParticlePalette palette,
            int priority,
            int step,
            Duration ttl
    ) {
        if (viewerUuid == null || id == null || id.isBlank() || box == null) return;
        long expiresAtNanos = ttl == null ? Long.MAX_VALUE : (System.nanoTime() + Math.max(0, ttl.toNanos()));
        Overlay overlay = new Overlay(box, mode, palette, priority, step, expiresAtNanos);
        overlays.computeIfAbsent(viewerUuid, u -> new ConcurrentHashMap<>()).put(id, overlay);
    }

    public void removeOverlay(UUID viewerUuid, String id) {
        if (viewerUuid == null || id == null) return;
        ConcurrentHashMap<String, Overlay> byId = overlays.get(viewerUuid);
        if (byId == null) return;
        byId.remove(id);
        if (byId.isEmpty()) overlays.remove(viewerUuid, byId);
    }

    private void appendSelection(
            Long2LongOpenHashMap points,
            int budget,
            RPlayer player,
            RBlockPos viewerBlock,
            RWorldRef viewerWorld,
            int range,
            long rangeSq
    ) {
        Box selection = player.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
        if (selection == null || selection.getMin() == null || selection.getMax() == null) return;

        ParticleRenderMode mode = parseMode(
                zones.getConfig().getString("visuals.particles.selection.mode", "FACES"),
                ParticleRenderMode.FACES
        );
        int step = zones.getConfig().getInt("visuals.particles.selection.step", 1);

        renderBox(
                points,
                budget,
                viewerBlock,
                viewerWorld,
                selection.getMin(),
                selection.getMax(),
                selection.getWorld(),
                mode,
                ParticlePalette.PRIMARY_CORNERS,
                100,
                step,
                range,
                rangeSq
        );
    }

    private void appendNearby(
            Long2LongOpenHashMap points,
            int budget,
            RPlayer player,
            RBlockPos viewerBlock,
            RWorldRef viewerWorld,
            int range,
            long rangeSq
    ) {
        boolean enabled = player.extras().get(ZonesExtraKeys.NEARBY_VISUALS).orElse(false);
        if (!enabled) return;

        boolean highlight = zones.getConfig().getBoolean("visuals.particles.nearby.highlight-effective", true);
        Region effective = highlight
                ? zones.getRegionManager().getEffectiveRegionAt(viewerBlock, viewerWorld)
                : null;

        ParticleRenderMode mode = parseMode(
                zones.getConfig().getString("visuals.particles.nearby.mode", "PERIMETER_AT_VIEWER_Y"),
                ParticleRenderMode.PERIMETER_AT_VIEWER_Y
        );
        int step = zones.getConfig().getInt("visuals.particles.nearby.step", 1);

        List<Region> candidates = zones.getRegionManager().getRegionsNear(viewerBlock, viewerWorld, range);
        for (Region region : candidates) {
            if (points.size() >= budget) break;
            if (region == null) continue;
            RBlockPos min = region.getMin();
            RBlockPos max = region.getMax();
            if (min == null || max == null) continue;

            boolean isEffective = effective != null && Objects.equals(effective.getKey(), region.getKey());
            ParticlePalette palette = isEffective ? ParticlePalette.SECONDARY_CORNERS : ParticlePalette.PRIMARY_CORNERS;

            renderBox(
                    points,
                    budget,
                    viewerBlock,
                    viewerWorld,
                    min,
                    max,
                    region.getWorld(),
                    mode,
                    palette,
                    isEffective ? 10 : 0,
                    step,
                    range,
                    rangeSq
            );
        }
    }

    private void appendOverlays(
            Long2LongOpenHashMap points,
            int budget,
            RPlayer player,
            RBlockPos viewerBlock,
            RWorldRef viewerWorld,
            int range,
            long rangeSq,
            long nowNanos
    ) {
        ConcurrentHashMap<String, Overlay> byId = overlays.get(player.uuid());
        if (byId == null || byId.isEmpty()) return;

        for (var entry : byId.entrySet()) {
            if (points.size() >= budget) break;
            Overlay overlay = entry.getValue();
            if (overlay == null) continue;
            if (overlay.expiresAtNanos <= nowNanos) {
                byId.remove(entry.getKey(), overlay);
                continue;
            }

            Box box = overlay.box;
            if (box == null || box.getMin() == null || box.getMax() == null) continue;

            renderBox(
                    points,
                    budget,
                    viewerBlock,
                    viewerWorld,
                    box.getMin(),
                    box.getMax(),
                    box.getWorld(),
                    overlay.mode,
                    overlay.palette,
                    overlay.priority,
                    overlay.step,
                    range,
                    rangeSq
            );
        }

        if (byId.isEmpty()) overlays.remove(player.uuid(), byId);
    }

    private void renderBox(
            Long2LongOpenHashMap points,
            int budget,
            RBlockPos viewerBlock,
            RWorldRef viewerWorld,
            RBlockPos min,
            RBlockPos max,
            RWorldRef world,
            ParticleRenderMode mode,
            ParticlePalette palette,
            int priority,
            int configuredStep,
            int range,
            long rangeSq
    ) {
        if (points.size() >= budget) return;
        if (min == null || max == null) return;
        if (world != null && !sameWorld(viewerWorld, world)) return;

        if (distanceSquaredToAabb(viewerBlock, min, max) > rangeSq) return;

        int remaining = budget - points.size();
        int step = computeAutoStep(mode, min, max, remaining, Math.max(1, configuredStep));

        ParticleHandler.emit(
                mode,
                min,
                max,
                viewerBlock,
                range,
                step,
                (x, y, z, corner) -> {
                    long dSq = ParticleHandler.distanceSquared(viewerBlock.x(), viewerBlock.y(), viewerBlock.z(), x, y,
                            z);
                    if (dSq > rangeSq) return true;

                    long packed = ParticleHandler.packBlockPos(x, y, z);
                    ZonesParticleRole role = corner ? palette.cornerRole() : palette.edgeRole();
                    long encoded = encode(priority, corner, role);

                    long existing = points.get(packed);
                    if (existing == NO_ENTRY) {
                        points.put(packed, encoded);
                        return points.size() < budget;
                    }

                    if (shouldReplace(existing, encoded)) {
                        points.put(packed, encoded);
                    }
                    return true;
                }
        );
    }

    private static ParticleRenderMode parseMode(String raw, ParticleRenderMode fallback) {
        if (raw == null) return fallback;
        try {
            return ParticleRenderMode.valueOf(raw.trim().toUpperCase());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static int computeAutoStep(
            ParticleRenderMode mode,
            RBlockPos min,
            RBlockPos max,
            int remaining,
            int configuredStep
    ) {
        if (remaining <= 0) return Integer.MAX_VALUE;

        int minX = Math.min(min.x(), max.x());
        int maxX = Math.max(min.x(), max.x()) + 1;
        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y()) + 1;
        int minZ = Math.min(min.z(), max.z());
        int maxZ = Math.max(min.z(), max.z()) + 1;

        long xLen = (long) (maxX - minX) + 1;
        long yLen = (long) (maxY - minY) + 1;
        long zLen = (long) (maxZ - minZ) + 1;

        long approx;
        switch (mode) {
            case PERIMETER_AT_VIEWER_Y -> approx = 2L * xLen + 2L * zLen;
            case WIRE_EDGES -> approx = 4L * (xLen + yLen + zLen);
            default -> {
                return configuredStep;
            }
        }

        if (approx <= remaining) return configuredStep;
        int stepFromBudget = (int) Math.ceil((double) approx / (double) remaining);
        return Math.max(configuredStep, Math.max(1, stepFromBudget));
    }

    private record Overlay(
            Box box,
            ParticleRenderMode mode,
            ParticlePalette palette,
            int priority,
            int step,
            long expiresAtNanos
    ) {
        private Overlay {
            if (mode == null) mode = ParticleRenderMode.WIRE_EDGES;
            if (palette == null) palette = ParticlePalette.PRIMARY_CORNERS;
            step = Math.max(1, step);
        }
    }

    private static long encode(int priority, boolean corner, ZonesParticleRole role) {
        long p = (long) priority;
        long c = corner ? 1L : 0L;
        long r = (long) (role == null ? 0 : role.ordinal()) & 0xFFFFL;
        return (p << 32) | (c << 16) | r;
    }

    private static boolean shouldReplace(long existing, long incoming) {
        int existingPriority = (int) (existing >> 32);
        int incomingPriority = (int) (incoming >> 32);
        if (incomingPriority != existingPriority) return incomingPriority > existingPriority;

        boolean existingCorner = ((existing >> 16) & 1L) == 1L;
        boolean incomingCorner = ((incoming >> 16) & 1L) == 1L;
        return incomingCorner && !existingCorner;
    }

    private static ZonesParticleRole decodeRole(long encoded) {
        int ordinal = (int) (encoded & 0xFFFFL);
        ZonesParticleRole[] values = ZonesParticleRole.values();
        if (ordinal < 0 || ordinal >= values.length) return ZonesParticleRole.PRIMARY;
        return values[ordinal];
    }

    private static boolean sameWorld(RWorldRef a, RWorldRef b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.identifier(), b.identifier());
    }

    private static long distanceSquaredToAabb(RBlockPos point, RBlockPos min, RBlockPos max) {
        int minX = Math.min(min.x(), max.x());
        int maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxZ = Math.max(min.z(), max.z());

        long dx = axisDistance(point.x(), minX, maxX);
        long dy = axisDistance(point.y(), minY, maxY);
        long dz = axisDistance(point.z(), minZ, maxZ);
        return dx * dx + dy * dy + dz * dz;
    }

    private static long axisDistance(int value, int min, int max) {
        if (value < min) return (long) min - value;
        if (value > max) return (long) value - max;
        return 0L;
    }
}
