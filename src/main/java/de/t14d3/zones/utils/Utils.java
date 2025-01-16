package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility methods for the plugin.
 * <p>
 * Currently only contains methods for checking if a BlockState or BlockData is a container or powerable.
 */
public class Utils {
    private final Zones plugin;

    /**
     * Constructor for Utility methods.
     *
     * @param plugin The plugin instance.
     */
    public Utils(Zones plugin) {
        this.plugin = plugin;
    }

    public static boolean isContainer(BlockState state) {
        return state instanceof Container;
    }

    public static boolean isPowerable(BlockData data) {
        return data instanceof Powerable;
    }

    public PersistentDataContainer getPDC(Player player) {
        return player.getPersistentDataContainer();
    }

    public static void setPDC(Player player, String key, String value) {
        player.getPersistentDataContainer().set(new NamespacedKey("zones", key), PersistentDataType.STRING, value);
    }

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

    public static Map<String, String> defaultFlags() {
        Map<String, String> flags = new HashMap<>();
        flags.put("break", "Allows breaking blocks");
        flags.put("place", "Allows placing blocks");
        flags.put("interact", "Allows interacting");
        flags.put("container", "Allows opening containers");
        flags.put("redstone", "Allows interacting with redstone");
        flags.put("entity", "Allows interacting with entities");
        flags.put("ignite", "Allows igniting tnt");
        flags.put("damage", "Allows damaging entities");
        flags.put("group", "Add a group to the player");
        return flags;
    }
}
