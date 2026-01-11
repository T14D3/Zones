package de.t14d3.zones;

import de.t14d3.rapunzellib.objects.RBlockPos;
import de.t14d3.rapunzellib.objects.RPlayer;
import de.t14d3.rapunzellib.objects.RWorldRef;
import de.t14d3.zones.permissions.PermissionManager;
import de.t14d3.zones.utils.Types;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;

public interface ZonesPlatform {
    File getDataFolder();

    Types getTypes();

    default PermissionManager createPermissionManager(Zones zones) {
        return new PermissionManager(zones);
    }

    void spawnParticle(ZonesParticleRole role, RBlockPos particleLocation, RPlayer player);

    void showBeacon(RPlayer player, RBlockPos location, RWorldRef world, NamedTextColor color);

    void removeBeacon(RPlayer player, RWorldRef world, RBlockPos location);
}
