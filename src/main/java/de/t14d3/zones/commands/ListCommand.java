package de.t14d3.zones.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Messages;
import dev.jorel.commandapi.CommandAPICommand;

public class ListCommand {
    private RegionManager regionManager;
    private Messages messages;

    public ListCommand(Zones plugin) {
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    // TODO
    public CommandAPICommand list = new CommandAPICommand("list");
}
