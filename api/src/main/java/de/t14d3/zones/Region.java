package de.t14d3.zones;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.objects.Box;
import de.t14d3.zones.objects.shapes.CuboidShape;
import de.t14d3.zones.objects.shapes.GlobalShape;
import de.t14d3.zones.objects.shapes.ShapeProvider;
import de.t14d3.zones.permissions.RegionMembership;
import de.t14d3.zones.permissions.RegionPermissions;
import de.t14d3.zones.permissions.subjects.GroupSubject;
import de.t14d3.zones.permissions.subjects.PlayerSubject;
import de.t14d3.zones.permissions.subjects.SubjectRef;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Represents a region in the plugin.
 * Constructive or destructive methods are implemented in the
 * {@link de.t14d3.zones.RegionManager}.
 */
public class Region {
    private String name;
    private ShapeProvider shape;
    private final RegionPermissions permissions;
    private final RegionMembership membership;
    private RegionKey key;
    private RegionKey parent;
    private int priority;

    public Region(@NotNull String name, @NotNull ShapeProvider shape,
                  @NotNull RegionPermissions permissions, @NotNull RegionMembership membership,
                  @NotNull RegionKey key, @Nullable RegionKey parent,
                  int priority) {
        // noinspection ConstantConditions
        this.name = name == null ? key.toString() : name;
        this.shape = shape;
        this.permissions = permissions != null ? permissions : new RegionPermissions();
        this.membership = membership != null ? membership : new RegionMembership();
        this.key = key;
        this.parent = parent;
        this.priority = priority;
    }

    // Constructor overload for regions without parent
    public Region(String name, ShapeProvider shape, RegionPermissions permissions, RegionMembership membership,
                  RegionKey key, int priority) {
        this(name, shape, permissions, membership, key, null, priority);
    }

    public Region(@NotNull String name, @NotNull RBlockPos min, @NotNull RBlockPos max, @NotNull RWorldRef world,
                  @NotNull RegionPermissions permissions, @NotNull RegionMembership membership,
                  @NotNull RegionKey key, @Nullable RegionKey parent,
                  int priority) {
        this(name, new CuboidShape(min, max, world), permissions, membership, key, parent, priority);
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name, RegionManager regionManager) {
        this.name = name;
        regionManager.saveRegion(key, this); // Ensure changes are saved
    }

    public RBlockPos getMin() {
        if (shape instanceof CuboidShape cuboid) {
            return cuboid.getMin();
        }
        return null; // For non-cuboid shapes
    }

    void setMin(RBlockPos min) {
        if (shape instanceof CuboidShape cuboid) {
            // Note: This creates a new shape, may break references
            this.shape = new CuboidShape(min, cuboid.getMax(), cuboid.getWorld());
        }
    }

    public String getMinString() {
        RBlockPos min = getMin();
        return min != null ? min.x() + "," + min.y() + "," + min.z() : null;
    }

    public RBlockPos getMax() {
        if (shape instanceof CuboidShape cuboid) {
            return cuboid.getMax();
        }
        return null; // For non-cuboid shapes
    }

    void setMax(RBlockPos max) {
        if (shape instanceof CuboidShape cuboid) {
            this.shape = new CuboidShape(cuboid.getMin(), max, cuboid.getWorld());
        }
    }

    public String getMaxString() {
        RBlockPos max = getMax();
        return max != null ? max.x() + "," + max.y() + "," + max.z() : null;
    }

    /**
     * Returns the permission set backing this region.
     */
    public @NotNull RegionPermissions getPermissions() {
        return permissions;
    }

    public @NotNull RegionMembership getMembership() {
        return membership;
    }

    /**
     * @deprecated Use {@link #getPermissions()}.
     */
    @Deprecated(since = "0.6.0", forRemoval = false)
    public RegionPermissions getPermissionsV2() {
        return getPermissions();
    }

    /**
     * Get the names of all groups in this region
     *
     * @return List of group names
     * @since 0.1.5
     */
    public List<String> getGroupNames() {
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (SubjectRef subject : permissions.subjects().keySet()) {
            if (subject instanceof GroupSubject g) names.add(g.name());
        }
        names.addAll(membership.groups().keySet());
        return new ArrayList<>(names);
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
        String groupName = RegionMembership.normalizeName(group);
        if (groupName == null) return groupMembers;

        for (Map.Entry<UUID, RegionMembership.PlayerEntry> e : membership.players().entrySet()) {
            UUID uuid = e.getKey();
            RegionMembership.PlayerEntry entry = e.getValue();
            if (uuid == null || entry == null) continue;
            if (entry.groups().allowValues().contains(groupName)) groupMembers.add(uuid.toString());
        }

        return groupMembers;
    }

    public boolean isMember(UUID uuid) {
        return isMember(uuid, null);
    }

    public boolean isAdmin(UUID uuid) {
        return isAdmin(uuid, null);
    }

    public boolean isMember(UUID uuid, @Nullable RegionManager regionManager) {
        if (uuid == null) return false;
        if (membership.players().containsKey(uuid)) return true;
        if (permissions.subjects().containsKey(new PlayerSubject(uuid))) return true;

        if (regionManager == null) return false;
        Region current = this;
        Set<Integer> visited = null;
        while (current != null) {
            if (current.membership.players().containsKey(uuid)) return true;
            if (current.permissions.subjects().containsKey(new PlayerSubject(uuid))) return true;

            if (current.parent == null) break;
            if (visited == null) visited = new HashSet<>();
            if (!visited.add(current.key.getValue())) break;
            current = current.getParentRegion(regionManager);
        }
        return false;
    }

