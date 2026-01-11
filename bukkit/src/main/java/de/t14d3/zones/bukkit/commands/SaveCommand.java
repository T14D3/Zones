package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.bukkit.ZonesBukkit;
import dev.jorel.commandapi.CommandAPICommand;

public class SaveCommand {
    private final ZonesBukkit plugin;
    private RegionManager regionManager;
    private MessageFormatService messages;

    public SaveCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public CommandAPICommand save = new CommandAPICommand("save")
            .withPermission("zones.save")
            .executes((sender, args) -> {
                regionManager.saveRegions();
                int count = regionManager.regions().size();
                sender.sendMessage(
                        messages.component("commands.save",
                                Placeholders.builder().string("count", String.valueOf(count)).build()));
            });
}
