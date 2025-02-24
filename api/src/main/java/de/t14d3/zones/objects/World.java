package de.t14d3.zones.objects;

import java.util.UUID;

public class World {
    private final String name;
    private final UUID uid;

    private World(String name, UUID uid) {
        this.name = name;
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public UUID getUID() {
        return uid;
    }

    public static World of(org.bukkit.World world) {
        return new World(world.getName(), world.getUID());
    }
}
