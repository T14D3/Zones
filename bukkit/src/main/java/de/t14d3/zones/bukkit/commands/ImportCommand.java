package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.bukkit.integrations.WorldGuardImporter;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;

import java.util.concurrent.CompletableFuture;

public class ImportCommand {
    private ZonesBukkit plugin;
    private MessageFormatService messages;

    public ImportCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand importcmd = new CommandAPICommand("import")
            .withPermission("zones.import")
            .withArguments(
                    new StringArgument("source")
                            .setOptional(false)
                            .replaceSuggestions(ArgumentSuggestions.stringsWithTooltipsAsync(info -> {
                                return CompletableFuture.supplyAsync(() -> {
                                    StringTooltip[] suggestions = new StringTooltip[1];
                                    suggestions[0] = StringTooltip.ofMessage("worldguard",
                                            BukkitTooltip.messageFromAdventureComponent(
                                                    messages.component("commands.import.worldguard-tooltip")));
                                    return suggestions;
                                });
                            })))
            .executes((sender, args) -> {
                        if (args.getRaw("source").equalsIgnoreCase("worldguard")) {
                            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                                sender.sendMessage(messages.component("commands.import.not-loaded",
                                        Placeholders.builder().string("plugin", "WorldGuard").build()));
                                return;
                            }
                            WorldGuardImporter worldGuardImporter = new WorldGuardImporter(plugin);
                            worldGuardImporter.importRegions();
                            sender.sendMessage(messages.component("commands.import.success"));
                        }
                    }
            );
}
