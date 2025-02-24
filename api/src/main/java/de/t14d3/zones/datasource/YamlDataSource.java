package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.utils.ConfigManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlDataSource extends AbstractDataSource {
    private final File regionsFile;
    private final Zones plugin;
    private ConfigManager regionsConfig;

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
        this.regionsConfig = new ConfigManager(plugin); // Updated to use default config path
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        for (String key : ((Map<String, Object>) regionsConfig.get("regions")).keySet()) {
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
        regionsConfig.saveConfig();
    }

    @Override
    public Region loadRegion(String key) {
        return new Region(
                regionsConfig.getString("regions." + key + ".name"),
                new BlockLocation(regionsConfig.getInt("regions." + key + ".min.x"),
                        regionsConfig.getInt("regions." + key + ".min.y"),
                        regionsConfig.getInt("regions." + key + ".min.z")),
                new BlockLocation(regionsConfig.getInt("regions." + key + ".max.x"),
                        regionsConfig.getInt("regions." + key + ".max.y"),
                        regionsConfig.getInt("regions." + key + ".max.z")),
                Zones.getInstance().getPlatform().getWorld(regionsConfig.getString("regions." + key + ".world")),
                loadMembers((Map<String, Object>) regionsConfig.get("regions." + key + ".members")),
                RegionKey.fromString(key),
                regionsConfig.getString("regions." + key + ".parent") != null
                        ? RegionKey.fromString(regionsConfig.getString("regions." + key + ".parent")) : null,
                regionsConfig.getInt("regions." + key + ".priority")
        );
    }

    private static Map<String, Map<String, String>> loadMembers(Map<String, Object> section) {
        Map<String, Map<String, String>> members = new HashMap<>();
        if (section == null) return members;
        for (String who : section.keySet()) {
            Map<String, String> permissions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (String perm : ((Map<String, Object>) section.get(who)).keySet()) {
                permissions.put(perm.toLowerCase(), ((String) section.get(who + "." + perm)).toLowerCase());
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
        regionsConfig.set("regions." + key + ".min.x", region.getMin().getX());
        regionsConfig.set("regions." + key + ".min.y", region.getMin().getY());
        regionsConfig.set("regions." + key + ".min.z", region.getMin().getZ());
        regionsConfig.set("regions." + key + ".max.x", region.getMax().getX());
        regionsConfig.set("regions." + key + ".max.y", region.getMax().getY());
        regionsConfig.set("regions." + key + ".max.z", region.getMax().getZ());
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
