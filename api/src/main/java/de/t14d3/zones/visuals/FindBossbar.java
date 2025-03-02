package de.t14d3.zones.visuals;

import de.t14d3.zones.Region;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Player;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class FindBossbar {
    private final Zones zones;
    public Map<Player, BossBar> players = new HashMap<>();
    private final BossBar.Color color;
    private final BossBar.Overlay overlay;
    private final float progress;
    private final NamedTextColor textColor;

    public FindBossbar(Zones zones) {
        this.zones = zones;
        BossBar.Color color = zones.getConfig().get("visuals.bossbar.color", BossBar.Color.class);
        this.color = color == null ? BossBar.Color.GREEN : color;
        BossBar.Overlay overlay = zones.getConfig().get("visuals.bossbar.style", BossBar.Overlay.class);
        this.overlay = overlay == null ? BossBar.Overlay.PROGRESS : overlay;
        NamedTextColor textColor = zones.getConfig().get("visuals.bossbar.text-color", NamedTextColor.class);
        this.textColor = textColor == null ? NamedTextColor.WHITE : textColor;
        float progress = zones.getConfig().getFloat("visuals.bossbar.progress", 1.0f);
        this.progress = (progress < 0.0f || progress > 1.0f) ? 1.0f : progress;

        Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(new FindBossbarRunnable(), 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }

    private class FindBossbarRunnable implements Runnable {
        @Override
        public void run() {
            for (Map.Entry<Player, BossBar> entry : players.entrySet()) {
                List<Region> regions = zones.getRegionManager()
                        .getRegionsAt(entry.getKey().getLocation(), entry.getKey().getWorld());
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
                zones.getPlatform().getAudience(entry.getKey()).showBossBar(bar);
            }
        }

    }
}
