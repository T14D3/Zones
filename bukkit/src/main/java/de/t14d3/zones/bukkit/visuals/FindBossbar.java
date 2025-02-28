package de.t14d3.zones.bukkit.visuals;

import de.t14d3.zones.Region;
import de.t14d3.zones.bukkit.ZonesBukkit;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.World;
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
    private final ZonesBukkit plugin;
    public Map<Player, BossBar> players = new HashMap<>();
    private final BukkitTask bossbarRunnable;

    public FindBossbar(ZonesBukkit plugin) {
        this.plugin = plugin;
        bossbarRunnable = new BossbarRunnable(plugin).runTaskTimerAsynchronously(plugin, 0, 20);
    }


    private class BossbarRunnable extends BukkitRunnable {
        private final ZonesBukkit plugin;

        public BossbarRunnable(ZonesBukkit plugin) {
            this.plugin = plugin;
        }

        @Override
        public void run() {
            for (Map.Entry<Player, BossBar> entry : players.entrySet()) {
                Player player = entry.getKey();
                List<String> regions = new ArrayList<>();
                for (Region region : plugin.getRegionManager()
                        .getRegionsAt(BlockLocation.of(player.getLocation()), World.of(player.getWorld()))) {
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
    }
}
