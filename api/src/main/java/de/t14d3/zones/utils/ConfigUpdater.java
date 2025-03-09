package de.t14d3.zones.utils;

import de.t14d3.zones.Zones;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigUpdater {
    private final Zones plugin;

    public ConfigUpdater(Zones plugin) {
        this.plugin = plugin;
        update();
    }

    private void update() {
        try {
            final YamlFile currentConfig = new YamlFile(new File(plugin.getDataFolder(), "config.yml"));
            currentConfig.loadWithComments();
            InputStream defaultConfigStream = plugin.getClass().getResourceAsStream("/config.yml");
            Path tempFile = Files.createTempFile("defaultConfig", ".yml");
            Files.copy(defaultConfigStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            final YamlFile defaultConfig = new YamlFile(tempFile.toFile());
            defaultConfig.loadWithComments();

            defaultConfig.getKeys(true).forEach(key -> {
                if (!currentConfig.contains(key)) {
                    currentConfig.set(key, defaultConfig.get(key));
                }
                if (currentConfig.getComment(key) == null) {
                    currentConfig.setComment(key, defaultConfig.getComment(key));
                }
            });

            currentConfig.save();
            Files.delete(tempFile);
        } catch (Exception e) {
            plugin.getLogger().error("Failed to update config: {}", e.getMessage());
        }
    }
}
