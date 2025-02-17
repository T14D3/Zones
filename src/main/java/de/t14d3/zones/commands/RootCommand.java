package de.t14d3.zones.commands;

import de.t14d3.zones.Zones;
import dev.jorel.commandapi.CommandAPICommand;

public class RootCommand {
    private final Zones plugin;

    public RootCommand(Zones plugin) {
        this.plugin = plugin;

        new CommandAPICommand("zone")
                .withSubcommand(new InfoCommand(plugin).info)
                .withSubcommand(new ListCommand(plugin).list)
                .register(plugin);
    }
}
