package de.t14d3.zones.objects;

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

    public BlockLocation setX(int x) {
        this.x = x;
        return this;
    }

    public BlockLocation setY(int y) {
        this.y = y;
        return this;
    }

    public BlockLocation setZ(int z) {
        this.z = z;
        return this;
    }

    public org.bukkit.Location toLocation(org.bukkit.World world) {
        return BlockLocation.asLocation(this, world);
    }

    public double getDistance(BlockLocation other) {
        return Math.sqrt(Math.pow(other.getX() - this.getX(), 2) + Math.pow(other.getY() - this.getY(), 2) + Math.pow(
                other.getZ() - this.getZ(), 2));
    }

    public static BlockLocation of(int x, int y, int z) {
        return new BlockLocation(x, y, z);
    }

    public static BlockLocation of(org.bukkit.Location location) {
        if (location == null) {
            return null;
        }
        return new BlockLocation(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockLocation of(org.bukkit.util.BlockVector blockVector) {
        return new BlockLocation(blockVector.getBlockX(), blockVector.getBlockY(), blockVector.getBlockZ());
    }

    public static org.bukkit.Location asLocation(BlockLocation blockLocation, org.bukkit.World world) {
        return new org.bukkit.Location(world, blockLocation.getX(), blockLocation.getY(), blockLocation.getZ());
    }

    @Override
    public BlockLocation clone() {
        return new BlockLocation(this.x, this.y, this.z);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockLocation that = (BlockLocation) o;
        return x == that.x && y == that.y && z == that.z;
    }

    @Override
    public String toString() {
        return "BlockLocation{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public double distance(BlockLocation end) {
        return Math.sqrt(Math.pow(end.getX() - this.getX(), 2) + Math.pow(end.getY() - this.getY(), 2) + Math.pow(
                end.getZ() - this.getZ(), 2));
    }
}
