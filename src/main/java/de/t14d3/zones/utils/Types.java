package de.t14d3.zones.utils;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for types.
 * <p>
 * Contains lists for blocks and entities for their respective {@link Actions} type.
 */
public class Types {


    public List<String> allTypes = new ArrayList<>();
    public List<String> blockTypes = new ArrayList<>();
    public List<String> entityTypes = new ArrayList<>();
    public List<String> containerTypes = new ArrayList<>();
    public List<String> redstoneTypes = new ArrayList<>();


    /**
     * Populates the type lists.
     * Called on plugin enable,
     * should not be called again.
     */
    @SuppressWarnings("UnstableApiUsage")
    @ApiStatus.Internal
    public void populateTypes() {
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                allTypes.add(material.name());
                allTypes.add("!" + material.name());
            }
        }
        for (EntityType entityType : EntityType.values()) {
            allTypes.add(entityType.name());
            allTypes.add("!" + entityType.name());
        }
        allTypes.add("TRUE");
        allTypes.add("FALSE");

        // Populate blockTypes
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                blockTypes.add(material.name());
                blockTypes.add("!" + material.name());
            }
        }
        blockTypes.add("TRUE");
        blockTypes.add("FALSE");

        // Populate entityTypes
        for (EntityType entityType : EntityType.values()) {
            entityTypes.add(entityType.name());
            entityTypes.add("!" + entityType.name());
        }
        entityTypes.add("TRUE");
        entityTypes.add("FALSE");

        // Populate containerTypes
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                BlockState state;
                try {
                    state = material.createBlockData().createBlockState();
                } catch (Exception ignored) {
                    continue;
                }
                if (state instanceof Container) {
                    containerTypes.add(material.name());
                    containerTypes.add("!" + material.name());
                }
            }
        }
        containerTypes.add("TRUE");
        containerTypes.add("FALSE");

        // Populate redstoneTypes
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                BlockData data;
                try {
                    data = material.createBlockData();
                } catch (Exception ignored) {
                    continue;
                }
                if (data instanceof Powerable) {
                    redstoneTypes.add(material.name());
                    redstoneTypes.add("!" + material.name());
                }
            }
        }
        redstoneTypes.add("TRUE");
        redstoneTypes.add("FALSE");
    }
}
