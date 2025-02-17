package de.t14d3.zones;

import de.t14d3.zones.permissions.CacheUtils;
import de.t14d3.zones.permissions.flags.Flags;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PaperBootstrap implements PluginBootstrap {

    private Zones zones;
    private Flags flags;
    private CacheUtils cacheUtils;

    @Override
    public void bootstrap(BootstrapContext context) {
//        CommandNode cmd = new CommandNode(this);
//        LifecycleEventManager<@NotNull BootstrapContext> eventManager = context.getLifecycleManager();
//        eventManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
//            final Commands commands = event.registrar();
//            commands.register(cmd.node());
//        });

    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        zones = new Zones(this);
        cacheUtils = new CacheUtils(zones);
        return zones;
    }


}