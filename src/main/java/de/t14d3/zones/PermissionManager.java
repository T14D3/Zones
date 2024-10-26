package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.HashMap;
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

    public void invalidateAllCaches() {
        permissionCache.clear();
    }

    public static boolean hasPermission(UUID uuid, String permission, String type, RegionManager.Region region) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.hasPermission("zones.bypass.claimed")) return true;

        Map<String, String> permissions = region.getMembers().get(uuid);
        String value = permissions.get(permission);

        if (value != null) {
            boolean explicitAllow = false;
            boolean explicitDeny = false;

            for (String permittedValue : value.split(",")) {
                permittedValue = permittedValue.trim();
                if (permittedValue.startsWith("!") && permittedValue.substring(1).equalsIgnoreCase(type)) {
                    explicitDeny = true;
                } else if ("true".equalsIgnoreCase(permittedValue)) {
                    explicitAllow = true;
                } else if ("false".equalsIgnoreCase(permittedValue)) {
                    explicitDeny = true;
                } else if (permittedValue.equalsIgnoreCase(type)) {
                    explicitAllow = true;
                }
            }

            return explicitDeny ? false : explicitAllow;
        }

        return false;
    }
}
