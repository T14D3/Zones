package de.t14d3.zones.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.t14d3.zones.PaperBootstrap;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.permissions.Flag;
import de.t14d3.zones.permissions.Flags;
import de.t14d3.zones.utils.Types;
import de.t14d3.zones.utils.Utils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.MessageComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class CommandNode {
    private final PaperBootstrap context;

    public CommandNode(PaperBootstrap context) {
        this.context = context;
    }

    /**
     * Helper function to get the argument of a command at the given index
     *
     * @param ctx   Command context
     * @param index Index of the argument
     * @return Argument at the given index
     */
    public static String arg(CommandContext<CommandSourceStack> ctx, int index) {
        return args(ctx)[index];
    }

    public static String[] args(CommandContext<CommandSourceStack> ctx) {
        String arg = ctx.getInput().replace("zones:", "");
        return arg.split(" ");
    }

    public LiteralCommandNode<CommandSourceStack> node() {
        return Commands.literal("zone").executes(ctx -> {
            MiniMessage mm = MiniMessage.miniMessage();
            Component comp = mm.deserialize(Zones.getInstance().getMessages().get("commands.invalid"));
            ctx.getSource().getSender().sendMessage(comp);
            return 1;
        }).then(Commands.argument("subcommand", new SubCommandArgument()).suggests((ctx, builder) -> {
            for (SubCommands subCommand : SubCommands.values()) {
                if (!ctx.getSource().getSender().hasPermission("zones." + subCommand.name().toLowerCase())) {
                    continue;
                }
                String[] args = ctx.getInput().split(" ");
                if (args.length == 1 || subCommand.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                    builder.suggest(subCommand.name().toLowerCase(),
                            MessageComponentSerializer.message().serialize(subCommand.getInfo()));
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
            return 1;
        }).then(Commands.argument("key", new RegionKeyArgument()).suggests((ctx, builder) -> {
            switch (arg(ctx, 1).toUpperCase()) {
                case "LIST", "CANCEL", "CREATE", "SAVE", "LOAD", "FIND" -> {
                    return builder.buildFuture();
                }
                case "IMPORT" -> {
                    builder.suggest("worldguard", MessageComponentSerializer.message()
                            .serialize(Component.text("Imports regions from WorldGuard")));
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
            boolean hasPerm = ctx.getSource().getSender()
                    .hasPermission("zones." + arg(ctx, 1).toLowerCase() + ".other");
            for (Region region : Zones.getInstance().getRegionManager().regions().values()) {
                if (hasPerm || ctx.getSource().getSender() instanceof Player player && region.isAdmin(
                        player.getUniqueId())) {
                    builder.suggest(region.getKey().toString(),
                            MessageComponentSerializer.message().serialize(RegionKeyArgument.regionInfo(region)));
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
            return 1;
        }).then(Commands.argument("name", StringArgumentType.string()).suggests((ctx, builder) -> {
            if (arg(ctx, 1).equalsIgnoreCase("RENAME")) {
                builder.suggest("<New Name>", MessageComponentSerializer.message()
                        .serialize(Component.text("Type the new name for the region")));
                return builder.buildFuture();
            }
            if (arg(ctx, 1).equalsIgnoreCase("SET")) {
                Utils.getPlayerNames().forEach(name -> {
                    if (name == null) {
                        return;
                    }
                    if (ctx.getInput().split(" ").length <= 3 || name.toLowerCase()
                            .startsWith(arg(ctx, 3).toLowerCase())) {
                        builder.suggest(name);
                    }
                });
                boolean perm = ctx.getSource().getSender().hasPermission("zones.info.other");
                for (Region region : Zones.getInstance().getRegionManager().regions().values()) {
                    String[] args = ctx.getInput().split(" ");
                    if (args.length <= 2 || !args[2].equalsIgnoreCase(region.getKey().toString())) {
                        continue;
                    }
                    if (perm || (ctx.getSource().getSender() instanceof Player player && region.isAdmin(
                            player.getUniqueId()))) {
                        region.getGroupNames().forEach(group -> {
                            List<String> groupMembers = new ArrayList<>();
                            region.getGroupMembers(group).forEach(val -> {
                                groupMembers.add(Utils.getPlayerName(UUID.fromString(val)));
                            });
                            if (ctx.getInput().split(" ").length <= 3 || group.toLowerCase()
                                    .startsWith(arg(ctx, 3).toLowerCase()) || group.toLowerCase().replace("+", "")
                                    .startsWith(arg(ctx, 3).toLowerCase())) {
                                builder.suggest(group, MessageComponentSerializer.message()
                                        .serialize(Component.text(groupMembers.toString())));
                            }
                        });
                    }
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
            return 1;
        }).then(Commands.argument("flag", new FlagArgument(context)).suggests((ctx, builder) -> {
            Flags flags = context.getFlags();
            MiniMessage mm = MiniMessage.miniMessage();
            for (Flag entry : Flags.getFlags()) {
                builder.suggest(entry.name(), MessageComponentSerializer.message().serialize(mm.deserialize(
                        Zones.getInstance().getMessages()
                                .getOrDefault("flags." + entry.name(), entry.getDescription()))));
            }
            return builder.buildFuture();
        }).then(Commands.argument("value", StringArgumentType.greedyString()).suggests((ctx, builder) -> {
            Zones plugin = Zones.getInstance();
            List<String> types;
            String[] args = args(ctx);
            switch (arg(ctx, 4).toUpperCase()) {
                case "PLACE", "BREAK" -> types = Types.blocks();
                case "CONTAINER" -> types = Types.containers();
                case "REDSTONE" -> types = Types.redstone();
                case "ENTITY", "DAMAGE" -> types = Types.entities();
                case "IGNITE" -> types = List.of("TRUE", "FALSE");
                case "GROUP" -> {
                    types = new ArrayList<>();
                    boolean perm = ctx.getSource().getSender().hasPermission("zones.info.other");
                    for (Region region : Zones.getInstance().getRegionManager().regions().values()) {
                        if (!ctx.getInput().split(" ")[2].equalsIgnoreCase(region.getKey().toString())) {
                            continue;
                        }
                        if (perm || (ctx.getSource().getSender() instanceof Player player && region.isAdmin(
                                player.getUniqueId()))) {
                            types.addAll(region.getGroupNames());
                        }
                    }
                }
                default -> types = Flags.getFlag(arg(ctx, 4)).getValidValues();
            }

            final String[] tokens = ctx.getInput().split(" ", 6);
            final String input = tokens.length == 6 ? tokens[5].toLowerCase() : "";
            final String prefix = input.contains(" ") ? input.substring(0, input.lastIndexOf(' ') + 1) : "";
            final boolean isBase = args.length == 5 || input.endsWith(" ");
            final String lastArg = args[args.length - 1].toLowerCase();

            for (String type : types) {
                if (isBase || type.startsWith(lastArg)) {
                    builder.suggest(prefix.concat(type));
                }
            }
            return builder.buildFuture();
        }).executes(ctx -> {
            Zones.getInstance().getCommandListener().execute(ctx.getSource(), ctx.getInput());
            return 1;
        })))))).build();
    }
}
