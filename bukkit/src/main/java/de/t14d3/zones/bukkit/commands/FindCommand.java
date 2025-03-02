package de.t14d3.zones.bukkit.commands;

import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.Player;
import dev.jorel.commandapi.CommandAPICommand;

public class FindCommand {
    private ZonesBukkit plugin;

    public FindCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand find = new CommandAPICommand("find")
            .withPermission("zones.find")
            .executes((sender, args) -> {
                if (sender instanceof org.bukkit.entity.Player nativePlayer) {
                    Player player = plugin.getPlatform().getPlayer(nativePlayer.getUniqueId());
                    if (plugin.getZones().getFindBossbar().players.containsKey(player)) {
                        plugin.getPlatform().getAudience(player)
                                .hideBossBar(plugin.getZones().getFindBossbar().players.get(player));
                        plugin.getZones().getFindBossbar().players.remove(player);
                    } else {
                        plugin.getZones().getFindBossbar().players.put(player,
                                null); // null to let the bossbar handler do the creation
                    }
                } else {
                    sender.sendMessage(plugin.getMessages().getCmp("commands.only-player"));
                }
            });
}
