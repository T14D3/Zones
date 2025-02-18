package de.t14d3.zones.commands;

import de.t14d3.zones.Zones;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.CompletableFuture;

public class ModeCommand {
    private Zones plugin;

    public ModeCommand(Zones plugin) {
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
                    PersistentDataContainer pdc = player.getPersistentDataContainer();
                    if (args.getRaw("mode").equalsIgnoreCase("2D")) {
                        pdc.set(new org.bukkit.NamespacedKey("zones", "mode"), PersistentDataType.STRING, "2D");
                        sender.sendMessage("2D Selection Mode Enabled");
                    } else if (args.getRaw("mode").equalsIgnoreCase("3D")) {
                        pdc.set(new org.bukkit.NamespacedKey("zones", "mode"), PersistentDataType.STRING, "3D");
                        sender.sendMessage("3D Selection Mode Enabled");
                    }
                }
            });
}
