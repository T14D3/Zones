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

    public static World of(String name, UUID uid) {
        return new World(name, uid);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        World world = (World) o;
        return uid.equals(world.uid);
    }
}
