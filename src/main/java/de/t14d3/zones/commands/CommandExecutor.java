package de.t14d3.zones.commands;

import de.t14d3.zones.*;
import de.t14d3.zones.integrations.WorldGuardImporter;
import de.t14d3.zones.utils.Direction;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static de.t14d3.zones.visuals.BeaconUtils.resetBeacon;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class CommandExecutor {

    private final Zones plugin;
    private final RegionManager regionManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Messages messages;
    private final PermissionManager pm;

    public CommandExecutor(Zones plugin, RegionManager regionManager) {
        this.plugin = plugin;
        this.regionManager = regionManager;
        this.messages = plugin.getMessages();
        this.pm = plugin.getPermissionManager();
    }

    @SuppressWarnings("UnstableApiUsage")
    public void execute(CommandSourceStack stack, String arg) {
        String[] args = arg.replaceFirst("zone ", "").replaceFirst("zones:", "").split(" ");
        CommandSender sender = stack.getSender();

        String command = args[0].toLowerCase();
        switch (command) {
            case "info":
                if (sender.hasPermission("zones.info")) {
                    handleInfoCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "delete":
                if (sender.hasPermission("zones.delete")) {
                    handleDeleteCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "create":
                if (sender.hasPermission("zones.create")) {
                    handleCreateCommand(sender);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "subcreate":
                if (sender.hasPermission("zones.subcreate")) {
                    handleSubCreateCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "cancel":
                if (sender.hasPermission("zones.cancel")) {
                    handleCancelCommand(sender);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "list":
                if (sender.hasPermission("zones.list")) {
                    handleListCommand(sender);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "set":
                if (sender.hasPermission("zones.set")) {
                    handleSetCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "save":
                if (sender.hasPermission("zones.save")) {
                    handleSaveCommand(sender);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "load":
                if (sender.hasPermission("zones.load")) {
                    handleLoadCommand(sender);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "expand":
                if (sender.hasPermission("zones.expand")) {
                    handleExpandCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "select":
                if (sender.hasPermission("zones.select")) {
                    handleSelectCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "rename":
                if (sender.hasPermission("zones.rename")) {
                    handleRenameCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "import":
                if (sender.hasPermission("zones.import")) {
                    handleImportCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "mode":
                if (sender.hasPermission("zones.mode")) {
                    handleModeCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            case "find":
                if (sender.hasPermission("zones.find")) {
                    handleFindCommand(sender, args);
                } else {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
                }
                break;
            default:
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid")));
                break;
        }
    }

    public void handleDeleteCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        RegionKey regionKey = RegionKey.fromString(args[1]);

        Int2ObjectOpenHashMap<Region> regions = regionManager.regions();
        if (!regions.containsKey(regionKey.getValue())) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return; // Failure
        }

        Region region = regions.get(regionKey.getValue());
        if (sender instanceof Player player && !pm.hasPermission(player.getUniqueId(), "role", "owner", region)) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
            return; // Failure
        }
        regionManager.deleteRegion(regionKey);
        sender.sendMessage(
                miniMessage.deserialize(
                        messages.get("commands.delete.success").replace("<region>", regionKey.toString())));
        regionManager.triggerSave();
    }

    public void handleCreateCommand(CommandSender sender) {
        if (sender instanceof Player player) {
            if (!plugin.selection.containsKey(player.getUniqueId())) {
                plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.create.click-corners")));
                return; // Failure
            }

            Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
            if (selectionPair.first() != null && selectionPair.second() != null) {
                if (regionManager.overlapsExistingRegion(selectionPair.first(),
                        selectionPair.second()) && !sender.hasPermission("zones.create.overlap")) {
                    sender.sendMessage(miniMessage.deserialize(messages.get("commands.create.overlap")));
                    return; // Failure
                }

                Map<String, String> perms = new HashMap<>();
                perms.put("role", "owner");
                Utils.Modes mode = Utils.Modes.getPlayerMode(player);
                RegionKey key;
                if (mode == Utils.Modes.CUBOID_3D && player.hasPermission("zones.mode.3d.main")) {
                    key = regionManager.createNewRegion(sender.getName(), selectionPair.first(), selectionPair.second(),
                            player.getUniqueId(), perms).getKey();
                } else {
                    key = regionManager.create2DRegion(sender.getName(), selectionPair.first(), selectionPair.second(),
                            player.getUniqueId(), perms).getKey();
                }
                resetBeacon(player, selectionPair.first());
                resetBeacon(player, selectionPair.second());
                sender.sendMessage(
                        miniMessage.deserialize(messages.get("commands.create.success"),
                                parsed("region", key.toString())));
                plugin.selection.remove(player.getUniqueId());
                return; // Success
            }
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.create.click-corners")));
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("command.only-player")));
        }
    }

    public void handleSubCreateCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (!plugin.selection.containsKey(player.getUniqueId())) {
                plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
                player.sendMessage(miniMessage.deserialize(messages.get("commands.create.click-corners")));
                return; // Failure
            }

            Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
            if (selectionPair.first() == null || selectionPair.second() == null) {
                player.sendMessage(miniMessage.deserialize(messages.get("commands.create.click-corners")));
                return; // Failure
            }

            Region parentRegion = null;
            if (args.length < 2) {
                for (Region region : regionManager.getRegionsAt(player.getLocation())) {
                    if (pm.isAdmin(player.getUniqueId().toString(), region)) {
                        parentRegion = region;
                        break;
                    }
                }
            } else {
                RegionKey regionKey = RegionKey.fromString(args[1]);
                Region tempRegion = regionManager.regions().get(regionKey.getValue());
                if (tempRegion == null || !tempRegion.isAdmin(player.getUniqueId())) {
                    player.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
                    return;
                } else {
                    parentRegion = tempRegion;
                }
            }

            if (parentRegion == null) {
                player.sendMessage(miniMessage.deserialize(messages.get("commands.subcreate.no-parent")));
                return; // Failure
            }

            if (!parentRegion.contains(selectionPair.first()) || !parentRegion.contains(selectionPair.second())) {
                player.sendMessage(miniMessage.deserialize(messages.get("commands.subcreate.outside-parent")));
                return; // Failure
            }

            Map<String, String> perms = new HashMap<>();
            perms.put("role", "owner");

            regionManager.createSubRegion(player.getName(), selectionPair.first().toVector().toBlockVector(),
                    selectionPair.second().toVector().toBlockVector(),
                    selectionPair.first().getWorld(), player.getUniqueId(), perms, parentRegion);
            resetBeacon(player, selectionPair.first());
            resetBeacon(player, selectionPair.second());
            player.sendMessage(miniMessage.deserialize(messages.get("commands.subcreate.success")));
            plugin.selection.remove(player.getUniqueId());
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.only-player")));
        }
    }

    public void handleCancelCommand(CommandSender sender) {
        if (sender instanceof Player player) {
            if (plugin.selection.containsKey(player.getUniqueId())) {
                Pair<Location, Location> selection = plugin.selection.get(player.getUniqueId());
                resetBeacon(player, selection.first());
                resetBeacon(player, selection.second());
                plugin.selection.remove(player.getUniqueId());
                plugin.particles.remove(player.getUniqueId());
                player.sendMessage(miniMessage.deserialize(messages.get("commands.cancel.success")));
            } else {
                player.sendMessage(miniMessage.deserialize(messages.get("commands.cancel.success")));
            }
        } else {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.only-player")));
        }
    }

    public void handleListCommand(CommandSender sender) {
        Int2ObjectOpenHashMap<Region> regions = regionManager.regions();
        if (regions.isEmpty()) {
            sender.sendMessage(miniMessage.deserialize(messages.get("region.none-found")));
            return;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        boolean perm = sender.hasPermission("zones.info.other");
        for (Region region : regions.values()) {
            if (!perm && (player != null && !region.isMember(player.getUniqueId()))) {
                continue;
            }
            Component hoverText = regionInfo(region, (perm || (player != null && this.plugin.getPermissionManager()
                    .isAdmin(player.getUniqueId().toString(), region))))
                    .join();
            var mm = MiniMessage.miniMessage();

            Component comp = mm.deserialize(messages.get("region.info.name"), parsed("name", region.getName()),
                    parsed("key", region.getKey().toString()));
            HoverEvent<Component> hover = hoverText.asHoverEvent();
            ClickEvent click = ClickEvent.runCommand("/zone info " + region.getKey());
            comp = comp.hoverEvent(hover);
            comp = comp.clickEvent(click);
            sender.sendMessage(comp);
        }
    }

    public void handleInfoCommand(CommandSender sender, String[] args) {
        RegionKey regionKey;
        if (args.length < 2) {
            if (sender instanceof Player player) {
                regionManager.getRegionsAtAsync(player.getLocation()).join().forEach(region ->
                        handleInfoCommand(sender, new String[]{"info", region.getKey().toString()}));
            } else {
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            }
            return;
        } else {
            regionKey = RegionKey.fromString(args[1]);
        }
        Int2ObjectOpenHashMap<Region> regions = regionManager.regions();
        if (!regions.containsKey(regionKey.getValue())) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (!sender.hasPermission("zones.info.other") && (player != null && !regions.get(regionKey.getValue())
                .isMember(player.getUniqueId()))) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.no-permission")));
            return;
        }
        Region region = regions.get(regionKey.getValue());
        regionInfo(region, region.isAdmin(player.getUniqueId()))
                .thenAccept(sender::sendMessage);

    }

    public void handleSetCommand(CommandSender sender, String[] args) {

        if (args.length < 5) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.set.invalid-usage")));
            return;
        }
        RegionKey regionKey = RegionKey.fromString(args[1]);
        Region region = regionManager.regions().get(regionKey.getValue());
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (region == null || (player != null && !pm.hasPermission(player.getUniqueId(), "role", "owner",
                region) && !player.hasPermission("zones.set.other"))) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        String target = args[2];
        if (!target.startsWith("+")) {
            target = Utils.getPlayerName(UUID.fromString(target));
        }
        String permission = args[3];
        // Yes, I know this is terrible.
        // No, I will not change it.
        List<String> arg = Arrays.stream(args).toList().subList(4, args.length);
        String value = String.join(", ", arg);
        regionManager.addMemberPermission(target, permission, value, regionKey);
        regionManager.triggerSave();
        sender.sendMessage(miniMessage.deserialize(messages.get("commands.set.success"),
                parsed("region", regionKey.toString()),
                parsed("player", target),
                parsed("permission", permission),
                parsed("value", value)));
    }

    public void handleExpandCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.expand.invalid-usage")));
            return;
        }
        RegionKey regionKey = RegionKey.fromString(args[1]);
        Direction direction;
        Region region = regionManager.regions().get(regionKey.getValue());
        if (sender instanceof Player player) {
            direction = Direction.fromYaw(player.getLocation().getYaw());
            if (region == null || !region.isAdmin(player.getUniqueId())) {
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
                return;
            }
        } else {
            direction = Direction.valueOf(args[4].toUpperCase());
        }

        int amount = Integer.parseInt(args[2]);
        boolean allowOverlap = false;
        if (args.length == 4) {
            allowOverlap = Objects.equals(args[3], "overlap") && sender.hasPermission("zones.expand.overlap");
        }
        if (regionManager.expandBounds(region, direction, amount, allowOverlap)) {
            sender.sendMessage(
                    miniMessage.deserialize(messages.get("commands.expand.success"),
                            parsed("region", regionKey.toString())));
        } else {
            sender.sendMessage(
                    miniMessage.deserialize(messages.get("commands.expand.fail"),
                            parsed("region", regionKey.toString())));
        }
    }

    public void handleRenameCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        RegionKey regionKey = RegionKey.fromString(args[1]);
        Region region = regionManager.regions().get(regionKey.getValue());
        Player player = null;
        if (sender instanceof Player) {
            player = (Player) sender;
        }
        if (region == null || (player != null && !region.isAdmin(player.getUniqueId()))) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.rename.provide-name")));
            return;
        }
        String name = String.join(" ", args).substring(16).replace("\"", "").replace("'", "");
        region.setName(name, regionManager);
        sender.sendMessage(miniMessage.deserialize(messages.get("commands.rename.success"),
                parsed("region", regionKey.toString()),
                parsed("name", name)));

    }

    public void handleSaveCommand(CommandSender sender) {
        regionManager.saveRegions();
        int count = regionManager.regions().size();
        sender.sendMessage(
                miniMessage.deserialize(messages.get("commands.save"), parsed("count", String.valueOf(count))));
    }

    public void handleLoadCommand(CommandSender sender) {
        regionManager.loadRegions();
        int count = regionManager.regions().size();
        sender.sendMessage(
                miniMessage.deserialize(messages.get("commands.load"), parsed("count", String.valueOf(count))));
    }

    public void handleSelectCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            Region region;
            if (args.length == 1) {
                region = regionManager.getEffectiveRegionAt(player.getLocation());
                if (region == null) {
                    plugin.particles.remove(player.getUniqueId());
                    player.sendMessage(miniMessage.deserialize(messages.get("commands.select.deselected")));
                    return;
                }
            } else {

                region = regionManager.regions().get(RegionKey.fromString(args[1]).getValue());
            }
            if (region == null) {
                player.sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
                return;
            }
            if (!plugin.particles.containsKey(player.getUniqueId()) || args.length >= 2) {
                plugin.particles.put(player.getUniqueId(), BoundingBox.of(region.getMin(), region.getMax()));
                player.sendMessage(miniMessage.deserialize(messages.get("commands.select.selected"),
                        parsed("region", region.getName())));
            } else {
                plugin.particles.remove(player.getUniqueId());
                player.sendMessage(miniMessage.deserialize(messages.get("commands.select.deselected")));
            }
        }
    }

    private void handleImportCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.import.no-plugin")));
            return;
        }
        if (args[1].equalsIgnoreCase("worldguard")) {
            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.import.not-loaded"),
                        parsed("plugin", "WorldGuard")));
                return;
            }
            WorldGuardImporter worldGuardImporter = new WorldGuardImporter(plugin);
            worldGuardImporter.importRegions();
            sender.sendMessage(miniMessage.deserialize(messages.get("commands.import.success")));
        }
    }

    private void handleModeCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            if (args.length < 2) {
                pdc.set(new NamespacedKey("zones", "mode"), PersistentDataType.STRING, Utils.Modes.CUBOID_2D.name());
                sender.sendMessage(miniMessage.deserialize(messages.get("commands.mode.set"),
                        parsed("mode", Utils.Modes.CUBOID_2D.name())));
            } else {
                Utils.Modes mode = Utils.Modes.getMode(args[1]);
                pdc.set(new NamespacedKey("zones", "mode"), PersistentDataType.STRING, mode.name());
                sender.sendMessage(
                        miniMessage.deserialize(messages.get("commands.mode.set"), parsed("mode", mode.name())));
            }
        }
    }

    private void handleFindCommand(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            if (plugin.getFindBossbar().players.containsKey(player)) {
                player.hideBossBar(plugin.getFindBossbar().players.get(player));
                plugin.getFindBossbar().players.remove(player);
            } else {
                BossBar bossbar = BossBar.bossBar(Component.text("Finding Regions..."), 1.0f, BossBar.Color.GREEN,
                        BossBar.Overlay.PROGRESS);
                plugin.getFindBossbar().players.put(player, bossbar);
                player.showBossBar(bossbar);
            }
        }
    }

    private CompletableFuture<Component> regionInfo(Region region, boolean showMembers) {
        var mm = MiniMessage.miniMessage();
        Component comp = Component.text("");
        comp = comp.append(mm.deserialize(messages.get("region.info.name"), parsed("name", region.getName())));
        comp = comp.appendNewline();
        if (region.getParent() != null) {
            comp = comp.append(
                    mm.deserialize(messages.get("region.info.parent"),
                            parsed("parent", region.getParent().toString())));
        }
        comp = comp.append(mm.deserialize(messages.get("region.info.min"), parsed("min", region.getMinString())));
        comp = comp.appendNewline();
        comp = comp.append(mm.deserialize(messages.get("region.info.max"), parsed("max", region.getMaxString())));

        if (showMembers) {
            // Iterate over members to format permissions
            for (Map.Entry<String, Map<String, String>> member : region.getMembers().entrySet()) {
                String playerName = null;
                try {
                    playerName = Bukkit.getOfflinePlayer(UUID.fromString(member.getKey())).getName();
                } catch (IllegalArgumentException ignored) {
                }
                if (playerName == null) {
                    playerName = member.getKey();
                }
                Component playerComponent = mm.deserialize(messages.get("region.info.members.name"),
                        parsed("name", playerName));
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
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.allowed"),
                                    parsed("value", value));
                        } else if ("false".equalsIgnoreCase(value) || value.startsWith("!")) {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.denied"),
                                    parsed("value", value));
                        } else {
                            formattedValue = mm.deserialize(messages.get("region.info.members.values.allowed"),
                                    parsed("value", value));
                        }
                        formattedComponents.add(formattedValue);
                    }
                    // Combine all formatted components into one line
                    Component permLine = mm.deserialize(messages.get("region.info.members.permission"),
                            parsed("permission", permKey));

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
        comp = comp.append(mm.deserialize(messages.get("region.info.key"), parsed("key", region.getKey().toString())));

        return CompletableFuture.completedFuture(comp);
    }
}
