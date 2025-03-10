package de.t14d3.zones.utils;

import de.t14d3.zones.objects.Player;

/**
 * Utility methods for the plugin.
 */
public class Utils {

    public enum SavingModes {
        SHUTDOWN,
        MODIFIED,
        PERIODIC;

        public static SavingModes fromString(String string) {
            SavingModes mode;
            try {
                mode = SavingModes.valueOf(string.toUpperCase());
            } catch (IllegalArgumentException e) {
                mode = SavingModes.MODIFIED;
            }
            return mode;
        }
    }

    public enum SelectionMode {
        CUBOID_2D("2D"),
        CUBOID_3D("3D");

        final String name;

        SelectionMode(String name) {
            this.name = name;
        }

        public static SelectionMode getPlayerMode(Player player) {
            SelectionMode mode;
            try {
                mode = SelectionMode.valueOf(player.getMetadata("mode").toUpperCase());
            } catch (IllegalArgumentException | NullPointerException e) {
                mode = SelectionMode.CUBOID_2D;
            }
            return mode;
        }

        public static SelectionMode getMode(String mode) {
            SelectionMode selectionMode;
            try {
                selectionMode = SelectionMode.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                selectionMode = SelectionMode.CUBOID_2D;
            }
            return selectionMode;
        }

        public String getName() {
            return this.name;
        }
    }
}
