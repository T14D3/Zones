package de.t14d3.zones.objects.shapes;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.objects.Box;

/**
 * Interface for defining different region shapes.
 * Implementations should provide methods to check if a location is contained
 * within the shape and if shapes intersect.
 */
public interface ShapeProvider {

    /**
     * Checks if the given block location is contained within this shape.
     *
     * @param location The location to check
     * @return true if the location is inside this shape
     */
    boolean contains(RBlockPos location);

    /**
     * Checks if this shape intersects with another shape.
     * Shapes intersect if there's any overlap in their areas.
     *
     * @param other The other shape to check intersection with
     * @return true if shapes intersect
     */
    boolean intersects(ShapeProvider other);

    /**
     * Gets the world this shape belongs to.
     *
     * @return The world containing this shape
     */
    RWorldRef getWorld();

    /**
     * Gets the axis-aligned bounding box for this shape if possible.
     * For simple shapes like cuboids, this should return the exact bounds.
     * For complex shapes, this can return a bounding box that fully contains the shape.
     * For infinite shapes like global, this should return null.
     *
     * @return Bounding box if applicable, null otherwise
     */
    default Box getBoundingBox() {
        return null;
    }

    /**
     * Gets the shape type for serialization.
     *
     * @return The shape type string
     */
    default String getShapeType() {
        if (this instanceof GlobalShape) return "global";
        if (this instanceof CuboidShape) return "cuboid";
        return "unknown";
    }

    String toJson();

    static ShapeProvider fromJson(String json) {
        throw new UnsupportedOperationException();
    }
}
