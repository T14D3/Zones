package de.t14d3.zones.bukkit.commands;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.rapunzellib.ZonesExtraKeys;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;

public class FindCommand {
    private ZonesBukkit plugin;

    public FindCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand find = new CommandAPICommand("find")
            .withPermission("zones.find")
            .withSubcommand(new CommandAPICommand("nearby")
                    .withOptionalArguments(new BooleanArgument("enabled"))
                    .executes((sender, args) -> {
                        if (sender instanceof org.bukkit.entity.Player nativePlayer) {
                            RPlayer player = RPlayer.wrap(nativePlayer).orElse(null);
                            if (player == null) return;

                            Boolean requested = (Boolean) args.get("enabled");
                            boolean current = player.extras().get(ZonesExtraKeys.NEARBY_VISUALS).orElse(
                                    plugin.getZones().getConfig().getBoolean("visuals.particles.nearby.enabled", false)
                            );
                            boolean next = requested != null ? requested : !current;
                            player.extras().put(ZonesExtraKeys.NEARBY_VISUALS, next);
                            sender.sendMessage(plugin.getMessages().component(
                                    next ? "commands.find.nearby.enabled" : "commands.find.nearby.disabled"
                            ));
                        } else {
                            sender.sendMessage(plugin.getMessages().component("commands.only-player"));
                        }
                    }))
            .executes((sender, args) -> {
                if (sender instanceof org.bukkit.entity.Player nativePlayer) {
                    RPlayer player = RPlayer.wrap(nativePlayer).orElse(null);   
                    if (player == null) return;

                    if (plugin.getZones().getFindBossbar().players.containsKey(player)) {
                        var bar = plugin.getZones().getFindBossbar().players.get(player);
                        if (bar != null) {
                            player.audience().hideBossBar(bar);
                        }
                        plugin.getZones().getFindBossbar().players.remove(player);
                    } else {
                        plugin.getZones().getFindBossbar().players.put(player, null);
                    }
                } else {
                    sender.sendMessage(plugin.getMessages().component("commands.only-player"));
                }
            });
}
