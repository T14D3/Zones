package de.t14d3.zones.visuals;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FindBossbar {
    private final Zones plugin;
    private BukkitTask bossbarRunnable;
    public Map<Player, BossBar> players = new HashMap<>();

    public FindBossbar(Zones plugin) {
        this.plugin = plugin;
        bossbarRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<Player, BossBar> entry : players.entrySet()) {
                    Player player = entry.getKey();
                    List<String> regions = new ArrayList<>();
                    for (Region region : plugin.getRegionManager().getRegionsAt(player.getLocation())) {
                        regions.add(region.getName());
                    }
                    StringBuilder builder = new StringBuilder();
                    for (String name : regions) {
                        if (!builder.isEmpty()) {
                            builder.append(", ");
                        }
                        builder.append(name);
                    }
                    BossBar bossbar = entry.getValue();
                    bossbar.name(Component.text(String.valueOf(builder)).color(NamedTextColor.GREEN));
                    player.showBossBar(bossbar);
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0, 20);
    }


}
