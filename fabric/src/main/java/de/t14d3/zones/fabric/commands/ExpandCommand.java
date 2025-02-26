package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.*;
import de.t14d3.zones.objects.Direction;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class ExpandCommand {
    private final ZonesFabric mod;
    private RegionManager regionManager;
    private Messages messages;
    private FabricPlatform platform;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ExpandCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
        this.platform = (FabricPlatform) mod.getZones().getPlatform();
    }

    int execute(CommandContext<ServerCommandSource> context) {
        Region region = regionManager.regions()
                .get(RegionKey.fromString(context.getArgument("key", String.class)).getValue());
        if (region == null) {
            context.getSource().sendMessage(messages.getCmp("commands.invalid-region"));
            return 1;
        }
        if (!Permissions.check(context.getSource(), "zones.expand.other") &&
                (context.getSource().getPlayer() == null || !region.isOwner(
                        context.getSource().getPlayer().getUuid()))) {
            context.getSource().sendMessage(messages.getCmp("commands.invalid-region"));
            return 1;
        }
        int amount = context.getArgument("amount", Integer.class);
        boolean allowOverlap = false;
        try {
            allowOverlap = context.getArgument("overlap", Boolean.class) && Permissions.check(context.getSource(),
                    "zones.expand.overlap");
        } catch (Exception ignored) {
        }

        Direction direction;
        try {
            direction = Direction.valueOf(context.getArgument("direction", String.class).toUpperCase());
        } catch (Exception ignored) {
            if (context.getSource().getPlayer() != null) {
                direction = Direction.fromYaw(context.getSource().getPlayer().getYaw());
            } else {
                context.getSource().sendMessage(messages.getCmp("commands.invalid"));
                return 1;
            }
        }
        if (regionManager.expandBounds(region, direction, amount, allowOverlap)) {
            context.getSource().sendMessage(
                    mm.deserialize(messages.get("commands.expand.success"),
                            parsed("region", region.getKey().toString())));
        } else {
            context.getSource().sendMessage(
                    mm.deserialize(messages.get("commands.expand.fail"),
                            parsed("region", region.getKey().toString())));
        }
        return 1;
    }

    LiteralArgumentBuilder<ServerCommandSource> command() {
        return CommandManager.literal("expand")
                .requires(source -> Permissions.check(source, "zones.expand"))
                .then(CommandManager.argument("key", StringArgumentType.string())
                        .suggests(RootCommand::regionKeySuggestion)
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer())
                                .then(CommandManager.argument("direction", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("north");
                                            builder.suggest("east");
                                            builder.suggest("south");
                                            builder.suggest("west");
                                            builder.suggest("up");
                                            builder.suggest("down");
                                            return builder.buildFuture();
                                        })
                                        .executes(this::execute)
                                        .then(CommandManager.argument("overlap", BoolArgumentType.bool())
                                                .requires(source -> Permissions.check(source, "zones.expand.overlap"))
                                                .executes(this::execute)))));
    }
}
