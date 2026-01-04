package de.t14d3.zones.fabric.commands;


import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import de.t14d3.zones.utils.Utils;
import net.minecraft.commands.CommandSourceStack;

public class ModeCommand {
    private final ZonesFabric mod;

    public ModeCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendMessage(mod.getMessages().component("commands.only-player"));
            return 0;
        }

        RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
        if (player == null) return 0;

        Utils.SelectionMode mode;
        try {
            mode = Utils.SelectionMode.getMode(context.getArgument("mode", String.class));
        } catch (Exception e) {
            mode = Utils.SelectionMode.CUBOID_2D;
        }

        player.extras().put(ZonesExtraKeys.SELECTION_MODE, mode.name());
        context.getSource().sendMessage(mod.getMessages().component("commands.mode.set",
                Placeholders.builder().string("mode", mode.getName()).build()));
        return 1;
    }
}

