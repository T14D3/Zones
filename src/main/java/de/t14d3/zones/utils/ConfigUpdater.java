package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigUpdater {
    private final Zones plugin;

    public ConfigUpdater(Zones plugin) {
        this.plugin = plugin;
        update();
    }

    private void update() {
        FileConfiguration config = plugin.getConfig();

        InputStream defConfigStream = plugin.getResource("config.yml");

        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
        if (config.getInt("config-version", 0) == defaultConfig.getInt("config-version", 0)) {
            plugin.getLogger().info("Config is up to date!");
            return;
        }
        plugin.getLogger().info("Updating config...");
        config.setDefaults(defaultConfig);
        config.options().copyDefaults(true);

        plugin.saveConfig();

    }
}
