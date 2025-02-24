package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;

public class ConfigManager {
    private final Zones plugin;
    private final Yaml yaml;
    private Map<String, Object> configData;
    private File configFile = new File("plugins/Zones/config.yml");

    public ConfigManager(Zones plugin) {
        this(plugin, new File("plugins/Zones/config.yml"));
    }

    public ConfigManager(Zones plugin, File configFile) {
        this.plugin = plugin;
        this.yaml = new Yaml();
        this.configFile = configFile;
        loadConfig();
    }

    private void loadConfig() {
        try (FileInputStream inputStream = new FileInputStream(configFile)) {
            configData = yaml.load(inputStream);
        } catch (FileNotFoundException e) {
            configFile.getParentFile().mkdirs();
            try {
                //noinspection ResultOfMethodCallIgnored
                configFile.createNewFile();
            } catch (IOException io1) {
                plugin.getLogger().error("Failed to create config.yml: " + io1.getMessage());
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                yaml.dump(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.yml"), writer);
            } catch (IOException io2) {
                plugin.getLogger().error("Failed to save config.yml: " + io2.getMessage());
            }
        } catch (IOException e) {
            plugin.getLogger().error("Failed to load config.yml: " + e.getMessage());
        }
    }

    public void saveConfig() {
        try (FileWriter writer = new FileWriter(configFile)) {
            yaml.dump(configData, writer);
        } catch (IOException e) {
            plugin.getLogger().error("Failed to save config.yml: " + e.getMessage());
        }
    }

    public Object get(String path) {
        return configData.get(path);
    }

    public int getInt(String path) {
        return (int) get(path);
    }

    public String getString(String path) {
        return (String) get(path);
    }

    public void set(String path, Object value) {
        configData.put(path, value);
        saveConfig();
    }

    public FileInputStream getDefaultConfig() {
        try {
            return new FileInputStream(configFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    public String getString(String path, String def) {
        return (get(path) != null) ? (String) get(path) : def;
    }

    public int getInt(String path, int def) {
        return (get(path) != null) ? (int) get(path) : def;
    }

    public Map<String, Object> getConfigData() {
        return configData;
    }
}
