package de.t14d3.zones.fabric.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.utils.Messages;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class DeleteCommand {
    private final ZonesFabric mod;
    private RegionManager regionManager;
    private Messages messages;

    public DeleteCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("delete")
                    .requires(source -> source.hasPermissionLevel(2)) // Adjust permission level as needed
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        source.sendMessage(Text.of("Hello, World!"));
                        // Implement command logic here
                        return 1;
                    }));
        });
    }
}
