package de.t14d3.zones.listeners;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.Flag;
import de.t14d3.zones.permissions.Flags;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import de.t14d3.zones.visuals.BeaconUtils;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.t14d3.zones.visuals.BeaconUtils.resetBeacon;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class PlayerInteractListener implements Listener {

    private final RegionManager regionManager;
    private final PermissionManager permissionManager;
    private final Zones plugin;
    private final BeaconUtils beaconUtils;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Messages messages;

    public PlayerInteractListener(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.permissionManager = plugin.getPermissionManager();
        this.beaconUtils = plugin.getBeaconUtils();
        this.messages = plugin.getMessages();

    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getClickedBlock() == null) {
            return; // No block clicked, exit early
        }

        Location location = event.getClickedBlock().getLocation();
        UUID playerUUID = player.getUniqueId();

        if (plugin.selection.containsKey(playerUUID)) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return;
            }
            event.setCancelled(true);

            Pair<Location, Location> selection = plugin.selection.get(playerUUID);
            Location min = selection.first(); // Current minimum location
            Location max = selection.second(); // Current maximum location

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                resetBeacon(player, min);
                min = location;
                beaconUtils.createBeacon(player, min, DyeColor.GREEN);
                player.sendMessage(miniMessage.deserialize(messages.get("create.primary")
                        , parsed("x", String.valueOf(location.getBlockX()))
                        , parsed("y", String.valueOf(location.getBlockY()))
                        , parsed("z", String.valueOf(location.getBlockZ()))
                ));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                resetBeacon(player, max);
                max = location;
                beaconUtils.createBeacon(player, max, DyeColor.RED);
                player.sendMessage(miniMessage.deserialize(messages.get("create.secondary")
                        , parsed("x", String.valueOf(location.getBlockX()))
                        , parsed("y", String.valueOf(location.getBlockY()))
                        , parsed("z", String.valueOf(location.getBlockZ()))
                ));
            }

            plugin.selection.put(playerUUID, Pair.of(min, max));
            if (min != null && max != null) {
                Utils.Modes mode = Utils.Modes.getPlayerMode(player);
                if (mode == Utils.Modes.CUBOID_3D) {
                    plugin.particles.put(playerUUID, BoundingBox.of(min.toBlockLocation(), max.toBlockLocation()));
                } else {
                    min.setY(-63);
                    max.setY(319);
                    plugin.particles.put(playerUUID, BoundingBox.of(min.toBlockLocation(), max.toBlockLocation()));
                }

            }
            return;
        }

        List<Flag> requiredPermissions = new ArrayList<>(); // Collect required permissions
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        // Interactable blocks
        if ((Utils.isContainer(event.getClickedBlock().getState(false)) || Utils.isPowerable(
                event.getClickedBlock().getBlockData())) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            requiredPermissions.add(Flags.INTERACT);
            if (Utils.isContainer(event.getClickedBlock().getState(false))) {
                requiredPermissions.add(Flags.CONTAINER);
            }
            if (Utils.isPowerable(event.getClickedBlock().getBlockData())) {
                requiredPermissions.add(Flags.REDSTONE);
            }
            if (event.getClickedBlock().getType() == Material.TNT) {
                requiredPermissions.add(Flags.IGNITE);
            }
        } else return;
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, playerUUID, action,
                    event.getClickedBlock().getType().name())) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, event.getClickedBlock().getType().name());
            }
        }
    }

    @EventHandler
    private void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        String type = event.getBlockPlaced().getType().name();
        Location location = event.getBlockPlaced().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.PLACE);
        if (Utils.isContainer(event.getBlockPlaced().getState(false))) {
            requiredPermissions.add(Flags.CONTAINER);
        }
        if (Utils.isPowerable(event.getBlockPlaced().getBlockData())) {
            requiredPermissions.add(Flags.REDSTONE);
        }
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(event.getBlockPlaced().getLocation(), player.getUniqueId(), action,
                    type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }

    }

    @EventHandler
    private void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        String type = event.getBlock().getType().name();
        Location location = event.getBlock().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.BREAK);
        if (Utils.isContainer(event.getBlock().getState(false))) {
            requiredPermissions.add(Flags.CONTAINER);
        }
        if (Utils.isPowerable(event.getBlock().getBlockData())) {
            requiredPermissions.add(Flags.REDSTONE);
        }
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(event.getBlock().getLocation(), player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }

    }


    @EventHandler
    private void onEntityInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getRightClicked().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.INTERACT);
        requiredPermissions.add(Flags.ENTITY);
        String type = event.getRightClicked().getType().name();
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (player.hasPermission("zones.bypass.claimed")) {
                return;
            }
            Location location = event.getEntity().getLocation();
            List<Flag> requiredPermissions = new ArrayList<>();
            requiredPermissions.add(Flags.DAMAGE);
            String type = event.getEntity().getType().name();
            for (Flag action : requiredPermissions) {
                if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                    event.setCancelled(true);
                    actionBar(player, location, requiredPermissions, type);
                }
            }
        }
    }

    @EventHandler
    private void onVehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player player) {
            if (player.hasPermission("zones.bypass.claimed")) {
                return;
            }
            Location location = event.getVehicle().getLocation();
            List<Flag> requiredPermissions = new ArrayList<>();
            requiredPermissions.add(Flags.DAMAGE);
            String type = event.getVehicle().getType().name();
            for (Flag action : requiredPermissions) {
                if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                    event.setCancelled(true);
                    actionBar(player, location, requiredPermissions, type);
                }
            }
        }
    }

    @EventHandler
    private void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getRightClicked().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.ENTITY);
        String type = event.getRightClicked().getType().name();
        requiredPermissions.add(Flags.CONTAINER);
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    @EventHandler
    private void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getEntity().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.PLACE);
        String type = event.getEntity().getType().name();
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    @EventHandler
    private void onHangingBreak(HangingBreakByEntityEvent event) {
        Player player = (Player) event.getRemover();
        if (player == null || player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getEntity().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.BREAK);
        requiredPermissions.add(Flags.ENTITY);
        String type = event.getEntity().getType().name();
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    @EventHandler
    private void onEntityPlace(EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player == null || player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getEntity().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Flags.PLACE);
        requiredPermissions.add(Flags.ENTITY);
        String type = event.getEntity().getType().name();
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    // Small util for message
    private void actionBar(Player player, Location location, List<Flag> actions, String type) {
        List<Region> regions = regionManager.getRegionsAt(location);
        String regionNames = regions.stream().map(Region::getName).collect(Collectors.joining(", "));

        StringBuilder permissionsString = new StringBuilder();
        for (Flag action : actions) {
            permissionsString.append(action.name()).append(", ");
        }
        permissionsString.deleteCharAt(permissionsString.length() - 2); // Remove trailing ", "
        permissionsString.deleteCharAt(permissionsString.length() - 1); // Remove trailing ", "

        player.sendActionBar(miniMessage.deserialize(messages.get("region.no-interact-permission"),
                parsed("region", regionNames),
                parsed("actions", permissionsString.toString()),
                parsed("type", type)));
    }
}
