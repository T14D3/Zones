package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class RegionManager {

    private final File regionsFile;
    private FileConfiguration regionsConfig;
    private final PermissionManager permissionManager;

    public RegionManager(JavaPlugin plugin, PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        regionsFile = new File(plugin.getDataFolder(), "regions.yml");

        if (!regionsFile.exists()) {
            regionsFile.getParentFile().mkdirs();
            plugin.saveResource("regions.yml", false);
        }

        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    public void saveRegions() {
        try {
            regionsConfig.save(regionsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load regions from the YAML file into memory
    public Map<String, Region> loadRegions() {
        Map<String, Region> regions = new HashMap<>();
        if (regionsConfig.contains("regions")) {
            for (String key : regionsConfig.getConfigurationSection("regions").getKeys(false)) {
                String name = regionsConfig.getString("regions." + key + ".name");

                Location min = loadLocation("regions." + key + ".min");
                Location max = loadLocation("regions." + key + ".max");

                Map<UUID, Map<String, String>> members = new HashMap<>();
                if (regionsConfig.contains("regions." + key + ".members")) {
                    for (String uuidStr : regionsConfig.getConfigurationSection("regions." + key + ".members").getKeys(false)) {
                        UUID uuid = UUID.fromString(uuidStr);
                        Map<String, String> permissions = new HashMap<>();
                        for (String perm : regionsConfig.getConfigurationSection("regions." + key + ".members." + uuidStr).getKeys(false)) {
                            permissions.put(perm, regionsConfig.getString("regions." + key + ".members." + uuidStr + "." + perm));
                        }
                        members.put(uuid, permissions);
                    }
                }
                Region region = new Region(name, min, max, members);
                regions.put(key, region);
            }
        }
        return regions;
    }
    // Save a region to the YAML file
    public void createRegion(String key, Region region) {
        regionsConfig.set("regions." + key + ".name", region.getName());

        saveLocation("regions." + key + ".min", region.getMin());
        saveLocation("regions." + key + ".max", region.getMax());

        Map<UUID, Map<String, String>> members = region.getMembers();
        for (Map.Entry<UUID, Map<String, String>> entry : members.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, String> perm : entry.getValue().entrySet()) {
                regionsConfig.set("regions." + key + ".members." + uuid + "." + perm.getKey(), perm.getValue());
            }
        }
        saveRegions();
    }
    // Load a location from the YAML file
    private Location loadLocation(String path) {
        String world = regionsConfig.getString(path + ".world");
        double x = regionsConfig.getDouble(path + ".x");
        double y = regionsConfig.getDouble(path + ".y");
        double z = regionsConfig.getDouble(path + ".z");
        return new Location(Bukkit.getWorld(world), x, y, z);
    }

    // Save a location to the YAML file
    private void saveLocation(String path, Location loc) {
        regionsConfig.set(path + ".world", loc.getWorld().getName());
        regionsConfig.set(path + ".x", loc.getX());
        regionsConfig.set(path + ".y", loc.getY());
        regionsConfig.set(path + ".z", loc.getZ());
    }

    /*
     *  Region Utility Methods
     */

    // Delete existing region
    public void deleteRegion(String regionKey) {
        loadRegions().remove(regionKey);
        saveRegions();
        permissionManager.invalidateAllCaches();
    }

    /**
     * Creates a new region from two Locations, a UUID for the owner and the owner's permissions
     *
     * @param name Name of the new Region
     * @param min First corner of the region
     * @param max Second corner of the region
     * @param playerUUID Region owner's UUID
     *
     */
    public void createNewRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {

        String regionKey;
        do {
            regionKey = UUID.randomUUID().toString().substring(0, 8);
        } while (loadRegions().containsKey(regionKey));

        // Create an empty map of members (in this case, just add the player UUID)
        Map<UUID, Map<String, String>> members = new HashMap<>();

        // Create a new region with the given name, min, max, and members
        Region newRegion = new Region(name, min, max, members);

        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value);
        });


        permissionManager.invalidateAllCaches();
        createRegion(regionKey, newRegion);

    }
    public void createNewRegion(String name, Location min, Location max, UUID playerUUID) {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("owner", "true");
        createNewRegion(name, min, max, playerUUID, permissions);

    }

    public void create2DRegion(String name, Location min, Location max, UUID playerUUID) {
        min.setY(-63);
        max.setY(319);
        createNewRegion(name, min, max, playerUUID);
    }
    public void create2DRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        min.setY(-63);
        max.setY(319);
        createNewRegion(name, min, max, playerUUID, ownerPermissions);
    }

    // Check if new region overlaps existing region
    public boolean overlapsExistingRegion(Region region) {
        boolean overlap = false;
        Map<String, Region> regions = loadRegions();
        for (Map.Entry<String, Region> entry : regions.entrySet()) {
            Region otherRegion = entry.getValue();
            BoundingBox otherBox = BoundingBox.of(otherRegion.getMin(), otherRegion.getMax());
            BoundingBox thisBox = BoundingBox.of(region.getMin(), region.getMax());
            if (thisBox.overlaps(otherBox)) {
                overlap = true;
            } else {
                return false;
            }
        }
        return overlap;
    }
    public boolean overlapsExistingRegion(Location min, Location max) {
        Region region = new Region(null, min, max, null);
        return overlapsExistingRegion(region);
    }

    public List<Region> getRegionsAt(Location location) {
        List<Region> foundRegions = new ArrayList<>();
        Map<String, Region> regions = loadRegions(); // Load regions from file

        for (Region region : regions.values()) {
            BoundingBox regionBox = BoundingBox.of(region.getMin(), region.getMax());
            // Check if the location's bounding box overlaps with the region's bounding box
            if (regionBox.contains(location.toVector())) {
                foundRegions.add(region);
            }
        }

        return foundRegions;
    }

    // Helper method to check if a location is within a given region
    private boolean isLocationInRegion(Location location, Region region) {
        Location min = region.getMin();
        Location max = region.getMax();

        return location.getWorld().equals(min.getWorld()) &&
                location.getX() >= min.getX() && location.getX() <= max.getX() &&
                location.getY() >= min.getY() && location.getY() <= max.getY() &&
                location.getZ() >= min.getZ() && location.getZ() <= max.getZ();
    }

    // Define the Region inner class
    public class Region {
        private String name;
        private Location min;
        private Location max;
        private Map<UUID, Map<String, String>> members;

        // Constructor
        public Region(String name, Location min, Location max, Map<UUID, Map<String, String>> members) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.members = (members != null) ? members : new HashMap<>();
        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Location getMin() {
            return min;
        }

        public void setMin(Location min) {
            this.min = min;
        }

        public Location getMax() {
            return max;
        }

        public void setMax(Location max) {
            this.max = max;
        }

        public Map<UUID, Map<String, String>> getMembers() {
            return members;
        }

        public void setMembers(Map<UUID, Map<String, String>> members) {
            this.members = members;
        }

        // Add a member with permissions or roles
        public void addMember(UUID uuid, Map<String, String> permissions) {
            this.members.put(uuid, permissions);
        }

        // Remove a member
        public void removeMember(UUID uuid) {
            this.members.remove(uuid);
        }

        // Check if a user is a member
        public boolean isMember(UUID uuid) {
            return this.members.containsKey(uuid);
        }

        // Get a member's permissions or roles
        public Map<String, String> getMemberPermissions(UUID uuid) {
            return this.members.get(uuid);
        }

        // Add or update a member's permission/role
        public void addMemberPermission(UUID uuid, String permission, String value) {
            permissionManager.invalidateCache(uuid);
            this.members.computeIfAbsent(uuid, k -> new HashMap<>()).put(permission, value);
        }
    }
}