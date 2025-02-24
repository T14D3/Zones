package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class ConfigManager {
    private final Zones plugin;
    private ConfigurationNode configData;
    private final YamlConfigurationLoader loader;
    private File configFile = new File("plugins/Zones/config.yml");

    public ConfigManager(Zones plugin) {
        this(plugin, new File("plugins/Zones/config.yml"));
    }

    public ConfigManager(Zones plugin, File configFile) {
        this.plugin = plugin;
        this.loader = YamlConfigurationLoader.builder()
                .path(configFile.toPath())
                .build();
        loadConfig();
    }

    private void loadConfig() {
        try {
            configData = loader.load();
        } catch (IOException e) {
            plugin.getLogger().error("Failed to load config.yml: " + e.getMessage());
        }
    }

    public void saveConfig() {
        try {
            loader.save(configData);
        } catch (IOException e) {
            plugin.getLogger().error("Failed to save config.yml: " + e.getMessage());
        }
    }

    public InputStream getDefaultConfig() {
        try {
            return plugin.getClass().getResourceAsStream("/config.yml");
        } catch (Exception e) {
            plugin.getLogger().error("Failed to load default config: " + e.getMessage());
            return null;
        }
    }

    public Object get(String path) {
        return configData.node(path).getString();
    }

    public int getInt(String path) {
        return configData.node(path).getInt();
    }

    public int getInt(String path, int def) {
        return configData.node(path).getInt(def);
    }

    public String getString(String path) {
        return configData.node(path).getString();
    }

    public String getString(String path, String def) {
        return configData.node(path).getString(def);
    }

    public void set(String path, Object value) {
        try {
            configData.node((Object[]) path.split("\\.")).set(value);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
        saveConfig();
    }

    public boolean getBoolean(String path, boolean def) {
        return configData.node(path).getBoolean(def);
    }

    public ConfigurationNode getConfig() {
        return configData;
    }
}
