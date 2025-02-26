package de.t14d3.zones.objects;

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
}
