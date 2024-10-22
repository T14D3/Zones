package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RegionManager {

    private final File regionsFile;
    private FileConfiguration regionsConfig;

    public RegionManager(JavaPlugin plugin) {
        // Initialize the regions.yml file
        regionsFile = new File(plugin.getDataFolder(), "regions.yml");

        if (!regionsFile.exists()) {
            regionsFile.getParentFile().mkdirs();
            plugin.saveResource("regions.yml", false);
        }

        // Load the YAML configuration
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
    }

    // Save regions to the file
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
    }

    // Create Region from Locations
    public void createNewRegion(String name, Location min, Location max, UUID playerUUID) {

        String regionKey;
        do {
            regionKey = UUID.randomUUID().toString().substring(0, 8);
        } while (loadRegions().containsKey(regionKey));

        // Create an empty map of members (in this case, just add the player UUID)
        Map<UUID, Map<String, String>> members = new HashMap<>();

        // Create a new region with the given name, min, max, and members
        Region newRegion = new Region(name, min, max, members);
        newRegion.setMemberPermission(playerUUID, "owner", "true");


        createRegion(regionKey, newRegion);

    }

    public void create2DRegion(String name, Location min, Location max, UUID playerUUID) {
        min.setY(-63);
        max.setY(319);
        createNewRegion(name, min, max, playerUUID);
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

        // Check if member has specific permission
        public boolean hasPermission(UUID uuid, String permission, String type) {
            Map<String, String> permissions = this.members.get(uuid);
            if (permissions != null) {
                String value = permissions.get(permission);

                // Handle boolean strings
                if (value != null) {
                    if ("true".equalsIgnoreCase(value)) {
                        return true; // Explicitly allowed
                    } else if ("false".equalsIgnoreCase(value)) {
                        return false; // Explicitly denied
                    } else {
                        // Check for lists or arrays
                        List<String> permittedValues = List.of(value.split(","));

                        for (String permittedValue : permittedValues) {
                            if (permittedValue.startsWith("!")) {
                                // Invert the result if it starts with "!"
                                if (permittedValue.substring(1).equals(type)) {
                                    return false; // Inverted result
                                }
                            } else {
                                if (permittedValue.equals(type)) {
                                    return true; // Standard match
                                }
                            }
                        }
                    }
                }
            }
            // Return false if no permissions found
            return false;
        }

        // Add or update a member's permission/role
        public void setMemberPermission(UUID uuid, String permission, String value) {
            this.members.computeIfAbsent(uuid, k -> new HashMap<>()).put(permission, value);
        }




    }
}
