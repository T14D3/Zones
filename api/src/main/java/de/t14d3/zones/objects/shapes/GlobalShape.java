package de.t14d3.zones.objects.shapes;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;

import java.util.Map;

/**
 * Global shape that covers the entire world.
 * Used for global/default permissions that apply everywhere.
 */
public class GlobalShape implements ShapeProvider {

    private static final Gson gson = new Gson();

    private final RWorldRef world;

    public GlobalShape(RWorldRef world) {
        this.world = world;
    }

    @Override
    public boolean contains(RBlockPos location) {
        // Global shape contains all locations in its world
        return true;
    }

    @Override
    public boolean intersects(ShapeProvider other) {
        // Global shape intersects with any shape in the same world
        if (other == null || other.getWorld() == null) return false;
        return world.identifier().equals(other.getWorld().identifier());
    }

    @Override
    public RWorldRef getWorld() {
        return world;
    }


    @Override
    public String toJson() {
        return "{\"world\":\"" + getWorld().identifier() + "\"}";
    }

    public static GlobalShape fromJson(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) gson.fromJson(json, Map.class);
            String worldId = (String) map.get("world");
            if (worldId == null || worldId.isBlank()) {
                throw new IllegalArgumentException("Missing world id for GlobalShape");
            }
            RWorldRef world = new RWorldRef(null, worldId);
            return new GlobalShape(world);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize GlobalShape from JSON", e);
        }
    }
}
