package de.t14d3.zones.utils;

import de.t14d3.zones.permissions.flags.Flag;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for types.
 * <p>
 * Contains lists for blocks and entities for their respective {@link Flag} type.
 */
public abstract class Types {
    protected static List<String> allTypes = new ArrayList<>();
    protected static List<String> blockTypes = new ArrayList<>();
    protected static List<String> entityTypes = new ArrayList<>();
    protected static List<String> containerTypes = new ArrayList<>();
    protected static List<String> redstoneTypes = new ArrayList<>();
    protected static List<String> damageTypes = new ArrayList<>();

    public static List<String> all() {
        return allTypes;
    }

    public static List<String> blocks() {
        return blockTypes;
    }

    public static List<String> entities() {
        return entityTypes;
    }

    public static List<String> containers() {
        return containerTypes;
    }

    public static List<String> redstone() {
        return redstoneTypes;
    }

    public static List<String> damage() {
        return damageTypes;
    }


    public abstract void populateTypes();


}
