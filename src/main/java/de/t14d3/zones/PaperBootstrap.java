package de.t14d3.zones;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.brigadier.*;
import de.t14d3.zones.utils.Actions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class PaperBootstrap implements PluginBootstrap {

    private Zones zones;

    @Override
    public void bootstrap(BootstrapContext context) {
        LifecycleEventManager<BootstrapContext> eventManager = context.getLifecycleManager();
        eventManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    Commands.literal("zone")
                            .then(Commands.argument("subcommand", new SubCommandArgument())
                                    .executes(ctx -> {
                                        Zones.getInstance().getCommandListener().execute(ctx.getSource(), new String[]{ctx.getArgument("subcommand", SubCommands.class).name()});
                                        ctx.getSource().getSender().sendMessage(ctx.getInput());
                                        return 1;
                                    })
                                    .then(Commands.argument("key", new RegionKeyArgument())
                                            .suggests((ctx, builder) -> {
                                                for (Map.Entry<String, Region> region : Zones.getInstance().getRegionManager().regions().entrySet()) {
                                                    if (ctx.getSource().getSender().hasPermission("zones.info.other")
                                                            || (ctx.getSource().getSender() instanceof Player player && region.getValue().isAdmin(player.getUniqueId()))) {
                                                        builder.suggest(
                                                                region.getKey(),
                                                                MessageComponentSerializer.message().serialize(
                                                                        RegionKeyArgument.regionInfo(Map.entry(region.getKey(), region.getValue()))
                                                                )
                                                        );
                                                    }
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput().substring(5).split(" "));
                                                ctx.getSource().getSender().sendMessage("arg: " + ctx.getInput());
                                                ctx.getSource().getSender().sendMessage("root: " + ctx.getRootNode().getName());
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(Commands.argument("player", new NameArgument())
                                                    .suggests((ctx, builder) -> {
                                                        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                                            builder.suggest(offlinePlayer.getName());
                                                        }
                                                        return builder.buildFuture();
                                                    })
                                                    .executes(ctx -> {
                                                        Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput().substring(5).split(" "));
                                                        ctx.getSource().getSender().sendMessage("arg: " + ctx.getInput());
                                                        ctx.getSource().getSender().sendMessage("root: " + ctx.getRootNode().getName());
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                                    .then(Commands.argument("flag", new FlagArgument())
                                                            .suggests((ctx, builder) -> {
                                                                for (Actions action : Actions.values()) {
                                                                    builder.suggest(action.name(), MessageComponentSerializer.message().serialize(Component.text(action.getKey())));
                                                                }
                                                                return builder.buildFuture();
                                                            })
                                                            .executes(ctx -> {
                                                                Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput().substring(5).split(" "));
                                                                ctx.getSource().getSender().sendMessage("arg: " + ctx.getInput());
                                                                ctx.getSource().getSender().sendMessage("root: " + ctx.getRootNode().getName());
                                                                return Command.SINGLE_SUCCESS;
                                                            })
                                                            .then(Commands.argument("value", ArgumentTypes.resource(RegistryKey.BLOCK))
                                                                    .suggests((ctx, builder) -> {
                                                                        Zones plugin = Zones.getInstance();
                                                                        List<String> types;
                                                                        switch (arg(ctx, 4).toUpperCase()) {
                                                                            case "PLACE", "BREAK" ->
                                                                                    types = plugin.getTypes().blockTypes;
                                                                            case "CONTAINER" ->
                                                                                    types = plugin.getTypes().containerTypes;
                                                                            case "REDSTONE" ->
                                                                                    types = plugin.getTypes().redstoneTypes;
                                                                            case "ENTITY", "DAMAGE" ->
                                                                                    types = plugin.getTypes().entityTypes;
                                                                            case "IGNITE" ->
                                                                                    types = List.of("TRUE", "FALSE");
                                                                            default ->
                                                                                    types = plugin.getTypes().allTypes;
                                                                        }
                                                                        for (String value : types) {
                                                                            builder.suggest(value, MessageComponentSerializer.message().serialize(Component.text(value)));
                                                                        }
                                                                        return builder.buildFuture();
                                                                    })
                                                            )
                                                    )
                                            )
                                    )
                            ).build()
            );
        });
    }

    @Override
    public JavaPlugin createPlugin(PluginProviderContext context) {
        zones = new Zones();
        return zones;
    }

    /**
     * Helper function to get the argument of a command at the given index
     *
     * @param ctx   Command context
     * @param index Index of the argument
     * @return Argument at the given index
     */
    public static String arg(CommandContext<CommandSourceStack> ctx, int index) {
        return ctx.getInput().split(" ")[index];
    }
}