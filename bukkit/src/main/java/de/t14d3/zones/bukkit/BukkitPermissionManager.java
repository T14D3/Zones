package de.t14d3.zones.bukkit;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.Zones;
import de.t14d3.zones.objects.Flag;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.permissions.flags.FlagContext;
import org.bukkit.Location;

import java.util.UUID;

public class BukkitPermissionManager extends PermissionManager {
    public BukkitPermissionManager(Zones zones) {
        super(zones);
    }

    public boolean checkAction(Location loc, String who, Flag action, String type) {
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return super.checkAction(pos, world, who, action, type);
    }


    public boolean checkAction(Location loc, Flag action, String type) {
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return super.checkAction(pos, world, action, type);
    }

    public boolean checkAction(Location loc, Flag action, String type, FlagContext context) {
        RWorldRef world = new RWorldRef(loc.getWorld().getName(), loc.getWorld().getKey().toString());
        RBlockPos pos = new RBlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        return super.checkAction(pos, world, action, type, context);
    }

    public boolean checkAction(Location location, UUID playerUUID, Flag action, String name) {
        return checkAction(location, playerUUID.toString(), action, name);
    }
}
