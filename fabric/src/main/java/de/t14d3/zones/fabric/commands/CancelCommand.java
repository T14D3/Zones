package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Messages;
import de.t14d3.zones.utils.PlayerRepository;
import net.minecraft.command.ControlFlowAware;
import net.minecraft.server.command.ServerCommandSource;

public class CancelCommand {
    private final Messages messages;

    public CancelCommand(ZonesFabric mod) {
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<ServerCommandSource> context) {
        if (context.getSource().getPlayer() != null) {
            Player player = PlayerRepository.get(context.getSource().getPlayer().getUuid());
            player.setSelection(null);
            player.setSelectionCreating(false);
            player.sendMessage(messages.getCmp("commands.cancel.success"));
        }
        return ControlFlowAware.Command.SINGLE_SUCCESS;
    }
}
