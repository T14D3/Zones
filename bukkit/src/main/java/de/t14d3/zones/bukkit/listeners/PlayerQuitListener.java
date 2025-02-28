package de.t14d3.zones.bukkit.listeners;

import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.permissions.CacheUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final Zones zones;
    private final CacheUtils cacheUtils;

    public PlayerQuitListener(Zones zones) {
        this.zones = zones;
        this.cacheUtils = CacheUtils.getInstance();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        java.util.UUID uuid = event.getPlayer().getUniqueId();
        PlayerRepository.remove(uuid);
        cacheUtils.invalidateInteractionCache(uuid);
        cacheUtils.invalidateCache(uuid.toString());
    }
}
