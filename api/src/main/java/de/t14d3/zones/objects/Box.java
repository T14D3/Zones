package de.t14d3.zones.objects;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Objects;

public class Box {
    private final RBlockPos min;
    private final RBlockPos max;
    private final RWorldRef world;

    @SuppressWarnings("ConstantConditions")
    public Box(RBlockPos pos1, RBlockPos pos2, RWorldRef world) {
        this(pos1, pos2, world, true);
    }

    /**
     * Creates a new box from two points.
     *
     * @param pos1      first point
     * @param pos2      second point
     * @param world     world of the box
     * @param normalize whether to normalize the points
     */
    public Box(RBlockPos pos1, RBlockPos pos2, RWorldRef world, boolean normalize) {
        if (normalize) {
            this.min = new RBlockPos(
                    Math.min(pos1.x(), pos2.x()),
                    Math.min(pos1.y(), pos2.y()),
                    Math.min(pos1.z(), pos2.z())
            );
            this.max = new RBlockPos(
                    Math.max(pos1.x(), pos2.x()),
                    Math.max(pos1.y(), pos2.y()),
                    Math.max(pos1.z(), pos2.z())
            );
        } else {
            this.min = pos1;
            this.max = pos2;
        }
        this.world = world;
    }


    public Box(int x1, int y1, int z1, int x2, int y2, int z2, RWorldRef world) {
        this(new RBlockPos(x1, y1, z1), new RBlockPos(x2, y2, z2), world);
    }

    public Box(RWorldRef world) {
        this.min = null;
        this.max = null;
        this.world = world;
    }

    public RBlockPos getMin() {
        return min;
    }

    public RBlockPos getMax() {
        return max;
    }

    public RWorldRef getWorld() {
        return world;
    }

    public RBlockPos getCenter() {
        return new RBlockPos((min.x() + max.x()) / 2, (min.y() + max.y()) / 2, (min.z() + max.z()) / 2);
    }

    public int getVolume() {
        return (max.x() - min.x() + 1) * (max.y() - min.y() + 1) * (max.z() - min.z() + 1);
    }

    public int getArea() {
        return (max.x() - min.x() + 1) * (max.y() - min.y() + 1);
    }

    public boolean contains(RBlockPos location) {
        return location.x() >= min.x() && location.x() <= max.x()
                && location.y() >= min.y() && location.y() <= max.y()
                && location.z() >= min.z() && location.z() <= max.z();
    }

    public boolean intersects(Box other) {
        return sameWorld(other.world, world) &&
                other.min.x() <= max.x() && other.max.x() >= min.x()
                && other.min.y() <= max.y() && other.max.y() >= min.y()
                && other.min.z() <= max.z() && other.max.z() >= min.z();
    }

    public boolean intersects(RBlockPos min, RBlockPos max, RWorldRef world) {
        return intersects(new Box(min, max, world));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Box box = (Box) o;
        return sameWorld(world, box.world) && min.equals(box.min) && max.equals(box.max);
    }

    private static boolean sameWorld(RWorldRef a, RWorldRef b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.identifier(), b.identifier());
    }
}
