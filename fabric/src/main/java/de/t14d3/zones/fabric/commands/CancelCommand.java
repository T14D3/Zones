package de.t14d3.zones.fabric.commands;

import de.t14d3.zones.RegionManager;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Messages;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.kyori.adventure.text.Component;
import net.minecraft.command.ControlFlowAware;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class CancelCommand {
    private final ZonesFabric mod;
    private RegionManager regionManager;
    private Messages messages;

    public CancelCommand(ZonesFabric mod) {
        this.mod = mod;
        this.regionManager = mod.getRegionManager();
        this.messages = mod.getMessages();
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("cancel")
                    .requires(source -> source.hasPermissionLevel(2)) // Adjust permission level as needed
                    .executes(context -> {
                        ServerCommandSource source = context.getSource();
                        Player player = new Player(source.getPlayer().getUuid(),
                                source.getPlayer().getName().getString());
                        player.sendMessage(Component.text("Hello, World!"));
                        // Implement command logic here
                        return ControlFlowAware.Command.SINGLE_SUCCESS;
                    }));
        });
    }
}
