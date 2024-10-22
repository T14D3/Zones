package de.t14d3.zones.listeners;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.PermissionManager;
import de.t14d3.zones.Zones;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Location;
import org.bukkit.Tag;
import org.bukkit.block.Container;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.UUID;

public class PlayerInteractListener implements Listener {

    private final RegionManager regionManager;
    private final PermissionManager permissionManager;
    private final Zones plugin;

    public PlayerInteractListener(RegionManager regionManager, PermissionManager permissionManager, Zones plugin) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.permissionManager = permissionManager;

    }
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) {
            return; // No block clicked, exit early
        }

        Location location = event.getClickedBlock().getLocation();
        UUID playerUUID = player.getUniqueId();

        if (plugin.selection.containsKey(playerUUID)) {
            event.setCancelled(true);

            Pair<Location, Location> selection = plugin.selection.get(playerUUID);
            Location min = selection.first(); // Current minimum location
            Location max = selection.second(); // Current maximum location

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                min = location;
                player.sendMessage("First Location set to " + location);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                max = location;
                player.sendMessage("Second Location set to " + location);
            }

            plugin.selection.put(playerUUID, Pair.of(min, max));
            return;
        }


        String action = "UNKNOWN";
        String type = "UNKNOWN";
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            type = event.getClickedBlock().getType().name();
            action = "BREAK";

        } else {
            if (event.getItem() != null) {
                type = event.getItem().getType().name();
                action = "PLACE";
            }
            if ((event.getClickedBlock().getState() instanceof Container container) && !player.isSneaking()) {
                action = "CONTAINER";
            } else if (event.getClickedBlock().getBlockData() instanceof Powerable powerable) {
                action = "REDSTONE";

            }
        }
        player.sendMessage("Action: " + action);
        player.sendMessage("Type: " + type);

        if (!permissionManager.canInteract(location, playerUUID, action.toLowerCase(), type)) {
            event.setCancelled(true);
            event.getClickedBlock().getState().update();
        }

    }

}
