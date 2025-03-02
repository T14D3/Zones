package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.objects.PlayerRepository;
import de.t14d3.zones.utils.Messages;
import net.minecraft.commands.CommandSourceStack;

public class CancelCommand {
    private final Messages messages;
    private final ZonesFabric mod;

    public CancelCommand(ZonesFabric mod) {
        this.mod = mod;
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() != null) {
            Player player = PlayerRepository.get(context.getSource().getPlayer().getUUID());
            mod.getPlatform().removeBeacon(player, player.getSelection().getWorld(), player.getSelection().getMin());
            mod.getPlatform().removeBeacon(player, player.getSelection().getWorld(), player.getSelection().getMax());
            player.setSelection(null);
            player.setSelectionCreating(false);
            player.sendMessage(messages.getCmp("commands.cancel.success"));
        }
        return 1;
    }
}
