package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.fabric.ZonesFabric;
import net.minecraft.commands.CommandSourceStack;

public class LoadCommand {
    private final ZonesFabric mod;

    public LoadCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        mod.getRegionManager().loadRegions();
        int count = mod.getRegionManager().regions().size();
        context.getSource().sendMessage(mod.getMessages().component("commands.load",
                Placeholders.builder().string("count", String.valueOf(count)).build()));
        return 1;
    }
}
