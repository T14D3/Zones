package de.t14d3.zones.fabric;

import de.t14d3.zones.utils.Types;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.registries.Registries;

public class FabricTypes extends Types {
    private final ZonesFabric mod;

    public FabricTypes(ZonesFabric mod) {
        super();
        this.mod = mod;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void populateTypes() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            server.registryAccess().lookupOrThrow(Registries.BLOCK).forEach(block -> {
                blockTypes.add(block.getDescriptionId().replace("minecraft:", ""));
                allTypes.add(block.getDescriptionId().replace("minecraft:", ""));
                if (block.defaultBlockState().hasBlockEntity()) {
                    containerTypes.add(block.getDescriptionId().replace("minecraft:", ""));
                }
                if (block.defaultBlockState().isSignalSource()) {
                    redstoneTypes.add(block.getDescriptionId().replace("minecraft:", ""));
                }
            });
            server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).forEach(entity -> {
                entityTypes.add(entity.getDescriptionId().replace("minecraft:", ""));
                allTypes.add(entity.getDescriptionId().replace("minecraft:", ""));
            });
        });
    }
}
