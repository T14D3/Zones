package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Utility methods for the plugin.
 * <p>
 * Currently only contains methods for checking if a BlockState or BlockData is a container or powerable.
 */
public class Utils {
    private final Zones plugin;

    public static List<OfflinePlayer> offlinePlayers = new ArrayList<>();
    public static Map<UUID, String> playerNames = new HashMap<>();

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

    public void populatePlayers() {
        offlinePlayers.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
        for (OfflinePlayer player : offlinePlayers) {
            playerNames.put(player.getUniqueId(), player.getName());
        }
    }

    public static OfflinePlayer getOfflinePlayer(UUID uuid) {
        for (OfflinePlayer player : offlinePlayers) {
            if (player.getUniqueId().equals(uuid)) {
                return player;
            }
        }
        offlinePlayers.add(Bukkit.getOfflinePlayer(uuid));
        return Bukkit.getOfflinePlayer(uuid);
    }

    public static String getPlayerName(UUID uuid) {
        if (playerNames.containsKey(uuid)) {
            return playerNames.get(uuid);
        }
        OfflinePlayer player = getOfflinePlayer(uuid);
        playerNames.put(uuid, player.getName());
        return player.getName();
    }

    public static List<OfflinePlayer> getOfflinePlayers() {
        return offlinePlayers;
    }

    public static List<String> getPlayerNames() {
        return new ArrayList<>(playerNames.values());
    }

    public enum Modes {
        CUBOID_2D("2D"),
        CUBOID_3D("3D");

        final String name;

        Modes(String name) {
            this.name = name;
        }

        public static Modes getPlayerMode(Player player) {
            Modes mode;
            try {
                mode = Modes.valueOf(player.getPersistentDataContainer().get(new NamespacedKey("zones", "mode"), PersistentDataType.STRING));
            } catch (IllegalArgumentException e) {
                mode = Modes.CUBOID_2D;
            }
            return mode;
        }

        public static Modes getMode(String mode) {
            Modes modes;
            try {
                modes = Modes.valueOf(mode.toUpperCase());
            } catch (IllegalArgumentException e) {
                modes = Modes.CUBOID_2D;
            }
            return modes;
        }

        public String getName() {
            return this.name;
        }
    }


}
