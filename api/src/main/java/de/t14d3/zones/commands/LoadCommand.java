package de.t14d3.zones.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.text.minimessage.MiniMessage;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class LoadCommand {
    private Zones plugin;
    private RegionManager regionManager;
    private Messages messages;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public LoadCommand(Zones plugin) {
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
                        mm.deserialize(messages.get("commands.load"), parsed("count", String.valueOf(count))));
            });
}
