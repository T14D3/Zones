package de.t14d3.zones.brigadier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum SubCommands {
    INFO(Component.text("Shows information about a region").color(NamedTextColor.GOLD)),
    LIST(Component.text("Lists all regions").color(NamedTextColor.GOLD)),
    DELETE(Component.text("Deletes a region").color(NamedTextColor.DARK_RED)),
    CREATE(Component.text("Creates a new region").color(NamedTextColor.GREEN)),
    EXPAND(Component.text("Expands a region").color(NamedTextColor.AQUA)),
    SELECT(Component.text("Visually selects a region").color(NamedTextColor.LIGHT_PURPLE)),
    SET(Component.text("Sets permissions for a region").color(NamedTextColor.BLUE)),
    CANCEL(Component.text("Cancels the current operation").color(NamedTextColor.RED)),
    RENAME(Component.text("Renames a region").color(NamedTextColor.YELLOW)),
    SUBCREATE(Component.text("Creates a sub-region").color(NamedTextColor.DARK_PURPLE)),
    SAVE(Component.text("Saves all regions to file").color(NamedTextColor.DARK_GREEN)),
    LOAD(Component.text("Loads all regions from file").color(NamedTextColor.DARK_AQUA));


    private final Component info;

    SubCommands(Component info) {
        this.info = info;
    }

    public Component getInfo() {
        return info;
    }

    public static SubCommands get(String input) {
        return SubCommands.valueOf(input.split(" ")[0]);
    }
}