    public boolean isAdmin(UUID uuid, @Nullable RegionManager regionManager) {
        if (uuid == null) return false;
        if (regionManager == null) {
            RegionMembership.PlayerEntry entry = membership.players().get(uuid);
            if (entry == null) return false;
            return entry.roles().allowValues().contains("owner") || entry.roles().allowValues().contains("admin");
        }

        ArrayDeque<Region> lineage = new ArrayDeque<>();
        Region current = this;
        Set<Integer> visited = null;
        while (current != null) {
            lineage.addFirst(current);
            if (current.parent == null) break;
            if (visited == null) visited = new HashSet<>();
            if (!visited.add(current.key.getValue())) break;
            current = current.getParentRegion(regionManager);
        }

        Set<String> roles = new HashSet<>();
        for (Region r : lineage) {
            RegionMembership.PlayerEntry entry = r.membership.players().get(uuid);
            if (entry == null) continue;
            for (String deny : entry.roles().denyValues()) roles.remove(deny);
            for (String allow : entry.roles().allowValues()) roles.add(allow);
        }
        return roles.contains("owner") || roles.contains("admin");
    }

    public RegionKey getParent() {
        return this.parent;
    }

    void setParent(RegionKey parent, RegionManager regionManager) {
        this.parent = parent;
        regionManager.saveRegion(key, this); // Ensure changes are saved        
    }

    public @Nullable Region getParentRegion(RegionManager regionManager) {
        if (regionManager == null) return null;
        if (parent == null) return null;

        Region parentRegion = regionManager.getRegion(parent);
        if (parentRegion == null) return null;
        if (!sameWorld(parentRegion.getWorld(), getWorld())) return null;
        return parentRegion;
    }

    public List<Region> getChildren(RegionManager regionManager) {
        List<Region> children = new ArrayList<>();
        for (Region region : regionManager.regions().values()) {
            RegionKey p = region.getParent();
            if (p != null && p.equals(key) && sameWorld(region.getWorld(), getWorld())) {
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


    public boolean contains(RBlockPos vec) {
        return shape.contains(vec);
    }

    public boolean intersects(@NotNull RBlockPos min, @NotNull RBlockPos max, RWorldRef world) {
        if (sameWorld(shape.getWorld(), world) && shape instanceof CuboidShape cuboid) {
            return getBoundingBox() != null && getBoundingBox().intersects(min, max, world);
        }
        // For global or other shapes, check if any point is inside
        return shape.intersects(new CuboidShape(min, max, world));
    }

    private static boolean sameWorld(@Nullable RWorldRef a, @Nullable RWorldRef b) {
        if (a == null || b == null) return false;
        return Objects.equals(a.identifier(), b.identifier());
    }

    public Box getBoundingBox() {
        return shape.getBoundingBox();
    }

    @Deprecated
    public Box getBounds() {
        return getBoundingBox();
    }

    public @Nullable UUID getOwner() {
        return getOwner(null);
    }

    public @Nullable UUID getOwner(@Nullable RegionManager regionManager) {
        if (regionManager == null) {
            for (Map.Entry<UUID, RegionMembership.PlayerEntry> e : membership.players().entrySet()) {
                UUID uuid = e.getKey();
                RegionMembership.PlayerEntry entry = e.getValue();
                if (uuid == null || entry == null) continue;
                if (entry.roles().allowValues().contains("owner")) return uuid;
            }
            return null;
        }

        ArrayDeque<Region> lineage = new ArrayDeque<>();
        Region current = this;
        Set<Integer> visited = null;
        while (current != null) {
            lineage.addFirst(current);
            if (current.parent == null) break;
            if (visited == null) visited = new HashSet<>();
            if (!visited.add(current.key.getValue())) break;
            current = current.getParentRegion(regionManager);
        }

        LinkedHashSet<UUID> candidates = new LinkedHashSet<>();
        for (Region r : lineage) candidates.addAll(r.membership.players().keySet());

        for (UUID uuid : candidates) {
            if (uuid == null) continue;
            Set<String> roles = new HashSet<>();
            for (Region r : lineage) {
                RegionMembership.PlayerEntry entry = r.membership.players().get(uuid);
                if (entry == null) continue;
                for (String deny : entry.roles().denyValues()) roles.remove(deny);
                for (String allow : entry.roles().allowValues()) roles.add(allow);
            }
            if (roles.contains("owner")) return uuid;
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

    public RWorldRef getWorld() {
        return shape.getWorld();
    }

    /**
     * Set the world of the region.
     * Should very likely never be used.
     * This creates a new shape with the new world.
     */
    @ApiStatus.Internal
    public void setWorld(RWorldRef world) {
        if (shape instanceof CuboidShape cuboid) {
            this.shape = new CuboidShape(cuboid.getMin(), cuboid.getMax(), world);
        } else if (shape instanceof GlobalShape) {
            this.shape = new GlobalShape(world);
        }
        // For future shapes, add handling
    }

    public ShapeProvider getShape() {
        return shape;
    }
}
