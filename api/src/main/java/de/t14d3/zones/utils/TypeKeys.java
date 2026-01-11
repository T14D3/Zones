package de.t14d3.zones.utils;

import java.util.Locale;

/**
 * Normalizes type keys (blocks/entities/items/etc) to the shortest stable key used by Zones.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code minecraft:stone -> stone}</li>
 *   <li>{@code my_mod:cool_block -> cool_block}</li>
 *   <li>{@code block.minecraft.stone -> stone}</li>
 *   <li>{@code entity.minecraft.zombie -> zombie}</li>
 * </ul>
 */
public final class TypeKeys {
    private TypeKeys() {
    }

    public static String normalize(String rawKey) {
        if (rawKey == null) return "";
        String key = rawKey.trim().toLowerCase(Locale.ROOT);
        if (key.isEmpty()) return "";

        int colon = key.indexOf(':');
        if (colon >= 0 && colon + 1 < key.length()) {
            key = key.substring(colon + 1);
        }

        // Common translation keys use dot-delimited namespaces (e.g. block.minecraft.stone)
        // - drop everything up to the last dot to keep the shortest stable identifier.
        int lastDot = key.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < key.length()) {
            key = key.substring(lastDot + 1);
        }

        return key;
    }
}

