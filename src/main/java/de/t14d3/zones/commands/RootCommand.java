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
                .withSubcommand(new SelectCommand(plugin).select)
                .withSubcommand(new RenameCommand(plugin).rename)
                .withSubcommand(new DeleteCommand(plugin).delete)
                .withSubcommand(new SaveCommand(plugin).save)
                .withSubcommand(new LoadCommand(plugin).load)
                .withSubcommand(new ImportCommand(plugin).importcmd)
                .withSubcommand(new ModeCommand(plugin).mode)
                .withSubcommand(new FindCommand(plugin).find)
                .register(plugin);
    }
}
