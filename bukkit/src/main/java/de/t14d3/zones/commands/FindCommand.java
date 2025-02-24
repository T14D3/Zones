package de.t14d3.zones.commands;

import de.t14d3.zones.ZonesBukkit;
import dev.jorel.commandapi.CommandAPICommand;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class FindCommand {
    private ZonesBukkit plugin;

    public FindCommand(ZonesBukkit plugin) {
        this.plugin = plugin;
    }

    public CommandAPICommand find = new CommandAPICommand("find")
            .withPermission("zones.find")
            .executes((sender, args) -> {
                if (sender instanceof Player player) {
                    if (plugin.getFindBossbar().players.containsKey(player)) {
                        player.hideBossBar(plugin.getFindBossbar().players.get(player));
                        plugin.getFindBossbar().players.remove(player);
                    } else {
                        BossBar bossbar = BossBar.bossBar(Component.text("Finding Regions..."), 1.0f,
                                BossBar.Color.GREEN,
                                BossBar.Overlay.PROGRESS);
                        plugin.getFindBossbar().players.put(player, bossbar);
                        player.showBossBar(bossbar);
                    }
                }
            });
}
