package de.t14d3.zones;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.brigadier.FlagArgument;
import de.t14d3.zones.brigadier.RegionKeyArgument;
import de.t14d3.zones.brigadier.SubCommandArgument;
import de.t14d3.zones.utils.Actions;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
                                        Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
                                        return 1;
                                    })
                                    .then(Commands.argument("key", new RegionKeyArgument())
                                            .suggests((ctx, builder) -> {
                                                switch (arg(ctx, 1).toUpperCase()) {
                                                    case "LIST", "CANCEL", "CREATE", "SAVE", "LOAD" -> {
                                                        return builder.buildFuture();
                                                    }
                                                    default -> {
                                                    }
                                                }
                                                boolean hasPerm = ctx.getSource().getSender().hasPermission("zones." + arg(ctx, 1).toLowerCase() + ".other");
                                                for (Map.Entry<String, Region> region : Zones.getInstance().getRegionManager().regions().entrySet()) {
                                                    if (hasPerm || ctx.getSource().getSender() instanceof Player player && region.getValue().isAdmin(player.getUniqueId())) {
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
                                                Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
                                                ctx.getSource().getSender().sendMessage(String.join(", ", ctx.getInput().split(" ")));
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(Commands.argument("player", StringArgumentType.word())
                                                    .suggests((ctx, builder) -> {
                                                        if (arg(ctx, 1).equalsIgnoreCase("INFO") || arg(ctx, 1).equalsIgnoreCase("DELETE") || arg(ctx, 1).equalsIgnoreCase("SELECT")) {
                                                            return builder.buildFuture();
                                                        }
                                                        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                                                            if (ctx.getInput().split(" ").length <= 3 || (offlinePlayer.getName() != null ? offlinePlayer.getName() : offlinePlayer.getUniqueId().toString()).toLowerCase().startsWith(arg(ctx, 3).toLowerCase())) {
                                                                builder.suggest(offlinePlayer.getName());
                                                            }
                                                        }
                                                        for (Map.Entry<String, Region> region : Zones.getInstance().getRegionManager().regions().entrySet()) {
                                                            if (!ctx.getInput().split(" ")[2].equalsIgnoreCase(region.getKey())) {
                                                                continue;
                                                            }
                                                            if (ctx.getSource().getSender().hasPermission("zones.info.other")
                                                                    || (ctx.getSource().getSender() instanceof Player player && region.getValue().isAdmin(player.getUniqueId()))) {
                                                                region.getValue().getGroupNames().forEach(group -> {
                                                                    List<String> groupMembers = new ArrayList<>();
                                                                    region.getValue().getGroupMembers(group).forEach(val -> {
                                                                        groupMembers.add(Bukkit.getOfflinePlayer(UUID.fromString(val)).getName());
                                                                    });
                                                                    if (ctx.getInput().split(" ").length <= 3
                                                                            || group.toLowerCase().startsWith(arg(ctx, 3).toLowerCase())
                                                                            || group.toLowerCase().replace("+", "").startsWith(arg(ctx, 3).toLowerCase())) {
                                                                        builder.suggest(group, MessageComponentSerializer.message().serialize(Component.text(groupMembers.toString())));
                                                                    }
                                                                });
                                                            }
                                                        }
                                                        return builder.buildFuture();
                                                    })
                                                    .then(Commands.argument("flag", new FlagArgument())
                                                            .suggests((ctx, builder) -> {
                                                                for (Actions action : Actions.values()) {
                                                                    builder.suggest(action.name().toLowerCase(), MessageComponentSerializer.message().serialize(Component.text(action.getKey())));
                                                                }
                                                                return builder.buildFuture();
                                                            })
                                                            .then(Commands.argument("value", StringArgumentType.greedyString())
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
                                                                        String[] args = ctx.getInput().split(" ");
                                                                        for (String value : types) {
                                                                            if (args.length == 5 || value.toLowerCase().startsWith(args[args.length - 1])) {
                                                                                builder.suggest(value.toLowerCase(), MessageComponentSerializer.message().serialize(
                                                                                        Component.text(value).color(value.startsWith("!") ? NamedTextColor.RED : NamedTextColor.GREEN)));
                                                                            }
                                                                        }
                                                                        return builder.buildFuture();
                                                                    })
                                                                    .executes(ctx -> {
                                                                        Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
                                                                        return Command.SINGLE_SUCCESS;
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