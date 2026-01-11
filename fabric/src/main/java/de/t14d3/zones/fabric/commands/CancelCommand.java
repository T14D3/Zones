package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.MessageFormatService;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import net.minecraft.commands.CommandSourceStack;

public class CancelCommand {
    private final MessageFormatService messages;
    private final ZonesFabric mod;

    public CancelCommand(ZonesFabric mod) {
        this.mod = mod;
        this.messages = mod.getMessages();
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            return 0;
        }
        RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
        if (player == null) return 0;

        Box selection = player.extras().get(ZonesExtraKeys.SELECTION).orElse(null);
        if (selection != null) {
            RWorldRef selectionWorld = selection.getWorld() != null ? selection.getWorld() : player.worldOrThrow()
                    .ref();
            mod.getPlatform().removeBeacon(player, selectionWorld, selection.getMin());
            mod.getPlatform().removeBeacon(player, selectionWorld, selection.getMax());
        }

        player.extras().remove(ZonesExtraKeys.SELECTION);
        player.extras().put(ZonesExtraKeys.SELECTION_CREATING, false);
        player.sendMessage(messages.component("commands.cancel.success"));
        return 1;
    }
}
