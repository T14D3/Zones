package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.t14d3.zones.ZonesFabric;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;

public class RootCommand {
    private final ZonesFabric mod;
    private CancelCommand cancelCommand;
    private CreateCommand createCommand;
    private DeleteCommand deleteCommand;
    private ListCommand listCommand;

    public RootCommand(ZonesFabric mod) {
        this.mod = mod;
        this.cancelCommand = new CancelCommand(mod);
        this.createCommand = new CreateCommand(mod);
        this.deleteCommand = new DeleteCommand(mod);
        this.listCommand = new ListCommand(mod);
        register();
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("zone")
                            .then(CommandManager.literal("cancel")
                                    .requires(source -> Permissions.check(source, "zones.cancel"))
                                    .executes(cancelCommand::execute))
                            .then(CommandManager.literal("create")
                                    .requires(source -> Permissions.check(source, "zones.create"))
                                    .executes(createCommand::execute))
                            .then(CommandManager.literal("delete")
                                    .requires(source -> Permissions.check(source, "zones.delete"))
                                    .then(CommandManager.argument("key", StringArgumentType.string())
                                            .executes(deleteCommand::execute)))
                            .then(CommandManager.literal("list")
                                    .requires(source -> Permissions.check(source, "zones.list"))
                                    .executes(context -> listCommand.execute(context, 1))
                                    .then(CommandManager.argument("page", IntegerArgumentType.integer())
                                            .executes(context -> listCommand.execute(context,
                                                    context.getArgument("page", Integer.class)))
                                    )
                            )
            );
        });
    }
}
