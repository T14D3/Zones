package de.t14d3.zones.objects;

import de.t14d3.zones.Zones;

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
        Player player = null;
        for (Player p : knownPlayers) {
            if (p.getUUID().equals(uuid)) {
                player = p;
                break;
            }
        }
        if (player == null) {
            player = new Player(uuid, name);
            knownPlayers.add(player);
        }
        return player;
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
