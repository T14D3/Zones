package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.utils.ConfigManager;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class YamlDataSource extends AbstractDataSource {
    private final File regionsFile;
    private final Zones zones;
    private final ConfigManager regionsConfig;

    public YamlDataSource(File dataFolder, Zones zones) {
        super(zones);
        this.zones = zones;
        this.regionsFile = new File(dataFolder, "regions.yml");
        if (!regionsFile.exists()) {
            // Initialize the file if it doesn't exist
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.regionsConfig = new ConfigManager(zones, regionsFile); // Updated to use default config path
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        ConfigurationNode regionsNode = regionsConfig.getConfig().node("regions");
        for (Object key : regionsNode.childrenMap().keySet()) {
            Region region = loadRegion((String) key);
            zones.getRegionManager().addRegion(region);
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
        World world = zones.getPlatform().getWorld(regionNode.node("world").getString());
        if (world == null) {
            zones.getLogger().warn("World {} for region {} not found, falling back to default world.",
                    regionNode.node("world").getString(), key);
            world = zones.getPlatform().getWorlds().get(0);
        }
        return new Region(
                regionNode.node("name").getString(),
                new BlockLocation(regionNode.node("min", "x").getInt(),
                        regionNode.node("min", "y").getInt(),
                        regionNode.node("min", "z").getInt()),
                new BlockLocation(regionNode.node("max", "x").getInt(),
                        regionNode.node("max", "y").getInt(),
                        regionNode.node("max", "z").getInt()),
                world,
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
        try {
            ConfigurationNode regionNode = regionsConfig.getConfig().node("regions", key);
            regionNode.node("name").set(region.getName());
            regionNode.node("priority").set(region.getPriority());
            regionNode.node("world").set(region.getWorld().getName());
            regionNode.node("min", "x").set(region.getMin().getX());
            regionNode.node("min", "y").set(region.getMin().getY());
            regionNode.node("min", "z").set(region.getMin().getZ());
            regionNode.node("max", "x").set(region.getMax().getX());
            regionNode.node("max", "y").set(region.getMax().getY());
            regionNode.node("max", "z").set(region.getMax().getZ());
            if (region.getParent() != null) {
                regionNode.node("parent").set(region.getParent().toString());
            }
            for (Map.Entry<String, Map<String, String>> entry : region.getMembers().entrySet()) {
                String who = entry.getKey();
                for (Map.Entry<String, String> perm : entry.getValue().entrySet()) {
                    regionNode.node("members", who, perm.getKey()).set(perm.getValue());
                }
            }
        } catch (Exception e) {
            zones.getLogger().error("Failed to save region {}: {}", key, e.getMessage());
        }
    }
}
