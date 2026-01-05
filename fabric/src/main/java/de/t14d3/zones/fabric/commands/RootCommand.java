package de.t14d3.zones.fabric.commands;


import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionManager;
import de.t14d3.zones.Zones;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.utils.Messages;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyori.adventure.platform.modcommon.impl.NonWrappingComponentSerializer;
import net.kyori.adventure.platform.modcommon.impl.WrappedComponent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RootCommand {
    private final CancelCommand cancelCommand;
    private final CreateCommand createCommand;
    private final DeleteCommand deleteCommand;
    private final ListCommand listCommand;
    private final PermCommand permCommand;
    private final InfoCommand infoCommand;
    private final RenameCommand renameCommand;
    private final ExpandCommand expandCommand;
    private final ModeCommand modeCommand;
    private final FindCommand findCommand;
    private final SelectCommand selectCommand;
    private final SubCreateCommand subCreateCommand;
    private final SaveCommand saveCommand;
    private final LoadCommand loadCommand;
    private final MigrateCommand migrateCommand;

    public RootCommand(ZonesFabric mod) {
        this.cancelCommand = new CancelCommand(mod);
        this.createCommand = new CreateCommand(mod);
        this.deleteCommand = new DeleteCommand(mod);
        this.listCommand = new ListCommand(mod);
        this.permCommand = new PermCommand(mod);
        this.infoCommand = new InfoCommand(mod);
        this.renameCommand = new RenameCommand(mod);
        this.expandCommand = new ExpandCommand(mod);
        this.modeCommand = new ModeCommand(mod);
        this.findCommand = new FindCommand(mod);
        this.selectCommand = new SelectCommand(mod);
        this.subCreateCommand = new SubCreateCommand(mod);
        this.saveCommand = new SaveCommand(mod);
        this.loadCommand = new LoadCommand(mod);
        this.migrateCommand = new MigrateCommand(mod);
        register();
    }

    protected static CompletableFuture<Suggestions> regionKeySuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        final Zones zones = Zones.getInstance();
        RegionManager regionManager = zones.getRegionManager();

        boolean canSeeAll = CommandPermissions.check(context.getSource(), "zones.info.other")
                || CommandPermissions.check(context.getSource(), "zones.set.other")
                || CommandPermissions.check(context.getSource(), "zones.delete.other")
                || CommandPermissions.check(context.getSource(), "zones.expand.other");

        UUID uuid = context.getSource().getPlayer() != null ? context.getSource().getPlayer().getUUID() : null;
        List<Region> regions = new ArrayList<>();
        if (canSeeAll) {
            regions.addAll(regionManager.regions().values());
        } else if (uuid != null) {
            for (Region region : regionManager.regions().values()) {
                if (regionManager.withWorldReadLock(region.getWorld(), () -> region.isMember(uuid))) {
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
                                .requires(source -> CommandPermissions.check(source, "zones.cancel"))
                                .executes(cancelCommand::execute))
                        .then(Commands.literal("create")
                                .requires(source -> CommandPermissions.check(source, "zones.create"))
                                .executes(createCommand::execute))
                        .then(Commands.literal("delete")
                                .requires(source -> CommandPermissions.check(source, "zones.delete"))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(deleteCommand::execute)))
                        .then(Commands.literal("list")
                                .requires(source -> CommandPermissions.check(source, "zones.list"))
                                .executes(context -> listCommand.execute(context, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer())
                                        .executes(context -> listCommand.execute(context,
                                                context.getArgument("page", Integer.class)))))
                        .then(permCommand.command())
                        .then(Commands.literal("info")
                                .requires(source -> CommandPermissions.check(source, "zones.info"))
                                .executes(infoCommand::execute)
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(infoCommand::execute)))
                        .then(Commands.literal("rename")
                                .requires(source -> CommandPermissions.check(source, "zones.rename"))
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .then(Commands.argument("New Name", StringArgumentType.string())
                                                .executes(renameCommand::execute))))
                        .then(expandCommand.command())
                        .then(Commands.literal("mode")
                                .requires(source -> CommandPermissions.check(source, "zones.mode"))
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("2D");
                                            builder.suggest("3D");
                                            return builder.buildFuture();
                                        })
                                        .executes(modeCommand::execute)))
                        .then(Commands.literal("find")
                                .requires(source -> CommandPermissions.check(source, "zones.find"))
                                .executes(findCommand::execute)
                                .then(Commands.literal("nearby")
                                        .executes(findCommand::toggleNearby)
                                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                                .executes(context -> findCommand.setNearby(
                                                        context,
                                                        BoolArgumentType.getBool(context, "enabled")
                                                )))))
                        .then(Commands.literal("select")
                                .requires(source -> CommandPermissions.check(source, "zones.select"))
                                .executes(selectCommand::execute)
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(selectCommand::execute)))
                        .then(Commands.literal("subcreate")
                                .requires(source -> CommandPermissions.check(source, "zones.subcreate"))
                                .executes(subCreateCommand::execute)
                                .then(Commands.argument("key", StringArgumentType.string())
                                        .suggests(RootCommand::regionKeySuggestion)
                                        .executes(subCreateCommand::execute)))
                        .then(Commands.literal("save")
                                .requires(source -> CommandPermissions.check(source, "zones.save"))
                                .executes(saveCommand::execute))
                        .then(Commands.literal("load")
                                .requires(source -> CommandPermissions.check(source, "zones.load"))
                                .executes(loadCommand::execute))
                        .then(Commands.literal("migrate")
                                .requires(source -> CommandPermissions.check(source, "zones.migrate"))
                                .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("yaml");
                                            builder.suggest("sqlite");
                                            builder.suggest("mysql");
                                            builder.suggest("h2");
                                            builder.suggest("postgresql");
                                            builder.suggest("custom");
                                            return builder.buildFuture();
                                        })
                                        .executes(migrateCommand::execute)))
        ));
    }
}
