package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Messages;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RootCommand {
    private final CancelCommand cancelCommand;
    private final CreateCommand createCommand;
    private final DeleteCommand deleteCommand;
    private final ListCommand listCommand;
    private final SetCommand setCommand;
    private final InfoCommand infoCommand;
    private final RenameCommand renameCommand;
    private final ExpandCommand expandCommand;
    private final ModeCommand modeCommand;
    private final FindCommand findCommand;
    private final SelectCommand selectCommand;

    public RootCommand(ZonesFabric mod) {
        RegionManager regionManager = mod.getRegionManager();
        this.cancelCommand = new CancelCommand(mod);
        this.createCommand = new CreateCommand(mod);
        this.deleteCommand = new DeleteCommand(mod);
        this.listCommand = new ListCommand(mod);
        this.setCommand = new SetCommand(mod);
        this.infoCommand = new InfoCommand(mod);
        this.renameCommand = new RenameCommand(mod);
        this.expandCommand = new ExpandCommand(mod);
        this.modeCommand = new ModeCommand(mod);
        this.findCommand = new FindCommand(mod);
        this.selectCommand = new SelectCommand(mod);
        register();
    }

    protected static CompletableFuture<Suggestions> regionKeySuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        final Zones zones = Zones.getInstance();
        List<Region> regions = new ArrayList<>();
        Player player = context.getSource().getPlayer() != null ? zones.getPlatform().getPlayer(
                context.getSource().getPlayer().getUUID()) : null;
        if (Permissions.check(context.getSource(), "zones.set.other")) {
            regions.addAll(zones.getRegionManager().regions().values());
        } else if (player != null) {
            for (Region region : zones.getRegionManager().regions().values()) {
                if (region.isMember(player.getUniqueId())) {
                    regions.add(region);
                }
            }
        }
        for (Region region : regions) {
            builder.suggest(region.getKey().toString(), new WrappedComponent(
                    Messages.regionInfo(region, false),
                    null,
                    null,
                    NonWrappingComponentSerializer.INSTANCE
            ));
        }
        return builder.buildFuture();
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                Commands.literal("zone")
                        .then(Commands.literal("cancel")
                                .requires(source -> Permissions.check(source, "zones.cancel"))
                                .executes(cancelCommand::execute))
                        .then(Commands.literal("create")
                                .requires(source -> Permissions.check(source, "zones.create"))
                                .executes(createCommand::execute))
                        .then(Commands.literal("delete")
                                .requires(source -> Permissions.check(source, "zones.delete"))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(deleteCommand::execute)))
                        .then(Commands.literal("list")
                                .requires(source -> Permissions.check(source, "zones.list"))
                                .executes(context -> listCommand.execute(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer())
                                        .executes(context -> listCommand.execute(context,
                                                context.getArgument("page", Integer.class)))
                                ))
                        .then(setCommand.command()
                                .requires(source -> Permissions.check(source, "zones.set"))
                        )
                        .then(Commands.literal("info")
                                .requires(source -> Permissions.check(source, "zones.info"))
                                .executes(infoCommand::execute)
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(infoCommand::execute)))
                        .then(Commands.literal("rename")
                                .requires(source -> Permissions.check(source, "zones.rename"))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .then(Commands.argument("New Name", StringArgumentType.string())
                                                .executes(renameCommand::execute))))
                        .then(expandCommand.command()
                                .requires(source -> Permissions.check(source, "zones.expand")))
                        .then(Commands.literal("mode")
                                .requires(source -> Permissions.check(source, "zones.mode"))
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("2D");
                                            builder.suggest("3D");
                                            return builder.buildFuture();
                                        })
                                        .executes(modeCommand::execute)))
                        .then(Commands.literal("find")
                                .requires(source -> Permissions.check(source, "zones.find"))
                                .executes(findCommand::execute))
                        .then(Commands.literal("select")
                                .requires(source -> Permissions.check(source, "zones.select"))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(selectCommand::execute)))
        ));
    }
}
