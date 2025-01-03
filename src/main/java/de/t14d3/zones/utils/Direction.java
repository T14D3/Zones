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
        if (yaw < -180) {
            yaw += 360;
        }
        if (yaw > 180) {
            yaw -= 360;
        }
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw > 360) {
            yaw -= 360;
        }
        Direction direction;
        if (yaw < 22.5) {
            direction = Direction.NORTH;
        } else if (yaw < 67.5) {
            direction = Direction.EAST;
        } else if (yaw < 112.5) {
            direction = Direction.SOUTH;
        } else if (yaw < 157.5) {
            direction = Direction.WEST;
        } else {
            direction = Direction.NORTH;
        }
        return direction;
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
