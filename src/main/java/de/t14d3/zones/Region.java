package de.t14d3.zones;

import de.t14d3.zones.permissions.PermissionManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.BlockVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;


/**
 * Represents a region in the plugin.
 * Constructive or destructive methods are implemented in the {@link de.t14d3.zones.RegionManager}.
 */
public class Region {
    private String name;
    private BlockVector min;
    private BlockVector max;
    private World world;
    private Map<String, Map<String, String>> members;
    private RegionKey key;
    private RegionKey parent;
    private int priority;

    // Constructor

    /**
     * Constructs a new region with the given name, minimum and maximum locations, members and parent.
     *
     * @param name     The name of the region (not unique).
     * @param min      The minimum location of the region.
     * @param max      The maximum location of the region.
     * @param members  The members of the region.
     * @param key      The key of the region.
     * @param parent   The parent (if any) of the region.
     * @param priority The priority of the region.
     * @see #Region(String, BlockVector, BlockVector, World, Map, RegionKey, RegionKey, int)
     * @deprecated use {@link #Region(String, BlockVector, BlockVector, World, Map, RegionKey, RegionKey, int)}
     */
    @Deprecated(since = "0.2.0", forRemoval = true)
    Region(@NotNull String name, @NotNull Location min, @NotNull Location max, Map<String, Map<String, String>> members, @NotNull RegionKey key, @Nullable RegionKey parent, int priority) {
        this.name = name;
        this.min = min.toVector().toBlockVector();
        this.max = max.toVector().toBlockVector();
        this.world = min.getWorld();
        this.members = (members != null) ? members : new HashMap<>();
        this.key = key;
        this.parent = parent;
        this.priority = priority;
    }

    /**
     * Constructs a new region with the given name, minimum and maximum locations, members and parent.
     *
     * @param name     The name of the region (not unique).
     * @param min      The minimum BlockVector of the region.
     * @param max      The maximum BlockVector of the region.
     * @param world    The world of the region.
     * @param members  The members of the region.
     * @param key      The key of the region.
     * @param parent   The parent (if any) of the region.
     * @param priority The priority of the region.
     * @see #Region(String, Location, Location, Map, RegionKey, int)
     */
    Region(@NotNull String name, @NotNull BlockVector min, @NotNull BlockVector max, @NotNull World world, Map<String, Map<String, String>> members, @NotNull RegionKey key, @Nullable RegionKey parent, int priority) {
        this.name = name;
        this.min = min;
        this.max = max;
        this.world = world;
        this.members = (members != null) ? members : new HashMap<>();
        this.key = key;
        this.parent = parent;
        this.priority = priority;
    }

    // Constructor overload for regions without parent

    /**
     * Constructs a new region with the given name, minimum and maximum locations, and members.
     *
     * @param name     The name of the region (not unique).
     * @param min      The minimum location of the region.
     * @param max      The maximum location of the region.
     * @param members  The members of the region.
     * @param key      The key of the region.
     * @param priority The priority of the region.
     * @see #Region(String, Location, Location, Map, RegionKey, int)
     */
    Region(String name, Location min, Location max, Map<String, Map<String, String>> members, RegionKey key, int priority) {
        this(name, min, max, members, key, null, priority);
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name, RegionManager regionManager) {
        this.name = name;
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    public BlockVector getMin() {
        return min;
    }

    void setMin(BlockVector min) {
        this.min = min;
    }

    public String getMinString() {
        return min.getBlockX() + "," + min.getBlockY() + "," + min.getBlockZ();
    }

    public BlockVector getMax() {
        return max;
    }

    void setMax(BlockVector max) {
        this.max = max;
    }

    public String getMaxString() {
        return max.getBlockX() + "," + max.getBlockY() + "," + max.getBlockZ();
    }

    /**
     * Get the members of this region and their permissions. <br>
     * Use {@link PermissionManager} to check player permissions.
     *
     * @return {@code Map<UUID player, Map<String permission, String value> permissions>}
     */
    public Map<String, Map<String, String>> getMembers() {
        return members;
    }

    /**
     * Get the names of all groups in this region
     *
     * @return List of group names
     * @since 0.1.5
     */
    public List<String> getGroupNames() {
        List<String> groupNames = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : members.entrySet()) {
            if (entry.getKey().startsWith("+group-")) {
                groupNames.add(entry.getKey());
            }
        }
        return groupNames;
    }

