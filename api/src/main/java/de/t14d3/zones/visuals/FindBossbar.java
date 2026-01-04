package de.t14d3.zones.visuals;

import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class FindBossbar {
    private final Zones zones;
    public Map<RPlayer, BossBar> players = new HashMap<>();
    private final BossBar.Color color;
    private final BossBar.Overlay overlay;
    private final float progress;
    private final NamedTextColor textColor;

    public FindBossbar(Zones zones) {
        this.zones = zones;
        this.color = BossBar.Color.valueOf(zones.getConfig().getString("visuals.bossbar.color", "GREEN"));
        this.overlay = BossBar.Overlay.valueOf(zones.getConfig().getString("visuals.bossbar.style", "PROGRESS"));
        this.textColor = NamedTextColor.NAMES.value(zones.getConfig().getString("visuals.bossbar.text-color", "WHITE"));
        this.progress = zones.getConfig().getFloat("visuals.bossbar.progress", 1.0f);

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(new FindBossbarRunnable(), 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    private class FindBossbarRunnable implements Runnable {
        @Override
        public void run() {
            for (Map.Entry<RPlayer, BossBar> entry : players.entrySet()) {
                RPlayer player = entry.getKey();
                var loc = player.location().orElse(null);
                if (loc == null) continue;

                List<Region> regions = zones.getRegionManager()
                        .getRegionsAt(loc.blockPos(), loc.world());
                BossBar bar = entry.getValue() == null ? BossBar.bossBar(Component.text("Initializing..."), progress,
                        color, overlay) : entry.getValue();
                Component[] names = new Component[regions.size()];
                int i = 0;
                for (Region region : regions) {
                    names[i] = Component.text(region.getName() + (i == regions.size() - 1 ? "" : ", "));
                    i++;
                }
                Component text = Component.textOfChildren(names).color(textColor);
                bar.name(text);
                entry.setValue(bar);
                player.audience().showBossBar(bar);
            }
        }

    }
}
