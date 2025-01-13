package de.t14d3.zones.listeners;

import de.t14d3.zones.Zones;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkEventListener implements Listener {

    private final Zones plugin;

    public ChunkEventListener(Zones plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        plugin.getPermissionManager().invalidateInteractionCacheForChunk(event.getChunk().getX(), event.getChunk().getZ(), event.getWorld().getName());
    }
}
