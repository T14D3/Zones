package de.t14d3.zones.utils;

public enum Actions {
    BREAK("Allows breaking blocks"),
    PLACE("Allows placing blocks"),
    INTERACT("Allows interacting"),
    CONTAINER("Allows opening containers"),
    REDSTONE("Allows interacting with redstone"),
    ENTITY("Allows interacting with entities"),
    IGNITE("Allows igniting tnt"),
    DAMAGE("Allows damaging entities");

    private final String key;

    Actions(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
