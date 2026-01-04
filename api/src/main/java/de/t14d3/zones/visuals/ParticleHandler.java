package de.t14d3.zones.visuals;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;

import java.time.Duration;

public class ParticleHandler {
    private final Zones zones;
    private final ZonesPlatform platform;
    private final double range;

    public ParticleHandler(Zones zones) {
        this.zones = zones;
        this.platform = zones.getPlatform();
        this.range = zones.getConfig().getInt("visuals.particles.range", 15);
    }

    void spawnParticleOutline(RPlayer player, RBlockPos min, RBlockPos max) {
        // Normalize coordinates so function works regardless of which corner is "min" or "max"
        int minX = Math.min(min.x(), max.x());
        int maxX = Math.max(min.x(), max.x());
        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());
        int minZ = Math.min(min.z(), max.z());
        int maxZ = Math.max(min.z(), max.z());

        int x1 = minX;
        int y1 = minY;
        int z1 = minZ;
        int x2 = maxX + 1;
        int y2 = maxY + 1;
        int z2 = maxZ + 1;

        // Generate particles for all 6 faces
        spawnFace(player, 'y', y1, x1, x2, z1, z2, x1, x2, y1, y2, z1, z2); // Floor
        spawnFace(player, 'y', y2, x1, x2, z1, z2, x1, x2, y1, y2, z1, z2); // Roof
        spawnFace(player, 'x', x1, y1, y2, z1, z2, x1, x2, y1, y2, z1, z2); // Left wall
        spawnFace(player, 'x', x2, y1, y2, z1, z2, x1, x2, y1, y2, z1, z2); // Right wall
        spawnFace(player, 'z', z1, x1, x2, y1, y2, x1, x2, y1, y2, z1, z2); // Front wall
        spawnFace(player, 'z', z2, x1, x2, y1, y2, x1, x2, y1, y2, z1, z2); // Back wall
    }

    public void spawnFace(RPlayer player, char axis, int fixedVal,
                          int axis1Start, int axis1End, int axis2Start, int axis2End,
                          int x1, int x2, int y1, int y2, int z1, int z2) {
        var playerLoc = player.location().orElse(null);
        if (playerLoc == null) return;
        RBlockPos playerBlock = playerLoc.blockPos();

        for (int a1 = axis1Start; a1 <= axis1End; a1++) {
            for (int a2 = axis2Start; a2 <= axis2End; a2++) {
                RBlockPos loc = createLocation(axis, fixedVal, a1, a2);

                // Use squared distance to avoid sqrt() call (speeeeed)
                long rangeSq = (long) range * (long) range;
                if (distanceSquared(loc, playerBlock) >= rangeSq) continue;

                platform.spawnParticle(isCorner(loc, x1, x2, y1, y2, z1, z2) ? 2 : 1, loc, player);
            }
        }
    }

    static RBlockPos createLocation(char axis, int fixedVal, int a1, int a2) {
        return switch (axis) {
            case 'x' -> new RBlockPos(fixedVal, a1, a2);
            case 'y' -> new RBlockPos(a1, fixedVal, a2);
            case 'z' -> new RBlockPos(a1, a2, fixedVal);
            default -> throw new IllegalArgumentException("Invalid axis");
        };
    }

    static boolean isCorner(RBlockPos loc, int x1, int x2, int y1, int y2, int z1, int z2) {
        boolean xEdge = loc.x() == x1 || loc.x() == x2;
        boolean yEdge = loc.y() == y1 || loc.y() == y2;
        boolean zEdge = loc.z() == z1 || loc.z() == z2;
        return (xEdge && yEdge) || (xEdge && zEdge) || (yEdge && zEdge);
    }

    static long distanceSquared(RBlockPos a, RBlockPos b) {
        long dx = (long) b.x() - a.x();
        long dy = (long) b.y() - a.y();
        long dz = (long) b.z() - a.z();
        return dx * dx + dy * dy + dz * dz;
    }

    public void particleScheduler() {
        if (!zones.getConfig().getBoolean("visuals.particles.enabled", false)) {
            return;
        }

        Rapunzel.context().scheduler().runRepeatingAsync(Duration.ofMillis(200), Duration.ofMillis(200), () -> {
            for (RPlayer player : RPlayer.online()) {
                Box selection = player.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
                if (selection == null || selection.getMin() == null || selection.getMax() == null) continue;
                spawnParticleOutline(player, selection.getMin(), selection.getMax());
            }
        });
    }
}
