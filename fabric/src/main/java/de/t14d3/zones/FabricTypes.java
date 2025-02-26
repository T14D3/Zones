package de.t14d3.zones;

import de.t14d3.zones.utils.Types;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.registry.RegistryKeys;

public class FabricTypes extends Types {
    private final ZonesFabric mod;

    public FabricTypes(ZonesFabric mod) {
        super();
        this.mod = mod;
    }

    @Override
    public void populateTypes() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            server.getRegistryManager().getOrThrow(RegistryKeys.BLOCK).forEach(block -> {
                blockTypes.add(block.getRegistryEntry().getIdAsString().replace("minecraft:", ""));
                allTypes.add(block.getRegistryEntry().getIdAsString().replace("minecraft:", ""));
                if (block.getDefaultState().hasBlockEntity()) {
                    containerTypes.add(block.getRegistryEntry().getIdAsString().replace("minecraft:", ""));
                }
                if (block.getDefaultState().emitsRedstonePower()) {
                    redstoneTypes.add(block.getRegistryEntry().getIdAsString().replace("minecraft:", ""));
                }
            });
            server.getRegistryManager().getOrThrow(RegistryKeys.ENTITY_TYPE).forEach(entity -> {
                entityTypes.add(entity.getRegistryEntry().getIdAsString().replace("minecraft:", ""));
                allTypes.add(entity.getRegistryEntry().getIdAsString().replace("minecraft:", ""));
            });
        });
    }
}
