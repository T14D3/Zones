package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ConfigUpdater {
    private final Zones plugin;

    public ConfigUpdater(Zones plugin) {
        this.plugin = plugin;
        update();
    }

    private void update() {
        ConfigManager configManager = new ConfigManager(plugin, new File(plugin.getDataFolder(), "config.yml"));
        ConfigurationNode defaultConfig = loadDefaultConfig();
        ConfigurationNode currentConfig = configManager.getConfig();

        if (defaultConfig == null || currentConfig == null) {
            plugin.getLogger().error("Cannot update config: default or current config is null.");
            return;
        }

        for (Object key : defaultConfig.childrenMap().keySet()) {
            ConfigurationNode currentNode = currentConfig.node(key);
            if (!currentNode.virtual()) {
                continue; // Skip if the current node is not virtual
            }
            try {
                currentNode.set(defaultConfig.node(key).get(Object.class));
                plugin.getLogger().info("Setting missing config key: {}", key);
            } catch (SerializationException e) {
                plugin.getLogger().error("Failed to serialize key {}: {}", key, e.getMessage());
            }
        }
        configManager.saveConfig();
    }

    private ConfigurationNode loadDefaultConfig() {
        try (InputStream inputStream = plugin.getClass().getResourceAsStream("/config.yml")) {
            return YamlConfigurationLoader.builder()
                    .path(new File("plugins/Zones/config.yml").toPath())
                    .build()
                    .load();
        } catch (IOException e) {
            plugin.getLogger().error("Failed to load default config: {}", e.getMessage());
            return null;
        }
    }
}
