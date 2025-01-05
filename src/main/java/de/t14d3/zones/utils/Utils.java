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
}
