package de.t14d3.zones.objects;

import org.bukkit.Location;

public class BlockLocation {
    private int x;
    private int y;
    private int z;

    public BlockLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public double getDistance(BlockLocation other) {
        return Math.sqrt(Math.pow(other.getX() - this.getX(), 2) + Math.pow(other.getY() - this.getY(), 2) + Math.pow(
                other.getZ() - this.getZ(), 2));
    }

    public static BlockLocation of(int x, int y, int z) {
        return new BlockLocation(x, y, z);
    }

    public static BlockLocation of(org.bukkit.Location location) {
        return new BlockLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockLocation of(org.bukkit.util.BlockVector blockVector) {
        return new BlockLocation(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
    }

    public static Location toLocation(BlockLocation blockLocation, org.bukkit.World world) {
        return new Location(world, blockLocation.getX(), blockLocation.getY(), blockLocation.getZ());
    }

    @Override
    public BlockLocation clone() {
        return new BlockLocation(this.x, this.y, this.z);
    }
}
