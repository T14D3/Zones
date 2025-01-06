package de.t14d3.zones.integrations;

import de.t14d3.zones.Zones;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPI extends PlaceholderExpansion {
    private final Zones plugin;

    public PlaceholderAPI(Zones plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "T14D3";
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "zones";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equalsIgnoreCase("get_here")) {
            return plugin.getRegionManager().getRegionsAtAsync(player.getLocation()).join().get(0).getName();
        }

        return null;
    }
}
