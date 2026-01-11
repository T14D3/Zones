package de.t14d3.zones.objects.shapes;

import com.google.gson.Gson;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.objects.Box;

import java.util.HashMap;
import java.util.Map;

/**
 * Cuboid (rectangular) shape implementation using Box for AABB checks.
 */
public class CuboidShape implements ShapeProvider {

    private static final Gson gson = new Gson();

    private final Box box;

    public CuboidShape(Box box) {
        this.box = box;
    }

    public CuboidShape(RBlockPos min, RBlockPos max, RWorldRef world, boolean normalize) {
        this.box = new Box(min, max, world, normalize);
    }

    public CuboidShape(RBlockPos min, RBlockPos max, RWorldRef world) {
        this(min, max, world, true);
    }

    @Override
    public boolean contains(RBlockPos location) {
        return box.contains(location);
    }

    @Override
    public boolean intersects(ShapeProvider other) {
        if (other instanceof CuboidShape cuboid) {
            return box.intersects(cuboid.box);
        } else if (other instanceof GlobalShape global) {
            // Global always intersects
            return true;
        }
        return false;
    }

    @Override
    public RWorldRef getWorld() {
        return box.getWorld();
    }

    @Override
    public Box getBoundingBox() {
        return box;
    }

    @Override
    public String toJson() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Integer> minMap = new HashMap<>();
        minMap.put("x", box.getMin().x());
        minMap.put("y", box.getMin().y());
        minMap.put("z", box.getMin().z());
        data.put("min", minMap);
        Map<String, Integer> maxMap = new HashMap<>();
        maxMap.put("x", box.getMax().x());
        maxMap.put("y", box.getMax().y());
        maxMap.put("z", box.getMax().z());
        data.put("max", maxMap);
        data.put("world", getWorld().identifier());
        return gson.toJson(data);
    }

    public static CuboidShape fromJson(String json) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) gson.fromJson(json, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Double> minMap = (Map<String, Double>) map.get("min");
            @SuppressWarnings("unchecked")
            Map<String, Double> maxMap = (Map<String, Double>) map.get("max");
            String worldId = (String) map.get("world");

            RBlockPos min = new RBlockPos(minMap.get("x").intValue(), minMap.get("y").intValue(),
                    minMap.get("z").intValue());
            RBlockPos max = new RBlockPos(maxMap.get("x").intValue(), maxMap.get("y").intValue(),
                    maxMap.get("z").intValue());

            if (worldId == null || worldId.isBlank()) {
                throw new IllegalArgumentException("Missing world id for CuboidShape");
            }
            RWorldRef world = new RWorldRef(null, worldId);
            return new CuboidShape(min, max, world);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize CuboidShape from JSON", e);
        }
    }

    public RBlockPos getMin() {
        return box.getMin();
    }

    public RBlockPos getMax() {
        return box.getMax();
    }

    public int getVolume() {
        return box.getVolume();
    }

    public int getArea() {
        return box.getArea();
    }
}
