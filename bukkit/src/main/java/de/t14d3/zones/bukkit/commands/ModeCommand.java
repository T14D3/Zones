package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import de.t14d3.zones.utils.Utils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class ModeCommand {
    private ZonesBukkit plugin;

    public ModeCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand mode = new CommandAPICommand("mode")
            .withPermission("zones.mode")
            .withArguments(
                    new StringArgument("mode")
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    StringTooltip[] suggestions = new StringTooltip[2];
                                    suggestions[0] = StringTooltip.ofString("2D", "2D Selection Mode");
                                    suggestions[1] = StringTooltip.ofString("3D", "3D Selection Mode");
                                    return suggestions;
                                });
                            })))
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    RPlayer rPlayer = RPlayer.wrap(player).orElse(null);
                    if (rPlayer == null) return;
                    if (args.getRaw("mode").equalsIgnoreCase("2D")) {
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION_MODE, Utils.SelectionMode.CUBOID_2D.name());
                        sender.sendMessage(plugin.getMessages().component("commands.mode.set",
                                Placeholders.builder().string("mode", "2D").build()));
                    } else if (args.getRaw("mode").equalsIgnoreCase("3D")) {
                        rPlayer.extras().put(ZonesExtraKeys.SELECTION_MODE, Utils.SelectionMode.CUBOID_3D.name());
                        sender.sendMessage(plugin.getMessages().component("commands.mode.set",
                                Placeholders.builder().string("mode", "3D").build()));
                    }
                }
            });
}
