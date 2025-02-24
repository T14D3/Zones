package de.t14d3.zones.listeners;

import de.t14d3.zones.*;
import de.t14d3.zones.permissions.flags.Flags;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class EntityEventListener implements Listener {

    private final Zones zones;
    private final ZonesBukkit plugin;
    private final RegionManager regionManager;
    private final BukkitPermissionManager permissionManager;

    public EntityEventListener(Zones zones) {
        this.zones = zones;
        this.regionManager = zones.getRegionManager();
        this.plugin = ((BukkitPlatform) zones.getPlatform()).getPlugin();
        this.permissionManager = plugin.getPermissionManager();
    }

    @EventHandler
    public void onEntitySpawn(CreatureSpawnEvent event) {
        if (event.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)) {
            return;
        }
        if (!permissionManager.checkAction(event.getLocation(), Flags.SPAWN, event.getEntity().getType().name(),
                event.getSpawnReason().name())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityHurt(EntityDamageEvent event) {
        DamageSource source = event.getDamageSource();
        if (source.getCausingEntity() instanceof org.bukkit.entity.Player) {
            return; // Checked separately
        }
        if (!permissionManager.checkAction(event.getEntity().getLocation(), Flags.HURT, event.getEntity().getType()
                .name(), source.getDamageType())) {
            event.setCancelled(true);
        }
    }
}
