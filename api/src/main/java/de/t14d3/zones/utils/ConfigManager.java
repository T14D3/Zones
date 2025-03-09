package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final Zones zones;
    private final YamlFile configFile;
    private Utils.SavingModes savingMode;

    public ConfigManager(Zones zones, File file) {
        this.zones = zones;
        this.configFile = new YamlFile(file);

        loadConfig();
    }

    private void loadConfig() {
        savingMode = Utils.SavingModes.fromString(
                configFile.getString("zone-saving.mode", "MODIFIED")
        );
    }

    public void saveConfig() {
        try {
            configFile.save();
        } catch (IOException e) {
            zones.getLogger().error("Failed to save config: {}", e.getMessage());
        }
    }

    /**
     * Gets an integer from the config.
     * @param path {@code .} separated path to the value.
     * @return The value at the given path.
     * @see #getInt(String, int)
     */
    public int getInt(String path, int def) {
        return configFile.getInt(path, def);
    }

    /**
     * Gets a String from the config.
     * @param path {@code .} separated path to the value.
     * @param def Default value to return if the value is not found.
     * @return The value at the given path.
     */
    public String getString(String path, String def) {
        return configFile.getString(path, def);
    }

    public String getString(String path) {
        return getString(path, null);
    }

    public void set(String path, Object value) {
        configFile.set(path, value);
    }

    /**
     * Gets a boolean from the config.
     * @param path {@code .} separated path to the value.
     * @param def Default value to return if the value is not found.
     * @return The value at the given path.
     */
    public boolean getBoolean(String path, boolean def) {
        return configFile.getBoolean(path, def);
    }

    public float getFloat(String path, float def) {
        return (float) configFile.getDouble(path, def);
    }

    public double getDouble(String path, double def) {
        return configFile.getDouble(path, def);
    }

    public YamlFile getConfig() {
        return configFile;
    }

    public Utils.SavingModes getSavingMode() {
        return savingMode;
    }
}