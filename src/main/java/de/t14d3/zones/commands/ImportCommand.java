package de.t14d3.zones.commands;

import de.t14d3.zones.Zones;
import de.t14d3.zones.integrations.WorldGuardImporter;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.BukkitTooltip;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.StringTooltip;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.CompletableFuture;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class ImportCommand {
    private Zones plugin;
    private Messages messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ImportCommand(Zones plugin) {
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
                                                    Component.text("Imports regions from WorldGuard")));
                                    return suggestions;
                                });
                            })))
            .executes((sender, args) -> {
                        if (args.getRaw("source").equalsIgnoreCase("worldguard")) {
                            if (plugin.getServer().getPluginManager().getPlugin("WorldGuard") == null) {
                                sender.sendMessage(mm.deserialize(messages.get("commands.import.not-loaded"),
                                        parsed("plugin", "WorldGuard")));
                                return;
                            }
                            WorldGuardImporter worldGuardImporter = new WorldGuardImporter(plugin);
                            worldGuardImporter.importRegions();
                            sender.sendMessage(mm.deserialize(messages.get("commands.import.success")));
                        }
                    }
            );
}
