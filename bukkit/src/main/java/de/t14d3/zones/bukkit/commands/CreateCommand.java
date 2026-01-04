package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

public class CreateCommand {
    private RegionManager regionManager;
    private MessageFormatService messages;
    private ZonesBukkit plugin;

    public CreateCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand create = new CommandAPICommand("create")
            .withPermission("zones.create")
            .withOptionalArguments(new StringArgument("name"))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
                    if (rPlayer == null) return;

                    Box selection = rPlayer.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
                    if (selection == null) {
                        var world = rPlayer.worldOrThrow().ref();
                        RBlockPos pos = new RBlockPos(player.getLocation().getBlockX(),
                                player.getLocation().getBlockY(), player.getLocation().getBlockZ());
                        selection = new Box(pos, pos, world, false);
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION, selection);
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION_CREATING, true);
                        sender.sendMessage(messages.component("commands.create.click-corners"));
                        return;
                    }
                    if (selection.getMin() != null && selection.getMax() != null) {
                        if (regionManager.overlapsExistingRegion(selection) && !sender.hasPermission(
                                "zones.create.overlap")) {
                            sender.sendMessage(messages.component("commands.create.overlap"));
                            return;
                        }
                        RegionPermissions permissions = new RegionPermissions();

                        RegionKey key = RegionKey.generate();
                        String name = (String) args.get("name");
                        if (name == null || name.isBlank()) name = key.toString();
                        var created = regionManager.createNewRegion(name, selection.getMin(),
                                selection.getMax(), selection.getWorld(), permissions, key, null, 0);
                        if (created != null) {
                            regionManager.withWorldWriteLock(created.getWorld(), () -> {
                                created.getMembership().player(player.getUniqueId()).roles().allow("owner");
                                regionManager.saveRegion(created.getKey(), created);
                            });
                        }

                        plugin.getPlatform().removeBeacon(rPlayer, selection.getWorld(), selection.getMin());
                        plugin.getPlatform().removeBeacon(rPlayer, selection.getWorld(), selection.getMax());
                        sender.sendMessage(messages.component(
                                "commands.create.success",
                                Placeholders.builder().string("region", key.toString()).build()
                        ));
                        rPlayer.extras().remove(ZonesExtraKeys.SELECTION);
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION_CREATING, false);
                    } else {
                        sender.sendMessage(messages.component("commands.create.click-corners"));
                    }
                } else {
                    sender.sendMessage(messages.component("commands.only-player"));
                }
            });
}
