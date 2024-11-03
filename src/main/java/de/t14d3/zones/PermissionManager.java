package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PermissionManager {

    private RegionManager regionManager;

    // Cache of player UUID -> (Region Name -> Action/Type Permission)
    private final Map<UUID, Map<String, Boolean>> permissionCache = new HashMap<>();

    public PermissionManager() {}

    // Setter for RegionManager to avoid circular dependency
    public void setRegionManager(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    /**
     * Checks if a player can interact with a region.
     * @param location The location of the interaction.
     * @param playerUUID The UUID of the player.
     * @param action The action the player wants to perform.
     * @param type The type of the block or entity the interaction happened with.
     * @return True if the player can interact with the region, false otherwise.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canInteract(Location location, UUID playerUUID, String action, String type) {
        Map<String, Boolean> playerCache = permissionCache.getOrDefault(playerUUID, new HashMap<>());
        String cacheKey = getCacheKey(location, action, type);

        if (playerCache.containsKey(cacheKey)) {
            return playerCache.get(cacheKey); // Return cached result
        }

        for (Map.Entry<String, RegionManager.Region> entry : regionManager.loadRegions().entrySet()) {
            RegionManager.Region region = entry.getValue();
            BoundingBox box = BoundingBox.of(region.getMin(), region.getMax());

            if (box.contains(location.toVector())) {
                boolean hasPermission = hasPermission(playerUUID, action, type, region);

                // Cache the result for this region
                playerCache.put(cacheKey, hasPermission);
                permissionCache.put(playerUUID, playerCache);

                return hasPermission;
            }
        }

        Player player = Bukkit.getPlayer(playerUUID);
        return player != null && player.hasPermission("zones.bypass.unclaimed");
    }

    private String getCacheKey(Location location, String action, String type) {
        return location.toString() + "|" + action + "|" + type;
    }

    public void invalidateCache(UUID playerUUID) {
        permissionCache.remove(playerUUID);
    }
    public void invalidateCacheForLocation(Location location) {
        List<RegionManager.Region> regions = regionManager.getRegionsAt(location);

        // Invalidate caches for all players that have permissions in the regions covering the location
        for (RegionManager.Region region : regions) {
            for (UUID playerUUID : permissionCache.keySet()) {
                // Remove the player's cached permissions related to this region
                permissionCache.get(playerUUID).keySet().removeIf(key -> key.startsWith(getCacheKey(location, "", "")));
            }
        }
    }

    public void invalidateAllCaches() {
        permissionCache.clear();
    }

    /**
     * Checks if a player has a specific permission for a given type in the provided region.
     *
     * @param uuid    The UUID of the player whose permission is being checked.
     * @param permission The permission being checked (e.g., "break", "place").
     * @param type    The type of object the permission applies to (e.g., "GRASS_BLOCK").
     * @param region  The region in which the permission is being checked.
     * @return True if the player has the specified permission for the type, false otherwise.
     */
    public static boolean hasPermission(UUID uuid, String permission, String type, RegionManager.Region region) {
        permission = permission.toLowerCase();
        Player player = Bukkit.getPlayer(uuid);

        // Check if player has a global bypass permission
        if (player != null && player.hasPermission("zones.bypass.claimed")) {
            return true;
        }

        // Retrieve the permissions for the player in the specified region
        Map<UUID, Map<String, String>> members = region.getMembers();

        // Check if the player is a member of the region
        if (!members.containsKey(uuid)) {
            return false; // Player is not a member, deny access
        }

        // Get the permissions for the player
        Map<String, String> permissions = members.get(uuid);
        String value = permissions.get(permission);
        String ownerValue = permissions.get("owner");
        if ("true".equalsIgnoreCase(ownerValue) && permissions.get(permission) == null) {
            return true;
        }
        // If no permission value is found, deny access
        if (value == null) {
            return false;
        }

        // Analyze permission values
        boolean explicitAllow = false;
        boolean explicitDeny = false;

        for (String permittedValue : value.split(",")) {
            permittedValue = permittedValue.trim(); // Trim whitespace

            // Check for wildcard allow
            if ("*".equals(permittedValue) || "true".equals(permittedValue)) {
                explicitAllow = true;
            }
            // Check for wildcard deny
            else if ("! *".equals(permittedValue) || "false".equals(permittedValue)) {
                explicitDeny = true;
            }
            // Check for specific type allow
            else if (permittedValue.equalsIgnoreCase(type)) {
                explicitAllow = true;
            }
            // Check for specific type deny
            else if (permittedValue.equalsIgnoreCase("!" + type)) {
                explicitDeny = true;
            }
        }

        // Determine final access based on explicit allow/deny flags
        if (explicitDeny) {
            return false;
        } else if (explicitAllow) {
            return true;
        }
        // Deny by default
        return false;
    }

    public static boolean isAdmin(UUID uuid, RegionManager.Region region) {
        return hasPermission(uuid, "role", "owner", region) || hasPermission(uuid, "role", "admin", region);
    }
}
