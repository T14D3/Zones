package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.fabric.ZonesFabric;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.CommandSourceStack;

import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed;

public class SaveCommand {
    private final ZonesFabric mod;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public SaveCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        mod.getRegionManager().saveRegions();
        context.getSource().sendMessage(mm.deserialize(mod.getMessages().get("commands.save"),
                parsed("count", String.valueOf(mod.getRegionManager().regions().size()))));
        return 1;
    }
}
