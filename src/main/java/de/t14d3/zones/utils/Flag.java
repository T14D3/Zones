package de.t14d3.zones.utils;

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

  private final Class<?>[] allowedExtra;

  public Flag(String name, String description, boolean defaultValue, Class<?>[] allowedExtra) {
        this.name = name.toUpperCase();
        this.description = description;
        this.defaultValue = defaultValue;
    this.allowedExtra = allowedExtra;
    }

  public Flag(String name, String description, boolean defaultValue) {
    this(name, description, defaultValue, null);
  }

    public Flag(String name, String description) {
    this(name, description, false, null);
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

  public Class<?>[] getAllowedExtra() {
    return allowedExtra;
  }
}
