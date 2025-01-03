package de.t14d3.zones.listeners;

import de.t14d3.zones.PermissionManager;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Actions;
import de.t14d3.zones.utils.Direction;
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
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
            player.sendMessage(miniMessage.deserialize(messages.get("commands.invalid")));
            return;
        }

        String command = args[0].toLowerCase();
        switch (command) {
            case "info":
                handleInfoCommand(stack.getSender(), args);
                break;
            case "delete":
                handleDeleteCommand(stack.getSender(), args);
                break;
            case "create":
                handleCreateCommand(stack.getSender());
                break;
            case "subcreate":
                handleSubCreateCommand(stack.getSender(), args);
                break;
            case "cancel":
                handleCancelCommand(stack.getSender());
                break;
            case "list":
                handleListCommand(stack.getSender());
                break;
            case "set":
                handleSetCommand(stack.getSender(), args);
                break;
            case "save":
                handleSaveCommand(stack.getSender(), args);
                break;
            case "load":
                handleLoadCommand(stack.getSender(), args);
                break;
            case "expand":
                handleExpandCommand(stack.getSender(), args);
                break;
            default:
                stack.getSender().sendMessage(miniMessage.deserialize(messages.get("commands.invalid")));
                break;
        }
    }

    @Override
    public @NotNull Collection<String> suggest(CommandSourceStack stack, String[] args) {
        Player player = (Player) stack.getSender();
        if (args.length <= 1) {
            return List.of("info", "delete", "create", "subcreate", "cancel", "list", "set", "load", "save", "expand");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info")
                || args[0].equalsIgnoreCase("delete")
                || args[0].equalsIgnoreCase("subcreate")
                || args[0].equalsIgnoreCase("expand"))) {
            List<String> builder = new ArrayList<>();
            regionManager.regions().forEach((regionKey, region) -> region.getMembers().keySet().stream()
                    .filter(uuid -> pm.isAdmin(uuid, region))
                    .forEach(uuid -> builder.add(regionKey)));
            return builder;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2) {
                List<String> suggestions = new ArrayList<>();
                for (String regionKey : regionManager.regions().keySet()) {
                    if (regionKey.toLowerCase().startsWith(args[1].toLowerCase())
                            && regionManager.regions().get(regionKey).isAdmin(player.getUniqueId())) {
                        suggestions.add(regionKey);
                    }
                }
                return suggestions;
            }
            if (args.length == 3) {
                List<String> suggestions = new ArrayList<>();
                for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                    if (offlinePlayer.getName() != null && offlinePlayer.getName().toLowerCase().startsWith(args[2].toLowerCase())) {
                        suggestions.add(offlinePlayer.getName());
                    }
                }
                return suggestions;
            }
            if (args.length == 4) {
                List<String> suggestions = new ArrayList<>();
                for (Actions action : Actions.values()) {
                    if (action.name().toLowerCase().startsWith(args[3].toLowerCase())) {
                        suggestions.add(action.name());
                    }
                }
                return suggestions;
            }
            if (args.length >= 5) {
                List<String> suggestions = new ArrayList<>();
                List<String> types;
                switch (args[3].toUpperCase()) {
                    case "PLACE", "BREAK" -> types = plugin.blockTypes;
                    case "CONTAINER" -> types = plugin.containerTypes;
                    case "REDSTONE" -> types = plugin.redstoneTypes;
                    case "ENTITY", "DAMAGE" -> types = plugin.entityTypes;
                    case "IGNITE" -> {
                        types = new ArrayList<>();
                        types.add("true");
                        types.add("false");
                    }
                    default -> types = plugin.types;

                }
                for (String value : types) {
                    if (value.toLowerCase().startsWith(args[4].toLowerCase())) {
                        suggestions.add(value);
                    }
                }
                return suggestions;
            }
        }
        if (args[0].equalsIgnoreCase("expand")) {
            if (args.length == 3) {
                List<String> suggestions = new ArrayList<>();
                for (int i = 1; i < 10; i++) {
                    suggestions.add(String.valueOf(i));
                }
                return suggestions;
            }
            if (args.length == 4 && stack.getSender().hasPermission("zones.expand.overlap")) {
                return List.of("overlap");
            }
        }


        return List.of();
    }

    private void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        String regionKey = args[1];

        Map<String, Region> regions = regionManager.regions();
        if (!regions.containsKey(regionKey)) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return; // Failure
        }

        Region region = regions.get(regionKey);
        if (sender instanceof Player player && !pm.hasPermission(player.getUniqueId(), "role", "owner", region)) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
            return; // Failure
        }
        regionManager.deleteRegion(regionKey);
        sender.sendMessage(miniMessage.deserialize(messages.get("delete.success").replace("<region>", regionKey)));
        regionManager.saveRegions();
    }

    private void handleCreateCommand(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!plugin.selection.containsKey(player.getUniqueId())) {
                plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
                sender.sendMessage(miniMessage.deserialize(messages.get("create.click_two_corners")));
                return; // Failure
            }

            Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
            if (selectionPair.first() != null && selectionPair.second() != null) {
                if (regionManager.overlapsExistingRegion(selectionPair.first(), selectionPair.second()) && !sender.hasPermission("zones.create.overlap")) {
                    sender.sendMessage(miniMessage.deserialize(messages.get("create.overlap")));
                    return; // Failure
                }

                Map<String, String> perms = new HashMap<>();
                perms.put("role", "owner");

                regionManager.create2DRegion(sender.getName(), selectionPair.first(), selectionPair.second(), player.getUniqueId(), perms);
                resetBeacon(player, selectionPair.first());
                resetBeacon(player, selectionPair.second());
                sender.sendMessage(miniMessage.deserialize(messages.get("create.region_created")));
                plugin.selection.remove(player.getUniqueId());
                return; // Success
            }
            sender.sendMessage(miniMessage.deserialize(messages.get("create.click_two_corners")));
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("command.only-player")));
        }
    }

    private void handleSubCreateCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
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
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.only-player")));
        }
    }

    private void handleCancelCommand(CommandSender sender) {
        if (sender instanceof Player player) {
            if (plugin.selection.containsKey(player.getUniqueId())) {
                Pair<Location, Location> selection = plugin.selection.get(player.getUniqueId());
                resetBeacon(player, selection.first());
                resetBeacon(player, selection.second());
                plugin.selection.remove(player.getUniqueId());
                player.sendMessage(miniMessage.deserialize(messages.get("selection_cancelled")));
            } else {
                player.sendMessage(miniMessage.deserialize(messages.get("no_selection")));
            }
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.only-player")));
        }
    }

    private void handleListCommand(CommandSender sender) {
        Map<String, Region> regions = regionManager.regions();
        if (regions.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize(messages.get("region.none-found")));
            return;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            if (!sender.hasPermission("zones.info.other") && (player != null && !entry.getValue().isMember(player.getUniqueId()))) {
                continue;
            }

            Component hoverText = regionInfo(
                    entry,
                    (sender.hasPermission("zones.info.other") ||
                            (player != null && this.plugin.getPermissionManager().isAdmin(player.getUniqueId(), regions.get(entry.getKey())))));
            var mm = MiniMessage.miniMessage();

            Component comp = mm.deserialize(messages.getOrDefault("region.info.name", "<gold><name> <dark_gray><italic>(#<key>)"), parsed("name", entry.getValue().getName()), parsed("key", entry.getKey()));
            HoverEvent<Component> hover = hoverText.asHoverEvent();
            ClickEvent click = ClickEvent.runCommand("/zone info " + entry.getKey());
            comp = comp.hoverEvent(hover);
            comp = comp.clickEvent(click);
            sender.sendMessage(comp);
        }
    }

    private void handleInfoCommand(CommandSender sender, String[] args) {
        String regionKey;
        if (args.length < 2) {
            if (sender instanceof Player player) {
                regionManager.getRegionsAt(player.getLocation()).forEach(region ->
                        handleInfoCommand(sender, new String[]{"info", region.getKey()}));
            } else {
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            }
            return;
        } else {
            regionKey = args[1];
        }
        Map<String, Region> regions = regionManager.regions();
        if (!regions.containsKey(regionKey)) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (!sender.hasPermission("zones.info.other") && (player != null && !this.plugin.getPermissionManager().isAdmin(player.getUniqueId(), regions.get(regionKey)))) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
            return;
        }
        Component comp = regionInfo(regions.entrySet().stream().filter(entry -> entry.getKey().equals(regionKey)).findFirst().get(), true);
        sender.sendMessage(comp);

    }

    private void handleSetCommand(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(miniMessage.deserialize(messages.get("set.invalid-usage")));
            return;
        }
        String regionKey = args[1];
        Region region = regionManager.regions().get(regionKey);
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (region == null || (player != null && !pm.hasPermission(player.getUniqueId(), "role", "owner", region))) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
        String permission = args[3];
        String value = args[4];
        regionManager.addMemberPermission(target.getUniqueId(), permission, value, regionKey);
        regionManager.saveRegions();
        sender.sendMessage(miniMessage.deserialize(messages.get("set.success"),
                parsed("region", regionKey),
                parsed("player", target.getName()),
                parsed("permission", permission),
                parsed("value", value)));
    }

    private void handleExpandCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        String regionKey = args[1];
        Direction direction;
        if (sender instanceof Player player) {
            direction = Direction.fromYaw(player.getLocation().getYaw());
        } else {
            direction = Direction.valueOf(args[4].toUpperCase());
        }

        int amount = Integer.parseInt(args[2]);
        boolean allowOverlap = false;
        if (args.length == 4) {
            allowOverlap = Objects.equals(args[3], "overlap") && sender.hasPermission("zones.expand.overlap");
        }
        if (regionManager.expandBounds(regionManager.regions().get(regionKey), direction, amount, allowOverlap)) {
            sender.sendMessage(miniMessage.deserialize(messages.get("expand.success"), parsed("region", regionKey)));
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("expand.fail"), parsed("region", regionKey)));
        }
    }

    private void handleSaveCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("zones.save")) {
            regionManager.saveRegions();
            int count = regionManager.regions().size();
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.save"), parsed("count", String.valueOf(count))));
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
        }
    }

    private void handleLoadCommand(CommandSender sender, String[] args) {
        if (sender.hasPermission("zones.load")) {
            regionManager.loadRegions();
            int count = regionManager.loadedRegions.size();
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.load"), parsed("count", String.valueOf(count))));
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
        }
    }

    private Component regionInfo(Map.Entry<String, Region> entry, boolean showMembers) {
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
