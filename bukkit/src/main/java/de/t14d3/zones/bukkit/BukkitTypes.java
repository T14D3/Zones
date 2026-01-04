package de.t14d3.zones.bukkit;

import de.t14d3.zones.utils.Types;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;

public class BukkitTypes extends Types {

    public BukkitTypes() {
        super();
    }

    @Override
    public void populateTypes() {
        allTypes = new ArrayList<>();
        blockTypes = new ArrayList<>();
        entityTypes = new ArrayList<>();
        containerTypes = new ArrayList<>();
        redstoneTypes = new ArrayList<>();
        damageTypes = new ArrayList<>();

        BuiltInRegistries.BLOCK.forEach(block -> {
            ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
            if (key == null) return;
            String id = key.getPath();

            blockTypes.add(id);
            allTypes.add(id);

            BlockState state = block.defaultBlockState();
            if (state.hasBlockEntity() && block instanceof EntityBlock entityBlock) {
                var blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, state);
                if (blockEntity instanceof Container) {
                    containerTypes.add(id);
                }
            }

            if (state.isSignalSource()) {
                redstoneTypes.add(id);
            }
        });

        BuiltInRegistries.ENTITY_TYPE.forEach(entityType -> {
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            if (key == null) return;
            String id = key.getPath();

            entityTypes.add(id);
            allTypes.add(id);
        });

    }
}
