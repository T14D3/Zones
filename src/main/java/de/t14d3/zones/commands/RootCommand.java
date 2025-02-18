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
                .withSubcommand(new CreateCommand(plugin).create)
                .withSubcommand(new SubCreateCommand(plugin).subcreate)
                .withSubcommand(new CancelCommand(plugin).cancel)
                .withSubcommand(new SetCommand(plugin).set)
                .withSubcommand(new ExpandCommand(plugin).expand)
                .register(plugin);
    }
}
