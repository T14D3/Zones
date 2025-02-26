package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerRepository {
    private static final Set<Player> knownPlayers = new HashSet<>();

    protected static Player add(Player player) {
        knownPlayers.add(player);
        return player;
    }

    public static Player getOrAdd(String name, UUID uuid) {
        Player player = PlayerRepository.get(name, uuid);
        if (player == null) {
            player = new Player(uuid, name);
            PlayerRepository.add(player);
        }
        return player;
    }

    public static Player get(String name, UUID uuid) {
        for (Player player : knownPlayers) {
            if (player.getName().equals(name) && player.getUUID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    public static Player get(UUID uuid) {
        for (Player player : knownPlayers) {
            if (player.getUUID().equals(uuid)) {
                return player;
            }
        }
        Player player = Zones.getInstance().getPlatform().getPlayer(uuid);
        if (player != null) {
            PlayerRepository.add(player);
        }
        return player;
    }

    public static void remove(UUID uuid) {
        knownPlayers.removeIf(player -> player.getUUID().equals(uuid));
    }

    public static Set<Player> getPlayers() {
        return knownPlayers;
    }
}
