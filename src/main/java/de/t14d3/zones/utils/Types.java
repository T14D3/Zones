package de.t14d3.zones.utils;

import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;


public class Types {

    /**
     * List of all possible permission types/values.
     * Includes all Block Materials and EntityTypes, their negated forms
     * and the wildcards "true" and "false".
     */
    public List<String> allTypes = new ArrayList<>();
    public List<String> blockTypes = new ArrayList<>();
    public List<String> entityTypes = new ArrayList<>();
    public List<String> containerTypes = new ArrayList<>();
    public List<String> redstoneTypes = new ArrayList<>();


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
        allTypes.add("true");
        allTypes.add("false");

        // Populate blockTypes
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                blockTypes.add(material.name());
                blockTypes.add("!" + material.name());
            }
        }
        blockTypes.add("true");
        blockTypes.add("false");

        // Populate entityTypes
        for (EntityType entityType : EntityType.values()) {
            entityTypes.add(entityType.name());
            entityTypes.add("!" + entityType.name());
        }
        entityTypes.add("true");
        entityTypes.add("false");

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
        containerTypes.add("true");
        containerTypes.add("false");

        // Populate redstoneTypes
        for (Material material : Material.values()) {
            if (material.isBlock()) {
                BlockState state;
                try {
                    state = material.createBlockData().createBlockState();
                } catch (Exception ignored) {
                    continue;
                }
                if (state instanceof Powerable) {
                    redstoneTypes.add(material.name());
                    redstoneTypes.add("!" + material.name());
                }
            }
        }
        redstoneTypes.add("true");
        redstoneTypes.add("false");
    }
}
