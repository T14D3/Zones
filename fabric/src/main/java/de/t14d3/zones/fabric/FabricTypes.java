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

    @Override
    public void populateTypes() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            server.registryAccess().lookupOrThrow(Registries.BLOCK).forEach(block -> {
                final String id = block.getDescriptionId().replace("block.minecraft.", "");
                blockTypes.add(id);
                blockTypes.add("!" + id);
                allTypes.add(id);
                allTypes.add("!" + id);
                if (block.defaultBlockState().hasBlockEntity()) {
                    containerTypes.add(id);
                    containerTypes.add("!" + id);
                }
                if (block.defaultBlockState().isSignalSource()) {
                    redstoneTypes.add(id);
                    redstoneTypes.add("!" + id);
                }
            });
            server.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE).forEach(entity -> {
                final String id = entity.getDescriptionId().replace("entity.minecraft.", "");
                entityTypes.add(id);
                entityTypes.add("!" + id);
                allTypes.add(id);
                allTypes.add("!" + id);
            });
        });
    }
}
