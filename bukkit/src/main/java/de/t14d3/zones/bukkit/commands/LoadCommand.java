package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import dev.jorel.commandapi.CommandAPICommand;

public class LoadCommand {
    private final ZonesBukkit plugin;
    private RegionManager regionManager;
    private MessageFormatService messages;

    public LoadCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand load = new CommandAPICommand("load")
            .withPermission("zones.load")
            .executes((sender, args) -> {
                regionManager.loadRegions();
                int count = regionManager.regions().size();
                sender.sendMessage(
                        messages.component("commands.load",
                                Placeholders.builder().string("count", String.valueOf(count)).build()));
            });
}
