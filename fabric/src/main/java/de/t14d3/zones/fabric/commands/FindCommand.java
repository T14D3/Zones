package de.t14d3.zones.fabric.commands;

import com.mojang.brigadier.context.CommandContext;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.objects.Player;
import net.minecraft.commands.CommandSourceStack;

public class FindCommand {
    private final ZonesFabric mod;

    public FindCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() != null) {
            Player player = mod.getPlatform().getPlayer(context.getSource().getPlayer().getUUID());
            if (mod.getZones().getFindBossbar().players.containsKey(player)) {
                mod.getPlatform().getAudience(player).hideBossBar(mod.getZones().getFindBossbar().players.get(player));
                mod.getZones().getFindBossbar().players.remove(player);
            } else {
                mod.getZones().getFindBossbar().players.put(player,
                        null); // null to let the bossbar handler do the creation
            }
        } else {
            context.getSource().sendMessage(mod.getMessages().getCmp("commands.only-player"));
        }
        return 1;
    }
}
