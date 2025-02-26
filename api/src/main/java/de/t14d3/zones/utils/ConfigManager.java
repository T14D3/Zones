package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;

public class ConfigManager {
    private final Zones zones;
    private ConfigurationNode configData;
    private final YamlConfigurationLoader loader;
    private File dataFolder;

    /**
     * Instantiates a new ConfigManager for the given parameters.
     *
     * @param zones      Main Zones instance.
     * @param configFile File to load config from.
     */
    public ConfigManager(Zones zones, File configFile) {
        this.zones = zones;
        if (!configFile.exists()) {
            this.dataFolder = configFile.getParentFile();
            if (!dataFolder.exists()) {
                dataFolder.mkdir();
                File config = new File(dataFolder, "config.yml");
                if (!config.exists()) {
                    try {
                        Files.copy(getClass().getResourceAsStream("/config.yml"), config.toPath());
                    } catch (IOException e) {
                    }
                }
            }
        }

        this.loader = YamlConfigurationLoader.builder()
                .path(configFile.toPath())
                .build();
        loadConfig();
    }

    private void loadConfig() {
        try {
            configData = loader.load();
        } catch (IOException e) {
            zones.getLogger().error("Failed to load config.yml: {}", e.getMessage());
        }
    }

    public void saveConfig() {
        try {
            loader.save(configData);
        } catch (IOException e) {
            zones.getLogger().error("Failed to save config.yml: {}", e.getMessage());
        }
    }

    /**
     * Gets a raw object from the config.
     *
     * @param path {@code .} separated path to the value.
     * @return The value at the given path.
     */
    public <T> T get(String path, Type type) {
        String[] pathParts = path.split("\\.");
        try {
            //noinspection unchecked
            return (T) configData.node((Object[]) pathParts).get(type);
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets an integer from the config.
     * @param path {@code .} separated path to the value.
     * @return The value at the given path.
     * @see #getInt(String, int)
     */
    public int getInt(String path) {
        return configData.node(path).getInt();
    }

    /**
     * Gets an integer from the config.
     * @param path {@code .} separated path to the value.
     * @param def Default value to return if the value is not found.
     * @return The value at the given path.
     */
    public int getInt(String path, int def) {
        return configData.node(path).getInt(def);
    }

    /**
     * Gets a String from the config.
     * @param path {@code .} separated path to the value.
     * @return The value at the given path.
     * @see #getString(String, String)
     */
    public String getString(String path) {
        return configData.node(path).getString();
    }

    /**
     * Gets a String from the config.
     * @param path {@code .} separated path to the value.
     * @param def Default value to return if the value is not found.
     * @return The value at the given path.
     */
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

    /**
     * Gets a boolean from the config.
     * @param path {@code .} separated path to the value.
     * @param def Default value to return if the value is not found.
     * @return The value at the given path.
     */
    public boolean getBoolean(String path, boolean def) {
        return configData.node(path).getBoolean(def);
    }

    public ConfigurationNode getConfig() {
        return configData;
    }
}
