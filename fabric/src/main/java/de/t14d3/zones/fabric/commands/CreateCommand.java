package de.t14d3.zones.fabric.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.utils.Messages;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CreateCommand {
    private final ZonesFabric plugin;
    private RegionManager regionManager;
    private Messages messages;

    public CreateCommand(ZonesFabric plugin) {
        this.plugin = plugin;
        this.regionManager = plugin.getRegionManager();
        this.messages = plugin.getMessages();
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("create")
                    .requires(source -> source.hasPermissionLevel(2)) // Adjust permission level as needed
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        // Implement command logic here
                        return 1;
                    }));
        });
    }
}
