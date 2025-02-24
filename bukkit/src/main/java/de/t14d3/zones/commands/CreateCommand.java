package de.t14d3.zones.commands;

import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.StringArgument;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static de.t14d3.zones.visuals.BeaconUtils.resetBeacon;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class CreateCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private Zones plugin;

    public CreateCommand(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand create = new CommandAPICommand("create")
            .withPermission("zones.create")
            .withOptionalArguments(new StringArgument("name"))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    if (!plugin.selection.containsKey(player.getUniqueId())) {
                        plugin.selection.put(player.getUniqueId(), Pair.of(null, null));
                        sender.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                        return;
                    }
                    Pair<Location, Location> selectionPair = plugin.selection.get(player.getUniqueId());
                    if (selectionPair.first() != null && selectionPair.second() != null) {
                        if (regionManager.overlapsExistingRegion(selectionPair.first(),
                                selectionPair.second()) && !sender.hasPermission("zones.create.overlap")) {
                            sender.sendMessage(mm.deserialize(messages.get("commands.create.overlap")));
                            return;
                        }
                        Map<String, String> perms = new HashMap<>();
                        perms.put("role", "owner");
                        Utils.Modes mode = Utils.Modes.getPlayerMode(player);
                        RegionKey key;
                        if (mode == Utils.Modes.CUBOID_3D && player.hasPermission("zones.mode.3d.main")) {
                            key = regionManager.createNewRegion(sender.getName(), selectionPair.first(),
                                    selectionPair.second(),
                                    player.getUniqueId(), perms).getKey();
                        } else {
                            key = regionManager.create2DRegion(sender.getName(), selectionPair.first(),
                                    selectionPair.second(),
                                    player.getUniqueId(), perms).getKey();
                        }
                        resetBeacon(player, selectionPair.first());
                        resetBeacon(player, selectionPair.second());
                        sender.sendMessage(
                                mm.deserialize(messages.get("commands.create.success"),
                                        parsed("region", key.toString())));
                        plugin.selection.remove(player.getUniqueId());
                    } else {
                        sender.sendMessage(mm.deserialize(messages.get("commands.create.click-corners")));
                    }
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.only-player")));
                }
            });
}
