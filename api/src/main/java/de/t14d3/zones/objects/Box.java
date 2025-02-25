package de.t14d3.zones.objects;

public class Box {
    private final BlockLocation min;
    private final BlockLocation max;
    private final World world;

    public Box(BlockLocation min, BlockLocation max, World world) {
        this.min = min;
        this.max = max;
        this.world = world;
    }

    public Box(int x1, int y1, int z1, int x2, int y2, int z2, World world) {
        this(new BlockLocation(x1, y1, z1), new BlockLocation(x2, y2, z2), world);
    }

    public Box(org.bukkit.Location min, org.bukkit.Location max, org.bukkit.World world) {
        this(BlockLocation.of(min), BlockLocation.of(max), World.of(world));
    }

    public BlockLocation getMin() {
        return min;
    }

    public BlockLocation getMax() {
        return max;
    }

    public World getWorld() {
        return world;
    }

    public BlockLocation getCenter() {
        return new BlockLocation((min.getX() + max.getX()) / 2, (min.getY() + max.getY()) / 2,
                (min.getZ() + max.getZ()) / 2);
    }

    public int getVolume() {
        return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
    }

    public int getArea() {
        return (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1);
    }

    public boolean contains(BlockLocation location) {
        return location.getX() >= min.getX() && location.getX() <= max.getX()
                && location.getY() >= min.getY() && location.getY() <= max.getY()
                && location.getZ() >= min.getZ() && location.getZ() <= max.getZ();
    }

    public boolean intersects(Box other) {
        return other.world.equals(world) &&
                other.min.getX() <= max.getX() && other.max.getX() >= min.getX()
                && other.min.getY() <= max.getY() && other.max.getY() >= min.getY()
                && other.min.getZ() <= max.getZ() && other.max.getZ() >= min.getZ();
    }

    public boolean intersects(BlockLocation min, BlockLocation max, World world) {
        return intersects(new Box(min, max, world));
    }

    public org.bukkit.util.BoundingBox toBoundingBox() {
        return new org.bukkit.util.BoundingBox(min.getX(), min.getY(), min.getZ(), max.getX(), max.getY(), max.getZ());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Box box = (Box) o;
        return world.equals(box.world) && min.equals(box.min) && max.equals(box.max);
    }
}
