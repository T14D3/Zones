package de.t14d3.zones.listeners;

import de.t14d3.zones.PermissionManager;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Flags;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class NonPlayerEventListener implements Listener {

    private final Zones plugin;
    private final RegionManager regionManager;
    private final PermissionManager permissionManager;

    public NonPlayerEventListener(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.permissionManager = plugin.getPermissionManager();
    }

    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)) {
            return;
        }
        if (!permissionManager.checkAction(event.getLocation(), "universal", Flags.SPAWN, event.getEntity().getType().name(), event.getSpawnReason().name())) {
            event.setCancelled(true);
        }
    }
}
