package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.utils.ConfigManager;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

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
        this.regionsConfig = new ConfigManager(plugin, regionsFile); // Updated to use default config path
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        ConfigurationNode regionsNode = regionsConfig.getConfig().node("regions");
        for (Object key : regionsNode.childrenMap().keySet()) {
            Region region = loadRegion((String) key);
            plugin.getRegionManager().addRegion(region);
            regions.add(region);
        }
        return regions;
    }

    @Override
    public void saveRegions(List<Region> regions) {
        CompletableFuture.runAsync(() -> {
            for (Region region : regions) {
                saveRegion(region.getKey().toString(), region);
            }
            regionsConfig.saveConfig();
        });
    }

    @Override
    public Region loadRegion(String key) {
        ConfigurationNode regionNode = regionsConfig.getConfig().node("regions", key);
        return new Region(
                regionNode.node("name").getString(),
                new BlockLocation(regionNode.node("min", "x").getInt(),
                        regionNode.node("min", "y").getInt(),
                        regionNode.node("min", "z").getInt()),
                new BlockLocation(regionNode.node("max", "x").getInt(),
                        regionNode.node("max", "y").getInt(),
                        regionNode.node("max", "z").getInt()),
                Zones.getInstance().getPlatform().getWorld(regionNode.node("world").getString()),
                loadMembers(regionNode.node("members")),
                RegionKey.fromString(key),
                regionNode.node("parent").isNull() ? null : RegionKey.fromString(regionNode.node("parent").getString()),
                regionNode.node("priority").getInt()
        );
    }

    private static Map<String, Map<String, String>> loadMembers(ConfigurationNode section) {
        Map<String, Map<String, String>> members = new HashMap<>();
        if (section.isNull()) return members;
        for (Object whoObj : section.childrenMap().keySet()) {
            String who = (String) whoObj;
            Map<String, String> permissions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (Object permObj : section.node(who).childrenMap().keySet()) {
                String perm = (String) permObj;
                permissions.put(perm.toLowerCase(), section.node(who, perm).getString().toLowerCase());
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
