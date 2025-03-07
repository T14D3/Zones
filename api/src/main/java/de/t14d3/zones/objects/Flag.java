package de.t14d3.zones.objects;

import de.t14d3.zones.permissions.flags.DefaultFlagHandler;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.permissions.flags.IFlagHandler;
import de.t14d3.zones.utils.Types;

import java.util.List;

/**
 * A flag is a permission that can be set for a region.
 * Flags are registered in the {@link Flags} class, and can be accessed using the {@link Flags#getFlag(String)} method.
 * New flags always have a name and a fallback description (in case the translation ís missing).
 * Flags can be registered with a default value, which is used when a region doesn't have a value set for it, which defaults to false.
 */
public class Flag implements IFlagHandler {
    private final String name;
    private final String description;
    private final IFlagHandler customHandler; // Optional custom logic

    public Flag(String name, String description) {
        this(name, description, new DefaultFlagHandler(false, Types.all()));
    }

    // Constructor for custom handlers
    public Flag(String name, String description, IFlagHandler customHandler) {
        super();
        this.name = name;
        this.description = description;
        this.customHandler = customHandler;
    }

    public String name() {
        return name;
    }

    public List<String> getValidValues() {
        return customHandler.getValidValues();
    }

    public boolean getDefaultValue() {
        return this.customHandler.getDefaultValue();
    }

    public boolean getDefaultValue(Object... optional) {
        return this.customHandler.getDefaultValue(optional);
    }

    public String getDescription() {
        return description;
    }

    public IFlagHandler getCustomHandler() {
        return customHandler;
    }
}
