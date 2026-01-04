package de.t14d3.zones.datasource;

import de.t14d3.rapunzellib.Rapunzel;
import de.t14d3.rapunzellib.config.ConfigurationSection;
import de.t14d3.rapunzellib.config.YamlConfig;
import de.t14d3.zones.Region;
import de.t14d3.zones.RegionKey;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.shapes.CuboidShape;
import de.t14d3.zones.objects.shapes.GlobalShape;
import de.t14d3.zones.objects.shapes.ShapeProvider;
import de.t14d3.zones.permissions.PermissionKeyRegistry;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.flags.FlagValueKind;
import de.t14d3.zones.permissions.subjects.Subjects;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YamlDataSource extends AbstractDataSource {
    private final YamlConfig regionsFile;
    private final Zones zones;

    /**
     * Creates a YAML data source rooted at the given data folder.
     *
     * <p>This loads (or creates) {@code regions.yml} immediately. Any initialization errors are logged.</p>
     *
     * @param dataFolder plugin/mod data directory
     * @param zones      owning plugin instance
     */
    public YamlDataSource(File dataFolder, Zones zones) {
        super(zones);
        this.zones = zones;
        this.regionsFile = Rapunzel.context().configs().load(new File(dataFolder, "regions.yml").toPath());
    }

    /**
     * Loads all regions found under the {@code regions} root node.
     *
     * @return list of loaded regions
     */
    @Override
    public List<Region> loadRegions() {
        List<Region> regions = new ArrayList<>();
        final ConfigurationSection regionsSection = regionsFile.getConfigurationSection("regions");

        if (regionsSection != null) {
            for (String regionKey : regionsSection.getKeys(false)) {
                Region region = loadRegion(regionKey);
                if (region != null) {
                    regions.add(region);
                }
            }
        }
        return regions;
    }

    /**
     * Loads a single region by key.
     *
     * @param key region key string
     * @return the loaded region, or {@code null} if it does not exist
     */
    @Override
    public Region loadRegion(String key) {
        final String pathPrefix = "regions." + key + ".";

        // Basic properties
        final String name = regionsFile.getString(pathPrefix + "name");
        final int priority = regionsFile.getInt(pathPrefix + "priority", 0);

        // Shape data
        ShapeProvider shape;
        ConfigurationSection shapeSection = regionsFile.getConfigurationSection(pathPrefix + "shape");
        if (shapeSection == null) {
            zones.getLogger().error("Region {} is missing required shape data; skipping", key);
            return null;
        }

        String type = shapeSection.getString("type");
        String data = shapeSection.getString("data");
        if (type == null || type.isBlank() || data == null || data.isBlank()) {
            zones.getLogger().error("Region {} has invalid shape data; skipping", key);
            return null;
        }

        try {
            switch (type) {
                case "global" -> shape = GlobalShape.fromJson(data);
                case "cuboid" -> shape = CuboidShape.fromJson(data);
                default -> {
                    zones.getLogger().error("Unknown shape type {} for region {}; skipping", type, key);
                    return null;
                }
            }
        } catch (Exception e) {
            zones.getLogger().error("Failed to parse shape for region {}; skipping", key, e);
            return null;
        }

        // Parent relationship
        final String parentKey = regionsFile.getString(pathPrefix + "parent");
        final RegionKey parent = parentKey != null ? RegionKey.fromString(parentKey) : null;

        RegionPermissions permissions = new RegionPermissions();
        RegionMembership membership = new RegionMembership();

        // ACL rules
        ConfigurationSection aclSection = regionsFile.getConfigurationSection(pathPrefix + "acl");
        if (aclSection != null) {
            ConfigurationSection universalSection = aclSection.getConfigurationSection("universal");
            if (universalSection != null) {
                readSubjectPermissions(universalSection, permissions.subject(Subjects.universal()));
            }

            ConfigurationSection playersSection = aclSection.getConfigurationSection("players");
            if (playersSection != null) {
                for (String playerKey : playersSection.getKeys(false)) {
                    ConfigurationSection subjectSection = playersSection.getConfigurationSection(playerKey);
                    if (subjectSection == null) continue;
                    try {
                        UUID uuid = UUID.fromString(playerKey);
                        readSubjectPermissions(subjectSection, permissions.subject(Subjects.player(uuid)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            ConfigurationSection groupsSection = aclSection.getConfigurationSection("groups");
            if (groupsSection != null) {
                for (String groupName : groupsSection.getKeys(false)) {
                    ConfigurationSection subjectSection = groupsSection.getConfigurationSection(groupName);
                    if (subjectSection == null) continue;
                    String normalized = Subjects.normalizeGroupName(groupName);
                    if (normalized == null) continue;
                    readSubjectPermissions(subjectSection, permissions.subject(Subjects.group(normalized)));
                }
            }
        }

        // Membership
        ConfigurationSection membershipSection = regionsFile.getConfigurationSection(pathPrefix + "membership");
        if (membershipSection != null) {
            readMembership(membershipSection, membership);
        }

        return new Region(name, shape, permissions, membership, RegionKey.fromString(key), parent, priority);
    }

    private static void readSubjectPermissions(ConfigurationSection subjectSection, RegionPermissions.SubjectPermissions subject) {
        for (String permName : subjectSection.getKeys(false)) {
            ConfigurationSection permSection = subjectSection.getConfigurationSection(permName);
            if (permSection == null) continue;

            String kind = permSection.getString("type", "targets");
            int permId = PermissionKeyRegistry.instance().getOrCreateId(permName);

            if ("set".equalsIgnoreCase(kind)) {
                RegionPermissions.StringSetValue set = (RegionPermissions.StringSetValue) subject.getOrCreate(permId,
                        FlagValueKind.STRING_SET);
                permSection.getStringList("allow").forEach(set::allow);
                permSection.getStringList("deny").forEach(set::deny);
            } else {
                RegionPermissions.TargetDecisionValue targeted = (RegionPermissions.TargetDecisionValue) subject.getOrCreate(
                        permId, FlagValueKind.TARGET_DECISION);
                permSection.getStringList("allow").forEach(targeted::allow);
                permSection.getStringList("deny").forEach(targeted::deny);
            }
        }
    }

    private static void readMembership(ConfigurationSection membershipSection, RegionMembership membership) {
        ConfigurationSection players = membershipSection.getConfigurationSection("players");
        if (players != null) {
            for (String playerKey : players.getKeys(false)) {
                ConfigurationSection playerSection = players.getConfigurationSection(playerKey);
                if (playerSection == null) continue;
                UUID uuid;
                try {
                    uuid = UUID.fromString(playerKey);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }

                RegionMembership.PlayerEntry entry = membership.player(uuid);

                ConfigurationSection rolesSection = playerSection.getConfigurationSection("roles");
                if (rolesSection != null) {
                    rolesSection.getStringList("allow").forEach(entry.roles()::allow);
                    rolesSection.getStringList("deny").forEach(entry.roles()::deny);
                }

                ConfigurationSection groupsSection = playerSection.getConfigurationSection("groups");
                if (groupsSection != null) {
                    groupsSection.getStringList("allow").forEach(entry.groups()::allow);
                    groupsSection.getStringList("deny").forEach(entry.groups()::deny);
                }
            }
        }

        ConfigurationSection groups = membershipSection.getConfigurationSection("groups");
        if (groups != null) {
            for (String groupName : groups.getKeys(false)) {
                ConfigurationSection groupSection = groups.getConfigurationSection(groupName);
                if (groupSection == null) continue;
                String normalized = Subjects.normalizeGroupName(groupName);
                if (normalized == null) continue;

                RegionMembership.GroupEntry entry = membership.group(normalized);
                ConfigurationSection includesSection = groupSection.getConfigurationSection("includes");
                if (includesSection != null) {
                    includesSection.getStringList("allow").forEach(entry.includes()::allow);
                    includesSection.getStringList("deny").forEach(entry.includes()::deny);
                }
            }
        }
    }

    /**
     * Saves the full region list asynchronously, replacing the entire {@code regions} section.
     *
     * <p>This performs the actual disk write by calling {@link YamlConfig#save()}.</p>
     *
     * @param regions regions to persist
     */
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
            } catch (Exception e) {
                zones.getLogger().error("Failed to save regions", e);
            }
        });
    }

    /**
     * Writes a single region into the in-memory YAML representation.
     *
     * <p>This method does not call {@link YamlConfig#save()}; flushing is handled by {@link #saveRegions(List)} and the
     * region manager's save mode.</p>
     *
     * @param key region key string
     * @param region region to persist
     */
    @Override
    public void saveRegion(String key, Region region) {
        final String pathPrefix = "regions." + key + ".";

        // Basic properties
        regionsFile.set(pathPrefix + "name", region.getName());
        regionsFile.setComment(pathPrefix + "name", "Region display name");

        regionsFile.set(pathPrefix + "priority", region.getPriority());
        regionsFile.setComment(pathPrefix + "priority", "Region priority (higher = stronger)");

        regionsFile.set(pathPrefix + "world", region.getWorld().identifier());
        regionsFile.setComment(pathPrefix + "world", "World where the region exists");

        // Shape data
        ConfigurationSection shapeSection = regionsFile.createSection(pathPrefix + "shape");
        ShapeProvider shape = region.getShape();
        shapeSection.set("type", shape.getShapeType());
        shapeSection.set("data", shape.toJson());
        regionsFile.setComment(pathPrefix + "shape", "Region shape specification");

        // Remove legacy min/max fields if they exist.
        regionsFile.set(pathPrefix + "min", null);
        regionsFile.set(pathPrefix + "max", null);


        // Parent relationship
        if (region.getParent() != null) {
            regionsFile.set(pathPrefix + "parent", region.getParent().toString());
            regionsFile.setComment(pathPrefix + "parent", "Parent region key");
        }

        // ACL
        regionsFile.set(pathPrefix + "acl", null);
        final ConfigurationSection aclSection = regionsFile.createSection(pathPrefix + "acl");
        RegionPermissions permissions = region.getPermissions();
        if (permissions != null) {
            writeAcl(aclSection, permissions);
        }
        regionsFile.setComment(pathPrefix + "acl", "Region ACL rules");

        // Membership
        regionsFile.set(pathPrefix + "membership", null);
        final ConfigurationSection membershipSection = regionsFile.createSection(pathPrefix + "membership");
        RegionMembership membership = region.getMembership();
        if (membership != null) {
            writeMembership(membershipSection, membership);
        }
        regionsFile.setComment(pathPrefix + "membership", "Region membership (roles/groups and group inheritance)");
    }

    private static void writeAcl(ConfigurationSection aclSection, RegionPermissions permissions) {
        ConfigurationSection universal = aclSection.createSection("universal");
        ConfigurationSection players = aclSection.createSection("players");
        ConfigurationSection groups = aclSection.createSection("groups");

        for (Map.Entry<de.t14d3.zones.permissions.subjects.SubjectRef, RegionPermissions.SubjectPermissions> subjectEntry : permissions.subjects()
                .entrySet()) {
            de.t14d3.zones.permissions.subjects.SubjectRef subject = subjectEntry.getKey();
            RegionPermissions.SubjectPermissions subjectPermissions = subjectEntry.getValue();
            if (subject == null || subjectPermissions == null) continue;

            ConfigurationSection subjectSection;
            if (subject instanceof de.t14d3.zones.permissions.subjects.UniversalSubject) {
                subjectSection = universal;
            } else if (subject instanceof de.t14d3.zones.permissions.subjects.PlayerSubject p) {
                subjectSection = players.createSection(p.uuid().toString());
            } else if (subject instanceof de.t14d3.zones.permissions.subjects.GroupSubject g) {
                subjectSection = groups.createSection(g.name());
            } else {
                continue;
            }

            for (var permEntry : subjectPermissions.values().int2ObjectEntrySet()) {
                int permId = permEntry.getIntKey();
                String permName = PermissionKeyRegistry.instance().getName(permId);
                if (permName == null) continue;

                RegionPermissions.PermissionValue pv = permEntry.getValue();
                if (pv instanceof RegionPermissions.TargetDecisionValue targeted) {
                    ConfigurationSection permSection = subjectSection.createSection(permName);
                    permSection.set("type", "targets");
                    permSection.set("allow", new ArrayList<>(targeted.allowTargets()));
                    permSection.set("deny", new ArrayList<>(targeted.denyTargets()));
                } else if (pv instanceof RegionPermissions.StringSetValue set) {
                    ConfigurationSection permSection = subjectSection.createSection(permName);
                    permSection.set("type", "set");
                    permSection.set("allow", new ArrayList<>(set.allowValues()));
                    permSection.set("deny", new ArrayList<>(set.denyValues()));
                }
            }
        }
    }

    private static void writeMembership(ConfigurationSection membershipSection, RegionMembership membership) {
        ConfigurationSection players = membershipSection.createSection("players");
        for (Map.Entry<UUID, RegionMembership.PlayerEntry> e : membership.players().entrySet()) {
            UUID uuid = e.getKey();
            RegionMembership.PlayerEntry entry = e.getValue();
            if (uuid == null || entry == null) continue;

            ConfigurationSection playerSection = players.createSection(uuid.toString());

            ConfigurationSection roles = playerSection.createSection("roles");
            roles.set("allow", new ArrayList<>(entry.roles().allowValues()));
            roles.set("deny", new ArrayList<>(entry.roles().denyValues()));

            ConfigurationSection groups = playerSection.createSection("groups");
            groups.set("allow", new ArrayList<>(entry.groups().allowValues()));
            groups.set("deny", new ArrayList<>(entry.groups().denyValues()));
        }

        ConfigurationSection groups = membershipSection.createSection("groups");
        for (Map.Entry<String, RegionMembership.GroupEntry> e : membership.groups().entrySet()) {
            String name = e.getKey();
            RegionMembership.GroupEntry entry = e.getValue();
            if (name == null || entry == null) continue;

            ConfigurationSection groupSection = groups.createSection(name);
            ConfigurationSection includes = groupSection.createSection("includes");
            includes.set("allow", new ArrayList<>(entry.includes().allowValues()));
            includes.set("deny", new ArrayList<>(entry.includes().denyValues()));
        }
    }
}
