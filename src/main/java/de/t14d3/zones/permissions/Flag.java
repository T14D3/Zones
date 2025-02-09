package de.t14d3.zones.permissions;

import java.util.List;

/**
 * A flag is a permission that can be set for a region.
 * Flags are registered in the {@link Flags} class, and can be accessed using the {@link Flags#getFlag(String)} method.
 * New flags always have a name and a fallback description (in case the translation ís missing).
 * Flags can be registered with a default value, which is used when a region doesn't have a value set for it, which defaults to false.
 */
public class Flag {
    private final String name;
    private final String description;
    private final boolean defaultValue;
    private final List<String> validValues;
    private final Object[] extra;

    public Flag(String name, String description, boolean defaultValue) {
        this.name = name.toLowerCase();
        this.description = description;
        this.defaultValue = defaultValue;
        this.validValues = null;
        this.extra = null;
    }

    public Flag(String name, String description, boolean defaultValue, List<String> validValues, Object... extra) {
        this.name = name.toLowerCase();
        this.description = description;
        this.defaultValue = defaultValue;
        this.validValues = validValues;
        this.extra = extra;
    }

    public Flag(String name, String description) {
        this(name, description, false);
    }

    public String name() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean getDefaultValue() {
        return defaultValue;
    }

    public List<String> getValidValues() {
        return validValues;
    }

    public Object[] getExtra() {
        return extra;
    }
}
