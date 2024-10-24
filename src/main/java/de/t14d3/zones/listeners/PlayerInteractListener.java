package de.t14d3.zones.listeners;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.PermissionManager;
import de.t14d3.zones.Utils;
import de.t14d3.zones.Zones;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.t14d3.zones.Utils.resetBeacon;

public class PlayerInteractListener implements Listener {

    private final RegionManager regionManager;
    private final PermissionManager permissionManager;
    private final Zones plugin;
    private final Utils utils;

    public PlayerInteractListener(RegionManager regionManager, PermissionManager permissionManager, Zones plugin) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.permissionManager = permissionManager;
        this.utils = plugin.getUtils();

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
                resetBeacon(player, min);
                min = location;
                utils.createBeacon(player, min, DyeColor.GREEN);
                player.sendMessage("First Location set to " + location);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                resetBeacon(player, max);
                max = location;
                utils.createBeacon(player, max, DyeColor.RED);
                player.sendMessage("Second Location set to " + location);
            }

            plugin.selection.put(playerUUID, Pair.of(min, max));
            return;
        }

        String type = "UNKNOWN";
        List<String> requiredPermissions = new ArrayList<>(); // Collect required permissions

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            type = event.getClickedBlock().getType().name();
            requiredPermissions.add("BREAK");

        } else {
            if (event.getItem() != null) {
                type = event.getItem().getType().name();
                requiredPermissions.add("PLACE");
            }
            if ((event.getClickedBlock().getState() instanceof Container container) && !player.isSneaking()) {
                requiredPermissions.add("CONTAINER");
            }
            if (event.getClickedBlock().getBlockData() instanceof Powerable) {
                requiredPermissions.add("REDSTONE");
            }
        }

        // Debug
        player.sendMessage("Required Permissions: " + String.join(", ", requiredPermissions));
        player.sendMessage("Type: " + type);

        // Check all required permissions
        for (String action : requiredPermissions) {
            if (!permissionManager.canInteract(location, playerUUID, action, type)) {
                event.setCancelled(true);
                event.getClickedBlock().getState().update();
                break; // Exit early if any permission is denied
            }
        }
    }


}