    /**
     * Get the members of a group in this region
     *
     * @param group Group name
     * @return List of members
     * @since 0.1.6
     */
    public List<String> getGroupMembers(String group) {
        List<String> groupMembers = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : members.entrySet()) {
            if (entry.getValue().containsKey("group") && entry.getValue().get("group").contains(group.substring(7))) {
                groupMembers.add(entry.getKey());
            }
        }
        return groupMembers;
    }

    void setMembers(Map<String, Map<String, String>> members, RegionManager regionManager) {
        this.members = members;
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    void addMember(UUID uuid, Map<String, String> permissions, RegionManager regionManager) {
        this.members.put(uuid.toString(), permissions);
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    void removeMember(UUID uuid, RegionManager regionManager) {
        this.members.remove(uuid.toString());
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    public boolean isMember(UUID uuid) {
        return this.members.containsKey(uuid.toString());
    }

    public boolean isAdmin(UUID uuid) {
        if (this.members.containsKey(uuid.toString()) && this.members.get(uuid.toString()).containsKey("role")) {
            return this.members.get(uuid.toString()).get("role").equals("admin") || this.members.get(uuid.toString())
                    .get("role").equals("owner");
        }
        return false; // Default to false
    }

    public Map<String, String> getMemberPermissions(String who) {
        return this.members.get(who);
    }

    public RegionKey getParent() {
        return this.parent;
    }

    void setParent(RegionKey parent, RegionManager regionManager) {
        this.parent = parent;
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    public Region getParentRegion(RegionManager regionManager) {
        return regionManager.regions().get(parent.getValue());
    }

    @ApiStatus.Experimental
    public Region getParentRegion() {
        return RegionManager.getRegion(parent);
    }

    public List<Region> getChildren(RegionManager regionManager) {
        List<Region> children = new ArrayList<>();
        for (Region region : regionManager.regions().values()) {
            if (region.getParent().equals(key)) {
                children.add(region);
            }
        }
        return children;
    }

    public RegionKey getKey() {
        return key;
    }

    /**
     * Careful, can easily break things.
     */
    void setKey(RegionKey key, RegionManager regionManager) {
        this.key = key;
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    @Deprecated
    public boolean contains(Location location) {
        return location.getWorld().equals(this.world) && contains(location.toVector().toBlockVector());
    }

    public boolean contains(BlockVector vec) {
        return vec.getX() >= this.min.getX() && vec.getX() < this.max.getX() + 1
                && vec.getY() >= this.min.getY() && vec.getY() < this.max.getY() + 1
                && vec.getZ() >= this.min.getZ() && vec.getZ() < this.max.getZ() + 1;
    }

    public boolean intersects(@NotNull BlockVector min, @NotNull BlockVector max) {
        return this.min.getX() <= max.getX() && this.max.getX() >= min.getX()
                && this.min.getY() <= max.getY() && this.max.getY() >= min.getY()
                && this.min.getZ() <= max.getZ() && this.max.getZ() >= min.getZ();
    }

    void addMemberPermission(UUID uuid, String permission, String value, RegionManager regionManager) {
        this.members.computeIfAbsent(uuid.toString(), k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                .put(permission, value);
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    void addMemberPermission(String who, String permission, String value, RegionManager regionManager) {
        this.members.computeIfAbsent(who, k -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                .put(permission, value);
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    public @Nullable UUID getOwner() {
        for (Map.Entry<String, Map<String, String>> entry : members.entrySet()) {
            Map<String, String> map = entry.getValue();
            if (map.containsKey("role") && map.get("role").equalsIgnoreCase("owner")) {
                return UUID.fromString(entry.getKey());
            }
        }
        return null;
    }

    public boolean isOwner(UUID uuid) {
        return getOwner() != null && getOwner().equals(uuid);
    }


    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public World getWorld() {
        return world;
    }

    /**
     * Set the world of the region.
     * Should very likely never be used.
     */
    @ApiStatus.Internal
    public void setWorld(World world) {
        this.world = world;
    }
}
