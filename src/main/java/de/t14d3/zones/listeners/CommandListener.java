package de.t14d3.zones.listeners;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Actions;
import de.t14d3.zones.utils.Utils;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static de.t14d3.zones.PermissionManager.hasPermission;
import static de.t14d3.zones.utils.BeaconUtils.resetBeacon;

@SuppressWarnings("SameReturnValue")
public class CommandListener implements CommandExecutor {

    private final Zones plugin;
    private final RegionManager regionManager;
    private final Utils utils;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages;

    public CommandListener(Zones plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.utils = plugin.getUtils();
        this.messages = plugin.getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(miniMessage.deserialize(messages.get("command_only_player")));
            return true;
        }

        Player player = (Player) sender;

        if (cmd.getName().equalsIgnoreCase("zone") && args.length > 0) {
            Map<String, RegionManager.Region> regions = regionManager.loadRegions();
            String regionKey;

            switch (args[0].toLowerCase()) {
                case "delete":
                    return handleDeleteCommand(player, args, regions);
                case "create":
                    return handleCreateCommand(player, args);
                case "cancel":
                    return handleCancelCommand(player);
                case "list":
                    return handleListCommand(player, regions);
                case "info":
                    return handleInfoCommand(player, args, regions);
                case "test":
                    player.sendMessage(Component.text("Test command executed.").color(TextColor.fromHexString("#00FF00")));
                    return true;
                default:
                    player.sendMessage(miniMessage.deserialize(messages.get("invalid_subcommand")));
                    return true;
            }
        }
        return false;
    }

    private boolean handleDeleteCommand(Player player, String[] args, Map<String, RegionManager.Region> regions) {
        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize(messages.get("usage_delete")));
            return true;
        }

        String regionKey = args[1];

        if (regions.containsKey(regionKey)) {
            RegionManager.Region region = regions.get(regionKey);
            if (!hasPermission(player.getUniqueId(), "owner", "true", region)) {
                player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
                return true;
            }
            regionManager.deleteRegion(regionKey);
            player.sendMessage(miniMessage.deserialize(messages.get("region_deleted").replace("{regionKey}", regionKey)));
            regionManager.saveRegions();
        } else {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
        }
        return true;
    }

    private boolean handleCreateCommand(Player player, String[] args) {
        if (!plugin.selection.containsKey(player.getUniqueId())) {
            plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
            player.sendMessage(miniMessage.deserialize(messages.get("click_two_corners")));
            return true;
        }

        Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
        if (selectionPair.first() != null && selectionPair.second() != null) {
            if (regionManager.overlapsExistingRegion(selectionPair.first(), selectionPair.second()) && !player.hasPermission("zones.create.overlap")) {
                player.sendMessage(Component.text("Error: Region overlaps existing region.").color(TextColor.fromHexString("#FF5555")));
                return true;
            }

            Map<String, String> perms = new HashMap<>();
            perms.put(Actions.BREAK.name(), "true");
            perms.put(Actions.PLACE.name(), "true");
            perms.put(Actions.CONTAINER.name(), "true");

            regionManager.create2DRegion(player.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId(), perms);
            resetBeacon(player, selectionPair.first());
            resetBeacon(player, selectionPair.second());
            player.sendMessage(miniMessage.deserialize(messages.get("region_created")));
            plugin.selection.remove(player.getUniqueId());
            return true;
        }

        player.sendMessage(Component.text("You must select two corners first.").color(TextColor.fromHexString("#FF5555")));
        return true;
    }

    private boolean handleCancelCommand(Player player) {
        if (plugin.selection.containsKey(player.getUniqueId())) {
            Pair<Location, Location> selection = plugin.selection.get(player.getUniqueId());
            resetBeacon(player, selection.first());
            resetBeacon(player, selection.second());
            plugin.selection.remove(player.getUniqueId());
            player.sendMessage(miniMessage.deserialize(messages.get("selection_cancelled")));
        } else {
            player.sendMessage(miniMessage.deserialize(messages.get("no_selection")));
        }
        return true;
    }

    private boolean handleListCommand(Player player, Map<String, RegionManager.Region> regions) {
        if (regions.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(messages.get("no_regions")));
            return true;
        }

        player.sendMessage(miniMessage.deserialize(messages.get("available_regions")));
        for (Map.Entry<String, RegionManager.Region> entry : regions.entrySet()) {
            player.sendMessage(miniMessage.deserialize(entry.getValue().getName()));
        }
        return true;
    }

    private boolean handleInfoCommand(Player player, String[] args, Map<String, RegionManager.Region> regions) {
        if (args.length < 2) {
            player.sendMessage(miniMessage.deserialize(messages.get("usage_info")));
            return true;
        }

        String regionKey = args[1];

        if (regions.containsKey(regionKey)) {
            RegionManager.Region region = regions.get(regionKey);
            player.sendMessage(miniMessage.deserialize(messages.get("region.info.name")
                .replace("{regionName}", region.getName())));
            player.sendMessage(miniMessage.deserialize(messages.get("region.info.min")
                .replace("{min}", region.getMin().toString())));
            player.sendMessage(miniMessage.deserialize(messages.get("region.info.max")
                .replace("{max}", region.getMax().toString())));
            player.sendMessage(miniMessage.deserialize(messages.get("region.info.members.header")));
            region.getMembers().forEach((uuid, permissions) -> {
                player.sendMessage(miniMessage.deserialize(messages.get("region.info.members.member")
                    .replace("{playerName}", Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getName() : uuid.toString())
                    .replace("{permissions}", permissions.toString())));
            });
            return true;
        } else {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
        }
        return true;
    }
}
