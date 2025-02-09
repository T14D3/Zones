package de.t14d3.zones.utils;

import de.t14d3.zones.permissions.Flag;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
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
 * Contains lists for blocks and entities for their respective {@link Flag} type.
 */
public class Types {


    static List<String> allTypes = new ArrayList<>();
    static List<String> blockTypes = new ArrayList<>();
    static List<String> entityTypes = new ArrayList<>();
    static List<String> containerTypes = new ArrayList<>();
    static List<String> redstoneTypes = new ArrayList<>();
    static List<String> damageTypes = new ArrayList<>();


    /**
     * Populates the type lists.
     * Called on plugin enable,
     * should not be called again.
     */
    @SuppressWarnings("UnstableApiUsage")
    @ApiStatus.Internal
    public static void populateTypes() {
        for (Material material : Material.values()) {
            if (material.isBlock() && !material.name().startsWith("LEGACY_")) {
                allTypes.add(material.name());
                allTypes.add("!" + material.name());
            }
        }
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().startsWith("LEGACY_")) {
                continue;
            }
            allTypes.add(entityType.name());
            allTypes.add("!" + entityType.name());
        }
        allTypes.add("true");
        allTypes.add("false");

        // Populate blockTypes
        for (Material material : Material.values()) {
            if (material.isBlock() && !material.name().startsWith("LEGACY_")) {
                blockTypes.add(material.name().toLowerCase());
                blockTypes.add("!" + material.name().toLowerCase());
            }
        }
        blockTypes.add("true");
        blockTypes.add("false");

        // Populate entityTypes
        for (EntityType entityType : EntityType.values()) {
            if (entityType.name().startsWith("LEGACY_")) {
                continue;
            }
            entityTypes.add(entityType.name());
            entityTypes.add("!" + entityType.name());
        }
        entityTypes.add("true");
        entityTypes.add("false");

        // Populate containerTypes
        for (Material material : Material.values()) {
            if (material.isBlock() && !material.name().startsWith("LEGACY_")) {
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
            if (material.isBlock() && !material.name().startsWith("LEGACY_")) {
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
        redstoneTypes.add("true");
        redstoneTypes.add("false");

        // Populate damageTypes
        RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).forEach(damageType -> {
            damageTypes.add(damageType.getTranslationKey());
            damageTypes.add("!" + damageType.getTranslationKey());
        });
    }

    public static List<String> all() {
        return allTypes;
    }

    public static List<String> blocks() {
        return blockTypes;
    }

    public static List<String> entities() {
        return entityTypes;
    }

    public static List<String> containers() {
        return containerTypes;
    }

    public static List<String> redstone() {
        return redstoneTypes;
    }

    public static List<String> damage() {
        return damageTypes;
    }

}
