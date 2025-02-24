package de.t14d3.zones.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import it.unimi.dsi.fastutil.Pair;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import static de.t14d3.zones.visuals.BeaconUtils.resetBeacon;

public class CancelCommand {
    private final MiniMessage mm = MiniMessage.miniMessage();
    private RegionManager regionManager;
    private Messages messages;
    private Zones plugin;

    public CancelCommand(Zones plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand cancel = new CommandAPICommand("cancel")
            .withPermission("zones.cancel")
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    if (plugin.selection.containsKey(player.getUniqueId())) {
                        Pair<Location, Location> selection = plugin.selection.get(player.getUniqueId());
                        resetBeacon(player, selection.first());
                        resetBeacon(player, selection.second());
                        plugin.particles.remove(player.getUniqueId());
                        player.sendMessage(mm.deserialize(messages.get("commands.cancel.success")));
                    } else {
                        player.sendMessage(mm.deserialize(messages.get("commands.cancel.success")));
                    }
                } else {
                    sender.sendMessage(mm.deserialize(messages.get("commands.only-player")));
                }
            });
}