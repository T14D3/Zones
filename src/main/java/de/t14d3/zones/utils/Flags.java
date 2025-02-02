package de.t14d3.zones.utils;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Flags {

    public static Flag BREAK;
    public static Flag PLACE;
    public static Flag INTERACT;
    public static Flag CONTAINER;
    public static Flag REDSTONE;
    public static Flag ENTITY;
    public static Flag IGNITE;
    public static Flag DAMAGE;
    public static Flag GROUP;
    public static Flag SPAWN;
    public static Flag EXPLOSION;
    public static Flag CREATE;
    public static Flag DESTROY;
    public static Flag TRANSFORM;
    public static Flag SPREAD;
    public static Flag RELOCATE;
    public static Flag PHYSICS;
    private static List<Flag> flags;

    public Flags() {
        flags = new ArrayList<>();


        BREAK = registerFlag(new Flag("break", "Allows breaking blocks", false));
        PLACE = registerFlag(new Flag("place", "Allows placing blocks", false));
        INTERACT = registerFlag(new Flag("interact", "Allows interacting", false));
        CONTAINER = registerFlag(new Flag("container", "Allows opening containers", false));
        REDSTONE = registerFlag(new Flag("redstone", "Allows interacting with redstone", false));
        ENTITY = registerFlag(new Flag("entity", "Allows interacting with entities", false));
        IGNITE = registerFlag(new Flag("ignite", "Allows igniting tnt", false));
        DAMAGE = registerFlag(new Flag("damage", "Allows damaging entities", false));

        GROUP = registerFlag(new Flag("group", "Add a group to the player"));

        SPAWN = registerFlag(new Flag("spawn", "Controls the spawning of entities", true));
        EXPLOSION = registerFlag(new Flag("explosion", "Controls the explosion of entities", true,
                new Class[]{Material.class, EntityType.class}));
        CREATE = registerFlag(new Flag("create", "Controls the creation of blocks through world events", true));
        DESTROY = registerFlag(new Flag("destroy", "Controls the removal of blocks through world events", true));
        TRANSFORM = registerFlag(
                new Flag("transform", "Controls the transformation of blocks into other blocks", true));
        SPREAD = registerFlag(new Flag("spread", "Controls the spread of blocks through world events", true));
        RELOCATE = registerFlag(new Flag("relocate", "Controls the ability for blocks to change their location", true));

        PHYSICS = registerFlag(new Flag("physics", "Controls the physics of blocks", true));
    }

    /**
     * Register a flag with a fallback description
     *
     * @param name Flag to register
     * @param desc Fallback description
     * @return {@code true} if it was registered, {@code false} if it already exists
     * @see #registerFlag(String, String, boolean)
     */
    public static boolean registerFlag(@NotNull String name, @NotNull String desc) {
        return registerFlag(name, desc, false);
    }

    /**
     * Register a flag with a fallback description
     *
     * @param name      Flag to register
     * @param desc      Fallback description
     * @param overwrite Whether to overwrite an existing flag
     * @return {@code true} if it was registered, {@code false} if it already exists and {@code overwrite} is {@code false}
     * @see #registerFlag(String, String)
     */
    public static boolean registerFlag(@NotNull String name, @NotNull String desc, boolean overwrite) {
        if (flags.stream().anyMatch(f -> f.name().equals(name)) && !overwrite) {
            return false;
        } else {
            flags.add(new Flag(name, desc));
            return true;
        }
    }

    public static Flag registerFlag(@NotNull Flag flag) {
        return registerFlag(flag, false);
    }

    public static Flag registerFlag(@NotNull Flag flag, boolean overwrite) {
        if (flags.stream().anyMatch(f -> f.name().equals(flag.name()))) {
            if (overwrite) {
                flags.removeIf(f -> f.name().equals(flag.name()));
                flags.add(flag);
                return flag;
            }
            return null;
        } else {
            flags.add(flag);
            return flag;
        }
    }

    /**
     * Immutable List of all flags recognized by the plugin
     *
     * @return List of {@link Flag} objects
     */
    public static List<Flag> getFlags() {
        return Collections.unmodifiableList(flags);
    }

    public static Flag getFlag(String name) {
        return flags.stream().filter(f -> f.name().equals(name)).findFirst().orElse(new Flag(name, "Unknown"));
    }


}
