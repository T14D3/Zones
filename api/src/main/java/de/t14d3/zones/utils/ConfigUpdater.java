package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;

import java.io.InputStream;
import java.util.Map;

public class ConfigUpdater {
    private final Zones plugin;

    public ConfigUpdater(Zones plugin) {
        this.plugin = plugin;
        update();
    }

    private void update() {
        ConfigManager config = plugin.getConfig();

        InputStream defConfigStream = config.getDefaultConfig();

        Map<String, Object> defaultConfig = new ConfigManager(plugin).getConfigData();
        if (config.getInt("config-version", 0) == (int) defaultConfig.get("config-version")) {
            plugin.getLogger().info("Config is up to date!");
            return;
        }
        plugin.getLogger().info("Updating config...");
        for (Map.Entry<String, Object> entry : defaultConfig.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        plugin.getConfig().saveConfig();
    }
}
