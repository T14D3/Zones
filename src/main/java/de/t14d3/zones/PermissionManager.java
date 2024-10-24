package de.t14d3.zones;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PermissionManager {

    private final RegionManager regionManager;

    public PermissionManager(RegionManager regionManager) {
        this.regionManager = regionManager;
    }

    public boolean canInteract(Location location, UUID playerUUID, String action, String type) {
        for (Map.Entry<String, RegionManager.Region> entry : regionManager.loadRegions().entrySet()) {
            RegionManager.Region region = entry.getValue();

            // Debug message: print region boundaries
            Location min = region.getMin();
            Location max = region.getMax();

            // Create a BoundingBox from the region's min and max locations
            BoundingBox box = BoundingBox.of(min, max);

            // Check if the block is inside this region
            if (box.contains(location.toVector())) {
                // Check if the player has permission to interact within this region
                return hasPermission(playerUUID, action, type, region);
            }
        }
        Player player = Bukkit.getPlayer(playerUUID);
        // Default to false if no regions found
        return player.hasPermission("zones.bypass.unclaimed");
    }

    // Check if member has specific permission
    public static boolean hasPermission(UUID uuid, String permission, String type, RegionManager.Region region) {
        Map<String, String> permissions = region.getMembers().get(uuid);
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
}
