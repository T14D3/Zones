package de.t14d3.zones.listeners;

import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldEventListener implements Listener {
    private final Zones plugin;

    public WorldEventListener(Zones plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        plugin.getRegionManager().loadRegions(World.of(event.getWorld()));
    }
}
