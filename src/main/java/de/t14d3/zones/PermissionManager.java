package de.t14d3.zones;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;

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
                if (region.hasPermission(playerUUID, action, type)) {
                    return true;  // Allowed to interact
                } else {
                    return false; // Not allowed to interact
                }
            }
        }
        // Default to false if no regions found
        return false;
    }
}
