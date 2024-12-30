package de.t14d3.zones.listeners;

import de.t14d3.zones.PermissionManager;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
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

import static de.t14d3.zones.utils.BeaconUtils.resetBeacon;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

@SuppressWarnings("UnstableApiUsage")
public class CommandListener implements BasicCommand {

    private final Zones plugin;
    private final RegionManager regionManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages;
    private final PermissionManager pm;

    public CommandListener(Zones plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.messages = plugin.getMessages();
        this.pm = plugin.getPermissionManager();
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
                    if (stack.getSender() instanceof Player) {
                        regionManager.getRegionsAt(player.getLocation()).forEach(region ->
                                handleInfoCommand(player, region.getName(), regionManager.regions()));
                    } else {
                        player.sendMessage(miniMessage.deserialize(messages.get("regionKeyRequired")));
                    }
                } else {
                    handleInfoCommand(player, args[1], regionManager.regions());
                }
                break;
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(miniMessage.deserialize(messages.get("regionKeyRequired")));
                } else {
                    handleDeleteCommand(player, args[1], regionManager.regions());
                }
                break;
            case "create":
                handleCreateCommand(player, args);
                break;
            case "subcreate":
                handleSubCreateCommand(player, args);
                break;
            case "cancel":
                handleCancelCommand(player);
                break;
            case "list":
                handleListCommand(player, regionManager.regions());
                break;
            case "set":
                handleSetCommand(player, args);
                break;
            default:
                player.sendMessage(miniMessage.deserialize(messages.get("invalidCommand")));
                break;
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        Player player = (Player) stack.getSender();
        if (args.length <= 1) {
            return List.of("info", "delete", "create", "subcreate", "cancel", "list", "set");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info")
                || args[0].equalsIgnoreCase("delete")
                || args[0].equalsIgnoreCase("subcreate"))) {
            List<String> builder = new ArrayList<>();
            regionManager.regions().forEach((regionKey, region) -> {
                    region.getMembers().keySet().stream()
                            .filter(uuid -> pm.isAdmin(uuid, region))
                            .forEach(uuid -> builder.add(regionKey));
            });
            return builder;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2) {
                List<String> builder = new ArrayList<>();
                regionManager.regions().forEach((regionKey, region) -> {
                    if (pm.hasPermission(player.getUniqueId(), "role", "owner", region)) {
                        builder.add(regionKey);
                    }
                });
                return builder;
            } else if (args.length == 3) {
                String regionKey = args[1];
                Region region = regionManager.regions().get(regionKey);
                if (region == null) {
                    return null;
                }
                if (pm.hasPermission(player.getUniqueId(), "role", "owner", region)) {
                    return List.of("role", "name", "min", "max", "members");
                }
            }
        }


        return List.of();
    }

    private void handleDeleteCommand(Player player, String regionKey, Map<String, Region> regions) {
        if (!regions.containsKey(regionKey)) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
            return; // Failure
        }

        Region region = regions.get(regionKey);
        if (!pm.hasPermission(player.getUniqueId(), "role", "owner", region)) {
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
            perms.put("role", "owner");

            regionManager.create2DRegion(player.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId(), perms);
            resetBeacon(player, selectionPair.first());
            resetBeacon(player, selectionPair.second());
            player.sendMessage(miniMessage.deserialize(messages.get("create.region_created")));
            plugin.selection.remove(player.getUniqueId());
            return; // Success
        }

        player.sendMessage(miniMessage.deserialize(messages.get("create.click_two_corners")));
    }

    private void handleSubCreateCommand(Player player, String[] args) {
        if (!plugin.selection.containsKey(player.getUniqueId())) {
            plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
            player.sendMessage(miniMessage.deserialize(messages.get("click_two_corners")));
            return; // Failure
        }

        Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
        if (selectionPair.first() == null || selectionPair.second() == null) {
            player.sendMessage(miniMessage.deserialize(messages.get("create.click_two_corners")));
            return; // Failure
        }

        Region parentRegion = null;
        if (args.length < 2) {
            for (Region region : regionManager.getRegionsAt(player.getLocation())) {
                if (pm.isAdmin(player.getUniqueId(), region)) {
                    parentRegion = region;
                    break;
                }
            }
        } else {
            String regionKey = args[1];
            parentRegion = regionManager.regions().get(regionKey);
        }

        if (parentRegion == null) {
            player.sendMessage(miniMessage.deserialize(messages.get("subcreate.no_parent")));
            return; // Failure
        }

        if (!parentRegion.contains(selectionPair.first()) || !parentRegion.contains(selectionPair.second())) {
            player.sendMessage(miniMessage.deserialize(messages.get("subcreate.outside_parent")));
            return; // Failure
        }

        Map<String, String> perms = new HashMap<>();
        perms.put("role", "owner");

        regionManager.createSubRegion(player.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId(), perms, parentRegion);
        resetBeacon(player, selectionPair.first());
        resetBeacon(player, selectionPair.second());
        player.sendMessage(miniMessage.deserialize(messages.get("subcreate.region_created")));
        plugin.selection.remove(player.getUniqueId());
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

    private void handleListCommand(Player player, Map<String, Region> regions) {
        if (regions.isEmpty()) {
            player.sendMessage(miniMessage.deserialize(messages.get("region.none-found")));
            return;
        }
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            if (!entry.getValue().isMember(player.getUniqueId()) || player.hasPermission("zones.info.other")) {
                continue;
            }
            Component hoverText = regionInfo(
                    player, entry,
                    this.plugin.getPermissionManager().isAdmin(player.getUniqueId(), regions.get(entry.getKey()))
                            || player.hasPermission("zones.info.other"));
            var mm = MiniMessage.miniMessage();

            Component comp = mm.deserialize(messages.getOrDefault("region.info.name", "<gold><name> <dark_gray><italic>(#<key>)"), parsed("name", entry.getValue().getName()), parsed("key", entry.getKey()));
            HoverEvent<Component> hover = hoverText.asHoverEvent();
            ClickEvent click = ClickEvent.runCommand("/zone info " + entry.getKey());
            comp = comp.hoverEvent(hover);
            comp = comp.clickEvent(click);
            player.sendMessage(comp);
        }
    }
    private void handleInfoCommand(Player player, String regionKey, Map<String, Region> regions) {
        if (!regions.containsKey(regionKey)) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist").replace("{regionKey}", regionKey)));
            return;
        }
        if (!this.plugin.getPermissionManager().isAdmin(player.getUniqueId(), regions.get(regionKey)) && !player.hasPermission("zones.info.other")) {
            player.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission"), parsed("region", regionKey)));
            return;
        }
        Component comp = regionInfo(player, regions.entrySet().stream().filter(entry -> entry.getKey().equals(regionKey)).findFirst().get(), true);
        player.sendMessage(comp);

    }

    private void handleSetCommand(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(miniMessage.deserialize(messages.get("invalidCommand")));
            return;
        }
        String regionKey = args[1];
        Region region = regionManager.regions().get(regionKey);
        if (region == null) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist"), parsed("key", regionKey)));
            return;
        }
        if (!pm.hasPermission(player.getUniqueId(), "role", "owner", region)) {
            player.sendMessage(miniMessage.deserialize(messages.get("region_not_exist"), parsed("key", regionKey)));
            return;
        }
        String permission = args[2];
        String value = args[3];
        regionManager.addMemberPermission(player.getUniqueId(), permission, value, regionManager, regionKey);
        regionManager.saveRegions();
        player.sendMessage(miniMessage.deserialize(messages.get("set.success"), parsed("permission", permission), parsed("value", value), parsed("region", regionKey)));
    }

    public Component regionInfo(Player player, Map.Entry<String, Region> entry, boolean showMembers) {
        var mm = MiniMessage.miniMessage();
        Component comp = Component.text("");
        comp = comp.append(mm.deserialize(messages.get("region.info.name"), parsed("name", entry.getValue().getName())));
        comp = comp.appendNewline();
        if (entry.getValue().getParent() != null) {
            comp = comp.append(mm.deserialize(messages.get("region.info.parent"), parsed("parent", entry.getValue().getParent())));
        }
        comp = comp.append(mm.deserialize(messages.get("region.info.min"), parsed("min", entry.getValue().getMinString())));
        comp = comp.appendNewline();
        comp = comp.append(mm.deserialize(messages.get("region.info.max"), parsed("max", entry.getValue().getMaxString())));

        if (showMembers) {
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
                comp = comp.appendNewline()
                        .append(playerComponent)
                        .append(permissionsComponent);
            }
        }
        comp = comp.append(mm.deserialize(messages.get("region.info.key"), parsed("key", entry.getKey())));

        return comp;
    }
}
