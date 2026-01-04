package de.t14d3.zones.permissions.flags;

import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.PermissionKeyRegistry;

import java.util.*;

/**
 * Registry for flags. Allows registering new flags and their platform-specific logic.
 */
public class FlagRegistry {
    private static final List<Flag> flags = new ArrayList<>();
    private static final Map<String, Flag> flagsByName = new HashMap<>();

    public static Flag registerFlag(String name, String description) {
        return registerFlag(name, description, null, null, FlagValueKind.TARGET_DECISION);
    }

    public static Flag registerFlag(String name, String description, IFlagHandler handler) {
        return registerFlag(name, description, handler, null, FlagValueKind.TARGET_DECISION);
    }

    public static Flag registerFlag(String name, String description, IFlagHandler handler, Flag.CanSetRule canSetRule) {
        return registerFlag(name, description, handler, canSetRule, FlagValueKind.TARGET_DECISION);
    }

    public static Flag registerFlag(String name, String description, IFlagHandler handler, Flag.CanSetRule canSetRule, FlagValueKind valueKind) {
        String normalized = normalizeName(name);
        int id = PermissionKeyRegistry.instance().getOrCreateId(normalized);
        Flag flag = new Flag(id, normalized, description, handler, canSetRule, valueKind);
        flags.add(flag);
        flagsByName.put(normalized, flag);
        return flag;
    }

    public static Flag registerFlag(Flag flag) {
        String normalized = normalizeName(flag.name());
        int id = PermissionKeyRegistry.instance().getOrCreateId(normalized);
        Flag wrapped = new Flag(id, normalized, flag.description(), flag.handler(), flag.canSetRule(),
                flag.valueKind());
        flags.add(wrapped);
        flagsByName.put(normalized, wrapped);
        return wrapped;
    }

    public static List<Flag> getFlags() {
        return Collections.unmodifiableList(flags);
    }

    public static Flag getFlag(String name) {
        return flagsByName.get(normalizeName(name));
    }

    private static String normalizeName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase();
    }
}
