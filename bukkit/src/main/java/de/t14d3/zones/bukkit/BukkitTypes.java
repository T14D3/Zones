package de.t14d3.zones.bukkit;

import de.t14d3.zones.utils.Types;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BukkitTypes extends Types {

    public BukkitTypes() {
        super();
    }

    @Override
    public void populateTypes() {

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
