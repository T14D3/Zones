package de.t14d3.zones.datasource;

import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.RegionFlagEntry;
import de.t14d3.zones.objects.World;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class YamlDataSource extends AbstractDataSource {
    private final YamlFile regionsFile;
    private final Zones zones;

    public YamlDataSource(File dataFolder, Zones zones) {
        super(zones);
        this.zones = zones;
        this.regionsFile = new YamlFile(new File(dataFolder, "regions.yml"));

        try {
            if (!regionsFile.exists()) {
                regionsFile.createOrLoadWithComments();
                regionsFile.save();
            }
            regionsFile.loadWithComments();
        } catch (IOException e) {
            zones.getLogger().error("Failed to initialize regions file", e);
        }
    }

    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        final ConfigurationSection regionsSection = regionsFile.getConfigurationSection("regions");

        if (regionsSection != null) {
            for (String regionKey : regionsSection.getKeys(false)) {
                Region region = loadRegion(regionKey);
                if (region != null) {
                    zones.getRegionManager().addRegion(region);
                    regions.add(region);
                }
            }
        }
        return regions;
    }

    @Override
    public Region loadRegion(String key) {
        final String pathPrefix = "regions." + key + ".";

        // Basic properties
        final String name = regionsFile.getString(pathPrefix + "name");
        final int priority = regionsFile.getInt(pathPrefix + "priority", 0);

        // World data
        final String worldName = regionsFile.getString(pathPrefix + "world");
        World world = zones.getPlatform().getWorld(worldName);
        if (world == null) {
            zones.getLogger().warn("World {} for region {} not found, using default", worldName, key);
            world = zones.getPlatform().getWorlds().get(0);
        }

        // Location data
        final BlockLocation min = new BlockLocation(
                regionsFile.getInt(pathPrefix + "min.x"),
                regionsFile.getInt(pathPrefix + "min.y"),
                regionsFile.getInt(pathPrefix + "min.z")
        );

        final BlockLocation max = new BlockLocation(
                regionsFile.getInt(pathPrefix + "max.x"),
                regionsFile.getInt(pathPrefix + "max.y"),
                regionsFile.getInt(pathPrefix + "max.z")
        );

        // Parent relationship
        final String parentKey = regionsFile.getString(pathPrefix + "parent");
        final RegionKey parent = parentKey != null ? RegionKey.fromString(parentKey) : null;

        // Member permissions
        final Map<String, List<RegionFlagEntry>> members = parseMembers(
                regionsFile.getConfigurationSection(pathPrefix + "members")
        );

        return new Region(name, min, max, world, members, RegionKey.fromString(key), parent, priority);
    }

    private Map<String, List<RegionFlagEntry>> parseMembers(ConfigurationSection membersSection) {
        final Map<String, List<RegionFlagEntry>> members = new HashMap<>();

        if (membersSection != null) {
            for (String who : membersSection.getKeys(false)) {
                final ConfigurationSection flagsSection = membersSection.getConfigurationSection(who);
                final List<RegionFlagEntry> flags = new ArrayList<>();

                if (flagsSection != null) {
                    for (String flagName : flagsSection.getKeys(false)) {
                        final String valuesStr = flagsSection.getString(flagName);
                        final List<RegionFlagEntry.FlagValue> values = new ArrayList<>();

                        for (String value : valuesStr.split(" ")) {
                            boolean inverted = value.startsWith("!");
                            String cleanValue = inverted ? value.substring(1) : value;
                            values.add(new RegionFlagEntry.FlagValue(cleanValue.toLowerCase(), inverted));
                        }

                        flags.add(new RegionFlagEntry(
                                flagName.toLowerCase().replaceFirst("!", ""),
                                values
                        ));
                    }
                }

                members.put(who, flags);
            }
        }
        return members;
    }

    @Override
    public void saveRegions(List<Region> regions) {
        CompletableFuture.runAsync(() -> {
            try {
                regionsFile.set("regions", null); // Clear existing regions

                for (Region region : regions) {
                    saveRegion(region.getKey().toString(), region);
                }

                regionsFile.setComment("regions", "All registered regions");
                regionsFile.save();
            } catch (IOException e) {
                zones.getLogger().error("Failed to save regions", e);
            }
        });
    }

    @Override
    public void saveRegion(String key, Region region) {
        final String pathPrefix = "regions." + key + ".";

        // Basic properties
        regionsFile.set(pathPrefix + "name", region.getName());
        regionsFile.setComment(pathPrefix + "name", "Region display name");

        regionsFile.set(pathPrefix + "priority", region.getPriority());
        regionsFile.setComment(pathPrefix + "priority", "Region priority (higher = stronger)");

        regionsFile.set(pathPrefix + "world", region.getWorld().getName());
        regionsFile.setComment(pathPrefix + "world", "World where the region exists");

        // Location data
        regionsFile.set(pathPrefix + "min.x", region.getMin().getX());
        regionsFile.set(pathPrefix + "min.y", region.getMin().getY());
        regionsFile.set(pathPrefix + "min.z", region.getMin().getZ());
        regionsFile.setComment(pathPrefix + "min", "Minimum bounding box coordinates");

        regionsFile.set(pathPrefix + "max.x", region.getMax().getX());
        regionsFile.set(pathPrefix + "max.y", region.getMax().getY());
        regionsFile.set(pathPrefix + "max.z", region.getMax().getZ());
        regionsFile.setComment(pathPrefix + "max", "Maximum bounding box coordinates");

        // Parent relationship
        if (region.getParent() != null) {
            regionsFile.set(pathPrefix + "parent", region.getParent().toString());
            regionsFile.setComment(pathPrefix + "parent", "Parent region key");
        }

        // Member permissions
        final ConfigurationSection membersSection = regionsFile.createSection(pathPrefix + "members");
        serializeMembers(membersSection, region.getMembers());
        regionsFile.setComment(pathPrefix + "members", "Region members and their permissions");
    }

    private void serializeMembers(ConfigurationSection membersSection, Map<String, List<RegionFlagEntry>> members) {
        for (Map.Entry<String, List<RegionFlagEntry>> entry : members.entrySet()) {
            final ConfigurationSection flagSection = membersSection.createSection(entry.getKey());

            for (RegionFlagEntry flag : entry.getValue()) {
                final StringBuilder values = new StringBuilder();
                for (RegionFlagEntry.FlagValue value : flag.getValues()) {
                    values.append(value.isInverted() ? "!" : "")
                            .append(value.getValue())
                            .append(" ");
                }

                flagSection.set(flag.getFlagValue(), values.toString().trim());
            }
        }
    }
}