package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class PermissionManager {

    private final RegionManager regionManager;

    // Cache of player UUID -> (Region Name -> Action/Type Permission)
    private final Map<UUID, Map<String, Boolean>> permissionCache = new HashMap<>();

    public PermissionManager(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public boolean canInteract(Location location, UUID playerUUID, String action, String type) {
        // Check if this action/type combination is already cached for this player
        Map<String, Boolean> playerCache = permissionCache.getOrDefault(playerUUID, new HashMap<>());
        String cacheKey = getCacheKey(location, action, type);

        if (playerCache.containsKey(cacheKey)) {
            return playerCache.get(cacheKey); // Return cached result
        }

        for (Map.Entry<String, RegionManager.Region> entry : regionManager.loadRegions().entrySet()) {
            RegionManager.Region region = entry.getValue();
            Location min = region.getMin();
            Location max = region.getMax();
            BoundingBox box = BoundingBox.of(min, max);

            if (box.contains(location.toVector())) {
                boolean hasPermission = hasPermission(playerUUID, action, type, region);

                // Cache the result for this region
                playerCache.put(cacheKey, hasPermission);
                permissionCache.put(playerUUID, playerCache);

                return hasPermission;
            }
        }

        Player player = Bukkit.getPlayer(playerUUID);
        // Real-time check for bypass permission without caching
        return player != null && player.hasPermission("zones.bypass.unclaimed");
    }

    // Helper method to generate a unique cache key for the action/type/location combination
    private String getCacheKey(Location location, String action, String type) {
        return location.toString() + "|" + action + "|" + type;
    }

    // Invalidate cache for a specific player
    public void invalidateCache(UUID playerUUID) {
        permissionCache.remove(playerUUID);
    }

    // Invalidate cache for all players in case of region changes
    public void invalidateAllCaches() {
        permissionCache.clear();
    }

    public static boolean hasPermission(UUID uuid, String permission, String type, RegionManager.Region region) {
        // Exit early if player has bypass
        if (Bukkit.getPlayer(uuid).hasPermission("zones.bypass.claimed")) {
            return true;
        }
        Map<String, String> permissions = region.getMembers().get(uuid);
        if (permissions != null) {
            String value = permissions.get(permission);

            if (value != null) {
                if ("true".equalsIgnoreCase(value)) {
                    return true;
                } else if ("false".equalsIgnoreCase(value)) {
                    return false;
                } else {
                    List<String> permittedValues = List.of(value.split(","));
                    for (String permittedValue : permittedValues) {
                        if (permittedValue.startsWith("!")) {
                            if (permittedValue.substring(1).equals(type)) {
                                return false;
                            }
                        } else {
                            if (permittedValue.equals(type)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}

