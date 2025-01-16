package de.t14d3.zones.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Flags {

    private Map<String, String> flags = new HashMap<>();

    public Flags() {
    }

    /**
     * Register a flag with a fallback description
     *
     * @param flag Flag to register
     * @param desc Fallback description
     * @return {@code true} if it was registered, {@code false} if it already exists
     * @see #registerFlag(String, String, boolean)
     */
    public boolean registerFlag(@NotNull String flag, @NotNull String desc) {
        return registerFlag(flag, desc, false);
    }

    /**
     * Register a flag with a fallback description
     *
     * @param flag      Flag to register
     * @param desc      Fallback description
     * @param overwrite Whether to overwrite an existing flag
     * @return {@code true} if it was registered, {@code false} if it already exists and {@code overwrite} is {@code false}
     * @see #registerFlag(String, String)
     */
    public boolean registerFlag(@NotNull String flag, @NotNull String desc, boolean overwrite) {
        if (flags.containsKey(flag) && !overwrite) {
            return false;
        }
        flags.put(flag, desc);
        return true;
    }

    /**
     * Immutable List of all flags recognized by the plugin
     *
     * @return Map of {@code <Flag, Description>}
     */
    public Map<String, String> getFlags() {
        return Collections.unmodifiableMap(flags);
    }
}
