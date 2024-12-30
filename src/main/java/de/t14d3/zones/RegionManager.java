package de.t14d3.zones;

import com.google.gson.JsonObject;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class RegionManager {

    private final File regionsFile;
    private final FileConfiguration regionsConfig;
    private final PermissionManager permissionManager;
    private final Zones plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<String, String> messages;

    public RegionManager(Zones plugin, PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
        this.plugin = plugin;
        this.messages = plugin.getMessages();

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

                Map<UUID, Map<String, String>> members = loadMembers(key);
                Region region = new Region(name, min, max, members);
                regions.put(key, region);
            }
        }
        return regions;
    }

    private Map<UUID, Map<String, String>> loadMembers(String key) {
        Map<UUID, Map<String, String>> members = new HashMap<>();
        if (regionsConfig.contains("regions." + key + ".members")) {
            for (String uuidStr : regionsConfig.getConfigurationSection("regions." + key + ".members").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, String> permissions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
                for (String perm : regionsConfig.getConfigurationSection("regions." + key + ".members." + uuidStr).getKeys(false)) {
                    permissions.put(perm, regionsConfig.getString("regions." + key + ".members." + uuidStr + "." + perm));
                }
                members.put(uuid, permissions);
            }
        }
        return members;
    }

    // Save a region to the YAML file
    public void createRegion(String key, Region region) {
        regionsConfig.set("regions." + key + ".name", region.getName());
        saveLocation("regions." + key + ".min", region.getMin());
        saveLocation("regions." + key + ".max", region.getMax());

        saveMembers(key, region.getMembers());
        saveRegions();
    }

    private void saveMembers(String key, Map<UUID, Map<String, String>> members) {
        for (Map.Entry<UUID, Map<String, String>> entry : members.entrySet()) {
            String uuid = entry.getKey().toString();
            for (Map.Entry<String, String> perm : entry.getValue().entrySet()) {
                regionsConfig.set("regions." + key + ".members." + uuid + "." + perm.getKey(), perm.getValue());
            }
        }
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

    /**
     * Deletes an existing region
     * @param regionKey
     */
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
     * @param ownerPermissions Owner's permissions map
     *
     */
    public void createNewRegion(String name, Location min, Location max, UUID playerUUID, Map<String, String> ownerPermissions) {
        String regionKey;
        do {
            regionKey = UUID.randomUUID().toString().substring(0, 8);
        } while (loadRegions().containsKey(regionKey));

        Map<UUID, Map<String, String>> members = new HashMap<>();
        Region newRegion = new Region(name, min, max, members);

        String finalRegionKey = regionKey;
        ownerPermissions.forEach((permission, value) -> {
            newRegion.addMemberPermission(playerUUID, permission, value, this, finalRegionKey);
        });

        permissionManager.invalidateAllCaches();
        createRegion(regionKey, newRegion);
    }

    public void createNewRegion(String name, Location min, Location max, UUID playerUUID) {
        Map<String, String> permissions = new HashMap<>();
        permissions.put("role", "owner");
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

    public void addMemberPermission(UUID uuid, String permission, String value, RegionManager regionManager, String key) {
        permissionManager.invalidateCache(uuid);
        RegionManager.Region region = regionManager.loadRegions().get(key);
        region.members.computeIfAbsent(uuid, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(permission, value);
        regionManager.createRegion(key, region); // Ensure changes are saved
    }

    // Check if new region overlaps existing region
    public boolean overlapsExistingRegion(Region region) {
        Map<String, Region> regions = loadRegions();
        for (Region otherRegion : regions.values()) {
            BoundingBox otherBox = BoundingBox.of(otherRegion.getMin(), otherRegion.getMax());
            BoundingBox thisBox = BoundingBox.of(region.getMin(), region.getMax());
            if (thisBox.overlaps(otherBox)) {
                return true; // Found an overlap
            }
        }
        return false; // No overlaps found
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

    public Map<String, String> getMemberPermissions(Player player, Region region) {
        return region.getMemberPermissions(player.getUniqueId());
    }

    public Map<String, String> getMemberPermissions(UUID uuid, Region region) {
        return region.getMemberPermissions(uuid);
    }

    // Define the Region inner class
    public class Region {
        private String name;
        private Location min;
        private Location max;
        private Map<UUID, Map<String, String>> members;
        private String parent;

        // Constructor
        public Region(String name, Location min, Location max, Map<UUID, Map<String, String>> members, String parent) {
            this.name = name;
            this.min = min;
            this.max = max;
            this.members = (members != null) ? members : new HashMap<>();
            this.parent = parent;
        }
        // Constructor overload for regions without parent
        public Region(String name, Location min, Location max, Map<UUID, Map<String, String>> members) {
            this(name, min, max, members, null);

        }

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name, RegionManager regionManager, String key) {
            this.name = name;
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public Location getMin() {
            return min;
        }
        public String getMinString() {
            return min.getBlockX() + "," + min.getBlockY() + "," + min.getBlockZ();
        }

        public void setMin(Location min, RegionManager regionManager, String key) {
            this.min = min;
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public Location getMax() {
            return max;
        }
        public String getMaxString() {
            return max.getBlockX() + "," + max.getBlockY() + "," + max.getBlockZ();
        }

        public void setMax(Location max, RegionManager regionManager, String key) {
            this.max = max;
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public Map<UUID, Map<String, String>> getMembers() {
            return members;
        }

        public void setMembers(Map<UUID, Map<String, String>> members, RegionManager regionManager, String key) {
            this.members = members;
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public void addMember(UUID uuid, Map<String, String> permissions, RegionManager regionManager, String key) {
            this.members.put(uuid, permissions);
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public void removeMember(UUID uuid, RegionManager regionManager, String key) {
            this.members.remove(uuid);
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public boolean isMember(UUID uuid) {
            return this.members.containsKey(uuid);
        }

        public Map<String, String> getMemberPermissions(UUID uuid) {
            return this.members.get(uuid);
        }

        public String getParent() {
            return this.parent;
        }
        public void setParent(String parent, RegionManager regionManager, String key) {
            this.parent = parent;
            regionManager.createRegion(key, this); // Ensure changes are saved
        }

        public Region getParentRegion(RegionManager regionManager) {
            return regionManager.loadRegions().get(parent);
        }

        public void addMemberPermission(UUID uuid, String permission, String value, RegionManager regionManager, String key) {
            permissionManager.invalidateCache(uuid);
            RegionManager.Region region = regionManager.loadRegions().get(key);
            region.members.computeIfAbsent(uuid, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)).put(permission, value);
            regionManager.createRegion(key, region); // Ensure changes are saved
        }

        public JsonObject getAsJson() {
            JsonObject json = new JsonObject();
            json.addProperty("name", getName());
            JsonObject membersJson = new JsonObject();
            for (Map.Entry<UUID, Map<String, String>> member : getMembers().entrySet()) {
                JsonObject memberJson = new JsonObject();
                memberJson.addProperty("player", Bukkit.getPlayer(member.getKey()) != null ? Bukkit.getPlayer(member.getKey()).getName() : member.getKey().toString());
                JsonObject permissions = new JsonObject();
                for (Map.Entry<String, String> perm : member.getValue().entrySet()) {
                    permissions.addProperty(perm.getKey(), perm.getValue());
                }
                memberJson.add("permissions", permissions);
                membersJson.add(member.getKey().toString(), memberJson);
            }
            json.add("members", membersJson);
            return json;
        }
    }
}