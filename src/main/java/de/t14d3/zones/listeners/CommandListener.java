package de.t14d3.zones.listeners;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Utils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

import static de.t14d3.zones.PermissionManager.hasPermission;
import static de.t14d3.zones.utils.BeaconUtils.resetBeacon;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

@SuppressWarnings("UnstableApiUsage")
public class CommandListener implements BasicCommand {

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
    public void execute(CommandSourceStack stack, String[] args) {
        Player player = (Player) stack.getSender();
        if (args.length < 1) {
            player.sendMessage(miniMessage.deserialize(messages.get("invalidCommand")));
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "info":
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize(messages.get("regionKeyRequired")));
                } else {
                    handleInfoCommand(player, args[1], regionManager.loadRegions());
                }
                break;
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize(messages.get("regionKeyRequired")));
                } else {
                    handleDeleteCommand(player, args[1], regionManager.loadRegions());
                }
                break;
            case "create":
                handleCreateCommand(player, args);
                break;
            case "cancel":
                handleCancelCommand(player);
                break;
            case "list":
                handleListCommand(player, regionManager.loadRegions());
                break;
            default:
                player.sendMessage(miniMessage.deserialize(messages.get("invalidCommand")));
                break;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        if (args.length <= 1) {
            return List.of("info", "delete", "create", "cancel", "list");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("delete"))) {
            List<String> builder = new ArrayList<>();
            regionManager.loadRegions().forEach((regionKey, region) -> {
                    region.getMembers().keySet().stream()
                            .filter(uuid -> hasPermission(uuid, "owner", "true", region))
                            .forEach(uuid -> builder.add(regionKey));
            });
            return builder;
        }
        return List.of();
    }

    private void handleDeleteCommand(Player player, String regionKey, Map<String, RegionManager.Region> regions) {
        if (!regions.containsKey(regionKey)) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
            return; // Failure
        }

        RegionManager.Region region = regions.get(regionKey);
        if (!hasPermission(player.getUniqueId(), "owner", "true", region)) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
            return; // Failure
        }
        regionManager.deleteRegion(regionKey);
        player.sendMessage(miniMessage.deserialize(messages.get("region_deleted").replace("{regionKey}", regionKey)));
        regionManager.saveRegions();
    }

    private void handleCreateCommand(Player player, String[] args) {
        if (!plugin.selection.containsKey(player.getUniqueId())) {
            plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
            player.sendMessage(miniMessage.deserialize(messages.get("click_two_corners")));
            return; // Failure
        }

        Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
        if (selectionPair.first() != null && selectionPair.second() != null) {
            if (regionManager.overlapsExistingRegion(selectionPair.first(), selectionPair.second()) && !player.hasPermission("zones.create.overlap")) {
                player.sendMessage(miniMessage.deserialize(messages.get("create.overlap")));
                return; // Failure
            }

            Map<String, String> perms = new HashMap<>();
            perms.put("owner", "true");


            regionManager.create2DRegion(player.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId(), perms);
            resetBeacon(player, selectionPair.first());
            resetBeacon(player, selectionPair.second());
            player.sendMessage(miniMessage.deserialize(messages.get("create.region_created")));
            plugin.selection.remove(player.getUniqueId());
            return; // Success
        }

        player.sendMessage(miniMessage.deserialize(messages.get("create.click_two_corners")));
    }

    private void handleCancelCommand(Player player) {
        if (plugin.selection.containsKey(player.getUniqueId())) {
            Pair<Location, Location> selection = plugin.selection.get(player.getUniqueId());
            resetBeacon(player, selection.first());
            resetBeacon(player, selection.second());
            plugin.selection.remove(player.getUniqueId());
            player.sendMessage(miniMessage.deserialize(messages.get("selection_cancelled")));
        } else {
            player.sendMessage(miniMessage.deserialize(messages.get("no_selection")));
        }
    }

    private void handleListCommand(Player player, Map<String, RegionManager.Region> regions) {
        if (regions.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(messages.get("region.none-found")));
            return;
        }
        for (Map.Entry<String, RegionManager.Region> entry : regions.entrySet()) {
            if (!entry.getValue().isMember(player.getUniqueId())) {
                continue;
            }

            var mm = MiniMessage.miniMessage();

            Component comp = mm.deserialize(messages.getOrDefault("region.info.name","<gold><name> <dark_gray><italic>(#<key>)"), parsed("name", entry.getValue().getName()), parsed("key", entry.getKey()));

            Component hoverText = mm.deserialize(messages.get("region.info.min"), parsed("min", entry.getValue().getMinString()));
            hoverText = hoverText.appendNewline();
            hoverText = hoverText.append(mm.deserialize(messages.get("region.info.max"), parsed("max", entry.getValue().getMaxString())));

            // Iterate over members to format permissions
            for (Map.Entry<UUID, Map<String, String>> member : entry.getValue().getMembers().entrySet()) {
                String playerName = Bukkit.getPlayer(member.getKey()) != null ? Bukkit.getPlayer(member.getKey()).getName() : member.getKey().toString();
                Component playerComponent = mm.deserialize(messages.get("region.info.members.name"), parsed("name", playerName));
                playerComponent = playerComponent.appendNewline();

                // Extract permissions and format them using Adventure Components
                Map<String, String> permissions = member.getValue();
                Component permissionsComponent = Component.empty();

                for (Map.Entry<String, String> permEntry : permissions.entrySet()) {
                    String permKey = permEntry.getKey();
                    String permValue = permEntry.getValue();

                    // Split permission values by comma and trim whitespace
                    String[] permValues = permValue.split(",\\s*");
                    List<Component> formattedComponents = new ArrayList<>();

                    for (String value : permValues) {
                        Component formattedValue;
                        if ("true".equalsIgnoreCase(value) || "*".equals(value)) {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.allowed"), parsed("value", value));
                        } else if ("false".equalsIgnoreCase(value) || value.startsWith("!")) {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.denied"), parsed("value", value));
                        } else {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.allowed"), parsed("value", value));
                        }
                        formattedComponents.add(formattedValue);
                    }

                    // Combine all formatted components into one line
                    Component permLine = mm.deserialize(messages.get("region.info.members.permission"), parsed("permission", permKey));

                    // Append all value components with comma separators
                    for (int i = 0; i < formattedComponents.size(); i++) {
                        permLine = permLine.append(formattedComponents.get(i));
                        if (i < formattedComponents.size() - 1) {
                            permLine = permLine.append(Component.text(", ").color(NamedTextColor.GRAY));
                        }
                    }

                    // Append the permission line and a newline
                    permissionsComponent = permissionsComponent.append(permLine).append(Component.newline());
                }

                // Append the player component and their permissions to hover text
                hoverText = hoverText.appendNewline()
                        .append(playerComponent)
                        .append(permissionsComponent);
            }
            hoverText = hoverText.append(mm.deserialize(messages.get("region.info.key"), parsed("key", entry.getKey())));


            HoverEvent<Component> hover = hoverText.asHoverEvent();
            ClickEvent click = ClickEvent.runCommand("/zone info " + entry.getKey());
            comp = comp.hoverEvent(hover);
            comp = comp.clickEvent(click);
            player.sendMessage(comp);
        }
    }

    private void handleInfoCommand(Player player, String regionKey, Map<String, RegionManager.Region> regions) {
        if (!regions.containsKey(regionKey)) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
            return; // Failure
        }

        RegionManager.Region region = regions.get(regionKey);
        player.sendMessage(miniMessage.deserialize(messages.get("region.info.name").replace("{regionName}", region.getName())));
        player.sendMessage(miniMessage.deserialize(messages.get("region.info.min").replace("{min}", region.getMin().toString())));
        player.sendMessage(miniMessage.deserialize(messages.get("region.info.max").replace("{max}", region.getMax().toString())));
        player.sendMessage(miniMessage.deserialize(messages.get("region.info.members.header")));
        region.getMembers().forEach((uuid, permissions) -> {
            player.sendMessage(miniMessage.deserialize(messages.get("region.info.members.member")
                    .replace("{playerName}", Bukkit.getPlayer(uuid) != null ? Bukkit.getPlayer(uuid).getName() : uuid.toString())
                    .replace("{permissions}", permissions.toString())));
        });
    }
}
