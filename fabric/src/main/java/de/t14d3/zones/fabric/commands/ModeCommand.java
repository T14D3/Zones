package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Player;
import de.t14d3.zones.utils.Utils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class ModeCommand {
    private final ZonesFabric mod;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ModeCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() != null) {
            Player player = mod.getPlatform().getPlayer(context.getSource().getPlayer().getUUID());
            Utils.SelectionMode mode;
            try {
                mode = Utils.SelectionMode.valueOf(context.getArgument("mode", String.class));
            } catch (Exception e) {
                mode = Utils.SelectionMode.CUBOID_3D;
            }
            player.setMetadata("mode", mode.name());
            context.getSource().sendMessage(mm.deserialize(mod.getMessages().get("commands.mode.set"),
                    parsed("mode", mode.name())));
        } else {
            context.getSource().sendMessage(mod.getMessages().getCmp("commands.only-player"));
        }
        return 1;
    }
}
