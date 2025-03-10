package de.t14d3.zones.bukkit.listeners;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.BukkitPermissionManager;
import de.t14d3.zones.bukkit.BukkitPlatform;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.*;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
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
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class PlayerEventListener implements Listener {

    private final RegionManager regionManager;
    private final BukkitPermissionManager permissionManager;
    private final ZonesBukkit plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Messages messages;
    private final BukkitPlatform platform;
    private final String ACTIONBAR_MESSAGE;

    public PlayerEventListener(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.permissionManager = plugin.getPermissionManager();
        this.messages = plugin.getMessages();
        this.platform = (BukkitPlatform) plugin.getPlatform();
        this.ACTIONBAR_MESSAGE = messages.get("region.no-interact-permission");
    }

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        de.t14d3.zones.objects.Player zplayer = PlayerRepository.get(player.getUniqueId());
        if (event.getClickedBlock() == null) {
            return; // No block clicked, exit early
        }

        Location location = event.getClickedBlock().getLocation();
        UUID playerUUID = player.getUniqueId();

        if (zplayer.getSelection() != null && zplayer.isSelectionCreating()) {
            if (event.getHand() == EquipmentSlot.OFF_HAND) {
                return;
            }
            event.setCancelled(true);

            Location min = zplayer.getSelection().getMin() == null ? null : zplayer.getSelection().getMin()
                    .toLocation(player.getWorld());
            Location max = zplayer.getSelection().getMax() == null ? null : zplayer.getSelection().getMax()
                    .toLocation(player.getWorld());

            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                platform.removeBeacon(player, min);
                min = location;
                platform.showBeacon(player, min, NamedTextColor.GREEN);
                player.sendMessage(miniMessage.deserialize(messages.get("create.primary")
                        , parsed("x", String.valueOf(location.getBlockX()))
                        , parsed("y", String.valueOf(location.getBlockY()))
                        , parsed("z", String.valueOf(location.getBlockZ()))
                ));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                platform.removeBeacon(player, max);
                max = location;
                platform.showBeacon(player, max, NamedTextColor.RED);
                player.sendMessage(miniMessage.deserialize(messages.get("create.secondary")
                        , parsed("x", String.valueOf(location.getBlockX()))
                        , parsed("y", String.valueOf(location.getBlockY()))
                        , parsed("z", String.valueOf(location.getBlockZ()))
                ));
            }
            if (min != null || max != null) {
                Utils.SelectionMode mode = Utils.SelectionMode.getPlayerMode(zplayer);
                if (mode == Utils.SelectionMode.CUBOID_3D) {
                    zplayer.setSelection(new Box(min, max, player.getWorld(), false));
                } else {
                    if (min != null) {
                        min.setY(-63);
                    }
                    if (max != null) {
                        max.setY(319);
                    }
                    zplayer.setSelection(new Box(min, max, player.getWorld(), false));
                }

            }
            return;
        }

        List<Flag> requiredPermissions = new ArrayList<>(); // Collect required permissions
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        // Interactable blocks
        if ((isContainer(event.getClickedBlock().getState(false)) || isPowerable(
                event.getClickedBlock().getBlockData())) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            requiredPermissions.add(Flags.INTERACT);
            if (isContainer(event.getClickedBlock().getState(false))) {
                requiredPermissions.add(Flags.CONTAINER);
            }
            if (isPowerable(event.getClickedBlock().getBlockData())) {
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
        if (isContainer(event.getBlockPlaced().getState(false))) {
            requiredPermissions.add(Flags.CONTAINER);
        }
        if (isPowerable(event.getBlockPlaced().getBlockData())) {
            requiredPermissions.add(Flags.REDSTONE);
        }
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(event.getBlockPlaced().getLocation(), player.getUniqueId(), action,
                    type.toLowerCase())) {
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
        if (isContainer(event.getBlock().getState(false))) {
            requiredPermissions.add(Flags.CONTAINER);
        }
        if (isPowerable(event.getBlock().getBlockData())) {
            requiredPermissions.add(Flags.REDSTONE);
        }
        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(event.getBlock().getLocation(), player.getUniqueId(), action,
                    type.toLowerCase())) {
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

    @EventHandler
    private void onBucketFill(PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getBlockClicked().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        String type = event.getBlockClicked().getType().name();
        requiredPermissions.add(Flags.BREAK);

        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    @EventHandler
    private void onBucketFillEntity(PlayerBucketEntityEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getEntity().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        String type = event.getEntity().getType().name();
        requiredPermissions.add(Flags.ENTITY);

        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    @EventHandler
    private void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("zones.bypass.claimed")) {
            return;
        }
        Location location = event.getBlockClicked().getLocation();
        List<Flag> requiredPermissions = new ArrayList<>();
        String type;
        switch (event.getBucket()) {
            case WATER_BUCKET -> type = "water";
            case LAVA_BUCKET -> type = "lava";
            case POWDER_SNOW_BUCKET -> type = "powder_snow";
            case TROPICAL_FISH_BUCKET, COD_BUCKET, SALMON_BUCKET, PUFFERFISH_BUCKET, TADPOLE_BUCKET, AXOLOTL_BUCKET -> {
                type = "water";
                requiredPermissions.add(Flags.ENTITY);
            }
            default -> type = event.getBlockClicked().getType().name();
        }
        requiredPermissions.add(Flags.PLACE);

        for (Flag action : requiredPermissions) {
            if (!permissionManager.checkAction(location, player.getUniqueId(), action, type)) {
                event.setCancelled(true);
                actionBar(player, location, requiredPermissions, type);
            }
        }
    }

    // Small util for message
    static final String METADATA_KEY = "zones_last_actionbar";
    private void actionBar(Player player, Location location, List<Flag> actions, String type) {

        // Cooldown on the actionbar processing, since spamming it several times per second causes lag
        int currentTick = Bukkit.getCurrentTick();
        if (player.hasMetadata(METADATA_KEY)) {
            int lastSent = player.getMetadata(METADATA_KEY).get(0).asInt();
            if (currentTick - lastSent < 5) { // 0.25 second cooldown
                return; // Cooldown active
            }
        }

        // Proceed to send action bar
        List<Region> regions = regionManager.getRegionsAt(BlockLocation.of(location), World.of(location.getWorld()));
        String regionNames = regions.stream().map(Region::getName).collect(Collectors.joining(", "));

        StringBuilder permissionsString = new StringBuilder();
        for (Flag action : actions) {
            permissionsString.append(action.name()).append(", ");
        }
        if (!actions.isEmpty()) {
            permissionsString.setLength(permissionsString.length() - 2); // Remove trailing ", "
        }

        player.sendActionBar(miniMessage.deserialize(ACTIONBAR_MESSAGE,
                parsed("region", regionNames),
                parsed("actions", permissionsString.toString()),
                parsed("type", type)));

        // Update last sent tick
        player.setMetadata(METADATA_KEY, new FixedMetadataValue(plugin, currentTick));
    }

    public static boolean isContainer(BlockState state) {
        return state instanceof Container;
    }

    public static boolean isPowerable(BlockData data) {
        return data instanceof Powerable;
    }
}
