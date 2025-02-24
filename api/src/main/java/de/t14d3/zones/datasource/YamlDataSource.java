package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlDataSource extends AbstractDataSource {
    private final File regionsFile;
    private final Zones plugin;
    private FileConfiguration regionsConfig;

    public YamlDataSource(File dataFolder, Zones plugin) {
        super(plugin);
        this.plugin = plugin;
        this.regionsFile = new File(dataFolder, "regions.yml");
        if (!regionsFile.exists()) {
            // Initialize the file if it doesn't exist
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        for (String key : regionsConfig.getConfigurationSection("regions").getKeys(false)) {
            Region region = loadRegion(key);
            plugin.getRegionManager().addRegion(region);
            regions.add(region);
        }
        return regions;
    }

    @Override
    public void saveRegions(List<Region> regions) {
        for (Region region : regions) {
            saveRegion(region.getKey().toString(), region);
        }
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Region loadRegion(String key) {
        return new Region(
                regionsConfig.getString("regions." + key + ".name"),
                new BlockVector(regionsConfig.getInt("regions." + key + ".min.x"),
                        regionsConfig.getInt("regions." + key + ".min.y"),
                        regionsConfig.getInt("regions." + key + ".min.z")),
                new BlockVector(regionsConfig.getInt("regions." + key + ".max.x"),
                        regionsConfig.getInt("regions." + key + ".max.y"),
                        regionsConfig.getInt("regions." + key + ".max.z")),
                Bukkit.getWorld(regionsConfig.getString("regions." + key + ".world")),
                loadMembers(regionsConfig.getConfigurationSection("regions." + key + ".members")),
                RegionKey.fromString(key),
                regionsConfig.getString("regions." + key + ".parent") != null
                        ? RegionKey.fromString(regionsConfig.getString("regions." + key + ".parent")) : null,
                regionsConfig.getInt("regions." + key + ".priority")
        );
    }

    private static Map<String, Map<String, String>> loadMembers(@Nullable ConfigurationSection section) {
        Map<String, Map<String, String>> members = new HashMap<>();
        if (section == null) return members;
        for (String who : section.getKeys(false)) {
            Map<String, String> permissions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String perm : section.getConfigurationSection(who).getKeys(false)) {
                permissions.put(perm.toLowerCase(), section.getString(who + "." + perm).toLowerCase());
            }
            members.put(who, permissions);
        }
        return members;
    }

    @Override
    public void saveRegion(String key, Region region) {
        regionsConfig.set("regions." + key + ".name", region.getName());
        regionsConfig.set("regions." + key + ".priority", region.getPriority());
        regionsConfig.set("regions." + key + ".world", region.getWorld().getName());
        regionsConfig.set("regions." + key + ".min.x", region.getMin().getBlockX());
        regionsConfig.set("regions." + key + ".min.y", region.getMin().getBlockY());
        regionsConfig.set("regions." + key + ".min.z", region.getMin().getBlockZ());
        regionsConfig.set("regions." + key + ".max.x", region.getMax().getBlockX());
        regionsConfig.set("regions." + key + ".max.y", region.getMax().getBlockY());
        regionsConfig.set("regions." + key + ".max.z", region.getMax().getBlockZ());
        if (region.getParent() != null) {
            regionsConfig.set("regions." + key + ".parent", region.getParent().toString());
        }
        for (Map.Entry<String, Map<String, String>> entry : region.getMembers().entrySet()) {
            String who = entry.getKey();
            for (Map.Entry<String, String> perm : entry.getValue().entrySet()) {
                regionsConfig.set("regions." + key + ".members." + who + "." + perm.getKey(), perm.getValue());
            }
        }
    }
}
