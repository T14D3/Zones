package de.t14d3.zones.utils;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public enum Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST,
    UP,
    DOWN;

    public static Direction fromYaw(double yaw) {
        if (yaw < -135 || yaw > 135) {
            return Direction.NORTH;
        }
        if (yaw < 135 && yaw > 45) {
            return Direction.WEST;
        }
        if (yaw < 45 && yaw > -45) {
            return Direction.SOUTH;
        }
        if (yaw < -45 && yaw > -135) {
            return Direction.EAST;
        }
        return Direction.NORTH;
    }

    /**
     * Gets the direction from a Vector.
     *
     * @param vector The Vector to get the direction from.
     * @return The direction of the Vector.
     */
    public static Direction fromVector(Vector vector) {
        vector = vector.normalize();

        if (vector.getY() > 0.5) {
            return Direction.UP;
        } else if (vector.getY() < -0.5) {
            return Direction.DOWN;
        }
        double yaw = Math.atan2(vector.getX(), vector.getZ()) * (180 / Math.PI);
        return fromYaw(yaw);

    }

    /**
     * Gets a BlockFace from the direction.
     *
     * @return The corresponding BlockFace.
     */
    public BlockFace toBlockFace() {
        return switch (this) {
            case NORTH -> BlockFace.NORTH;
            case EAST -> BlockFace.EAST;
            case SOUTH -> BlockFace.SOUTH;
            case WEST -> BlockFace.WEST;
            case UP -> BlockFace.UP;
            case DOWN -> BlockFace.DOWN;
        };
    }
}
