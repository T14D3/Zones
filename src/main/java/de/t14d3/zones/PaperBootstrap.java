package de.t14d3.zones;

import de.t14d3.zones.commands.CommandNode;
import de.t14d3.zones.permissions.Flags;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PaperBootstrap implements PluginBootstrap {

    private Zones zones;
    private Flags flags;

    @Override
    public void bootstrap(BootstrapContext context) {
        flags = new Flags();
        CommandNode cmd = new CommandNode(this);
        LifecycleEventManager<@NotNull BootstrapContext> eventManager = context.getLifecycleManager();
        eventManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(cmd.node());
        });
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        zones = new Zones(this);
        return zones;
    }

    public Flags getFlags() {
        return flags;
    }


}