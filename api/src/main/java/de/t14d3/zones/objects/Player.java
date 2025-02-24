package de.t14d3.zones.objects;

import de.t14d3.zones.Zones;
import net.kyori.adventure.audience.Audience;

import java.util.UUID;

public class Player implements Audience {
    private final UUID uuid;
    private final String name;
    private Box selection;

    public Player(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
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

    public Box getSelection() {
        return selection;
    }

    public void setSelection(Box selection) {
        this.selection = selection;
    }
}
