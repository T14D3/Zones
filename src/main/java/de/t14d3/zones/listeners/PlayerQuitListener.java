package de.t14d3.zones.listeners;

import de.t14d3.zones.Zones;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final Zones zones;

    public PlayerQuitListener(Zones zones) {
        this.zones = zones;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        zones.selection.remove(uuid);
        zones.getPermissionManager().invalidateCache(uuid);
    }
}
