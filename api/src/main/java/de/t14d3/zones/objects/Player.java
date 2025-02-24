package de.t14d3.zones.objects;

import de.t14d3.zones.Zones;

import java.util.UUID;

public class Player {
    private final UUID uuid;
    private final String name;

    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public static Player of(org.bukkit.entity.Player player) {
        return new Player(player.getUniqueId(), player.getName());
    }

    public UUID getUUID() {
        return uuid;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean hasPermission(String permission) {
        return Zones.getInstance().getPlatform().hasPermission(this, permission);
    }
}
