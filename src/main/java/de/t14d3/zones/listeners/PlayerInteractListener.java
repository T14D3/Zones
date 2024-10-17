package de.t14d3.zones.listeners;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.PermissionManager;
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

    public PlayerInteractListener(RegionManager regionManager, PermissionManager permissionManager) {
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
