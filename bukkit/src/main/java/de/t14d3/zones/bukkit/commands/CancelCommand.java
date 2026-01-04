package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import dev.jorel.commandapi.CommandAPICommand;
import org.bukkit.entity.Player;


public class CancelCommand {
    private final RegionManager regionManager;
    private MessageFormatService messages;
    private ZonesBukkit plugin;

    public CancelCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand cancel = new CommandAPICommand("cancel")
            .withPermission("zones.cancel")
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
                    if (rPlayer == null) return;

                    Box selection = rPlayer.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
                    if (selection != null) {
                        RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : rPlayer.worldOrThrow()
                                .ref();
                        plugin.getPlatform().removeBeacon(rPlayer, selectionWorld, selection.getMin());
                        plugin.getPlatform().removeBeacon(rPlayer, selectionWorld, selection.getMax());
                        rPlayer.extras().remove(ZonesExtraKeys.SELECTION);
                        rPlayer.extras().remove(ZonesExtraKeys.SELECTION_CREATING);
                        player.sendMessage(messages.component("commands.cancel.success"));
                    } else {
                        player.sendMessage(messages.component("commands.cancel.success"));
                    }
                } else {
                    sender.sendMessage(messages.component("commands.only-player"));
                }
            });
}
