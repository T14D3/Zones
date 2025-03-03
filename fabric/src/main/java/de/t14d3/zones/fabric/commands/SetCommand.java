package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.permissions.flags.Flags;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.*;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class SetCommand {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final RegionManager regionManager;
    private final Messages messages;
    private final ZonesFabric mod;

    public SetCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    public LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("set")
                .requires(source -> Permissions.check(source, "zones.set"))
                .then(Commands.argument("key", StringArgumentType.string())
                        .suggests(RootCommand::regionKeySuggestion)
                        .then(Commands.argument("target", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    Region region = regionManager.regions()
                                            .get(RegionKey.fromString(context.getArgument("key", String.class))
                                                    .getValue());
                                    if (region == null) {
                                        return builder.buildFuture();
                                    } else if (!Permissions.check(context.getSource(), "zones.set.other")) {
                                        if (context.getSource().getPlayer() == null || !region.isOwner(
                                                context.getSource().getPlayer().getUUID())) {
                                            return builder.buildFuture();
                                        }
                                    }
                                    Map<String, Component> targets = new HashMap<>();
                                    region.getGroupNames().forEach(group -> {
                                        List<String> groupMembers = new ArrayList<>();
                                        region.getGroupMembers(group).forEach(
                                                val -> groupMembers.add(
                                                        mod.getPlatform().getPlayer(UUID.fromString(val))
                                                                .getName()));
                                        targets.put(group, Component.text(groupMembers.toString()));
                                    });
                                    PlayerRepository.getPlayers().forEach(player ->
                                            targets.put(player.getName(), Component.text(player.getUUID().toString())));
                                    targets.put("+universal", (messages.getCmp("flags.universal")));

                                    for (Map.Entry<String, Component> target : targets.entrySet()) {
                                        builder.suggest(target.getKey(), new WrappedComponent(
                                                target.getValue(),
                                                null,
                                                null,
                                                NonWrappingComponentSerializer.INSTANCE
                                        ));
                                    }
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("flag", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            for (Flag flag : Flags.getFlags()) {
                                                builder.suggest(flag.name(), new WrappedComponent(
                                                        messages.getCmp("flags." + flag.name()),
                                                        null,
                                                        null,
                                                        NonWrappingComponentSerializer.INSTANCE
                                                ));
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("values", StringArgumentType.greedyString())
                                                .suggests((context, builder) -> {
                                                    Flag flag = Flags.getFlag(
                                                            context.getArgument("flag", String.class));
                                                    for (String type : flag.getValidValues()) {
                                                        builder.suggest(type);
                                                    }
                                                    return builder.buildFuture();
                                                })
                                                .executes(this::execute)))
                        )
                );
    }


    int execute(CommandContext<CommandSourceStack> context) {
        RegionKey regionKey = RegionKey.fromString(context.getArgument("key", String.class));
        Region region = regionManager.regions().get(regionKey.getValue());
        if (Permissions.check(context.getSource(), "zones.set.other") ||
                (context.getSource().getPlayer() != null && region.isOwner(
                        context.getSource().getPlayer().getUUID()))) {
            StringBuilder builder = new StringBuilder();
            String[] values = context.getArgument("values", String.class).split(" ");
            for (String value : values) {
                builder.append(value).append(" ");
            }
            String target = context.getArgument("target", String.class);
            String display = target;
            if (!display.startsWith("+")) {
                Player temp = mod.getPlatform().getPlayer(target);
                if (temp != null) {
                    target = temp.getUniqueId().toString();
                }
            }
            regionManager.addMemberPermission(target, context.getArgument("flag", String.class), builder.toString(),
                    regionKey);
            context.getSource().sendMessage(miniMessage.deserialize(messages.get("commands.set.success"),
                    parsed("region", regionKey.toString()),
                    parsed("target", display),
                    parsed("permission", context.getArgument("flag", String.class)),
                    parsed("value", builder.toString())));
        } else {
            context.getSource().sendMessage(miniMessage.deserialize(messages.get("commands.invalid-region")));
        }
        return 1;
    }
}
