package de.t14d3.zones.visuals;

import de.t14d3.zones.Zones;
import de.t14d3.zones.ZonesPlatform;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.PlayerRepository;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ParticleHandler {
    private final Zones zones;
    private final ZonesPlatform platform;
    private final double range;
    private ScheduledFuture<?> particleScheduler;

    public ParticleHandler(Zones zones) {
        this.zones = zones;
        this.platform = zones.getPlatform();
        this.range = zones.getConfig().getInt("selection-particles.range", 15);
    }

    void spawnParticleOutline(Player player, BlockLocation min, BlockLocation max) {
        int x1 = min.getX();
        int y1 = min.getY();
        int z1 = min.getZ();
        int x2 = max.getX() + 1;
        int y2 = max.getY() + 1;
        int z2 = max.getZ() + 1;

        // Generate particles for all 6 faces
        spawnFace(player, 'y', y1, x1, x2, z1, z2, x1, x2, y1, y2, z1, z2); // Floor
        spawnFace(player, 'y', y2, x1, x2, z1, z2, x1, x2, y1, y2, z1, z2); // Roof
        spawnFace(player, 'x', x1, y1, y2, z1, z2, x1, x2, y1, y2, z1, z2); // Left wall
        spawnFace(player, 'x', x2, y1, y2, z1, z2, x1, x2, y1, y2, z1, z2); // Right wall
        spawnFace(player, 'z', z1, x1, x2, y1, y2, x1, x2, y1, y2, z1, z2); // Front wall
        spawnFace(player, 'z', z2, x1, x2, y1, y2, x1, x2, y1, y2, z1, z2); // Back wall
    }

    public void spawnFace(Player player, char axis, int fixedVal,
                          int axis1Start, int axis1End, int axis2Start, int axis2End,
                          int x1, int x2, int y1, int y2, int z1, int z2) {
        BlockLocation loc = new BlockLocation(0, 0, 0);
        for (int a1 = axis1Start; a1 <= axis1End; a1++) {
            for (int a2 = axis2Start; a2 <= axis2End; a2++) {
                loc = createLocation(axis, fixedVal, a1, a2, loc);
                if (loc.distance(player.getLocation()) >= range) continue;

                platform.spawnParticle(isCorner(loc, x1, x2, y1, y2, z1, z2) ? 2 : 1, loc, player);
            }
        }
    }

    private static BlockLocation createLocation(char axis, int fixedVal, int a1, int a2, BlockLocation locToChange) {
        return switch (axis) {
            case 'x' -> locToChange.setX(fixedVal).setY(a1).setZ(a2);
            case 'y' -> locToChange.setX(a1).setY(fixedVal).setZ(a2);
            case 'z' -> locToChange.setX(a1).setY(a2).setZ(fixedVal);
            default -> throw new IllegalArgumentException("Invalid axis");
        };
    }

    private static boolean isCorner(BlockLocation loc, int x1, int x2, int y1, int y2, int z1, int z2) {
        boolean xEdge = loc.getX() == x1 || loc.getX() == x2;
        boolean yEdge = loc.getY() == y1 || loc.getY() == y2;
        boolean zEdge = loc.getZ() == z1 || loc.getZ() == z2;
        return (xEdge && yEdge) || (xEdge && zEdge) || (yEdge && zEdge);
    }

    public void particleScheduler() {
        if (!zones.getConfig().getBoolean("selection-particles.enabled", false)) {
            return;
        }
        Runnable runnable = () -> {
            for (Player player : PlayerRepository.getPlayers()) {
                Box selection = player.getSelection();
                if (selection == null) {
                    continue;
                } else if (selection.getMin() == null || selection.getMax() == null) {
                    continue;
                }
                spawnParticleOutline(
                        player,
                        selection.getMin(),
                        selection.getMax());
            }
        };
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(runnable, 0L, 200, TimeUnit.MILLISECONDS);
    }
}
