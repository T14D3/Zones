package de.t14d3.zones.bukkit.listeners;

import de.t14d3.zones.permissions.CacheUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;

public class ChunkEventListener implements Listener {
    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        CacheUtils.getInstance().invalidateInteractionCacheForChunk(event.getChunk().getX(), event.getChunk().getZ(),
                        event.getWorld().getName());
    }
}
