package de.t14d3.zones.utils;

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

// TODO: Either make this work platform-independent, or move to bukkit platform implementation and create fabric equivalent

/**
 * Utility methods for the plugin.
 * <p>
 * Currently only contains methods for checking if a BlockState or BlockData is a container or powerable.
 */
public class Utils {
    public static List<OfflinePlayer> offlinePlayers = new ArrayList<>();
    public static Map<UUID, String> playerNames = new HashMap<>();

    public static boolean isContainer(BlockState state) {
        return state instanceof Container;
    }

    public static boolean isPowerable(BlockData data) {
        return data instanceof Powerable;
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

    public PersistentDataContainer getPDC(Player player) {
        return player.getPersistentDataContainer();
    }

    public void populatePlayers() {
        offlinePlayers.addAll(Arrays.asList(Bukkit.getOfflinePlayers()));
        for (OfflinePlayer player : offlinePlayers) {
            playerNames.put(player.getUniqueId(), player.getName());
        }
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
                mode = Modes.valueOf(player.getPersistentDataContainer()
                        .get(new NamespacedKey("zones", "mode"), PersistentDataType.STRING));
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
