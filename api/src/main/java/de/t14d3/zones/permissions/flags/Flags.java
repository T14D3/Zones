package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.utils.Types;

import java.util.List;

@SuppressWarnings("ClassWithTooManyFields")
public class Flags {

    public static Flag FALLBACK;

    public static Flag BREAK;
    public static Flag PLACE;
    public static Flag INTERACT;
    public static Flag CONTAINER;
    public static Flag REDSTONE;
    public static Flag ENTITY;
    public static Flag IGNITE;
    public static Flag DAMAGE;
    public static Flag ROLE;
    public static Flag GROUP;
    public static Flag SPAWN;
    public static Flag EXPLOSION;
    public static Flag CREATE;
    public static Flag DESTROY;
    public static Flag TRANSFORM;
    public static Flag SPREAD;
    public static Flag RELOCATE;
    public static Flag PHYSICS;
    public static Flag HURT;

    public Flags() {
        FALLBACK = FlagRegistry.registerFlag("fallback", "Fallback description");

        ROLE = FlagRegistry.registerFlag("role", "Defines a member role",
                new DefaultFlagHandler(false, List.of("owner", "admin")),
                null, FlagValueKind.STRING_SET);

        BREAK = FlagRegistry.registerFlag("break", "Allows breaking blocks",
                new DefaultFlagHandler(false, Types.blocks()));
        PLACE = FlagRegistry.registerFlag("place", "Allows placing blocks",
                new DefaultFlagHandler(false, Types.blocks()));
        INTERACT = FlagRegistry.registerFlag("interact", "Allows interacting",
                new DefaultFlagHandler(false, Types.all()));
        CONTAINER = FlagRegistry.registerFlag("container", "Allows opening containers",
                new DefaultFlagHandler(false, Types.containers()));
        REDSTONE = FlagRegistry.registerFlag("redstone", "Allows interacting with redstone",
                new DefaultFlagHandler(false, Types.redstone()));
        ENTITY = FlagRegistry.registerFlag("entity", "Allows interacting with entities",
                new DefaultFlagHandler(false, Types.entities()));
        IGNITE = FlagRegistry.registerFlag("ignite", "Allows igniting tnt",
                new IgnitionFlagHandler(false, Types.blocks()));
        DAMAGE = FlagRegistry.registerFlag("damage", "Allows damaging entities",
                new DefaultFlagHandler(false, Types.entities()));

        GROUP = FlagRegistry.registerFlag("group", "Add a group to the player",
                new DefaultFlagHandler(false, List.of()),
                null, FlagValueKind.STRING_SET);

        SPAWN = FlagRegistry.registerFlag("spawn", "Controls the spawning of entities",
                new DefaultFlagHandler(true, Types.entities()));
        EXPLOSION = FlagRegistry.registerFlag("explosion", "Controls the explosion of entities",
                new DefaultFlagHandler(true, Types.entities()));
        CREATE = FlagRegistry.registerFlag("create", "Controls the creation of blocks through world events",
                new DefaultFlagHandler(true, Types.blocks()));
        DESTROY = FlagRegistry.registerFlag("destroy", "Controls the removal of blocks through world events",
                new DefaultFlagHandler(true, Types.blocks()));
        TRANSFORM = FlagRegistry.registerFlag("transform", "Controls the transformation of blocks into other blocks",
                new DefaultFlagHandler(true, Types.blocks()));
        SPREAD = FlagRegistry.registerFlag("spread", "Controls the spread of blocks through world events",
                new DefaultFlagHandler(true, Types.blocks()));
        RELOCATE = FlagRegistry.registerFlag("relocate", "Controls the ability for blocks to change their location",
                new DefaultFlagHandler(true, Types.blocks()));
        HURT = FlagRegistry.registerFlag("hurt", "Controls the ability for entities to be hurt",
                new DefaultFlagHandler(true, Types.entities()));

        PHYSICS = FlagRegistry.registerFlag("physics", "Controls the physics of blocks",
                new DefaultFlagHandler(true, Types.blocks()));
    }

    private static final class IgnitionFlagHandler extends DefaultFlagHandler {

        public IgnitionFlagHandler(boolean defaultValue, List<String> validValues) {
            super(defaultValue, validValues);
        }

        @Override
        public boolean getDefaultValue(FlagContext context) {
            if (context != null && context.defaultDecisionOverride() != null) return context.defaultDecisionOverride();
            return super.getDefaultValue(FlagContext.none());
        }
    }

    /**
     * Register a flag with a fallback description
     *
     * @param name Flag to register
     * @param desc Fallback description
     * @return {@code true} if it was registered, {@code false} if it already exists
     */
    public static boolean registerFlag(String name, String desc) {
        return registerFlag(name, desc, false);
    }

    /**
     * Register a flag with a fallback description
     *
     * @param name      Flag to register
     * @param desc      Fallback description
     * @param overwrite Whether to overwrite an existing flag
     * @return {@code true} if it was registered, {@code false} if it already exists and {@code overwrite} is {@code false}
     */
    public static boolean registerFlag(String name, String desc, boolean overwrite) {
        if (FlagRegistry.getFlags().stream().anyMatch(f -> f.name().equals(name)) && !overwrite) {
            return false;
        } else {
            FlagRegistry.registerFlag(name, desc);
            return true;
        }
    }

    public static Flag registerFlag(Flag flag) {
        return registerFlag(flag, false);
    }

    public static Flag registerFlag(Flag flag, boolean overwrite) {
        if (FlagRegistry.getFlags().stream().anyMatch(f -> f.name().equals(flag.name()))) {
            if (overwrite) {
                FlagRegistry.registerFlag(flag);
                return flag;
            }
            return null;
        } else {
            FlagRegistry.registerFlag(flag);
            return flag;
        }
    }

    /**
     * Immutable List of all flags recognized by the plugin
     *
     * @return List of {@link Flag} objects
     */
    public static List<Flag> getFlags() {
        return FlagRegistry.getFlags();
    }

    public static Flag getFlag(String name) {
        return FlagRegistry.getFlag(name);
    }
}
