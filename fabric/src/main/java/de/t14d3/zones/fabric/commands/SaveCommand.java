package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.message.Placeholders;
import de.t14d3.zones.fabric.ZonesFabric;
import net.minecraft.commands.CommandSourceStack;

public class SaveCommand {
    private final ZonesFabric mod;

    public SaveCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        mod.getRegionManager().saveRegions();
        context.getSource().sendMessage(mod.getMessages().component("commands.save",
                Placeholders.builder().string("count", String.valueOf(mod.getRegionManager().regions().size()))
                        .build()));
        return 1;
    }
}
