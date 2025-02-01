package de.t14d3.zones.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.t14d3.zones.PaperBootstrap;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.utils.Flag;
import de.t14d3.zones.utils.Flags;
import de.t14d3.zones.utils.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class CommandNode {
    private final PaperBootstrap context;

    public CommandNode(PaperBootstrap context) {
        this.context = context;
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("zone")
                .executes(ctx -> {
                    MiniMessage mm = MiniMessage.miniMessage();
                    Component comp = mm.deserialize(Zones.getInstance().getMessages().get("commands.invalid"));
                    ctx.getSource().getSender().sendMessage(comp);
                    return 1;
                })
                .then(Commands.argument("subcommand", new SubCommandArgument())
                        .suggests((ctx, builder) -> {
                            for (SubCommands subCommand : SubCommands.values()) {
                                if (!ctx.getSource().getSender().hasPermission("zones." + subCommand.name().toLowerCase())) {
                                    continue;
                                }
                                String[] args = ctx.getInput().split(" ");
                                if (args.length == 1 || subCommand.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                                    builder.suggest(subCommand.name().toLowerCase(), MessageComponentSerializer.message().serialize(subCommand.getInfo()));
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> {
                            Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
                            return 1;
                        })
                        .then(Commands.argument("key", new RegionKeyArgument())
                                .suggests((ctx, builder) -> {
                                    switch (arg(ctx, 1).toUpperCase()) {
                                        case "LIST", "CANCEL", "CREATE", "SAVE", "LOAD", "FIND" -> {
                                            return builder.buildFuture();
                                        }
                                        case "IMPORT" -> {
                                            builder.suggest("worldguard", MessageComponentSerializer.message().serialize(Component.text("Imports regions from WorldGuard")));
                                            return builder.buildFuture();
                                        }
                                        case "MODE" -> {
                                            for (Utils.Modes mode : Utils.Modes.values()) {
                                                builder.suggest(mode.name());
                                            }
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
                                    return 1;
                                })
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            if (arg(ctx, 1).equalsIgnoreCase("RENAME")) {
                                                builder.suggest("<New Name>", MessageComponentSerializer.message().serialize(Component.text("Type the new name for the region")));
                                                return builder.buildFuture();
                                            }
                                            if (arg(ctx, 1).equalsIgnoreCase("SET")) {
                                                Utils.getPlayerNames().forEach(name -> {
                                                    if (name == null) {
                                                        return;
                                                    }
                                                    if (ctx.getInput().split(" ").length <= 3 || name.toLowerCase().startsWith(arg(ctx, 3).toLowerCase())) {
                                                        builder.suggest(name);
                                                    }
                                                });
                                                boolean perm = ctx.getSource().getSender().hasPermission("zones.info.other");
                                                for (Map.Entry<String, Region> region : Zones.getInstance().getRegionManager().regions().entrySet()) {
                                                    String[] args = ctx.getInput().split(" ");
                                                    if (args.length <= 2 || !args[2].equalsIgnoreCase(region.getKey())) {
                                                        continue;
                                                    }
                                                    if (perm || (ctx.getSource().getSender() instanceof Player player && region.getValue().isAdmin(player.getUniqueId()))) {
                                                        region.getValue().getGroupNames().forEach(group -> {
                                                            List<String> groupMembers = new ArrayList<>();
                                                            region.getValue().getGroupMembers(group).forEach(val -> {
                                                                groupMembers.add(Utils.getPlayerName(UUID.fromString(val)));
                                                            });
                                                            if (ctx.getInput().split(" ").length <= 3
                                                                    || group.toLowerCase().startsWith(arg(ctx, 3).toLowerCase())
                                                                    || group.toLowerCase().replace("+", "").startsWith(arg(ctx, 3).toLowerCase())) {
                                                                builder.suggest(group, MessageComponentSerializer.message().serialize(Component.text(groupMembers.toString())));
                                                            }
                                                        });
                                                    }
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
                                            return 1;
                                        })
                                        .then(Commands.argument("flag", new FlagArgument(context))
                                                .suggests((ctx, builder) -> {
                                                    Flags flags = context.getFlags();
                                                    MiniMessage mm = MiniMessage.miniMessage();
                                                    for (Flag entry : Flags.getFlags()) {
                                                        builder.suggest(entry.name(),
                                                                MessageComponentSerializer.message().serialize(
                                                                        mm.deserialize(Zones.getInstance().getMessages().getOrDefault("flags." + entry.name(), entry.getDescription()))
                                                                ));
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
                                                                case "IGNITE" -> types = List.of("TRUE", "FALSE");
                                                                case "GROUP" -> {
                                                                    types = new ArrayList<>();
                                                                    boolean perm = ctx.getSource().getSender().hasPermission("zones.info.other");
                                                                    for (Map.Entry<String, Region> region : Zones.getInstance().getRegionManager().regions().entrySet()) {
                                                                        if (!ctx.getInput().split(" ")[2].equalsIgnoreCase(region.getKey())) {
                                                                            continue;
                                                                        }
                                                                        if (perm || (ctx.getSource().getSender() instanceof Player player && region.getValue().isAdmin(player.getUniqueId()))) {
                                                                            types.addAll(region.getValue().getGroupNames());
                                                                        }
                                                                    }
                                                                }
                                                                default -> types = plugin.getTypes().allTypes;
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
                                                            return 1;
                                                        })
                                                )
                                        )
                                )
                        )
                ).build();
    }

    /**
     * Helper function to get the argument of a command at the given index
     *
     * @param ctx   Command context
     * @param index Index of the argument
     * @return Argument at the given index
     */
    public static String arg(CommandContext<CommandSourceStack> ctx, int index) {
        String arg = ctx.getInput().replace("zones:", "");
        return arg.split(" ")[index];
    }
}
