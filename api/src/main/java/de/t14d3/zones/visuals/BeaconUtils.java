package de.t14d3.zones.visuals;

import de.t14d3.zones.objects.BlockLocation;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class BeaconUtils {
    public static List<BlockChange> resetList(BlockLocation location) {
        List<BlockChange> changes = new ArrayList<>();

        if (location == null) {
            return changes;
        }

        int x = location.getX();
        int y = -62;
        int z = location.getZ();

        changes.add(new BlockChange(x, 1, z, null)); // Assuming null will be handled as default block data

        for (int i = y; i <= 319; i++) {
            changes.add(new BlockChange(x, i, z, null));
        }

        for (int xPoint = x - 1; xPoint <= x + 1; xPoint++) {
            for (int zPoint = z - 1; zPoint <= z + 1; zPoint++) {
                changes.add(new BlockChange(xPoint, y, zPoint, null));
            }
        }

        return changes;
    }

    public static List<BlockChange> createList(BlockLocation location, NamedTextColor color) {
        List<BlockChange> changes = new ArrayList<>();

        if (location == null) {
            return changes;
        }

        int x = location.getX();
        int y = -62;
        int z = location.getZ();

        for (int xPoint = x - 1; xPoint <= x + 1; xPoint++) {
            for (int zPoint = z - 1; zPoint <= z + 1; zPoint++) {
                changes.add(new BlockChange(xPoint, y, zPoint, "IRON_BLOCK"));
            }
        }

        changes.add(new BlockChange(x, -61, z, "BEACON"));

        int highestYPoint = 0;

        for (int yPoint = -60; yPoint <= 319; yPoint++) {
            changes.add(new BlockChange(x, yPoint, z, color.toString().toUpperCase() + "_STAINED_GLASS"));
            highestYPoint = yPoint;
        }

        changes.add(new BlockChange(x, highestYPoint, z, color.toString().toUpperCase() + "_STAINED_GLASS"));

        return changes;
    }

    public static class BlockChange {
        private final int x, y, z;
        private final String blockData;

        public BlockChange(int x, int y, int z, String blockData) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.blockData = blockData;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public String getBlockData() {
            return blockData;
        }
    }
}
