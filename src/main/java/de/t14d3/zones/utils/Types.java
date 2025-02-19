package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.flags.Flag;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Zones plugin;

    public Types(Zones plugin) {
        this.plugin = plugin;
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

    /**
     * Populates the type lists.
     * Called on plugin enable,
     * should not be called again.
     */
    @SuppressWarnings("UnstableApiUsage")
    @ApiStatus.Internal
    public void populateTypes() {
        plugin.getLogger()
                .info("Populating Block- and EntityTypes - may take a couple of seconds and will trigger a CraftLegacy warning");

        blockTypes = Arrays.stream(Material.values())
                .parallel()
                .filter(material -> !material.isLegacy() && material.isBlock())
                .flatMap(material -> Stream.of(material.name().toLowerCase(), "!" + material.name().toLowerCase()))
                .collect(Collectors.toList());

        entityTypes = Arrays.stream(EntityType.values())
                .parallel()
                .flatMap(
                        entityType -> Stream.of(entityType.name().toLowerCase(),
                                "!" + entityType.name().toLowerCase()))
                .collect(Collectors.toList());

        allTypes = new ArrayList<>();
        allTypes.addAll(blockTypes);
        allTypes.addAll(entityTypes);

        allTypes.addAll(List.of("owner", "admin", "true", "false"));
        blockTypes.addAll(List.of("owner", "admin", "true", "false"));
        entityTypes.addAll(List.of("owner", "admin", "true", "false"));


        new Thread(() -> {
            containerTypes = Arrays.stream(Material.values())
                    .parallel()
                    .filter(material -> material.isBlock() && !material.isLegacy())
                    .flatMap(material -> {
                        try {
                            BlockData data = material.createBlockData();
                            BlockState state = data.createBlockState();
                            return (state instanceof Container) ? Stream.of(material) : Stream.empty();
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    })
                    .flatMap(material -> Stream.of(material.name().toLowerCase(), "!" + material.name().toLowerCase()))
                    .collect(Collectors.toList());
            containerTypes.addAll(List.of("owner", "admin", "true", "false"));

            redstoneTypes = Arrays.stream(Material.values())
                    .parallel()
                    .filter(material -> material.isBlock() && !material.isLegacy())
                    .flatMap(material -> {
                        try {
                            BlockData data = material.createBlockData();
                            return (data instanceof Powerable) ? Stream.of(material) : Stream.empty();
                        } catch (Exception e) {
                            return Stream.empty();
                        }
                    })
                    .flatMap(material -> Stream.of(material.name().toLowerCase(), "!" + material.name().toLowerCase()))
                    .collect(Collectors.toList());
            redstoneTypes.addAll(List.of("owner", "admin", "true", "false"));
        }).start();

        damageTypes = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).stream()
                .parallel()
                .flatMap(damageType -> Stream.of(
                        damageType.getTranslationKey().toLowerCase(),
                        "!" + damageType.getTranslationKey().toLowerCase()
                ))
                .collect(Collectors.toList());
        damageTypes.addAll(List.of("owner", "admin", "true", "false"));
    }

}
