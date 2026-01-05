package de.t14d3.zones.fabric.commands;


import com.mojang.brigadier.context.CommandContext;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.fabric.ZonesFabric;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import net.minecraft.commands.CommandSourceStack;

public class FindCommand {
    private final ZonesFabric mod;

    public FindCommand(ZonesFabric mod) {
        this.mod = mod;
    }

    int execute(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendMessage(mod.getMessages().component("commands.only-player"));
            return 0;
        }

        RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
        if (player == null) return 0;

        if (mod.getZones().getFindBossbar().players.containsKey(player)) {
            var bar = mod.getZones().getFindBossbar().players.get(player);
            if (bar != null) {
                player.audience().hideBossBar(bar);
            }
            mod.getZones().getFindBossbar().players.remove(player);
        } else {
            mod.getZones().getFindBossbar().players.put(player, null);
        }
        return 1;
    }

    int toggleNearby(CommandContext<CommandSourceStack> context) {
        return setNearby(context, null);
    }

    int setNearby(CommandContext<CommandSourceStack> context, Boolean enabled) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendMessage(mod.getMessages().component("commands.only-player"));
            return 0;
        }

        RPlayer player = RPlayer.wrap(context.getSource().getPlayer()).orElse(null);
        if (player == null) return 0;

        boolean current = player.extras().get(ZonesExtraKeys.NEARBY_VISUALS).orElse(
                mod.getZones().getConfig().getBoolean("visuals.particles.nearby.enabled", false)
        );
        boolean next = enabled != null ? enabled : !current;
        player.extras().put(ZonesExtraKeys.NEARBY_VISUALS, next);

        context.getSource().sendMessage(mod.getMessages().component(
                next ? "commands.find.nearby.enabled" : "commands.find.nearby.disabled"
        ));
        return 1;
    }
}
