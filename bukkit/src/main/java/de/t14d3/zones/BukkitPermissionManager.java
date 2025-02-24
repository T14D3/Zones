package de.t14d3.zones;

import de.t14d3.zones.objects.BlockLocation;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.objects.World;
import de.t14d3.zones.permissions.PermissionManager;
import org.bukkit.Location;

import java.util.UUID;

public class BukkitPermissionManager extends PermissionManager {

    public BukkitPermissionManager(Zones zones) {
        super(zones);
    }

    public boolean checkAction(Location loc, String who, Flag action, String type, Object... extra) {
        return super.checkAction(BlockLocation.of(loc), World.of(loc.getWorld()), who, action, type, extra);
    }

    public boolean checkAction(Location loc, Flag action, String type, Object... extra) {
        return super.checkAction(BlockLocation.of(loc), World.of(loc.getWorld()), action, type, extra);
    }

    public boolean checkAction(Location location, UUID playerUUID, Flag action, String name) {
        return checkAction(location, playerUUID.toString(), action, name);
    }
}
